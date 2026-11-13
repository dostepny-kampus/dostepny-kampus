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
import io.github.dostepnykampus.feature.map.data.CampusBounds
import io.github.dostepnykampus.feature.map.data.MapSelection
import io.github.dostepnykampus.feature.map.data.SelectedEntrance
import io.github.dostepnykampus.feature.map.data.campusMapData
import io.github.dostepnykampus.feature.map.data.selection
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Styl mapy V1 z jednym konturem „Budynek testowy” (nie dane PWr). */
class CampusMapStyleTest {
    private val a = GeoPoint(longitude = 0.0, latitude = 0.0)
    private val outline = GeoPolygon(listOf(listOf(a, a.copy(longitude = 1.0), a.copy(latitude = 1.0), a)))
    private val building = Building(symbol = "C-13", name = "Budynek testowy", outline = outline)
    private val catalog = BuildingCatalog(buildings = listOf(building), entrances = emptyList(), attribution = "")
    private val colors =
        MapStyleColors(land = "#E8EDEE", building = "#BFE0EC", buildingOutline = "#004E5E", selectedOutline = "#001F27")
    private val data = campusMapData(catalog)
    private val style = Json.parseToJsonElement(styled()).jsonObject

    @Test
    fun nfr39_outlines_over_basemap() {
        val layers = style.array("layers").map { it.jsonObject }
        val below = listOf("lad", "osm", BUILDINGS_LAYER, "kontury", SELECTED_LAYER, ENTRANCES_LAYER)
        val barriers = listOf(BARRIER_WORDS_LAYER, BARRIERS_LAYER)
        val above = listOf("podpisy", CHOSEN_BARRIER_LAYER, OUTSIDE_LAYER, EDGE_LAYER)
        val expected = below + barriers + above + listOf(ENTRANCE_PIN_LAYER, ENTRANCE_CAPTION_LAYER)
        assertEquals(expected, layers.map { it.text("id") })
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

    /**
     * Podkład jest przygaszony i prześwituje przez niego tło lądu, więc budynki PWr są najwyraźniejsze; zieleń, woda
     * i ulice zostają rozpoznawalne, bo barwy nie znikają całkiem (makieta hi-fi V1, NFR-16).
     */
    @Test
    fun nfr16_basemap_muted_not_grey() {
        val paint = style.array("layers")[1].jsonObject.obj("paint")
        assertEquals(-0.4, paint.number("raster-saturation"))
        assertEquals(0.72, paint.number("raster-opacity"))
        assertNull(paint["raster-brightness-min"])
    }

    /** W motywie ciemnym jasność kafelków jest odwrócona: najjaśniejsze miejsca kafelka wychodzą najciemniej. */
    @Test
    fun nfr16_night_basemap_inverted() {
        val night = styled(colors = colors.copy(isNight = true))
        val layers = Json.parseToJsonElement(night).jsonObject.array("layers")
        val paint = layers[1].jsonObject.obj("paint")
        assertTrue(paint.number("raster-brightness-min") > paint.number("raster-brightness-max"))
    }

    /**
     * Wiersz: V1, obszar poza terenem jest przyciemniony, a granicę pokazuje linia (US1.7, BR-G5): wielokąt
     * przyciemnienia ma otwór o brzegach terenu, a linia biegnie po tych samych brzegach.
     */
    @Test
    fun us1_7_outside_dimmed_edge_line() {
        val bounds = CampusBounds(west = 16.9, south = 59.9, east = 17.1, north = 60.1)
        val json = styled(MapSelection(bounds = bounds), colors.copy(ink = "#111111"))
        val styled = Json.parseToJsonElement(json).jsonObject
        val area = styled.obj("sources").obj("teren")
        val listed = area.obj("data").array("features")
        val features = listed.map { it.jsonObject }
        val outside = features.first().obj("geometry")
        val hole = outside.array("coordinates")[1].jsonArray
        val line = features.last().obj("geometry")
        val edge = line.array("coordinates")
        val corners = setOf(listOf(16.9, 59.9), listOf(17.1, 59.9), listOf(17.1, 60.1), listOf(16.9, 60.1))
        assertEquals(corners, hole.map { point -> point.jsonArray.map { it.jsonPrimitive.double } }.toSet())
        assertEquals(hole.reversed(), edge.toList())
        val layers = styled.array("layers").map { it.jsonObject }
        assertEquals(0.5, layers.single { it.text("id") == OUTSIDE_LAYER }.obj("paint").number("fill-opacity"))
        assertEquals("#111111", layers.single { it.text("id") == EDGE_LAYER }.obj("paint").text("line-color"))
    }

    /** Wiersz: V1, mapa zostaje na obszarze kampusu; podkładu spoza tego prostokąta aplikacja nie pobiera (NFR-39). */
    @Test
    fun nfr39_basemap_only_campus_area() {
        val bounds = CampusBounds(west = 16.9, south = 59.9, east = 17.1, north = 60.1)
        val bounded = Json.parseToJsonElement(styled(MapSelection(bounds = bounds))).jsonObject
        val osm = bounded.obj("sources").obj("osm")
        assertEquals(listOf(16.9, 59.9, 17.1, 60.1), osm["bounds"]?.run { jsonArray.map { it.jsonPrimitive.double } })
    }

    @Test
    fun us1_labels_from_point_source() {
        val layers = style.array("layers").map { it.jsonObject }
        assertEquals("podpisy", layers.single { it.text("id") == "podpisy" }.text("source"))
        val source = style.obj("sources").obj("podpisy")
        val points = source.obj("data").array("features")
        val geometry = points.single().jsonObject.obj("geometry")
        assertEquals("Point", geometry.text("type"))
    }

    @Test
    fun us1_1_focused_outline() {
        val focused = Json.parseToJsonElement(styled(data.selection(building = "C-13"))).jsonObject
        val layers = focused.array("layers")
        val selected = layers.map { it.jsonObject }.single { it.text("id") == SELECTED_LAYER }
        val own = """["case",["any",["in","C-13",["get","symbols"]]],true,false]"""
        assertEquals(own, selected.array("filter").toString())
        assertEquals("#001F27", selected.obj("paint").text("line-color"))
        val plain = style.array("layers").map { it.jsonObject }.single { it.text("id") == SELECTED_LAYER }
        assertEquals("false", plain.getValue("filter").toString())
    }

    /**
     * Warstwy stoją w stałej kolejności. Warstwy zaznaczenia są zawsze, a warstw terenu nie ma bez granic
     * (US1.7, US3.3).
     */
    @Test
    fun us1_7_optional_layer_order() {
        val bare = Json.parseToJsonElement(styled(MapSelection()))
        val base = listOf("lad", "osm", BUILDINGS_LAYER, "kontury")
        val marks = listOf(ENTRANCES_LAYER, BARRIER_WORDS_LAYER, BARRIERS_LAYER, "podpisy", CHOSEN_BARRIER_LAYER)
        val pin = listOf(ENTRANCE_PIN_LAYER, ENTRANCE_CAPTION_LAYER)
        val bareIds = bare.jsonObject.array("layers").map { it.jsonObject.text("id") }
        assertEquals(base + SELECTED_LAYER + marks + pin, bareIds)
        val sources = setOf("osm", "budynki", "podpisy", "wejscia", "bariery", CHOSEN_SOURCE)
        assertEquals(sources, bare.jsonObject.obj("sources").keys)
        val entrance = SelectedEntrance(id = "w1", location = a, label = "Główne", buildingSymbol = "C-13")
        val full = Json.parseToJsonElement(styled(data.selection(building = "C-13", entrance = entrance)))
        val expected = base + SELECTED_LAYER + marks + listOf(OUTSIDE_LAYER, EDGE_LAYER) + pin
        assertEquals(expected, full.jsonObject.array("layers").map { it.jsonObject.text("id") })
    }

    private fun styled(selection: MapSelection = data.selection(), colors: MapStyleColors = this.colors): String =
        campusMapStyle(data = data, selection = selection, colors = colors)

    private fun JsonObject.obj(key: String): JsonObject = getValue(key).jsonObject

    private fun JsonObject.array(key: String): JsonArray = getValue(key).jsonArray

    private fun JsonObject.number(key: String): Double = getValue(key).jsonPrimitive.double

    private fun JsonObject.text(key: String): String = getValue(key).jsonPrimitive.content
}
