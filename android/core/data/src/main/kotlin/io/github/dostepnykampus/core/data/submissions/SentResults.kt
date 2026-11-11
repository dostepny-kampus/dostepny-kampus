/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submissions

import io.github.dostepnykampus.core.database.OwnSubmissionDao
import io.github.dostepnykampus.core.database.PendingSubmissionDao
import io.github.dostepnykampus.core.database.PendingSubmissionEntity
import io.github.dostepnykampus.core.database.PendingSubmissionWithPhotos
import io.github.dostepnykampus.core.network.ApiResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.CoroutineContext

/**
 * Zapis wyników wysyłki w kolejce i historii pod blokadą wierszy (#855).
 *
 * Blokada chroni tylko odczyt i zapis wierszy, nie wywołania serwera, więc poprawka z C3 zapisuje się od razu,
 * także gdy wysyłka czeka na sieć. Wynik trafia do wiersza tylko wtedy, gdy wiersza nie zastąpiła w tym czasie
 * poprawka: inaczej kolejka oznaczyłaby jako wysłaną albo usunęła treść, której serwer nie dostał.
 */
internal class SentResults(
    private val store: PendingSubmissionDao,
    private val history: OwnSubmissionDao?,
    private val ioContext: CoroutineContext,
) {
    private val lock = Mutex()

    /** [write] pod tą samą blokadą co wyniki wysyłki, np. zapis poprawki z C3. */
    suspend fun <T> locked(write: suspend () -> T): T = lock.withLock { write() }

    /**
     * Wynik wysłania treści [item]. Gdy w tym czasie zapisano poprawkę, wersja nadana starej treści staje się jej
     * wersją bazową, więc poprawka idzie jako edycja, a nie drugie utworzenie (BR-9, BR-10).
     */
    suspend fun content(item: PendingSubmissionWithPhotos, result: ApiResult<*>, outcome: Outcome): Outcome =
        lock.withLock {
            val replacement = store.replacementOf(item)
            if (replacement == null) contentOf(item, result, outcome) else keepVersion(replacement, result)
        }

    /** Wynik wysłania zdjęcia [photoId]; `null`, gdy telefon nie odczytał pliku. */
    suspend fun photo(item: PendingSubmissionWithPhotos, photoId: String, result: ApiResult<Unit>?): Outcome =
        ifCurrent(item) {
            if (result is ApiResult.Success) {
                store.markUploaded(submissionId = item.submission.id, photoId = photoId)
                Outcome.SENT
            } else {
                val outcome = result?.let(::outcomeOf) ?: Outcome.REFUSED
                if (outcome == Outcome.REFUSED) refused(item.submission.id, result?.let(::refusalCodeOf))
                outcome
            }
        }

    /** Serwer ma całe zgłoszenie [item]: wiersz i pliki zdjęć znikają z telefonu (NFR-5, #857). */
    suspend fun finish(item: PendingSubmissionWithPhotos): Outcome =
        ifCurrent(item) {
            store.delete(item.submission.id)
            deletePhotoFiles(item.photos.map { it.path }, ioContext)
            history?.markSent(id = item.submission.id)
            Outcome.SENT
        }

    /** Telefon nie odczytał [item]; odmowa bez kodu, więc jeden zły wiersz nie zatrzymuje kolejki (#856). */
    suspend fun unreadable(item: PendingSubmissionWithPhotos): Outcome =
        ifCurrent(item) { refused(item.submission.id, code = null) }

    private suspend fun ifCurrent(item: PendingSubmissionWithPhotos, write: suspend () -> Outcome): Outcome =
        lock.withLock { if (store.replacementOf(item) == null) write() else Outcome.REPLACED }

    private suspend fun contentOf(item: PendingSubmissionWithPhotos, result: ApiResult<*>, outcome: Outcome): Outcome {
        if (outcome == Outcome.SENT) {
            store.markCreated(id = item.submission.id)
            versionOf(result)?.let { history?.saveVersion(id = item.submission.id, version = it) }
        }
        conflictVersionOf(result)?.let { server ->
            store.upsertSubmission(item.submission.copy(baseVersion = server))
            history?.saveVersion(id = item.submission.id, version = server)
        }
        if (outcome == Outcome.REFUSED) refused(item.submission.id, refusalCodeOf(result))
        return outcome
    }

    private suspend fun keepVersion(replacement: PendingSubmissionEntity, result: ApiResult<*>): Outcome {
        versionOf(result)?.let { version ->
            store.upsertSubmission(replacement.copy(baseVersion = version))
            history?.saveVersion(id = replacement.id, version = version)
        }
        return Outcome.REPLACED
    }

    private suspend fun refused(id: String, code: Int?): Outcome {
        store.markRefused(id)
        history?.markRefused(id = id, code = code)
        return Outcome.REFUSED
    }
}

/** Wiersz, który zastąpił [item] w kolejce: inna chwila zapisu znaczy poprawkę zapisaną w trakcie wysyłki. */
internal suspend fun PendingSubmissionDao.replacementOf(item: PendingSubmissionWithPhotos): PendingSubmissionEntity? {
    val current = all().firstOrNull { it.submission.id == item.submission.id }?.submission
    return current?.takeIf { it.savedAtMillis != item.submission.savedAtMillis }
}
