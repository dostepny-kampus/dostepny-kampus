/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.database.room

import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * Kopia pomieszczenia z planu piętra; grupę, punkt podpisu i znacznik podpisu policzył serwer.
 *
 * Wymagania: US16.1, NFR-10
 *
 * @property building symbol budynku
 * @property floor identyfikator piętra
 * @property id identyfikator pomieszczenia
 * @property number numer z danych PWr; `null`, gdy plan go nie podaje
 * @property function funkcja z danych PWr
 * @property category nazwa grupy funkcji z kontraktu API, np. `ELEVATOR`
 * @property geometryJson kontur pomieszczenia jako GeoJSON `MultiPolygon`
 * @property labelLongitude długość geograficzna punktu podpisu
 * @property labelLatitude szerokość geograficzna punktu podpisu
 * @property isLabelled czy numer jest podpisany na planie
 */
@Entity(tableName = "floor_rooms", primaryKeys = ["building_symbol", "floor_id", "id"])
public data class FloorRoomEntity(
    @ColumnInfo(name = "building_symbol") val building: String,
    @ColumnInfo(name = "floor_id") val floor: String,
    val id: String,
    val number: String?,
    val function: String,
    val category: String,
    @ColumnInfo(name = "geometry_json") val geometryJson: String,
    @ColumnInfo(name = "label_longitude") val labelLongitude: Double,
    @ColumnInfo(name = "label_latitude") val labelLatitude: Double,
    @ColumnInfo(name = "labelled") val isLabelled: Boolean,
)
