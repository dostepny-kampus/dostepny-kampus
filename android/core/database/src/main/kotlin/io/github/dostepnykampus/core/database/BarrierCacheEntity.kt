/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Kopia barier ze zgłoszeń przyjętych pobranych z serwera, do czytania bez sieci.
 *
 * Jeden wiersz o stałym identyfikatorze; nowy zapis zastępuje poprzedni, więc zgłoszenie, które przestało być
 * przyjęte, znika także z kopii.
 *
 * Wymagania: NFR-10
 *
 * @property id identyfikator wiersza, zawsze [BARRIER_CACHE_SINGLE_ROW]
 * @property listJson lista barier jako dokument JSON
 * @property syncedAt chwila pobrania listy w milisekundach epochy
 */
@Entity(tableName = "barrier_cache")
data class BarrierCacheEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "list_json") val listJson: String,
    @ColumnInfo(name = "synced_at") val syncedAt: Long,
)

/** Identyfikator jedynego wiersza kopii barier. */
const val BARRIER_CACHE_SINGLE_ROW = 1
