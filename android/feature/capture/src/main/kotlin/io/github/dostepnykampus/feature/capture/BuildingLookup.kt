/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import io.github.dostepnykampus.core.geo.BuildingHints
import io.github.dostepnykampus.core.geo.buildingHints
import io.github.dostepnykampus.core.geo.containingBuilding
import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.core.model.GeoPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/** Obliczenia położenia względem konturów dla ekranów zgłoszeń. */
class BuildingLookup(private val computeContext: CoroutineContext) {
    @Inject
    constructor() : this(Dispatchers.Default)

    internal suspend fun hints(point: GeoPoint, buildings: List<Building>): BuildingHints =
        withContext(computeContext) { buildingHints(point, buildings) }

    internal suspend fun containing(point: GeoPoint, buildings: List<Building>): Building? =
        withContext(computeContext) { containingBuilding(point, buildings) }
}
