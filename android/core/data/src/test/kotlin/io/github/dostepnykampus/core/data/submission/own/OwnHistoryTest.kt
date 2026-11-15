/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submission.own

import io.github.dostepnykampus.core.data.server.ServerEdit
import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.data.submission.sync.EditedSubmission
import io.github.dostepnykampus.core.data.submission.sync.FakeHistory
import io.github.dostepnykampus.core.data.submission.sync.FakeStore
import io.github.dostepnykampus.core.data.submission.sync.SavedSubmission
import io.github.dostepnykampus.core.data.submission.sync.SubmissionFixtures.photo
import io.github.dostepnykampus.core.data.submission.sync.SubmissionFixtures.precise
import io.github.dostepnykampus.core.data.submission.sync.SubmissionFixtures.server
import io.github.dostepnykampus.core.data.submission.sync.SubmissionQueue
import io.github.dostepnykampus.core.data.submission.sync.SubmissionServer
import io.github.dostepnykampus.core.data.submission.sync.SubmissionStamps
import io.github.dostepnykampus.core.database.submission.OwnSubmissionEntity
import io.github.dostepnykampus.core.database.submission.OwnSubmissionState
import io.github.dostepnykampus.core.model.Submission
import io.github.dostepnykampus.core.model.SubmissionStatus
import io.github.dostepnykampus.core.model.submission.SubmissionContent
import io.github.dostepnykampus.core.model.submission.SubmissionKind
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.io.File
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Kolejka zapisuje stan wysyłki własnych zgłoszeń dla listy C3.
 *
 * Wymagania: US8, NFR-5, US4.4, BR-9, NFR-50, US8.3, BR-23
 */
class OwnHistoryTest {
    @TempDir
    lateinit var tempDir: File

    private val store = FakeStore()
    private val history = FakeHistory()
    private var photoAttempts = 0

    @Test
    fun pointsResultToRow() =
        runBlocking {
            val saved: SavedSubmission = queue().save(precise(PHOTO), photo(tempDir, PHOTO))
            val pending = store.all().single()
            assertEquals(ID, saved.id)
            assertEquals(saved.id, pending.submission.id)
            assertEquals(saved.id, history.rows.getValue(saved.id).id)
        }

    @Test
    fun savesAsOnPhone() =
        runBlocking {
            queue().save(precise(PHOTO), photo(tempDir, PHOTO))

            val row = history.rows.getValue(ID)
            assertEquals(OwnSubmissionState.LOCAL, row.state)
            assertEquals("TB-5", row.barrierType)
        }

    @Test
    fun keepsQuickOffList() =
        runBlocking {
            queue().save(precise(PHOTO).copy(kind = SubmissionKind.QUICK), photo(tempDir, PHOTO))

            assertTrue(history.rows.isEmpty())
        }

    /**
     * Scenariusz (częściowo, przykład: potwierdzi odbiór zgłoszenia): zgloszenia-zbierajacego.feature:
     *   Status „wysłane” dopiero po potwierdzeniu odbioru przez serwer.
     */
    @Test
    fun keepsVersionWhenSent() =
        runBlocking {
            val target = queue()
            target.save(precise(PHOTO), photo(tempDir, PHOTO))
            target.sendAll()

            val row = history.rows.getValue(ID)
            assertEquals(OwnSubmissionState.SENT, row.state)
            assertEquals(3, row.serverVersion)
        }

    /**
     * Scenariusz (częściowo, przykład: nie potwierdzi odbioru zgłoszenia): zgloszenia-zbierajacego.feature:
     *   Status „wysłane” dopiero po potwierdzeniu odbioru przez serwer.
     */
    @Test
    fun staysLocalOffline() =
        runBlocking {
            val target = queue()
            target.save(precise(PHOTO), photo(tempDir, PHOTO))
            store.createResults.addLast(ServerReply.Offline)
            target.sendAll()

            assertEquals(OwnSubmissionState.LOCAL, history.rows.getValue(ID).state)
        }

