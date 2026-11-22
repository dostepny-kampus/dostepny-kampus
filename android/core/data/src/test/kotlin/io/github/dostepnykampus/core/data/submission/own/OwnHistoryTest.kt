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
import io.github.dostepnykampus.core.data.submission.sync.SubmissionStamps
import io.github.dostepnykampus.core.data.submission.sync.submissionServer
import io.github.dostepnykampus.core.database.submission.OwnSubmissionEntity
import io.github.dostepnykampus.core.database.submission.OwnSubmissionState
import io.github.dostepnykampus.core.model.submission.Submission
import io.github.dostepnykampus.core.model.submission.SubmissionContent
import io.github.dostepnykampus.core.model.submission.SubmissionKind
import io.github.dostepnykampus.core.model.submission.SubmissionStatus
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
     * Scenariusz: synchronizacja.feature: Przerwana synchronizacja
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

    /**
     * Zgłoszenie zapisane bez sieci zostaje w kolejce i na liście jako „lokalne”, z typem i uciążliwością.
     * Scenariusz: synchronizacja.feature: Bez sieci widać wskaźnik, a zgłoszenie czeka ze statusem „lokalne”
     */
    @Test
    fun savesOfflineAsLocal() =
        runBlocking {
            val target = queue()
            target.save(precise(PHOTO, severity = 3).copy(barrierType = "TB-8"), photo(tempDir, PHOTO))
            store.createResults.addLast(ServerReply.Offline)

            assertFalse(target.sendAll(), "bez sieci przebieg kończy się ponowieniem")
            val row = history.rows.getValue(ID)
            assertEquals(OwnSubmissionState.LOCAL, row.state, "zgłoszenie czeka jako lokalne")
            assertEquals("TB-8", row.barrierType, "typ zgłoszenia")
            assertEquals(3, row.severity, "uciążliwość zgłoszenia")
            assertEquals(listOf(ID), store.all().map { it.submission.id }, "zgłoszenie czeka w kolejce")
        }

    /**
     * Zgłoszenia zapisane bez sieci wysyła kolejny przebieg planowany przy zapisie, bez działania Zbierającego.
     * Scenariusz: synchronizacja.feature: Po powrocie sieci synchronizacja rusza sama
     */
    @Test
    fun sendsBothWhenNetworkReturns() =
        runBlocking {
            val ids = ArrayDeque(listOf(ID, SECOND))
            var planned = 0
            val target = queue(newId = { ids.removeFirst() }, onSaved = { planned++ })
            target.save(precise(PHOTO), photo(tempDir, PHOTO))
            target.save(precise(SECOND_PHOTO), photo(tempDir, SECOND_PHOTO))
            store.createResults.addLast(ServerReply.Offline)

            assertFalse(target.sendAll(), "bez sieci przebieg kończy się ponowieniem")
            assertEquals(listOf(ID, SECOND), history.rows.keys.sorted(), "oba zgłoszenia są lokalne")
            assertEquals(2, planned, "każdy zapis planuje wysyłkę")

            assertTrue(target.sendAll(), "po powrocie sieci przebieg wysyła wszystko")
            val states = history.rows.values.map { it.state }
            assertEquals(listOf(OwnSubmissionState.SENT, OwnSubmissionState.SENT), states, "oba zgłoszenia wysłane")
            assertTrue(store.rows.isEmpty(), "kolejka jest pusta")
        }

    /**
     * Odpowiedź 401 zostawia zgłoszenie jako „lokalne” i czeka na zalogowanie, które samo planuje wysyłkę.
     * Scenariusz: synchronizacja.feature: Wygasłe zalogowanie czeka na ponowne zalogowanie
     */
    @Test
    fun sendsAfterRelogin() =
        runBlocking {
            var planned = 0
            val target = queue(onSaved = { planned++ })
            target.save(precise(PHOTO), photo(tempDir, PHOTO))
            store.createResults.addLast(ServerReply.Fault(code = 401))

            target.sendAll()
            assertEquals(OwnSubmissionState.LOCAL, history.rows.getValue(ID).state, "zgłoszenie zostaje lokalne")
            assertTrue(target.loginExpired.value, "aplikacja prosi o ponowne zalogowanie")
            val before = planned

            target.onLoggedIn()
            assertFalse(target.loginExpired.value, "prośba znika po zalogowaniu")
            assertEquals(before + 1, planned, "zalogowanie planuje wysyłkę")
            target.sendAll()
            assertEquals(OwnSubmissionState.SENT, history.rows.getValue(ID).state, "zgłoszenie zostaje wysłane")
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
     * Scenariusz: synchronizacja.feature: Odrzucona treść albo zdjęcie
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
            assertEquals(
                RefusalReason.NOT_ACCEPTED,
                refusalReasonOf(row.state, row.refusalCode),
                "powód na liście to „Serwer nie przyjął zgłoszenia”",
            )

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

    /**
     * Scenariusz (przykład: lokalne): zgloszenia-zbierajacego.feature: Edycja zgłoszenia przed decyzją Weryfikatora.
     */
    @Test
    fun editsUnsent() =
        runBlocking {
            val target = queue()
            target.save(precise(PHOTO, severity = 1), photo(tempDir, PHOTO))
            target.saveEdit(
                EditedSubmission(id = ID, baseVersion = null),
                precise(PHOTO, severity = 2),
                photo(tempDir, PHOTO),
            )

            val row = history.rows.getValue(ID)
            assertEquals(OwnSubmissionState.LOCAL, row.state, "zgłoszenie zostaje lokalne")
            assertEquals(2, row.severity, "nowa uciążliwość")
        }

    /**
     * Scenariusz (częściowo): zgloszenia-zbierajacego.feature: Zmiana zgłoszenia przyjętego tworzy nowe zgłoszenie.
     */
    @Test
    fun replacesAcceptedAsLocal() =
        runBlocking {
            history.rows[OLD] =
                OwnSubmissionEntity(id = OLD, savedAtMillis = 1, kind = "PRECISE", barrierType = "TB-13")
            history.saveServerStatus(id = OLD, status = SubmissionStatus.ACCEPTED.name, reason = null, version = 2)

            queue().save(precise(PHOTO, severity = 3).copy(replacesSubmissionId = OLD), photo(tempDir, PHOTO))

            val pending = store.all().single().submission
            assertEquals(OLD, pending.replacesSubmissionId, "nowe wskazuje przyjęte")
            assertEquals(OwnSubmissionState.LOCAL, history.rows.getValue(ID).state, "nowe zgłoszenie jest lokalne")
            val original = history.rows.getValue(OLD)
            assertEquals("ACCEPTED", original.serverStatus, "przyjęte zostaje przyjęte")
            assertEquals(2, original.serverVersion, "przyjęte nie zmienia wersji")
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
        newId: () -> String = { ID },
        onSaved: () -> Unit = {},
        create: suspend (String, SubmissionContent) -> ServerReply<Submission> = { id, content ->
            store.createResults.removeFirstOrNull() ?: accepted(id, content)
        },
    ): SubmissionQueue =
        SubmissionQueue(
            store = store,
            ioContext = EmptyCoroutineContext,
            server =
                submissionServer(
                    create = create,
                    putPhoto = { _, _, _ ->
                        photoAttempts++
                        store.photoResults.removeFirstOrNull() ?: ServerReply.Ok(Unit)
                    },
                    update = { id, _, content -> ServerReply.Ok(ServerEdit.Saved(server(id, content, version = 3))) },
                ),
            stamps = SubmissionStamps(newId = newId),
            history = history,
            onSaved = onSaved,
        )

    private fun accepted(id: String, content: SubmissionContent): ServerReply<Submission> =
        ServerReply.Ok(server(id, content, version = 3))
}

private const val ID = "s1"
private const val PHOTO = "p1"
private const val OLD = "s-old"
private const val SECOND = "s2"
private const val SECOND_PHOTO = "p2"
private const val SERVICE_UNAVAILABLE = 503
