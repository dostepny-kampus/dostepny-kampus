/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.inspection

import io.github.dostepnykampus.core.database.inspection.PendingMarkerChangeDao
import io.github.dostepnykampus.core.database.inspection.PendingMarkerChangeEntity
import io.github.dostepnykampus.core.database.inspection.PendingMarkerChangeType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.io.IOException
import java.util.UUID

/**
 * Zmiana znacznika piętra obejrzanego, która czeka w telefonie na wysłanie.
 *
 * @property markerId identyfikator UUID znacznika
 * @property buildingSymbol budynek znacznika
 * @property floorId piętro budynku szczegółowego; `null` to znacznik całego budynku podstawowego
 * @property isRevoke `true` dla cofnięcia znacznika, `false` dla postawienia
 */
public data class PendingMarkerChange(
    val markerId: String,
    val buildingSymbol: String,
    val floorId: String?,
    val isRevoke: Boolean,
)

/**
 * Znaczniki piętra obejrzanego zapisywane w telefonie. Każda zmiana trafia najpierw do kolejki w bazie telefonu
 * i czeka na synchronizację, więc działa bez sieci, a postęp liczony na serwerze zmienia się dopiero po wysyłce.
 *
 * Wymagania: US15.1, US15.2, US15.3, BR-9, BR-20
 */
public interface FloorMarkers {
    /** Zmiany czekające na wysłanie w kolejności zapisu, najstarsza pierwsza; strumień odświeża się po zapisie. */
    public fun pending(): Flow<List<PendingMarkerChange>>

    /**
     * Zapisuje w telefonie znacznik piętra obejrzanego.
     *
     * @param buildingSymbol budynek znacznika
     * @param floorId piętro budynku szczegółowego; `null` oznacza cały budynek podstawowy
     * @return identyfikator UUID nowego znacznika, potrzebny do jego cofnięcia
     */
    public suspend fun mark(buildingSymbol: String, floorId: String?): String

    /**
     * Zapisuje w telefonie cofnięcie znacznika. Znacznik, którego postawienie jeszcze czeka w kolejce, znika z niej
     * bez śladu, bo serwer go nie zna.
     *
     * @param markerId identyfikator cofanego znacznika
     * @param buildingSymbol budynek znacznika
     * @param floorId piętro znacznika; `null` dla znacznika całego budynku
     */
    public suspend fun revoke(markerId: String, buildingSymbol: String, floorId: String?)
}

/**
 * Znaczniki z kolejką [queue] w bazie telefonu.
 *
 * @param queue kolejka zmian znaczników
 * @param subjects identyfikator zalogowanego konta
 * @param newId źródło identyfikatorów UUID znacznika i operacji
 * @param nowMillis zegar w milisekundach od epoki Unix
 * @param onSaved wywołanie po każdym zapisie zmiany, np. zaplanowanie wysyłki
 */
internal fun floorMarkersOf(
    queue: PendingMarkerChangeDao,
    subjects: Flow<String?>,
    newId: () -> String = { UUID.randomUUID().toString() },
    nowMillis: () -> Long = System::currentTimeMillis,
    onSaved: () -> Unit = {},
): FloorMarkers =
    object : FloorMarkers {
        override fun pending(): Flow<List<PendingMarkerChange>> =
            combine(queue.observe(), subjects) { rows, owner ->
                rows.filter { owner != null && it.ownerSub == owner }.map { it.toPendingMarkerChange() }
            }

        override suspend fun mark(buildingSymbol: String, floorId: String?): String {
            val markerId = newId()
            queue.save(Marker(markerId, buildingSymbol, floorId).change(PendingMarkerChangeType.SET))
            onSaved()
            return markerId
        }

        override suspend fun revoke(markerId: String, buildingSymbol: String, floorId: String?) {
            queue.saveRevoke(Marker(markerId, buildingSymbol, floorId).change(PendingMarkerChangeType.REVOKE))
            onSaved()
        }

        private suspend fun Marker.change(type: PendingMarkerChangeType): PendingMarkerChangeEntity =
            PendingMarkerChangeEntity(
                ownerSub = subjects.first() ?: throw IOException("Brak zalogowanego konta"),
                operationId = newId(),
                type = type,
                markerId = markerId,
                buildingSymbol = buildingSymbol,
                floorId = floorId,
                savedAtMillis = nowMillis(),
            )
    }

internal fun PendingMarkerChangeEntity.toPendingMarkerChange(): PendingMarkerChange =
    PendingMarkerChange(
        markerId = markerId,
        buildingSymbol = buildingSymbol,
        floorId = floorId,
        isRevoke = type == PendingMarkerChangeType.REVOKE,
    )

private data class Marker(val markerId: String, val buildingSymbol: String, val floorId: String?)
