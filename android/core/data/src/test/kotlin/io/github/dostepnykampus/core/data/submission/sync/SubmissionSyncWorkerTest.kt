/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submission.sync

import androidx.work.ListenableWorker
import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.submission.Submission
import io.github.dostepnykampus.core.model.submission.SubmissionContent
import io.github.dostepnykampus.core.model.submission.SubmissionKind
import io.github.dostepnykampus.core.model.submission.SubmissionStatus
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File
import java.time.Instant
import kotlin.coroutines.EmptyCoroutineContext

/** Zadanie WorkManagera ponawia wysyłkę, gdy kolejka każe spróbować później. Wymagania: NFR-6, US9.2, BR-9, BR-26 */
class SubmissionSyncWorkerTest {
    @TempDir
    lateinit var tempDir: File

    private val store = FakeStore()
    private var sends = 0
    private var schedules = 0

    @Test
    fun finishesOnEmptyQueue() =
        runBlocking {
            assertEquals(
                ListenableWorker.Result.success(),
                queue().syncResult(),
                "pusta kolejka kończy zadanie powodzeniem",
            )
        }

    @Test
    fun retriesOffline() =
        runBlocking {
            val target = queue()
            target.save(content(), photos())
            store.createResults.addLast(ServerReply.Offline)

            assertEquals(ListenableWorker.Result.retry(), target.syncResult(), "brak sieci ponawia zadanie")
            assertEquals(
                listOf(SENT_ID),
                store.rows.map { it.submission.id },
                "brak sieci zostawia zgłoszenie w kolejce",
            )
        }

    /**
     * Scenariusz (częściowo, przykład: nie potwierdzi odbioru zgłoszenia): zgloszenia-zbierajacego.feature:
     *   Status „wysłane” dopiero po potwierdzeniu odbioru przez serwer.
     */
    @ParameterizedTest
    @ValueSource(ints = [500, 503])
    fun `awaria 5xx ponawia zadanie`(code: Int): Unit =
        runBlocking {
            val target = queue()
            target.save(content(), photos())
            store.createResults.addLast(ServerReply.Fault(code = code))

            assertEquals(ListenableWorker.Result.retry(), target.syncResult(), "awaria 5xx daje ponowienie")
            assertEquals(
                listOf(SENT_ID),
                store.rows.map { it.submission.id },
                "awaria 5xx zostawia zgłoszenie w kolejce",
            )
            assertFalse(target.loginExpired.value, "awaria 5xx nie wygasza logowania")
        }

    @Test
    fun waitsForSignInOn401() =
        runBlocking {
            val target = queue()
            target.save(content(), photos())
            store.createResults.addLast(ServerReply.Fault(code = 401))

            assertEquals(ListenableWorker.Result.success(), target.syncResult(), "401 kończy zadanie bez ponowienia")
            assertTrue(target.loginExpired.value, "401 wygasza logowanie")
            assertEquals(listOf(SENT_ID), store.rows.map { it.submission.id }, "401 zostawia zgłoszenie w kolejce")
            assertEquals(
                ListenableWorker.Result.success(),
                target.syncResult(),
                "kolejny przebieg po 401 bez ponowienia",
            )
            assertEquals(1, sends, "po 401 wysyłka nie jest ponawiana")
        }

    @Test
    fun sendsAfterSignIn() =
        runBlocking {
            val target = queue()
            target.save(content(), photos())
            store.createResults.addLast(ServerReply.Fault(code = 401))
            target.syncResult()
            val scheduledBefore = schedules

            target.onLoggedIn()

            assertFalse(target.loginExpired.value, "logowanie czyści wygaśnięcie")
            assertEquals(scheduledBefore + 1, schedules, "logowanie planuje wysyłkę")
            assertEquals(
                ListenableWorker.Result.success(),
                target.syncResult(),
                "po logowaniu zadanie kończy się powodzeniem",
            )
            assertEquals(emptyList<String>(), store.rows.map { it.submission.id }, "po logowaniu kolejka jest pusta")
        }

    @ParameterizedTest
    @ValueSource(ints = [400, 403, 404, 413])
    fun `odmowa 4xx bez ponawiania`(code: Int): Unit =
        runBlocking {
            val target = queue()
            target.save(content(), photos())
            store.createResults.addLast(if (code == 404) ServerReply.Missing else ServerReply.Fault(code = code))

            assertEquals(
                ListenableWorker.Result.success(),
                target.syncResult(),
                "odmowa 4xx kończy zadanie bez ponowienia",
            )
            assertEquals(
                listOf(SENT_ID),
                store.rows.map { it.submission.id },
                "odmowa 4xx zostawia zgłoszenie w kolejce",
            )
            assertFalse(target.loginExpired.value, "odmowa 4xx nie wygasza logowania")
        }

    @Test
    fun removesSentFromQueue() =
        runBlocking {
            val target = queue()
            target.save(content(), photos())

            assertEquals(
                ListenableWorker.Result.success(),
                target.syncResult(),
                "wysłane zgłoszenie kończy zadanie powodzeniem",
            )
            assertEquals(listOf(SENT_ID), store.created, "treść wysłana raz")
            assertEquals(listOf(SENT_ID to "p1"), store.photos, "zdjęcie wysłane razem z treścią")
            assertEquals(emptyList<String>(), store.rows.map { it.submission.id }, "wysłane znika z kolejki")
        }

    private fun queue(): SubmissionQueue =
        SubmissionQueue(
            store = store,
            ioContext = EmptyCoroutineContext,
            create = { id, content ->
                sends++
                store.createResults.removeFirstOrNull() ?: accepted(id, content)
            },
            putPhoto = { _, _, _ -> ServerReply.Ok(Unit) },
            newId = { SENT_ID },
            onSaved = { schedules++ },
        )

    private fun photos(): List<PendingPhotoDraft> {
        val file = tempDir.resolve("p1.jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        return listOf(PendingPhotoDraft(photoId = "p1", path = file.path))
    }

    private fun accepted(id: String, content: SubmissionContent): ServerReply<Submission> =
        ServerReply.Ok(
            Submission(
                id = id,
                version = 1,
                status = SubmissionStatus.SENT,
                createdAt = Instant.parse("2026-01-01T00:00:00Z"),
                content = content,
            ),
        )

    private fun content(): SubmissionContent =
        SubmissionContent(
            kind = SubmissionKind.QUICK,
            location = GeoPoint(longitude = 17.06209, latitude = 51.10785),
            photoIds = listOf("p1"),
            severity = 1,
        )
}
