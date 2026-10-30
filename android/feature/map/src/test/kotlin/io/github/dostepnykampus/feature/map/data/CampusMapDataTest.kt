/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.data

import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.core.model.BuildingCatalog
import io.github.dostepnykampus.core.model.Entrance
import io.github.dostepnykampus.core.model.EntranceMarking
import io.github.dostepnykampus.core.model.EntranceMarking.ACCESSIBLE
import io.github.dostepnykampus.core.model.EntranceMarking.INACCESSIBLE
import io.github.dostepnykampus.core.model.EntranceMarking.NO_DATA
import io.github.dostepnykampus.core.model.GeoMultiPolygon
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Geometry
import org.maplibre.spatialk.geojson.MultiPolygon
import org.maplibre.spatialk.geojson.Polygon

/** Warstwy mapy V1 z syntetycznego katalogu „Budynek testowy” (nie dane PWr). */
class CampusMapDataTest {
    private val c3 = Building(symbol = "C-3", name = null, outline = square(x = 0.0))
    private val c4 = c3.copy(symbol = "C-4")
    private val c13 =
        Building(
            symbol = "C-13",
            name = "Budynek testowy",
            outline = GeoMultiPolygon(listOf(square(x = 2.0), square(x = 4.0))),
        )
    private val data =
        campusMapData(
            BuildingCatalog(
                buildings = listOf(c3, c4, c13),
                entrances =
                    listOf(
                        entrance(id = "node/1", building = "C-3", marking = NO_DATA, isMain = true),
                        entrance(id = "node/1", building = "C-4", marking = NO_DATA, isMain = true),
                        entrance(id = "node/2", building = "C-13", marking = ACCESSIBLE, isMain = false),
                        entrance(id = "node/3", building = "C-13", marking = INACCESSIBLE, isMain = true),
                    ),
                attribution = "© OpenStreetMap contributors",
            ),
        )

    @Test
    fun us1_outline_with_symbol() {
        val buildings = parse(data.buildings)
        assertEquals(listOf("C-3", "C-4", "C-13"), buildings.map { it.properties.text("symbol") })
        assertEquals(listOf(true, true, false), buildings.map { it.geometry is Polygon })
        assertEquals(2, (buildings.last().geometry as? MultiPolygon)?.run { coordinates.size })
    }

    @Test
    fun us1_4_shared_entrance_once() {
        val entrances = parse(data.entrances)
        assertEquals(listOf("NO_DATA", "ACCESSIBLE", "INACCESSIBLE"), entrances.map { it.properties.text("marking") })
        assertEquals(listOf("true", "false", "true"), entrances.map { it.properties.text("main") })
    }

    @Test
    fun us1_bounds_cover_all_outlines() {
        val expected =
            GeoBounds(
                southWest = GeoPoint(longitude = 0.0, latitude = 0.0),
                northEast = GeoPoint(longitude = 5.0, latitude = 1.0),
            )
        assertEquals(expected, data.bounds)
    }

    @Test
    fun us1_empty_catalog_no_bounds() {
        val empty = campusMapData(BuildingCatalog(buildings = emptyList(), entrances = emptyList(), attribution = ""))
        assertEquals(null, empty.bounds)
        assertEquals(0, parse(empty.buildings).size)
    }

    private fun parse(json: String) = FeatureCollection.fromJson<Geometry?, JsonObject?>(json).features

    private fun JsonObject?.text(key: String): String? = this?.get(key)?.run { jsonPrimitive.content }

    private fun square(x: Double): GeoPolygon =
        GeoPolygon(
            listOf(
                listOf(0.0 to 0.0, 1.0 to 0.0, 1.0 to 1.0, 0.0 to 1.0, 0.0 to 0.0).map { (dx, y) ->
                    GeoPoint(longitude = x + dx, latitude = y)
                },
            ),
        )

    private fun entrance(id: String, building: String, marking: EntranceMarking, isMain: Boolean): Entrance =
        Entrance(
            id = id,
            buildingSymbol = building,
            name = null,
            isMain = isMain,
            type = if (isMain) "main" else "yes",
            location = GeoPoint(longitude = 0.5, latitude = 0.0),
            marking = marking,
        )
}
