/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.database.inspection

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Rodzaj zmiany znacznika piętra obejrzanego czekającej w telefonie. Wymagania: US15.3 */
public enum class PendingMarkerChangeType {
    /** Postawienie znacznika. */
    SET,

    /** Cofnięcie znacznika, który serwer już ma. */
    REVOKE,
}

/**
 * Zmiana znacznika piętra obejrzanego zapisana w telefonie do wysłania.
 *
 * Wymagania: US15.3, BR-9, NFR-5
 *
 * @property operationId identyfikator UUID operacji nadany w telefonie; ponowne wysłanie nie powtarza zmiany
 * @property type rodzaj zmiany
 * @property markerId identyfikator UUID znacznika: stawianego albo cofanego
 * @property buildingSymbol budynek znacznika
 * @property floorId piętro budynku szczegółowego; `null` to znacznik całego budynku podstawowego
 * @property ownerSub identyfikator konta; `null` przed przypisaniem po migracji
 * @property savedAtMillis chwila zapisu w telefonie w milisekundach od epoki Unix
 */
@Entity(tableName = "pending_marker_changes")
public data class PendingMarkerChangeEntity(
    @PrimaryKey @ColumnInfo(name = "operation_id") val operationId: String,
    val type: PendingMarkerChangeType,
    @ColumnInfo(name = "marker_id") val markerId: String,
    @ColumnInfo(name = "building_symbol") val buildingSymbol: String,
    @ColumnInfo(name = "floor_id") val floorId: String?,
    @ColumnInfo(name = "saved_at") val savedAtMillis: Long,
    @ColumnInfo(name = "owner_sub") val ownerSub: String? = null,
)
