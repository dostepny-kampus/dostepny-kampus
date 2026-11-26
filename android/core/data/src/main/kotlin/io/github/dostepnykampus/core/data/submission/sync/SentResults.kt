/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submission.sync

import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.data.submission.own.refusalCodeOf
import io.github.dostepnykampus.core.data.submission.own.versionOf
import io.github.dostepnykampus.core.database.cache.ServerCopyDao
import io.github.dostepnykampus.core.database.submission.OwnSubmissionDao
import io.github.dostepnykampus.core.database.submission.PendingSubmissionDao
import io.github.dostepnykampus.core.database.submission.PendingSubmissionEntity
import io.github.dostepnykampus.core.database.submission.PendingSubmissionWithPhotos
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import kotlin.coroutines.CoroutineContext

/**
 * Zapis wyników wysyłki w kolejce i historii pod blokadą wierszy.
 *
 * Blokada chroni tylko odczyt i zapis wierszy, nie wywołania serwera, więc poprawka z C3 zapisuje się od razu,
 * także gdy wysyłka czeka na sieć. Wynik trafia do wiersza tylko wtedy, gdy wiersza nie zastąpiła w tym czasie
 * poprawka: inaczej kolejka oznaczyłaby jako wysłaną albo usunęła treść, której serwer nie dostał.
 * Zapisy wiersza kolejki, historii i kopii wersji z serwera jednego wyniku są jedną transakcją bazy.
 */
internal class SentResults(
    private val store: PendingSubmissionDao,
    records: SentRecords,
    private val ioContext: CoroutineContext,
) {
    private val lock = Mutex()
    private val history = records.history
    private val conflicts = records.conflicts

    /** [write] pod tą samą blokadą co wyniki wysyłki, np. zapis poprawki z C3. */
    suspend fun <T> locked(write: suspend () -> T): T = lock.withLock { write() }

    /**
     * Wynik wysłania treści [item]. Gdy w tym czasie zapisano poprawkę, wersja nadana starej treści staje się jej
     * wersją bazową, więc poprawka idzie jako edycja, a nie drugie utworzenie.
     *
     * Wymagania: BR-9, BR-10
     */
    suspend fun content(item: PendingSubmissionWithPhotos, result: ServerReply<*>, outcome: Outcome): Outcome =
        lock.withLock {
            inTransaction {
                val replacement = store.replacementOf(item)
                if (replacement == null) contentOf(item, result, outcome) else keepVersion(replacement, result)
            }
        }

    /** Wynik wysłania zdjęcia [photoId]; `null`, gdy telefon nie odczytał pliku. */
    suspend fun photo(item: PendingSubmissionWithPhotos, photoId: String, result: ServerReply<Unit>?): Outcome =
        ifCurrent(item) {
            if (result is ServerReply.Ok) {
                store.markUploaded(submissionId = item.submission.id, photoId = photoId)
                Outcome.SENT
            } else {
                val outcome = result?.let(::outcomeOf) ?: Outcome.REFUSED
                if (outcome == Outcome.REFUSED) refused(item.submission.id, result?.let(::refusalCodeOf))
                outcome
            }
        }

    /**
     * Serwer ma całe zgłoszenie [item]: wiersz kolejki znika. Pliki zdjęć zgłoszenia z historii zostają do decyzji
     * na poprawkę z C3 (usuwa je sprzątanie przy starcie), pozostałe znikają od razu.
     *
     * Wymagania: NFR-5, US8.3
     */
    suspend fun finish(item: PendingSubmissionWithPhotos): Outcome {
        val outcome =
            ifCurrent(item) {
                store.delete(item.submission.id)
                history?.markSent(id = item.submission.id)
                Outcome.SENT
            }
        val isKept = history?.find(item.submission.id) != null
        if (outcome == Outcome.SENT && !isKept) deletePhotoFiles(item.photos.map { it.path }, ioContext)
        return outcome
    }

    /** Telefon nie odczytał [item]; odmowa bez kodu, więc jeden zły wiersz nie zatrzymuje kolejki. */
    suspend fun unreadable(item: PendingSubmissionWithPhotos): Outcome =
        ifCurrent(item) { refused(item.submission.id, code = null) }

    private suspend fun ifCurrent(item: PendingSubmissionWithPhotos, write: suspend () -> Outcome): Outcome =
        lock.withLock { inTransaction { if (store.replacementOf(item) == null) write() else Outcome.REPLACED } }

    private suspend fun inTransaction(write: suspend () -> Outcome): Outcome {
        var outcome = Outcome.LATER
        store.together { outcome = write() }
        return outcome
    }

    private suspend fun contentOf(
        item: PendingSubmissionWithPhotos,
        result: ServerReply<*>,
        outcome: Outcome,
    ): Outcome {
        if (outcome == Outcome.SENT) {
            store.markCreated(id = item.submission.id)
            versionOf(result)?.let { history?.saveVersion(id = item.submission.id, version = it) }
        }
        serverVersionOf(result)?.let { server ->
            store.upsertSubmission(item.submission.copy(baseVersion = server.version))
            history?.saveVersion(id = item.submission.id, version = server.version)
            conflicts?.save(serverVersionRow(server, syncedAt = Instant.now()))
        }
        if (outcome == Outcome.REFUSED) refused(item.submission.id, refusalCodeOf(result))
        return outcome
    }

    private suspend fun keepVersion(replacement: PendingSubmissionEntity, result: ServerReply<*>): Outcome {
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

/**
 * Zapisy wyników wysyłki poza wierszami kolejki.
 *
 * Wymagania: US8, US9.3
 *
 * @property history historia zgłoszeń Zbierającego; `null` dla kolejki bez historii
 * @property conflicts kopie wersji z serwera po odmowie 409; `null`, gdy kolejka ich nie zapisuje
 */
internal data class SentRecords(val history: OwnSubmissionDao? = null, val conflicts: ServerCopyDao? = null)

/** Wiersz, który zastąpił [item] w kolejce: inna chwila zapisu znaczy poprawkę zapisaną w trakcie wysyłki. */
internal suspend fun PendingSubmissionDao.replacementOf(item: PendingSubmissionWithPhotos): PendingSubmissionEntity? {
    val current = all().firstOrNull { it.submission.id == item.submission.id }?.submission
    return current?.takeIf { it.savedAtMillis != item.submission.savedAtMillis }
}
