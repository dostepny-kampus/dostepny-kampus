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

/** Kolejka zgłoszeń zapisanych w telefonie. Wymagania: NFR-5, US9.1, US9.2 */
@Dao
public interface PendingSubmissionDao : PendingSubmissionRows {
    /**
     * Zapisuje zgłoszenie razem ze zdjęciami. Ten sam identyfikator zastępuje poprzedni zapis.
     *
     * @param submission zgłoszenie
     * @param photos zdjęcia zgłoszenia
     * @return ścieżki plików zdjęć poprzedniego zapisu, których nowy zapis już nie ma; pliki można usunąć
     */
    @Transaction
    public suspend fun save(submission: PendingSubmissionEntity, photos: List<PendingPhotoEntity>): List<String> {
        val previous = photoPaths(submission.id)
        upsertSubmission(submission)
        deletePhotos(submission.id)
        upsertPhotos(photos)
        return previous - photos.map { it.path }.toSet()
    }

    /**
     * [writes] jako jedna transakcja bazy, razem z zapisami historii [OwnSubmissionDao]: błąd albo zabicie procesu
     * w środku cofa wszystkie, więc kolejka i historia nie zostają w sprzeczności.
     */
    @Transaction
    public suspend fun together(writes: suspend () -> Unit) {
        writes()
    }

    /** Zgłoszenia czekające na wysłanie, bez odrzuconych, od najstarszego; zmienia się po każdym zapisie. */
    @Transaction
    @Query("SELECT * FROM pending_submissions WHERE refused = 0 ORDER BY saved_at, id")
    public fun observe(): Flow<List<PendingSubmissionWithPhotos>>

    /** Zgłoszenia czekające na wysłanie, bez odrzuconych, od najstarszego. */
    @Transaction
    @Query("SELECT * FROM pending_submissions WHERE refused = 0 ORDER BY saved_at, id")
    public suspend fun all(): List<PendingSubmissionWithPhotos>

    /**
     * Zaznacza, że serwer ma już zgłoszenie, a czekają tylko zdjęcia.
     *
     * @param id identyfikator zgłoszenia
     */
    @Query("UPDATE pending_submissions SET created_on_server = 1 WHERE id = :id")
    public suspend fun markCreated(id: String)

    /**
     * Zaznacza odrzucenie zgłoszenia przez serwer: zostaje w telefonie, ale [all] go nie zwraca.
     *
     * Wymagania: BR-26
     *
     * @param id identyfikator zgłoszenia
     */
    @Query("UPDATE pending_submissions SET refused = 1 WHERE id = :id")
    public suspend fun markRefused(id: String)

    /**
     * Zaznacza zdjęcie wysłane na serwer.
     *
     * @param submissionId identyfikator zgłoszenia
     * @param photoId identyfikator zdjęcia
     */
    @Query("UPDATE pending_photos SET uploaded = 1 WHERE submission_id = :submissionId AND photo_id = :photoId")
    public suspend fun markUploaded(submissionId: String, photoId: String)

    /**
     * Usuwa wysłane zgłoszenie razem ze zdjęciami.
     *
     * @param id identyfikator zgłoszenia
     */
    @Query("DELETE FROM pending_submissions WHERE id = :id")
    public suspend fun delete(id: String)
}

/** Pojedyncze kroki zapisu w [PendingSubmissionDao.save]; poza nim nie są używane. */
public interface PendingSubmissionRows {
    /** Ścieżki plików wszystkich zdjęć w kolejce, także zgłoszeń odrzuconych, do sprzątania przy starcie. */
    @Query("SELECT path FROM pending_photos")
    public suspend fun allPhotoPaths(): List<String>

    /** Zapis wiersza zgłoszenia; tylko dla [PendingSubmissionDao.save]. */
    @Upsert
    public suspend fun upsertSubmission(submission: PendingSubmissionEntity)

    /** Zapis wierszy zdjęć; tylko dla [PendingSubmissionDao.save]. */
    @Upsert
    public suspend fun upsertPhotos(photos: List<PendingPhotoEntity>)

    /** Ścieżki plików zdjęć zgłoszenia przed nowym zapisem; tylko dla [PendingSubmissionDao.save]. */
    @Query("SELECT path FROM pending_photos WHERE submission_id = :submissionId")
    public suspend fun photoPaths(submissionId: String): List<String>

    /** Usunięcie zdjęć zgłoszenia przed nowym zapisem; tylko dla [PendingSubmissionDao.save]. */
    @Query("DELETE FROM pending_photos WHERE submission_id = :submissionId")
    public suspend fun deletePhotos(submissionId: String)
}

/**
 * Zgłoszenie z kolejki razem ze zdjęciami.
 *
 * @property submission zgłoszenie
 * @property photos zdjęcia zgłoszenia
 */
public data class PendingSubmissionWithPhotos(
    @Embedded val submission: PendingSubmissionEntity,
    @Relation(parentColumn = "id", entityColumn = "submission_id") val photos: List<PendingPhotoEntity>,
)
