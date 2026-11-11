/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.database

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Kolejka zgłoszeń zapisanych w telefonie (NFR-5, US9.1, US9.2). */
@Dao
interface PendingSubmissionDao : PendingSubmissionRows {
    /**
     * Zapisuje zgłoszenie razem ze zdjęciami. Ten sam identyfikator zastępuje poprzedni zapis.
     *
     * @param submission zgłoszenie
     * @param photos zdjęcia zgłoszenia
     * @return ścieżki plików zdjęć poprzedniego zapisu, których nowy zapis już nie ma; pliki można usunąć
     */
    @Transaction
    suspend fun save(submission: PendingSubmissionEntity, photos: List<PendingPhotoEntity>): List<String> {
        val previous = photoPaths(submission.id)
        upsertSubmission(submission)
        deletePhotos(submission.id)
        upsertPhotos(photos)
        return previous - photos.map { it.path }.toSet()
    }

    /** Zgłoszenia czekające na wysłanie, bez odrzuconych, od najstarszego; zmienia się po każdym zapisie. */
    @Transaction
    @Query("SELECT * FROM pending_submissions WHERE refused = 0 ORDER BY saved_at, id")
    fun observe(): Flow<List<PendingSubmissionWithPhotos>>

    /** Zgłoszenia czekające na wysłanie, bez odrzuconych, od najstarszego. */
    @Transaction
    @Query("SELECT * FROM pending_submissions WHERE refused = 0 ORDER BY saved_at, id")
    suspend fun all(): List<PendingSubmissionWithPhotos>

    /**
     * Zaznacza, że serwer ma już zgłoszenie, a czekają tylko zdjęcia.
     *
     * @param id identyfikator zgłoszenia
     */
    @Query("UPDATE pending_submissions SET created_on_server = 1 WHERE id = :id")
    suspend fun markCreated(id: String)

    /**
     * Zaznacza odrzucenie zgłoszenia przez serwer: zostaje w telefonie, ale [all] go nie zwraca (BR-26).
     *
     * @param id identyfikator zgłoszenia
     */
    @Query("UPDATE pending_submissions SET refused = 1 WHERE id = :id")
    suspend fun markRefused(id: String)

    /**
     * Zaznacza zdjęcie wysłane na serwer.
     *
     * @param submissionId identyfikator zgłoszenia
     * @param photoId identyfikator zdjęcia
     */
    @Query("UPDATE pending_photos SET uploaded = 1 WHERE submission_id = :submissionId AND photo_id = :photoId")
    suspend fun markUploaded(submissionId: String, photoId: String)

    /**
     * Usuwa wysłane zgłoszenie razem ze zdjęciami.
     *
     * @param id identyfikator zgłoszenia
     */
    @Query("DELETE FROM pending_submissions WHERE id = :id")
    suspend fun delete(id: String)
}

/** Pojedyncze kroki zapisu w [PendingSubmissionDao.save]; poza nim nie są używane. */
interface PendingSubmissionRows {
    /** Zapis wiersza zgłoszenia; tylko dla [PendingSubmissionDao.save]. */
    @Upsert
    suspend fun upsertSubmission(submission: PendingSubmissionEntity)

    /** Zapis wierszy zdjęć; tylko dla [PendingSubmissionDao.save]. */
    @Upsert
    suspend fun upsertPhotos(photos: List<PendingPhotoEntity>)

    /** Ścieżki plików zdjęć zgłoszenia przed nowym zapisem; tylko dla [PendingSubmissionDao.save]. */
    @Query("SELECT path FROM pending_photos WHERE submission_id = :submissionId")
    suspend fun photoPaths(submissionId: String): List<String>

    /** Usunięcie zdjęć zgłoszenia przed nowym zapisem; tylko dla [PendingSubmissionDao.save]. */
    @Query("DELETE FROM pending_photos WHERE submission_id = :submissionId")
    suspend fun deletePhotos(submissionId: String)
}

/**
 * Zgłoszenie z kolejki razem ze zdjęciami.
 *
 * @property submission zgłoszenie
 * @property photos zdjęcia zgłoszenia
 */
data class PendingSubmissionWithPhotos(
    @Embedded val submission: PendingSubmissionEntity,
    @Relation(parentColumn = "id", entityColumn = "submission_id") val photos: List<PendingPhotoEntity>,
)
