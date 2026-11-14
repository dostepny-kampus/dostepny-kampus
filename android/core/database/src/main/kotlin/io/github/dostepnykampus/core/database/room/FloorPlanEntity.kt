/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.database.room

import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * Kopia piętra i jego planu pobranych z serwera, do rysowania bez sieci.
 *
 * Wymagania: US16.1, NFR-10, NFR-40
 *
 * @property building symbol budynku
 * @property floor identyfikator piętra
 * @property label etykieta z danych PWr, np. „3 piętro”
 * @property order kolejność w budynku; większa liczba to wyższe piętro
 * @property source źródło planu do podpisu, np. „Politechnika Wrocławska”
 * @property outlineJson obrys piętra jako GeoJSON `MultiPolygon`; bez wielokątów, gdy piętro nie ma pomieszczeń
 * @property syncedAt chwila pobrania planu w milisekundach epochy
 */
@Entity(tableName = "floor_plans", primaryKeys = ["building_symbol", "floor_id"])
public data class FloorPlanEntity(
    @ColumnInfo(name = "building_symbol") val building: String,
    @ColumnInfo(name = "floor_id") val floor: String,
    val label: String,
    @ColumnInfo(name = "sort_order") val order: Int,
    val source: String,
    @ColumnInfo(name = "outline_json") val outlineJson: String,
    @ColumnInfo(name = "synced_at") val syncedAt: Long,
)
