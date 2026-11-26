/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.database.inspection

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** Kopia znaczników piętra obejrzanego z serwera. Wymagania: US15.1, US15.2 */
@Dao
public interface InspectionMarkerDao {
    /**
     * Znaczniki budynku [buildingSymbol]; strumień odświeża się po każdej zmianie tabeli.
     *
     * @param buildingSymbol budynek znaczników
     */
    @Query("SELECT * FROM inspection_markers WHERE building_symbol = :buildingSymbol ORDER BY created_at, id")
    public fun observe(buildingSymbol: String): Flow<List<InspectionMarkerEntity>>

    /**
     * Stosuje zmiany z serwera w jednej transakcji: zapisuje [saved] (znacznik o tym samym `id` zastępuje poprzedni),
     * potem usuwa [revokedIds].
     *
     * @param saved znaczniki do zapisania
     * @param revokedIds identyfikatory znaczników do usunięcia
     */
    @Transaction
    public suspend fun apply(saved: List<InspectionMarkerEntity>, revokedIds: List<String>) {
        upsert(saved)
        delete(revokedIds)
    }

    /** Usuwa wszystkie znaczniki. */
    @Query("DELETE FROM inspection_markers")
    public suspend fun clear()

    /** Zapisuje [markers]; znacznik o tym samym `id` zastępuje poprzedni. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun upsert(markers: List<InspectionMarkerEntity>)

    /** Usuwa znaczniki o identyfikatorach [ids]. */
    @Query("DELETE FROM inspection_markers WHERE id IN (:ids)")
    public suspend fun delete(ids: List<String>)
}
