/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.inspection

import io.github.dostepnykampus.core.database.inspection.InspectionMarkerDao
import io.github.dostepnykampus.core.database.inspection.InspectionMarkerEntity
import io.github.dostepnykampus.core.model.inspection.InspectionMarker
import io.github.dostepnykampus.core.model.inspection.MarkerPlace
import io.github.dostepnykampus.core.network.ApiResult
import io.github.dostepnykampus.core.network.sync.SyncPull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

/**
 * Kopia znaczników pięter z serwera (`GET /sync/pull`) przechowywana w bazie telefonu.
 *
 * Wymagania: US15.1, US15.2, BR-20
 */
public interface ServerFloorMarkers {
    /**
     * Strumień aktywnych znaczników budynku [buildingSymbol] z serwera; odświeża się po każdym [pull].
     *
     * @param buildingSymbol symbol budynku
     */
    public fun markers(buildingSymbol: String): Flow<List<InspectionMarker>>

    /**
     * Pobiera zmiany znaczników z serwera od ostatniego kursora, zapisuje nowe i usuwa cofnięte.
     *
     * @return `true`, gdy serwer odpowiedział listą zmian; `false` przy braku sieci lub błędzie
     */
    public suspend fun pull(): Boolean
}

/**
 * Tworzy [ServerFloorMarkers] oparte o [dao] i wywołanie [pullChanges] z kursorem `since`.
 *
 * @param dao tabela znaczników pobranych z serwera
 * @param pullChanges odczyt `GET /sync/pull` dla kursora albo `null` przy pierwszym pobraniu
 */
internal fun serverFloorMarkersOf(
    dao: InspectionMarkerDao,
    pullChanges: suspend (since: String?) -> ApiResult<SyncPull>,
): ServerFloorMarkers =
    object : ServerFloorMarkers {
        @Volatile private var cursor: String? = null

        override fun markers(buildingSymbol: String): Flow<List<InspectionMarker>> =
            dao.observe(buildingSymbol).map { rows -> rows.map(InspectionMarkerEntity::toModel) }

        override suspend fun pull(): Boolean {
            val result = pullChanges(cursor)
            if (result !is ApiResult.Success) return false
            val (revoked, active) = result.value.inspectionMarkers.partition { it.isRevoked }
            dao.apply(saved = active.map(InspectionMarker::toEntity), revokedIds = revoked.map(InspectionMarker::id))
            cursor = result.value.cursor
            return true
        }
    }

private fun InspectionMarkerEntity.toModel(): InspectionMarker =
    InspectionMarker(
        id = id,
        buildingSymbol = buildingSymbol,
        place = floorId?.let(MarkerPlace::Floor) ?: MarkerPlace.WholeBuilding,
        createdAt = Instant.ofEpochMilli(createdAtMillis),
        isRevoked = false,
        isMine = isMine,
    )

private fun InspectionMarker.toEntity(): InspectionMarkerEntity =
    InspectionMarkerEntity(
        id = id,
        buildingSymbol = buildingSymbol,
        floorId = (place as? MarkerPlace.Floor)?.floorId,
        createdAtMillis = createdAt.toEpochMilli(),
        isMine = isMine,
    )
