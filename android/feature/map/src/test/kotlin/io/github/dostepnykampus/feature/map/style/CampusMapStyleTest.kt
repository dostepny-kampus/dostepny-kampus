/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.style

import io.github.dostepnykampus.feature.map.campus.CampusBounds
import io.github.dostepnykampus.feature.map.campus.MapSelection
import io.github.dostepnykampus.feature.map.campus.SelectedEntrance
import io.github.dostepnykampus.feature.map.campus.campusMapData
import io.github.dostepnykampus.feature.map.campus.selection
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

/** Styl mapy V1 z jednym konturem „Budynek testowy” (nie dane PWr). Wymagania: US1, US1.1, NFR-10 */
class CampusMapStyleTest {
    private val catalog = styleCatalog()
    private val colors = styleColors()
    private val data = campusMapData(catalog)
    private val style = Json.parseToJsonElement(styled()).jsonObject

    @Test
    fun drawsOutlinesOverBasemap() {
        val layers = style.array("layers").map { it.jsonObject }
        val below = listOf("lad", "osm", BUILDINGS_LAYER, "kontury") + PLAN + listOf(SELECTED_LAYER)
        val marks = listOf(ENTRANCES_LAYER, BARRIER_WORDS_LAYER, BARRIERS_LAYER, "podpisy", CHOSEN_BARRIER_LAYER)
        val expected = below + marks + listOf(ENTRANCE_PIN_LAYER, ENTRANCE_CAPTION_LAYER)
        assertEquals(expected, layers.map { it.text("id") })
        assertEquals("#E8EDEE", layers.first().obj("paint").text("background-color"))
    }

    @Test
    fun readsOutlinesOffline() {
        val sources = style.obj("sources")
        val tiles = sources.obj("osm").array("tiles")
        assertEquals(OSM_TILES, tiles.single().jsonPrimitive.content)
        val buildings = sources.obj("budynki").obj("data").array("features")
        val properties = buildings.single().jsonObject.obj("properties")
        assertEquals("C-13", properties.text("label"))
    }

    /**
     * Podkład jest przygaszony i prześwituje przez niego tło lądu, więc budynki PWr są najwyraźniejsze; zieleń, woda
     * i ulice zostają rozpoznawalne, bo barwy nie znikają całkiem (makieta hi-fi V1).
     *
     * Wymagania: NFR-16
     */
    @Test
    fun mutesBasemapWithoutGrey() {
        val paint = style.array("layers")[1].jsonObject.obj("paint")
        assertEquals(-0.4, paint.number("raster-saturation"))
        assertEquals(0.72, paint.number("raster-opacity"))
        assertNull(paint["raster-brightness-min"])
    }

    /** W motywie ciemnym jasność kafelków jest odwrócona: najjaśniejsze miejsca kafelka wychodzą najciemniej. */
    @Test
    fun invertsNightBasemap() {
        val night = styled(colors = colors.copy(isNight = true))
        val layers = Json.parseToJsonElement(night).jsonObject.array("layers")
        val paint = layers[1].jsonObject.obj("paint")
        assertTrue(paint.number("raster-brightness-min") > paint.number("raster-brightness-max"))
    }

    /**
     * Wiersz: V1, podkładu spoza prostokąta widoku terenu ani poniżej najmniejszego
     * powiększenia podkładu aplikacja nie pobiera; bez podanego podkładu jest nim prostokąt terenu od powiększenia 0.
     *
     * Wymagania: NFR-39
     */
    @Test
    fun limitsBasemapToTiles() {
        val bounds = CampusBounds(west = 16.9, south = 59.9, east = 17.1, north = 60.1)
        val tiles = CampusBounds(west = 16.8, south = 59.8, east = 17.2, north = 60.2)
        val area = MapSelection(bounds = bounds)
        val basemap = Basemap(bounds = tiles, minZoom = 12.5)
        val text = campusMapStyle(data = data, selection = area, colors = colors, basemap = basemap)
        val style = Json.parseToJsonElement(text).jsonObject
        val plain = Json.parseToJsonElement(styled(area)).jsonObject
        assertEquals(listOf(16.8, 59.8, 17.2, 60.2), style.basemapSource().boundsDegrees(), "w prostokącie podkładu")
        assertEquals(12.5, style.basemapLayer().number("minzoom"), "podkład od najmniejszego powiększenia")
        assertEquals(listOf(16.9, 59.9, 17.1, 60.1), plain.basemapSource().boundsDegrees(), "bez niego w terenie")
        assertEquals(0.0, plain.basemapLayer().number("minzoom"), "bez niego od powiększenia 0")
    }

