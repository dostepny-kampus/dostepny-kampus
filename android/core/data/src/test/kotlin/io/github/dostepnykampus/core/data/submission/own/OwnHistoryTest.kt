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
            assertEquals(ID, saved.id, "wynik wskazuje zapisany wiersz")
            assertEquals(saved.id, pending.submission.id, "kolejka trzyma zgłoszenie o tym samym id")
            assertEquals(saved.id, history.rows.getValue(saved.id).id, "historia ma wiersz o id zapisu")
        }

    @Test
    fun savesAsOnPhone() =
        runBlocking {
            queue().save(precise(PHOTO), photo(tempDir, PHOTO))

            val row = history.rows.getValue(ID)
            assertEquals(OwnSubmissionState.LOCAL, row.state, "nowy zapis ma stan lokalny")
            assertEquals("TB-5", row.barrierType, "typ bariery trafia do historii")
        }

    @Test
    fun keepsQuickOffList() =
        runBlocking {
            queue().save(precise(PHOTO).copy(kind = SubmissionKind.QUICK), photo(tempDir, PHOTO))

            assertTrue(history.rows.isEmpty(), "szybkie zgłoszenie nie trafia do historii")
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
            assertEquals(OwnSubmissionState.SENT, row.state, "potwierdzone zgłoszenie ma stan wysłane")
            assertEquals(3, row.serverVersion, "wersja serwera trafia do historii")
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

            assertEquals(
                OwnSubmissionState.LOCAL,
                history.rows.getValue(ID).state,
                "bez sieci zgłoszenie zostaje lokalne",
            )
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

            assertFalse(target.sendAll(), "przerwana odpowiedź kończy przebieg niepowodzeniem")
            assertEquals(setOf(ID), remote.keys, "serwer zapamiętał jedno zgłoszenie")
            val pending = store.rows.single().submission
            assertEquals(ID, pending.id, "kolejka zachowuje id zgłoszenia")
            assertFalse(pending.isCreatedOnServer, "treść niepotwierdzona nie jest uznana za utworzoną")
            assertTrue(store.photos.isEmpty(), "zdjęcie nie jest wysłane przed potwierdzeniem treści")
            assertTrue(File(photos.single().path).exists(), "plik zdjęcia zostaje na dysku")
            assertEquals(
                OwnSubmissionState.LOCAL,
                history.rows.getValue(ID).state,
                "po przerwaniu historia pokazuje stan lokalny",
            )

            assertTrue(target.sendAll(), "drugi przebieg kończy się powodzeniem")
            assertEquals(listOf(ID, ID), sent.map { it.first }, "ponowienie używa tego samego id")
            assertEquals(sent.first(), sent.last(), "ponowienie wysyła tę samą treść")
            assertEquals(1, remote.size, "serwer nie dostaje duplikatu")
            assertEquals(listOf(ID to PHOTO), store.photos, "zdjęcie wysłane po ponowieniu treści")
            assertTrue(store.rows.isEmpty(), "po wysyłce kolejka jest pusta")
            assertEquals(
                OwnSubmissionState.SENT,
                history.rows.getValue(ID).state,
                "po ponowieniu historia pokazuje stan wysłane",
            )
            assertEquals(3, history.rows.getValue(ID).serverVersion, "po ponowieniu historia ma wersję serwera")
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

            assertFalse(target.sendAll(), "awaria serwera kończy przebieg niepowodzeniem")
            val row = history.rows.getValue(ID)
            assertEquals(OwnSubmissionState.LOCAL, row.state, "awaria 503 zostawia stan lokalny")
            assertNull(row.refusalCode, "awaria 503 nie ustawia kodu odmowy")
            assertEquals(listOf(ID), store.all().map { it.submission.id }, "po awarii zgłoszenie czeka w kolejce")

            assertTrue(target.sendAll(), "kolejny przebieg po awarii kończy się powodzeniem")
            assertEquals(
                OwnSubmissionState.SENT,
                history.rows.getValue(ID).state,
                "kolejny przebieg po awarii wysyła zgłoszenie",
            )
        }

    @Test
    fun keepsRefusalCode() =
        runBlocking {
            val target = queue()
            target.save(precise(PHOTO), photo(tempDir, PHOTO))
            store.createResults.addLast(ServerReply.Fault(code = 422))
            target.sendAll()

            val row = history.rows.getValue(ID)
            assertEquals(OwnSubmissionState.REFUSED, row.state, "odmowa treści daje stan odrzucone")
            assertEquals(422, row.refusalCode, "odmowa zapisuje kod w historii")
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

            assertTrue(target.sendAll(), "przebieg z odmową kończy się powodzeniem")
            val pending = store.rows.single().submission
            assertTrue(pending.isRefused, "odmowa oznacza zgłoszenie w kolejce")
            assertEquals(isPhoto, pending.isCreatedOnServer, "treść utworzona tylko przy odmowie zdjęcia")
            assertTrue(File(photos.single().path).exists(), "plik zdjęcia zostaje po odmowie")
            val row = history.rows.getValue(ID)
            assertEquals(OwnSubmissionState.REFUSED, row.state, "historia pokazuje odrzucone po odmowie")
            assertEquals(code, row.refusalCode, "historia zapisuje kod odmowy z serwera")

            assertTrue(target.sendAll(), "drugi przebieg pomija odrzucone")
            assertTrue(target.sendAll(), "trzeci przebieg pomija odrzucone")
            assertEquals(if (isPhoto) 1 else 0, photoAttempts, "zdjęcie nie jest ponawiane po odmowie")
            assertEquals(
                if (isPhoto) listOf(ID) else emptyList<String>(),
                store.created,
                "treść nie jest ponawiana po odmowie",
            )
            assertEquals(pending, store.rows.single().submission, "odrzucone zgłoszenie w kolejce bez zmian")
            assertEquals(row, history.rows.getValue(ID), "wiersz historii po odmowie bez zmian")
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
            assertEquals(2, row.severity, "edycja zmienia nasilenie w historii")
            assertEquals(OwnSubmissionState.SENT, row.state, "edycja zachowuje stan wysłane")
            assertEquals(status.name, row.serverStatus, "edycja zachowuje status z serwera")
            assertEquals(3, row.serverVersion, "edycja zachowuje wersję serwera")
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
            assertEquals(OwnSubmissionState.LOCAL, row.state, "edycja niewysłanego zostaje lokalna")
            assertEquals(1, row.severity, "edycja niewysłanego zmienia nasilenie")
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
            assertEquals(OwnSubmissionState.LOCAL, row.state, "nowe zgłoszenie ma stan lokalny")
            assertEquals("TB-9", row.barrierType, "nowe zgłoszenie ma typ bariery z zapisu")
            val pending = store.rows.single().submission
            assertNull(pending.replacesSubmissionId, "nowe zgłoszenie nie zastępuje starego")
            assertNull(pending.baseVersion, "nowe zgłoszenie nie ma wersji bazowej")
            assertEquals(
                "nieaktualne",
                history.rows.getValue(OLD).rejectionReason,
                "stare zgłoszenie zachowuje powód odrzucenia",
            )
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