    /**
     * Serwer zapisał treść, ale telefon nie dostał odpowiedzi: ponawia to samo id i treść, potem wysyła zdjęcie.
     * Scenariusz (częściowo): synchronizacja.feature: Przerwana synchronizacja
     *   nie gubi zgłoszenia i nie tworzy drugiego
     */
    @Test
    fun handlesLostReply() =
        runBlocking {
            val sent = mutableListOf<Pair<String, SubmissionContent>>()
            val remote = mutableMapOf<String, Submission>()
            val target =
                queue { id, content ->
                    sent.add(id to content)
                    val saved = remote.getOrPut(id) { server(id, content, version = 3) }
                    if (sent.size == 1) {
                        ServerReply.Offline
                    } else {
                        ServerReply.Ok(saved)
                    }
                }
            val photos = photo(tempDir, PHOTO)
            target.save(precise(PHOTO), photos)

            assertFalse(target.sendAll())
            assertEquals(setOf(ID), remote.keys)
            val pending = store.rows.single().submission
            assertEquals(ID, pending.id)
            assertFalse(pending.isCreatedOnServer)
            assertTrue(store.photos.isEmpty())
            assertTrue(File(photos.single().path).exists())
            assertEquals(OwnSubmissionState.LOCAL, history.rows.getValue(ID).state)

            assertTrue(target.sendAll())
            assertEquals(listOf(ID, ID), sent.map { it.first })
            assertEquals(sent.first(), sent.last())
            assertEquals(1, remote.size)
            assertEquals(listOf(ID to PHOTO), store.photos)
            assertTrue(store.rows.isEmpty())
            assertEquals(OwnSubmissionState.SENT, history.rows.getValue(ID).state)
            assertEquals(3, history.rows.getValue(ID).serverVersion)
        }

    /**
     * Odpowiedź 503 kończy przebieg do kolejnej próby jak brak sieci; następny przebieg wysyła zgłoszenie sam.
     * Scenariusz: synchronizacja.feature: Awaria serwera działa jak brak sieci
     */
    @Test
    fun staysLocalOnServerFault() =
        runBlocking {
            val target = queue()
            target.save(precise(PHOTO), photo(tempDir, PHOTO))
            store.createResults.addLast(ServerReply.Fault(code = SERVICE_UNAVAILABLE))

            assertFalse(target.sendAll())
            val row = history.rows.getValue(ID)
            assertEquals(OwnSubmissionState.LOCAL, row.state)
            assertNull(row.refusalCode)
            assertEquals(listOf(ID), store.all().map { it.submission.id })

            assertTrue(target.sendAll())
            assertEquals(OwnSubmissionState.SENT, history.rows.getValue(ID).state)
        }

    @Test
    fun keepsRefusalCode() =
        runBlocking {
            val target = queue()
            target.save(precise(PHOTO), photo(tempDir, PHOTO))
            store.createResults.addLast(ServerReply.Fault(code = 422))
            target.sendAll()

            val row = history.rows.getValue(ID)
            assertEquals(OwnSubmissionState.REFUSED, row.state)
            assertEquals(422, row.refusalCode)
        }

    /**
     * Odmowa treści albo zdjęcia zostaje w historii z kodem i nie wraca do wysyłki przy kolejnych przebiegach.
     * Scenariusz (częściowo): synchronizacja.feature: Odrzucona treść albo zdjęcie
     *   zostawia zgłoszenie ze statusem „Nie wysłano”
     */
    @ParameterizedTest
    @CsvSource("400, false", "404, true", "413, true")
    fun `odmowa nie wraca do wysyłki`(code: Int, isPhoto: Boolean): Unit =
        runBlocking {
            val target = queue()
            val photos = photo(tempDir, PHOTO)
            target.save(precise(PHOTO), photos)
            val refusal = if (code == 404) ServerReply.Missing else ServerReply.Fault(code)
            if (isPhoto) store.photoResults.addLast(refusal) else store.createResults.addLast(refusal)

            assertTrue(target.sendAll())
            val pending = store.rows.single().submission
            assertTrue(pending.isRefused)
            assertEquals(isPhoto, pending.isCreatedOnServer)
            assertTrue(File(photos.single().path).exists())
            val row = history.rows.getValue(ID)
            assertEquals(OwnSubmissionState.REFUSED, row.state)
            assertEquals(code, row.refusalCode)

            assertTrue(target.sendAll())
            assertTrue(target.sendAll())
            assertEquals(if (isPhoto) 1 else 0, photoAttempts)
            assertEquals(if (isPhoto) listOf(ID) else emptyList<String>(), store.created)
            assertEquals(pending, store.rows.single().submission)
            assertEquals(row, history.rows.getValue(ID))
        }

