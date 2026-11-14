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
 * Kopia listy budynków pobranej z serwera, do czytania bez sieci.
 *
 * Jeden wiersz o stałym identyfikatorze; nowy zapis zastępuje poprzedni.
 *
 * Wymagania: NFR-10
 *
 * @property id identyfikator wiersza, zawsze [BUILDING_CACHE_SINGLE_ROW]
 * @property listJson lista budynków jako dokument JSON zgodny z listą z serwera
 * @property syncedAt chwila pobrania listy w milisekundach epochy
 */
@Entity(tableName = "building_cache")
public data class BuildingCacheEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "list_json") val listJson: String,
    @ColumnInfo(name = "synced_at") val syncedAt: Long,
)

/** Identyfikator jedynego wiersza kopii listy budynków. */
public const val BUILDING_CACHE_SINGLE_ROW: Int = 1
