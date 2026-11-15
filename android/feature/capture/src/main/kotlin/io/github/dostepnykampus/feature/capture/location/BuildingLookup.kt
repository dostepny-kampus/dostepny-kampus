/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.location

import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.di.ComputeContext
import io.github.dostepnykampus.core.geo.CampusGrounds
import io.github.dostepnykampus.core.geo.PwrGrounds
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.building.Building
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * Budynki z katalogu i teren PWr dla obliczeń położenia w modelach widoku ekranów zgłoszeń, poza wątkiem głównym.
 *
 * Wymagania: BR-G5, US4.9
 *
 * @param catalog katalog budynków z konturami
 * @param computeContext wątek obliczeń położenia
 * @param grounds teren, na którym wolno złożyć zgłoszenie
 */
class BuildingLookup
    @Inject
    constructor(
        private val catalog: BuildingCatalogRepository,
        @ComputeContext private val computeContext: CoroutineContext,
        private val grounds: CampusGrounds = PwrGrounds,
    ) {
        /** Wynik [compute] na bieżących budynkach katalogu, liczony w kontekście obliczeń. */
        internal suspend fun <T> withBuildings(compute: (List<Building>) -> T): T {
            val buildings = currentBuildings()
            return withContext(computeContext) { compute(buildings) }
        }

        /** Czy [point] leży na terenie PWr (wewnątrz granicy albo na jej linii), liczone w kontekście obliczeń. */
        internal suspend fun isOnGrounds(point: GeoPoint): Boolean =
            withContext(computeContext) { grounds.covers(point) }

        private suspend fun currentBuildings(): List<Building> = catalog.catalogUpdates().first().buildings
    }
