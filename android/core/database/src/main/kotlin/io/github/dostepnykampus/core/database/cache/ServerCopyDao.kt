/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.database.cache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/** Odczyt i zapis kopii odpowiedzi serwera według klucza. Wymagania: NFR-10 */
@Dao
public interface ServerCopyDao {
    /** Kopia pod kluczem [key] albo `null`, gdy serwer dotąd jej nie podał. */
    @Query("SELECT * FROM server_copy WHERE `key` = :key")
    public suspend fun get(key: String): ServerCopyEntity?

    /** Zapisuje kopię; nowy zapis zastępuje poprzedni pod tym samym kluczem. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun save(entity: ServerCopyEntity)
}
