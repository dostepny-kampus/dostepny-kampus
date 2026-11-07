/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submissions

import androidx.work.ListenableWorker
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.Submission
import io.github.dostepnykampus.core.model.SubmissionContent
import io.github.dostepnykampus.core.model.SubmissionKind
import io.github.dostepnykampus.core.model.SubmissionStatus
import io.github.dostepnykampus.core.network.ApiResult
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException
import java.time.Instant
import kotlin.coroutines.EmptyCoroutineContext

/** Zadanie WorkManagera ponawia wysyłkę, gdy kolejka każe spróbować później (NFR-6, US9.2). */
class SubmissionSyncWorkerTest {
    @TempDir
    lateinit var tempDir: File

    private val store = FakeStore()

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
            store.createResults.addLast(ApiResult.NetworkError(IOException("sieć")))

            assertEquals(ListenableWorker.Result.retry(), target.syncResult())
            assertEquals(listOf(SENT_ID), store.rows.map { it.submission.id })
        }

    @Test
    fun `400 bez ponawiania zadania`() =
        runBlocking {
            val target = queue()
            target.save(content(), photos())
            store.createResults.addLast(ApiResult.ServerError(code = 400))

            assertEquals(ListenableWorker.Result.success(), target.syncResult())
            assertEquals(listOf(SENT_ID), store.rows.map { it.submission.id })
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
            create = { id, content -> store.createResults.removeFirstOrNull() ?: accepted(id, content) },
            putPhoto = { _, _, _ -> ApiResult.Success(Unit) },
            newId = { SENT_ID },
        )

    private fun photos(): List<PendingPhotoDraft> {
        val file = tempDir.resolve("p1.jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        return listOf(PendingPhotoDraft(photoId = "p1", path = file.path))
    }

    private fun accepted(id: String, content: SubmissionContent): ApiResult<Submission> =
        ApiResult.Success(
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
