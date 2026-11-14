/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.location

import io.github.dostepnykampus.core.data.buildings.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.di.ComputeContext
import io.github.dostepnykampus.core.model.Building
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/** Budynki z katalogu dla obliczeń położenia w modelach widoku ekranów zgłoszeń, poza wątkiem głównym. */
class BuildingLookup
    @Inject
    constructor(
        private val catalog: BuildingCatalogRepository,
        @ComputeContext private val computeContext: CoroutineContext,
    ) {
        /** Wynik [compute] na bieżących budynkach katalogu, liczony w kontekście obliczeń. */
        internal suspend fun <T> withBuildings(compute: (List<Building>) -> T): T {
            val buildings = currentBuildings()
            return withContext(computeContext) { compute(buildings) }
        }

        private suspend fun currentBuildings(): List<Building> = catalog.catalogUpdates().first().buildings
    }
