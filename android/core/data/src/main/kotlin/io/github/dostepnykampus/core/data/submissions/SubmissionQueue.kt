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
import io.github.dostepnykampus.core.model.SubmissionContent
import io.github.dostepnykampus.core.network.ApiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.coroutines.CoroutineContext

/**
 * Zdjęcie do wysłania po treści zgłoszenia.
 *
 * @property photoId identyfikator UUID zdjęcia nadany w telefonie (BR-7)
 * @property path ścieżka pliku JPEG w pamięci aplikacji
 */
data class PendingPhotoDraft(val photoId: String, val path: String)

/** Wiersz zdjęcia z projektu do zapisania w bazie. */
internal fun PendingPhotoDraft.toPendingPhotoEntity(submissionId: String): PendingPhotoEntity =
    PendingPhotoEntity(
        submissionId = submissionId,
        photoId = photoId,
        path = path,
        isUploaded = false,
    )

private enum class Outcome { SENT, OFFLINE, REFUSED }

/**
 * Kolejka zgłoszeń: zapis w telefonie i wysyłanie od najstarszego (NFR-5, US9.1, US9.2, BR-9).
 *
 * Bez sieci albo po błędzie serwera zgłoszenie zostaje w kolejce: zapis w bazie mówi, co serwer
 * już ma, więc kolejna próba wysyła treść raz i tylko brakujące zdjęcia (NFR-8).
 * Identyfikator powstaje raz, więc serwer nie dostanie drugiego zgłoszenia (BR-9).
 * Po zapisie [onSaved] planuje wysyłkę po powrocie sieci (NFR-6); zapis działa także bez niego.
 */
class SubmissionQueue(
    private val store: PendingSubmissionDao,
    private val ioContext: CoroutineContext = Dispatchers.IO,
    private val create: suspend (id: String, content: SubmissionContent) -> ApiResult<Submission>,
    private val putPhoto: suspend (submissionId: String, photoId: String, jpeg: ByteArray) -> ApiResult<Unit>,
    private val newId: () -> String = { UUID.randomUUID().toString() },
    private val onSaved: () -> Unit = {},
) {
    private val lock = Mutex()

    /**
     * Zapisuje zgłoszenie w kolejce z nowym identyfikatorem w telefonie (BR-9).
     *
     * @param content treść zgłoszenia
     * @param photos zdjęcia do wysłania; identyfikatory wracają w [content] (BR-7, BR-8)
     */
    suspend fun save(content: SubmissionContent, photos: List<PendingPhotoDraft>): PendingSubmissionEntity {
        require(photos.map { it.photoId } == content.photoIds) {
            "Zdjęcia w kolejce muszą odpowiadać identyfikatorom treści (BR-7, BR-8)"
        }
        val id = newId()
        val entity = content.toPendingSubmissionEntity(id = id, savedAtMillis = System.currentTimeMillis())
        store.save(entity, photos.map { it.toPendingPhotoEntity(submissionId = id) })
        // Awaria planowania nie może zgubić zapisanego zgłoszenia (NFR-5).
        runCatching(onSaved)
        return entity
    }

    /**
     * Wysyła zgłoszenia z kolejki od najstarszych: treść raz, potem tylko nie wysłane zdjęcia.
     * Bez sieci wysyłanie się kończy do zasięgu, a błąd serwera zostawia zgłoszenie w kolejce (NFR-5, US9.2).
     *
     * @return `true`, gdy wysyłka doszła do końca kolejki; `false`, gdy zabrakło sieci i trzeba spróbować później
     */
    suspend fun sendAll(): Boolean =
        lock.withLock {
            val isOffline = store.all().any { item -> sendOne(item) == Outcome.OFFLINE }
            !isOffline
        }

    private suspend fun sendOne(item: PendingSubmissionWithPhotos): Outcome =
        if (item.submission.isCreatedOnServer) {
            sendRemaining(item)
        } else {
            val status = sendContent(item)
            if (status == Outcome.SENT) sendRemaining(item) else status
        }

    private suspend fun sendContent(item: PendingSubmissionWithPhotos): Outcome {
        val result = create(item.submission.id, item.toSubmissionContent())
        val outcome = outcomeOf(result)
        if (outcome == Outcome.SENT) {
            store.markCreated(id = item.submission.id)
        }
        return outcome
    }

    private suspend fun sendRemaining(item: PendingSubmissionWithPhotos): Outcome {
        for (photo in item.photos.filterNot { it.isUploaded }) {
            val result = putPhoto(item.submission.id, photo.photoId, jpeg(photo.path))
            if (result !is ApiResult.Success) {
                return outcomeOf(result)
            }
            store.markUploaded(submissionId = item.submission.id, photoId = photo.photoId)
        }
        store.delete(item.submission.id)
        return Outcome.SENT
    }

    private fun outcomeOf(result: ApiResult<*>): Outcome =
        when (result) {
            is ApiResult.NetworkError -> Outcome.OFFLINE
            ApiResult.NotFound, is ApiResult.ServerError -> Outcome.REFUSED
            is ApiResult.Success -> Outcome.SENT
        }

    private suspend fun jpeg(path: String): ByteArray = withContext(ioContext) { File(path).readBytes() }
}
