/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/** Odczyt i zapis kopii listy budynków z serwera. Wymagania: NFR-10 */
@Dao
interface BuildingCacheDao {
    /** Kopia listy albo `null`, gdy serwer dotąd nie odpowiedział. */
    @Query("SELECT * FROM building_cache WHERE id = $BUILDING_CACHE_SINGLE_ROW")
    suspend fun get(): BuildingCacheEntity?

    /** Zapisuje kopię; nowy zapis zastępuje poprzedni, bo wiersz ma stały identyfikator. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entity: BuildingCacheEntity)
}
