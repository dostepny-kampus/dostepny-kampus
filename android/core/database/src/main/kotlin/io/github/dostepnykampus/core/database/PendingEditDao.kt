/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction

/** Odczyt jednego zgłoszenia z kolejki do poprawki w C2; zapis zostaje w [PendingSubmissionDao]. Wymagania: US8.3 */
@Dao
interface PendingEditDao {
    /**
     * Zgłoszenie z kolejki razem ze zdjęciami, także odrzucone.
     *
     * Wymagania: BR-26
     *
     * @param id identyfikator zgłoszenia
     * @return zgłoszenie albo `null`, gdy telefon już go nie ma, np. po wysłaniu
     */
    @Transaction
    @Query("SELECT * FROM pending_submissions WHERE id = :id")
    suspend fun find(id: String): PendingSubmissionWithPhotos?
}
