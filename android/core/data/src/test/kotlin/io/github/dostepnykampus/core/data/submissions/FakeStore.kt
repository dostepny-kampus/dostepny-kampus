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
import io.github.dostepnykampus.core.model.Submission
import io.github.dostepnykampus.core.network.ApiResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Kolejka w pamięci, jak w testach Room, tylko z dziennikiem wywołań serwerowych. */
internal class FakeStore : PendingSubmissionDao {
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
            if (item.submission.id != submissionId) {
                item
            } else {
                item.copy(photos = item.photos.map { it.copy(isUploaded = it.isUploaded || it.photoId == photoId) })
            }
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

internal const val SENT_ID = "0b6f6c1e-8a43-4d5e-9f43-2f1d3c4b5a69"
