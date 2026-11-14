/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import io.github.dostepnykampus.core.data.server.ServerBuildings
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.core.model.building.Building

private val SERVER_OUTLINE =
    GeoPolygon(
        listOf(
            listOf(
                GeoPoint(longitude = 0.0, latitude = 0.0),
                GeoPoint(longitude = 1.0, latitude = 0.0),
                GeoPoint(longitude = 1.0, latitude = 1.0),
                GeoPoint(longitude = 0.0, latitude = 0.0),
            ),
        ),
    )

internal val SERVER_BUILDING = Building(symbol = "A-1", name = "Z serwera", outline = SERVER_OUTLINE)

internal val SERVER_LIST = ServerBuildings(source = "Serwer", buildings = listOf(SERVER_BUILDING))

internal const val ASSET_JSON =
    """{"type":"FeatureCollection","attribution":"© OpenStreetMap contributors","features":[""" +
        """{"type":"Feature","properties":{"kind":"building","symbols":["A-1"],"display_name":"Z pliku",""" +
        """"osm_id":"way/1"},"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}},""" +
        """{"type":"Feature","properties":{"kind":"building","symbols":["B-2"],"display_name":"Drugi",""" +
        """"osm_id":"way/2"},"geometry":{"type":"Polygon","coordinates":[[[2,0],[3,0],[3,1],[2,0]]]}}]}"""
