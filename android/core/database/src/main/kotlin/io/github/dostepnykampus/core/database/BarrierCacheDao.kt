/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/** Odczyt i zapis kopii barier ze zgłoszeń przyjętych. Wymagania: NFR-10 */
@Dao
public interface BarrierCacheDao {
    /** Kopia barier albo `null`, gdy serwer dotąd ich nie podał. */
    @Query("SELECT * FROM barrier_cache WHERE id = $BARRIER_CACHE_SINGLE_ROW")
    public suspend fun get(): BarrierCacheEntity?

    /** Zapisuje kopię; nowy zapis zastępuje poprzedni, bo wiersz ma stały identyfikator. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun save(entity: BarrierCacheEntity)
}