    @Test
    fun labelsFromPointSource() {
        val layers = style.array("layers").map { it.jsonObject }
        assertEquals("podpisy", layers.single { it.text("id") == "podpisy" }.text("source"))
        val source = style.obj("sources").obj("podpisy")
        val points = source.obj("data").array("features")
        val geometry = points.single().jsonObject.obj("geometry")
        assertEquals("Point", geometry.text("type"))
    }

    @Test
    fun drawsFocusedOutline() {
        val focused = Json.parseToJsonElement(styled(data.selection(building = "C-13"))).jsonObject
        val layers = focused.array("layers")
        val selected = layers.map { it.jsonObject }.single { it.text("id") == SELECTED_LAYER }
        val own = """["case",["any",["in","C-13",["get","symbols"]]],true,false]"""
        assertEquals(own, selected.array("filter").toString())
        assertEquals("#001F27", selected.obj("paint").text("line-color"))
        val plain = style.array("layers").map { it.jsonObject }.single { it.text("id") == SELECTED_LAYER }
        assertEquals("""["!",true]""", plain.getValue("filter").toString())
    }

    /**
     * Warstwy stoją w stałej kolejności, a warstwy zaznaczenia są zawsze, więc zmiana zaznaczenia nie wymaga nowego
     * stylu. Mapa nie ma warstw granicy terenu ani wyblaknięcia poza nią.
     * Scenariusz (częściowo): przegladanie-mapy.feature:
     *   Przy najmniejszym powiększeniu teren wypełnia mapę w krótszym wymiarze
     *
     * Wymagania: US1.7, US3.3
     */
    @Test
    fun ordersOptionalLayers() {
        val bare = Json.parseToJsonElement(styled(MapSelection()))
        val base = listOf("lad", "osm", BUILDINGS_LAYER, "kontury") + PLAN
        val marks = listOf(ENTRANCES_LAYER, BARRIER_WORDS_LAYER, BARRIERS_LAYER, "podpisy", CHOSEN_BARRIER_LAYER)
        val pin = listOf(ENTRANCE_PIN_LAYER, ENTRANCE_CAPTION_LAYER)
        val bareIds = bare.jsonObject.array("layers").map { it.jsonObject.text("id") }
        assertEquals(base + SELECTED_LAYER + marks + pin, bareIds)
        val sources = setOf("osm", "budynki", "podpisy", "wejscia", "bariery", CHOSEN_SOURCE, PLAN_SOURCE)
        assertEquals(sources, bare.jsonObject.obj("sources").keys)
        val entrance = SelectedEntrance(id = "w1", location = STYLE_ORIGIN, label = "Główne", buildingSymbol = "C-13")
        val full = Json.parseToJsonElement(styled(data.selection(building = "C-13", entrance = entrance)))
        val expected = base + SELECTED_LAYER + marks + pin
        assertEquals(expected, full.jsonObject.array("layers").map { it.jsonObject.text("id") })
    }

    private fun styled(selection: MapSelection = data.selection(), colors: MapStyleColors = this.colors): String =
        campusMapStyle(data = data, selection = selection, colors = colors)

    private fun JsonObject.obj(key: String): JsonObject = getValue(key).jsonObject

    private fun JsonObject.basemapSource(): JsonObject = obj("sources").obj("osm")

    private fun JsonObject.basemapLayer(): JsonObject =
        array("layers").map { it.jsonObject }.single { it.text("id") == "osm" }

    private fun JsonObject.boundsDegrees(): List<Double> = array("bounds").map { it.jsonPrimitive.double }

    private fun JsonObject.array(key: String): JsonArray = getValue(key).jsonArray

    private fun JsonObject.number(key: String): Double = getValue(key).jsonPrimitive.double

    private fun JsonObject.text(key: String): String = getValue(key).jsonPrimitive.content
}

private val PLAN =
    listOf(PLAN_ROOMS_LAYER, PLAN_WALLS_LAYER, PLAN_OUTLINE_LAYER, PLAN_SIGNS_LAYER, PLAN_NUMBERS_LAYER)
