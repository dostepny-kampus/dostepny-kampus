/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submissions

import androidx.work.ListenableWorker
import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.Submission
import io.github.dostepnykampus.core.model.SubmissionContent
import io.github.dostepnykampus.core.model.SubmissionKind
import io.github.dostepnykampus.core.model.SubmissionStatus
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
    fun `pusta kolejka kończy zadanie`() =
        runBlocking {
            assertEquals(ListenableWorker.Result.success(), queue().syncResult())
        }

    @Test
    fun `brak sieci ponawia zadanie`() =
        runBlocking {
            val target = queue()
            target.save(content(), photos())
            store.createResults.addLast(ServerReply.Offline)

            assertEquals(ListenableWorker.Result.retry(), target.syncResult())
            assertEquals(listOf(SENT_ID), store.rows.map { it.submission.id })
        }

    @ParameterizedTest
    @ValueSource(ints = [500, 503])
    fun `awaria 5xx ponawia zadanie`(code: Int): Unit =
        runBlocking {
            val target = queue()
            target.save(content(), photos())
            store.createResults.addLast(ServerReply.Fault(code = code))

            assertEquals(ListenableWorker.Result.retry(), target.syncResult())
            assertEquals(listOf(SENT_ID), store.rows.map { it.submission.id })
            assertFalse(target.loginExpired.value)
        }

    @Test
    fun `401 czeka na zalogowanie`() =
        runBlocking {
            val target = queue()
            target.save(content(), photos())
            store.createResults.addLast(ServerReply.Fault(code = 401))

            assertEquals(ListenableWorker.Result.success(), target.syncResult())
            assertTrue(target.loginExpired.value)
            assertEquals(listOf(SENT_ID), store.rows.map { it.submission.id })
            assertEquals(ListenableWorker.Result.success(), target.syncResult())
            assertEquals(1, sends)
        }

    @Test
    fun `po zalogowaniu wysyła sama`() =
        runBlocking {
            val target = queue()
            target.save(content(), photos())
            store.createResults.addLast(ServerReply.Fault(code = 401))
            target.syncResult()
            val scheduledBefore = schedules

            target.onLoggedIn()

            assertFalse(target.loginExpired.value)
            assertEquals(scheduledBefore + 1, schedules)
            assertEquals(ListenableWorker.Result.success(), target.syncResult())
            assertEquals(emptyList<String>(), store.rows.map { it.submission.id })
        }

    @ParameterizedTest
    @ValueSource(ints = [400, 403, 404, 413])
    fun `odmowa 4xx bez ponawiania`(code: Int): Unit =
        runBlocking {
            val target = queue()
            target.save(content(), photos())
            store.createResults.addLast(if (code == 404) ServerReply.Missing else ServerReply.Fault(code = code))

            assertEquals(ListenableWorker.Result.success(), target.syncResult())
            assertEquals(listOf(SENT_ID), store.rows.map { it.submission.id })
            assertFalse(target.loginExpired.value)
        }

    @Test
    fun `wysłane znika z kolejki`() =
        runBlocking {
            val target = queue()
            target.save(content(), photos())

            assertEquals(ListenableWorker.Result.success(), target.syncResult())
            assertEquals(listOf(SENT_ID), store.created)
            assertEquals(listOf(SENT_ID to "p1"), store.photos)
            assertEquals(emptyList<String>(), store.rows.map { it.submission.id })
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
