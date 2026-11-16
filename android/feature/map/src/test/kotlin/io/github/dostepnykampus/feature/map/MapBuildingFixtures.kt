/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.core.model.building.Building
import kotlin.coroutines.EmptyCoroutineContext

internal fun unitSquareBuilding(symbol: String): Building {
    val corners = listOf(0.0 to 0.0, 1.0 to 0.0, 1.0 to 1.0, 0.0 to 0.0)
    val outline = GeoPolygon(listOf(corners.map { (lon, lat) -> GeoPoint(longitude = lon, latitude = lat) }))
    return Building(symbol = symbol, name = "Budynek testowy", outline = outline)
}

internal fun unitSquareCatalog(symbol: String): BuildingCatalogRepository =
    BuildingCatalogRepository(ioContext = EmptyCoroutineContext) {
        """{"type":"FeatureCollection","attribution":"© OpenStreetMap contributors","features":[""" +
            """{"type":"Feature","properties":{"kind":"building","symbols":["$symbol"],"osm_id":"way/1"},""" +
            """"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}}]}"""
    }
