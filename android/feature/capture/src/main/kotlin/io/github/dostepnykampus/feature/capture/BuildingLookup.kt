/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import io.github.dostepnykampus.core.data.buildings.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.di.ComputeContext
import io.github.dostepnykampus.core.geo.BuildingHints
import io.github.dostepnykampus.core.geo.buildingHints
import io.github.dostepnykampus.core.geo.containingBuilding
import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.core.model.GeoPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/** Obliczenia położenia względem konturów dla ekranów zgłoszeń. */
class BuildingLookup
    @Inject
    constructor(
        private val catalog: BuildingCatalogRepository,
        @ComputeContext private val computeContext: CoroutineContext,
    ) {
        internal suspend fun measure(point: GeoPoint): MeasuredBuildings {
            val buildings = currentBuildings()
            return withContext(computeContext) { MeasuredBuildings(buildings, buildingHints(point, buildings)) }
        }

        internal suspend fun containing(point: GeoPoint): Building? {
            val buildings = currentBuildings()
            return withContext(computeContext) { containingBuilding(point, buildings) }
        }

        private suspend fun currentBuildings(): List<Building> = catalog.catalogUpdates().first().buildings
    }

/**
 * Dane ekranu po zakończeniu pomiaru.
 *
 * @property catalog budynki dostępne przy ręcznym wyborze
 * @property hints dopasowanie obliczone z tego katalogu
 */
internal data class MeasuredBuildings(val catalog: List<Building>, val hints: BuildingHints)