    /** Scenariusz: zgloszenia-zbierajacego.feature: Edycja zgłoszenia przed decyzją Weryfikatora. */
    @ParameterizedTest
    @CsvSource("SENT", "IN_REVIEW")
    fun `us8_3 edycja zachowuje status`(status: SubmissionStatus) {
        runBlocking {
            val target = queue()
            target.save(precise(PHOTO, severity = 1), photo(tempDir, PHOTO))
            target.sendAll()
            history.saveServerStatus(id = ID, status = status.name, reason = null, version = 3)

            target.saveEdit(
                EditedSubmission(id = ID, baseVersion = 3),
                precise(PHOTO, severity = 2),
                photo(tempDir, PHOTO),
            )

            val row = history.rows.getValue(ID)
            assertEquals(2, row.severity)
            assertEquals(OwnSubmissionState.SENT, row.state)
            assertEquals(status.name, row.serverStatus)
            assertEquals(3, row.serverVersion)
        }
    }

    @Test
    fun editsUnsent() =
        runBlocking {
            val target = queue()
            target.save(precise(PHOTO), photo(tempDir, PHOTO))
            target.saveEdit(
                EditedSubmission(id = ID, baseVersion = null),
                precise(PHOTO, severity = 1),
                photo(tempDir, PHOTO),
            )

            val row = history.rows.getValue(ID)
            assertEquals(OwnSubmissionState.LOCAL, row.state)
            assertEquals(1, row.severity)
        }

    /**
     * Zmiana autora odrzucona jako „nieaktualne” zostaje w historii, a nowe zgłoszenie tej samej bariery jest osobne.
     * Scenariusz: weryfikacja.feature: Po przyjęciu „bariera usunięta” barierę, która nadal istnieje,
     *   zgłasza się od nowa
     */
    @Test
    fun createsNewAfterOutdated() =
        runBlocking {
            history.rows[OLD] =
                OwnSubmissionEntity(id = OLD, savedAtMillis = 1, kind = "PRECISE", barrierType = "TB-9")
            history.saveServerStatus(
                id = OLD,
                status = SubmissionStatus.REJECTED.name,
                reason = "nieaktualne",
                version = 2,
            )

            queue().save(precise(PHOTO).copy(barrierType = "TB-9", buildingSymbol = "D-2"), photo(tempDir, PHOTO))

            val row = history.rows.getValue(ID)
            assertEquals(OwnSubmissionState.LOCAL, row.state)
            assertEquals("TB-9", row.barrierType)
            val pending = store.rows.single().submission
            assertNull(pending.replacesSubmissionId)
            assertNull(pending.baseVersion)
            assertEquals("nieaktualne", history.rows.getValue(OLD).rejectionReason)
        }

    private fun queue(
        create: suspend (String, SubmissionContent) -> ServerReply<Submission> = { id, content ->
            store.createResults.removeFirstOrNull() ?: accepted(id, content)
        },
    ): SubmissionQueue =
        SubmissionQueue(
            store = store,
            ioContext = EmptyCoroutineContext,
            server =
                SubmissionServer(
                    create = create,
                    putPhoto = { _, _, _ ->
                        photoAttempts++
                        store.photoResults.removeFirstOrNull() ?: ServerReply.Ok(Unit)
                    },
                    update = { id, _, content -> ServerReply.Ok(ServerEdit.Saved(server(id, content, version = 3))) },
                ),
            stamps = SubmissionStamps(newId = { ID }),
            history = history,
        )

    private fun accepted(id: String, content: SubmissionContent): ServerReply<Submission> =
        ServerReply.Ok(server(id, content, version = 3))
}

private const val ID = "s1"
private const val PHOTO = "p1"
private const val OLD = "s-old"
private const val SERVICE_UNAVAILABLE = 503
