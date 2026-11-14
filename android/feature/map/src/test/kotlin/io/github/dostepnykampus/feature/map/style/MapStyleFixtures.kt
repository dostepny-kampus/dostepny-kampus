/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.style

import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.core.model.building.Building
import io.github.dostepnykampus.core.model.building.BuildingCatalog

internal val STYLE_ORIGIN = GeoPoint(longitude = 0.0, latitude = 0.0)

internal fun styleCatalog(): BuildingCatalog {
    val corner = STYLE_ORIGIN
    val outline =
        GeoPolygon(listOf(listOf(corner, corner.copy(longitude = 1.0), corner.copy(latitude = 1.0), corner)))
    val building = Building(symbol = "C-13", name = "Budynek testowy", outline = outline)
    return BuildingCatalog(buildings = listOf(building), entrances = emptyList(), attribution = "")
}

internal fun styleColors(): MapStyleColors =
    MapStyleColors(land = "#E8EDEE", building = "#BFE0EC", buildingOutline = "#004E5E", selectedOutline = "#001F27")
