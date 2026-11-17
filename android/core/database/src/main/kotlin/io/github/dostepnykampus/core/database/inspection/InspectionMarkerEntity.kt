/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.database.inspection

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Znacznik piętra obejrzanego pobrany z serwera, w kopii w telefonie.
 *
 * Wymagania: US15.1, US15.2
 *
 * @property id identyfikator UUID znacznika
 * @property buildingSymbol budynek znacznika
 * @property floorId piętro budynku szczegółowego; `null` to znacznik całego budynku podstawowego
 * @property createdAtMillis chwila odbioru znacznika przez serwer w milisekundach od epoki Unix
 * @property isMine czy znacznik postawiła osoba zalogowana w telefonie
 */
@Entity(tableName = "inspection_markers", indices = [Index(value = ["building_symbol"])])
public data class InspectionMarkerEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "building_symbol") val buildingSymbol: String,
    @ColumnInfo(name = "floor_id") val floorId: String?,
    @ColumnInfo(name = "created_at") val createdAtMillis: Long,
    @ColumnInfo(name = "is_mine") val isMine: Boolean,
)
