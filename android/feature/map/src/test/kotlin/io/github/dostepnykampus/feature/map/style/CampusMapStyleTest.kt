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
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
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
        val below = listOf("lad") + BASEMAP_LAYERS + listOf(BUILDINGS_LAYER, "kontury") + PLAN + listOf(SELECTED_LAYER)
        val marks = listOf(ENTRANCES_LAYER, BARRIER_WORDS_LAYER, BARRIERS_LAYER, "podpisy", CHOSEN_BARRIER_LAYER)
        val expected = below + marks + listOf(ENTRANCE_PIN_LAYER, ENTRANCE_CAPTION_LAYER)
        assertEquals(expected, layers.map { it.text("id") })
        assertEquals("#E8EDEE", layers.first().obj("paint").text("background-color"))
    }

    @Test
    fun readsOutlinesOffline() {
        val sources = style.obj("sources")
        assertEquals(BASEMAP_TILES, sources.obj(BASEMAP).text("url"), "podkład wektorowy OpenFreeMap")
        assertEquals("vector", sources.obj(BASEMAP).text("type"), "kafelki wektorowe")
        val buildings = sources.obj("budynki").obj("data").array("features")
        val properties = buildings.single().jsonObject.obj("properties")
        assertEquals("C-13", properties.text("label"))
    }

    /**
     * Podkład ma barwy tokenów: zieleń, woda i budynki miasta, a budynki miasta są dopiero od powiększenia 13. Mapa nie
     * ma punktów usług ani numerów domów, więc nic nie zasłania budynków PWr (ADR-0014).
     *
     * Wymagania: NFR-16, US1.4
     */
    @Test
    fun colorsBasemapFromTokens() {
        val layers = style.array("layers").map { it.jsonObject }.associateBy { it.text("id") }
        assertEquals("#CBE2C6", layers.getValue("zielen").obj("paint").text("fill-color"), "zieleń z tokenu")
        assertEquals("#A7CADD", layers.getValue("woda").obj("paint").text("fill-color"), "woda z tokenu")
        assertEquals(13.0, layers.getValue("budynki_miasta").number("minzoom"), "budynki miasta od powiększenia 13")
        val basemap = layers.values.filter { it["source"] == JsonPrimitive(BASEMAP) }
        val used = basemap.map { it.text("source-layer") }
        val allowed =
            setOf(
                "landcover",
                "park",
                "water",
                "waterway",
                "transportation",
                "building",
                "transportation_name",
                "place",
            )
        assertEquals(allowed, used.toSet(), "bez punktów usług i numerów domów")
    }

    /**
     * Wiersz: V1, podkładu poniżej najmniejszego powiększenia podkładu terenu aplikacja nie rysuje ani nie pobiera;
     * warstwy z własnym progiem, np. budynki miasta, zostają przy nim. Bez podanego podkładu warstwy są od progu
     * własnego albo od powiększenia 0.
     *
     * Wymagania: NFR-39
     */
    @Test
    fun hidesBasemapBelowArea() {
        val area = MapSelection(bounds = CampusBounds(west = 16.9, south = 59.9, east = 17.1, north = 60.1))
        val text = campusMapStyle(data = data, selection = area, colors = colors, basemap = Basemap(minZoom = 14.5))
        val zooms = Json.parseToJsonElement(text).jsonObject.basemapZooms()
        val plain = Json.parseToJsonElement(styled(area)).jsonObject.basemapZooms()
        assertEquals(14.5, zooms.getValue("woda"), "woda od powiększenia terenu")
        assertEquals(15.0, zooms.getValue("chodniki"), "chodniki od własnego progu")
        assertEquals(0.0, plain.getValue("woda"), "bez podkładu terenu od powiększenia 0")
        assertEquals(13.0, plain.getValue("budynki_miasta"), "budynki miasta od własnego progu")
    }

    /**
     * Napisy podkładu: glify z OpenFreeMap, nazwy ulic od powiększenia 15, rzeka kursywą, dzielnice wielkimi
     * literami; kolory z tokenów, a wysokość tekstu z rozmiaru napisów mapy, który rośnie ze skalą czcionki.
     *
     * Wymagania: NFR-16, US1.4
     */
    @Test
    fun namesStreetsRiverAndDistricts() {
        val large = Json.parseToJsonElement(styled(colors = colors.copy(labelSize = 22.4))).jsonObject
        val layers = large.array("layers").map { it.jsonObject }.associateBy { it.text("id") }
        val names = listOf("nazwy_rzek", "nazwy_ulic", "dzielnice").map(layers::getValue)
        assertEquals(BASEMAP_GLYPHS, large.text("glyphs"), "glify OpenFreeMap")
        assertEquals(listOf(22.4, 22.4, 22.4), names.map { it.obj("layout").number("text-size") }, "rozmiar napisów")
        val paints = names.map { it.obj("paint").text("text-color") }
        assertEquals(listOf("#2B454D", "#3A4549", "#4E5A5E"), paints, "kolory napisów z tokenów")
        assertEquals("#FFFFFF", names.first().obj("paint").text("text-halo-color"), "obwódka z tokenu")
        assertEquals(15.0, layers.getValue("nazwy_ulic").number("minzoom"), "nazwy ulic od powiększenia 15")
        assertEquals("uppercase", layers.getValue("dzielnice").obj("layout").text("text-transform"), "dzielnice")
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
        val base = listOf("lad") + BASEMAP_LAYERS + listOf(BUILDINGS_LAYER, "kontury") + PLAN
        val marks = listOf(ENTRANCES_LAYER, BARRIER_WORDS_LAYER, BARRIERS_LAYER, "podpisy", CHOSEN_BARRIER_LAYER)
        val pin = listOf(ENTRANCE_PIN_LAYER, ENTRANCE_CAPTION_LAYER)
        val bareIds = bare.jsonObject.array("layers").map { it.jsonObject.text("id") }
        assertEquals(base + SELECTED_LAYER + marks + pin, bareIds)
        val sources = setOf(BASEMAP, "budynki", "podpisy", "wejscia", "bariery", CHOSEN_SOURCE, PLAN_SOURCE)
        assertEquals(sources, bare.jsonObject.obj("sources").keys)
        val entrance = SelectedEntrance(id = "w1", location = STYLE_ORIGIN, label = "Główne", buildingSymbol = "C-13")
        val full = Json.parseToJsonElement(styled(data.selection(building = "C-13", entrance = entrance)))
        val expected = base + SELECTED_LAYER + marks + pin
        assertEquals(expected, full.jsonObject.array("layers").map { it.jsonObject.text("id") })
    }

    private fun styled(selection: MapSelection = data.selection(), colors: MapStyleColors = this.colors): String =
        campusMapStyle(data = data, selection = selection, colors = colors)

    private fun JsonObject.obj(key: String): JsonObject = getValue(key).jsonObject

    private fun JsonObject.basemapZooms(): Map<String, Double> =
        array("layers")
            .map { it.jsonObject }
            .filter { it.text("id") in BASEMAP_LAYERS }
            .associate { it.text("id") to it.number("minzoom") }

    private fun JsonObject.array(key: String): JsonArray = getValue(key).jsonArray

    private fun JsonObject.number(key: String): Double = getValue(key).jsonPrimitive.double

    private fun JsonObject.text(key: String): String = getValue(key).jsonPrimitive.content
}

private val PLAN =
    listOf(PLAN_ROOMS_LAYER, PLAN_WALLS_LAYER, PLAN_OUTLINE_LAYER, PLAN_SIGNS_LAYER, PLAN_NUMBERS_LAYER)
