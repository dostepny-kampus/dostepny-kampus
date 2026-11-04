/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
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
interface PendingSubmissionDao {
    /**
     * Zapisuje zgłoszenie razem ze zdjęciami. Ten sam identyfikator zastępuje poprzedni zapis.
     *
     * @param submission zgłoszenie
     * @param photos zdjęcia zgłoszenia
     */
    @Transaction
    suspend fun save(submission: PendingSubmissionEntity, photos: List<PendingPhotoEntity>) {
        upsertSubmission(submission)
        deletePhotos(submission.id)
        upsertPhotos(photos)
    }

    /** Zgłoszenia czekające na wysłanie, od najstarszego; zmienia się po każdym zapisie. */
    @Transaction
    @Query("SELECT * FROM pending_submissions ORDER BY saved_at, id")
    fun observe(): Flow<List<PendingSubmissionWithPhotos>>

    /** Zgłoszenia czekające na wysłanie, od najstarszego. */
    @Transaction
    @Query("SELECT * FROM pending_submissions ORDER BY saved_at, id")
    suspend fun all(): List<PendingSubmissionWithPhotos>

    /**
     * Zaznacza, że serwer ma już zgłoszenie, a czekają tylko zdjęcia.
     *
     * @param id identyfikator zgłoszenia
     */
    @Query("UPDATE pending_submissions SET created_on_server = 1 WHERE id = :id")
    suspend fun markCreated(id: String)

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

    /** Zapis wiersza zgłoszenia; tylko dla [save]. */
    @Upsert
    suspend fun upsertSubmission(submission: PendingSubmissionEntity)

    /** Zapis wierszy zdjęć; tylko dla [save]. */
    @Upsert
    suspend fun upsertPhotos(photos: List<PendingPhotoEntity>)

    /** Usunięcie zdjęć zgłoszenia przed nowym zapisem; tylko dla [save]. */
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
