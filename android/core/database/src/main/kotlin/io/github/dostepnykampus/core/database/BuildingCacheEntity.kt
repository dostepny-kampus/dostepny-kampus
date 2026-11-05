/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Kopia listy budynków pobranej z serwera, do czytania bez sieci (NFR-10).
 *
 * Jeden wiersz o stałym identyfikatorze; nowy zapis zastępuje poprzedni.
 *
 * @property id identyfikator wiersza, zawsze [BUILDING_CACHE_SINGLE_ROW]
 * @property listJson lista budynków jako dokument JSON zgodny z listą z serwera (NFR-10)
 * @property syncedAt chwila pobrania listy w milisekundach epochy
 */
@Entity(tableName = "building_cache")
data class BuildingCacheEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "list_json") val listJson: String,
    @ColumnInfo(name = "synced_at") val syncedAt: Long,
)

/** Identyfikator jedynego wiersza kopii listy budynków. */
const val BUILDING_CACHE_SINGLE_ROW = 1
