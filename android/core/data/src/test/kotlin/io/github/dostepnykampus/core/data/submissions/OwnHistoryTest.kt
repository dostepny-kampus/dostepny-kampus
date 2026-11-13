/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submissions

import io.github.dostepnykampus.core.database.OwnSubmissionEntity
import io.github.dostepnykampus.core.database.OwnSubmissionState
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.Submission
import io.github.dostepnykampus.core.model.SubmissionContent
import io.github.dostepnykampus.core.model.SubmissionKind
import io.github.dostepnykampus.core.model.SubmissionStatus
import io.github.dostepnykampus.core.network.ApiResult
import io.github.dostepnykampus.core.network.SubmissionEdit
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
import java.io.IOException
import java.time.Instant
import kotlin.coroutines.EmptyCoroutineContext

/** Kolejka zapisuje stan wysyłki własnych zgłoszeń dla listy C3. Wymagania: US8, NFR-5 */
class OwnHistoryTest {
    @TempDir
    lateinit var tempDir: File

    private val store = FakeStore()
    private val history = FakeHistory()
    private var photoAttempts = 0

    @Test
    fun `wynik zapisu wskazuje wiersz`() =
        runBlocking {
            val saved: SavedSubmission = queue().save(precise(), photo())
            assertEquals(ID, saved.id)
            assertEquals(saved.id, store.all().single().submission.id)
            assertEquals(saved.id, history.rows.getValue(saved.id).id)
        }

    @Test
    fun `us8 zapis to W telefonie`() =
        runBlocking {
            queue().save(precise(), photo())

            val row = history.rows.getValue(ID)
            assertEquals(OwnSubmissionState.LOCAL, row.state)
            assertEquals("TB-5", row.barrierType)
        }

    @Test
    fun `us4_4 szybkie poza listą`() =
        runBlocking {
            queue().save(precise().copy(kind = SubmissionKind.QUICK), photo())

            assertTrue(history.rows.isEmpty())
        }

    @Test
    fun `nfr5 wysłane z wersją`() =
        runBlocking {
            val target = queue()
            target.save(precise(), photo())
            target.sendAll()

            val row = history.rows.getValue(ID)
            assertEquals(OwnSubmissionState.SENT, row.state)
            assertEquals(3, row.serverVersion)
        }

    @Test
    fun `nfr5 bez sieci zostaje lokalne`() =
        runBlocking {
            val target = queue()
            target.save(precise(), photo())
            store.createResults.addLast(ApiResult.NetworkError(IOException("sieć")))
            target.sendAll()

            assertEquals(OwnSubmissionState.LOCAL, history.rows.getValue(ID).state)
        }

    /**
     * Serwer zapisał treść, ale telefon nie dostał odpowiedzi: ponawia to samo id i treść, potem wysyła zdjęcie.
     * Scenariusz (częściowo): synchronizacja.feature: Przerwana synchronizacja
     *   nie gubi zgłoszenia i nie tworzy drugiego
     */
    @Test
    fun `br9 zgubiona odpowiedź`() =
        runBlocking {
            val sent = mutableListOf<Pair<String, SubmissionContent>>()
            val remote = mutableMapOf<String, Submission>()
            val target =
                queue { id, content ->
                    sent.add(id to content)
                    val saved = remote.getOrPut(id) { server(id, content) }
                    if (sent.size == 1) {
                        ApiResult.NetworkError(IOException("Odpowiedź zginęła po zapisie treści"))
                    } else {
                        ApiResult.Success(saved)
                    }
                }
            val photos = photo()
            target.save(precise(), photos)

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
    fun `nfr50 awaria serwera lokalne`() =
        runBlocking {
            val target = queue()
            target.save(precise(), photo())
            store.createResults.addLast(ApiResult.ServerError(code = SERVICE_UNAVAILABLE))

            assertFalse(target.sendAll())
            val row = history.rows.getValue(ID)
            assertEquals(OwnSubmissionState.LOCAL, row.state)
            assertNull(row.refusalCode)
            assertEquals(listOf(ID), store.all().map { it.submission.id })

            assertTrue(target.sendAll())
            assertEquals(OwnSubmissionState.SENT, history.rows.getValue(ID).state)
        }

    @Test
    fun `nfr5 odmowa z kodem`() =
        runBlocking {
            val target = queue()
            target.save(precise(), photo())
            store.createResults.addLast(ApiResult.ServerError(code = 422))
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
            val photos = photo()
            target.save(precise(), photos)
            val refusal = if (code == 404) ApiResult.NotFound else ApiResult.ServerError(code)
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

    @Test
    fun `us8_3 poprawka znów lokalna`() =
        runBlocking {
            val target = queue()
            target.save(precise(), photo())
            target.sendAll()
            target.saveEdit(EditedSubmission(id = ID, baseVersion = 3), precise().copy(severity = 1), photo())

            val row = history.rows.getValue(ID)
            assertEquals(OwnSubmissionState.LOCAL, row.state)
            assertEquals(1, row.severity)
            assertNull(row.serverVersion)
        }

    /**
     * Zmiana autora odrzucona jako „nieaktualne” zostaje w historii, a nowe zgłoszenie tej samej bariery jest osobne.
     * Scenariusz: weryfikacja.feature: Po przyjęciu „bariera usunięta” barierę, która nadal istnieje,
     *   zgłasza się od nowa
     */
    @Test
    fun `br23 nowe po nieaktualnej`() =
        runBlocking {
            history.rows[OLD] =
                OwnSubmissionEntity(id = OLD, savedAtMillis = 1, kind = "PRECISE", barrierType = "TB-9")
            history.saveServerStatus(
                id = OLD,
                status = SubmissionStatus.REJECTED.name,
                reason = "nieaktualne",
                version = 2,
            )

            queue().save(precise().copy(barrierType = "TB-9", buildingSymbol = "D-2"), photo())

            val row = history.rows.getValue(ID)
            assertEquals(OwnSubmissionState.LOCAL, row.state)
            assertEquals("TB-9", row.barrierType)
            val pending = store.rows.single().submission
            assertNull(pending.replacesSubmissionId)
            assertNull(pending.baseVersion)
            assertEquals("nieaktualne", history.rows.getValue(OLD).rejectionReason)
        }

    private fun queue(
        create: suspend (String, SubmissionContent) -> ApiResult<Submission> = { id, content ->
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
                        store.photoResults.removeFirstOrNull() ?: ApiResult.Success(Unit)
                    },
                    update = { id, _, content -> ApiResult.Success(SubmissionEdit.Saved(server(id, content))) },
                ),
            stamps = SubmissionStamps(newId = { ID }),
            history = history,
        )

    private fun accepted(id: String, content: SubmissionContent): ApiResult<Submission> =
        ApiResult.Success(server(id, content))

    private fun server(id: String, content: SubmissionContent): Submission =
        Submission(
            id = id,
            version = 3,
            status = SubmissionStatus.SENT,
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            content = content,
        )

    private fun photo(): List<PendingPhotoDraft> {
        val file = tempDir.resolve("$PHOTO.jpg")
        file.writeBytes(byteArrayOf(1, 2, 3))
        return listOf(PendingPhotoDraft(photoId = PHOTO, path = file.path))
    }

    private fun precise(): SubmissionContent =
        SubmissionContent(
            kind = SubmissionKind.PRECISE,
            location = GeoPoint(longitude = 17.06209, latitude = 51.10785),
            photoIds = listOf(PHOTO),
            barrierType = "TB-5",
            severity = 2,
        )
}

private const val ID = "s1"
private const val PHOTO = "p1"
private const val OLD = "s-old"
private const val SERVICE_UNAVAILABLE = 503
