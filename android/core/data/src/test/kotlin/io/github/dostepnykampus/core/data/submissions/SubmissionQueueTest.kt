/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submissions

import io.github.dostepnykampus.core.database.PendingPhotoEntity
import io.github.dostepnykampus.core.database.PendingSubmissionDao
import io.github.dostepnykampus.core.database.PendingSubmissionEntity
import io.github.dostepnykampus.core.database.PendingSubmissionWithPhotos
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.Submission
import io.github.dostepnykampus.core.model.SubmissionContent
import io.github.dostepnykampus.core.model.SubmissionKind
import io.github.dostepnykampus.core.model.SubmissionStatus
import io.github.dostepnykampus.core.network.ApiResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException
import java.time.Instant
import kotlin.coroutines.EmptyCoroutineContext

/** Kolejka wysyła treść raz i tylko brakujące zdjęcia, a błędy zostawiają zgłoszenie w kolejce (NFR-5, US9.2). */
class SubmissionQueueTest {
    @TempDir
    lateinit var tempDir: File

    private val store = FakeStore()

    @Test
    fun `wysyła treść i czyści kolejkę`() =
        runBlocking {
            saveTwoPhotos()
            queue().sendAll()

            assertEquals(listOf(SENT_ID to "p1", SENT_ID to "p2"), store.photos)
            assertTrue(store.rows.isEmpty())
        }

    @Test
    fun `po zerwaniu kontynuuje zdjęcia`() =
        runBlocking {
            saveTwoPhotos()
            store.photoResults.addLast(ApiResult.NetworkError(IOException("sieć")))
            queue().sendAll()

            assertEquals(SENT_ID, store.created.single())
            val row = store.rows.single()
            assertTrue(store.photos.isEmpty() && row.photos.none { it.isUploaded })

            queue().sendAll()

            assertEquals(1, store.created.size)
            assertEquals(listOf(SENT_ID to "p1", SENT_ID to "p2"), store.photos)
            assertTrue(store.rows.isEmpty())
        }

    @Test
    fun `odmowa zostawia w kolejce`() =
        runBlocking {
            saveTwoPhotos()
            store.createResults.addLast(ApiResult.ServerError(code = 400))
            queue().sendAll()

            assertTrue(store.created.isEmpty())
            assertTrue(store.photos.isEmpty())
            val left = store.rows.single()
            assertTrue(left.submission.isCreatedOnServer == false)
        }

    private fun queue(): SubmissionQueue =
        SubmissionQueue(
            store = store,
            ioContext = EmptyCoroutineContext,
            create = { id, content -> store.createResults.removeFirstOrNull() ?: serverSubmission(content) },
            putPhoto = { _, _, _ -> store.photoResults.removeFirstOrNull() ?: ApiResult.Success(Unit) },
            newId = { SENT_ID },
        )

    private fun saveTwoPhotos() {
        val drafts = listOf("p1", "p2").map { photo -> PendingPhotoDraft(photoId = photo, path = pathOf(photo)) }
        drafts.forEach { File(it.path).writeBytes(byteArrayOf(1, 2, 3)) }
        runBlocking { queue().save(fullContent(), drafts) }
    }

    private fun pathOf(photo: String): String = tempDir.resolve("$photo.jpg").path

    private fun serverSubmission(content: SubmissionContent): ApiResult<Submission> =
        ApiResult.Success(
            Submission(
                id = "s1",
                version = 1,
                status = SubmissionStatus.SENT,
                createdAt = Instant.parse("2026-01-01T00:00:00Z"),
                content = content,
            ),
        )

    private fun fullContent(): SubmissionContent =
        SubmissionContent(
            kind = SubmissionKind.QUICK,
            location = GeoPoint(longitude = 17.06209, latitude = 51.10785),
            photoIds = listOf("p1", "p2"),
            severity = 2,
            description = "Progi przy sekretariacie",
        )
}

/** Kolejka w pamięci, jak w testach Room, tylko z dziennikiem wywołań serwerowych. */
private class FakeStore : PendingSubmissionDao {
    val rows = mutableListOf<PendingSubmissionWithPhotos>()
    val created = mutableListOf<String>()
    val photos = mutableListOf<Pair<String, String>>()
    val createResults = ArrayDeque<ApiResult<Submission>>()
    val photoResults = ArrayDeque<ApiResult<Unit>>()

    override fun observe(): Flow<List<PendingSubmissionWithPhotos>> = flow { emit(all()) }

    override suspend fun all(): List<PendingSubmissionWithPhotos> =
        rows.sortedWith(compareBy({ it.submission.savedAtMillis }, { it.submission.id }))

    override suspend fun markCreated(id: String) {
        created.add(id)
        rows.replaceAll { item ->
            val updated = item.submission.takeIf { it.id != id } ?: item.submission.copy(isCreatedOnServer = true)
            item.copy(submission = updated)
        }
    }

    override suspend fun markUploaded(submissionId: String, photoId: String) {
        photos.add(submissionId to photoId)
        rows.replaceAll { item ->
            item.copy(photos = item.photos.map { it.copy(isUploaded = it.photoId == photoId) })
        }
    }

    override suspend fun delete(id: String) {
        rows.removeAll { it.submission.id == id }
    }

    override suspend fun upsertSubmission(submission: PendingSubmissionEntity) {
        rows.removeAll { it.submission.id == submission.id }
        rows.add(PendingSubmissionWithPhotos(submission, emptyList()))
    }

    override suspend fun upsertPhotos(photos: List<PendingPhotoEntity>) {
        rows.replaceAll { item ->
            if (item.submission.id != photos.first().submissionId) item else item.copy(photos = item.photos + photos)
        }
    }

    override suspend fun deletePhotos(submissionId: String) {
        rows.replaceAll { item -> if (item.submission.id == submissionId) item.copy(photos = emptyList()) else item }
    }
}

private const val SENT_ID = "0b6f6c1e-8a43-4d5e-9f43-2f1d3c4b5a69"
