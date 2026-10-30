/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.core.model.BuildingCatalog
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.feature.map.data.campusMapData
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Styl mapy V1 z jednym konturem „Budynek testowy” (nie dane PWr). */
class CampusMapStyleTest {
    private val a = GeoPoint(longitude = 0.0, latitude = 0.0)
    private val outline = GeoPolygon(listOf(listOf(a, a.copy(longitude = 1.0), a.copy(latitude = 1.0), a)))
    private val building = Building(symbol = "C-13", name = "Budynek testowy", outline = outline)
    private val catalog = BuildingCatalog(buildings = listOf(building), entrances = emptyList(), attribution = "")
    private val colors = MapStyleColors(land = "#E8EDEE", building = "#BFE0EC", buildingOutline = "#004E5E")
    private val style =
        Json.parseToJsonElement(campusMapStyle(data = campusMapData(catalog), colors = colors)).jsonObject

    @Test
    fun nfr39_outlines_over_basemap() {
        val layers = style.array("layers").map { it.jsonObject }
        assertEquals(listOf("lad", "osm", BUILDINGS_LAYER, "kontury", "podpisy"), layers.map { it.text("id") })
        assertEquals("#E8EDEE", layers.first().obj("paint").text("background-color"))
    }

    @Test
    fun nfr10_outlines_offline_source() {
        val sources = style.obj("sources")
        val tiles = sources.obj("osm").array("tiles")
        assertEquals(OSM_TILES, tiles.single().jsonPrimitive.content)
        val buildings = sources.obj("budynki").obj("data").array("features")
        val properties = buildings.single().jsonObject.obj("properties")
        assertEquals("C-13", properties.text("label"))
    }

    private fun JsonObject.obj(key: String): JsonObject = getValue(key).jsonObject

    private fun JsonObject.array(key: String): JsonArray = getValue(key).jsonArray

    private fun JsonObject.text(key: String): String = getValue(key).jsonPrimitive.content
}
