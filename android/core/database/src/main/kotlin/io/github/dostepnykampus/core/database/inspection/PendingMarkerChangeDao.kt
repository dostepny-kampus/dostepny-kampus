/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.database.inspection

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** Kolejka zmian znaczników piętra obejrzanego czekających na wysłanie. Wymagania: US15.3, BR-9, NFR-5 */
@Dao
public interface PendingMarkerChangeDao {
    /** Zmiany w kolejności zapisu, najstarsza pierwsza; strumień odświeża się po każdym zapisie. */
    @Query("SELECT * FROM pending_marker_changes ORDER BY saved_at, operation_id")
    public fun observe(): Flow<List<PendingMarkerChangeEntity>>

    /** Zapisuje zmianę; ponowny zapis tej samej operacji zastępuje poprzedni. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun save(change: PendingMarkerChangeEntity)

    /**
     * Zapisuje cofnięcie znacznika w jednej transakcji. Znacznik, którego postawienie jeszcze czeka w kolejce, nie
     * dotarł na serwer, więc cofnięcie usuwa to postawienie i samo do kolejki nie trafia.
     *
     * @param revoke cofnięcie znacznika
     */
    @Transaction
    public suspend fun saveRevoke(revoke: PendingMarkerChangeEntity) {
        if (deleteSet(revoke.markerId) == 0) save(revoke)
    }

    /**
     * Usuwa z kolejki postawienie znacznika.
     *
     * @param markerId identyfikator znacznika
     * @return liczba usuniętych wierszy: 1, gdy postawienie czekało, inaczej 0
     */
    @Query("DELETE FROM pending_marker_changes WHERE marker_id = :markerId AND type = 'SET'")
    public suspend fun deleteSet(markerId: String): Int

    /** Usuwa zmianę [operationId] po odbiorze przez serwer. */
    @Query("DELETE FROM pending_marker_changes WHERE operation_id = :operationId")
    public suspend fun delete(operationId: String)
}
