/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.style

import io.github.dostepnykampus.core.model.GeoMultiPolygon
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
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
import org.maplibre.spatialk.geojson.LineString
import org.maplibre.spatialk.geojson.Polygon
import org.maplibre.spatialk.geojson.Position

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
        val area = listOf(OUTSIDE_LAYER, EDGE_HALO_LAYER, EDGE_LAYER)
        val marks = listOf(ENTRANCES_LAYER, BARRIER_WORDS_LAYER, BARRIERS_LAYER, "podpisy", CHOSEN_BARRIER_LAYER)
        val expected = below + area + marks + listOf(ENTRANCE_PIN_LAYER, ENTRANCE_CAPTION_LAYER)
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
     * Wiersz: V1, poza terenem na mapę pada kolor tła z kryciem z tokenów, a granicę pokazuje linia z obwódką pod
     * spodem: wielokąt poza terenem ma otwór o brzegach terenu, a obie linie biegną po tych samych brzegach. Bez
     * granicy z pliku brzegiem jest prostokąt terenu.
     * Scenariusz (częściowo): przegladanie-mapy.feature: Mapa nie wychodzi poza wybrany teren
     *
     * Wymagania: US1.7, BR-G5, NFR-16
     */
    @Test
    fun fadesOutsideWithEdgeLine() {
        val bounds = CampusBounds(west = 16.9, south = 59.9, east = 17.1, north = 60.1)
        val look = colors.copy(edge = "#111111", edgeHalo = "#FFFFFF", outside = "#FAFAFA", outsideOpacity = 0.75)
        val styled = Json.parseToJsonElement(styled(MapSelection(bounds = bounds), look)).jsonObject
        val area = styled.obj("sources").obj("teren")
        val features = area.obj("data").array("features").map { it.jsonObject }
        val outsideShape = features.first().obj("geometry")
        val hole = outsideShape.array("coordinates")[1].jsonArray
        val edge = features.last().obj("geometry").array("coordinates")
        val corners = setOf(listOf(16.9, 59.9), listOf(17.1, 59.9), listOf(17.1, 60.1), listOf(16.9, 60.1))
        assertEquals(corners, hole.map { point -> point.jsonArray.map { it.jsonPrimitive.double } }.toSet())
        assertEquals(hole.reversed(), edge.toList(), "Linia biegnie po brzegu otworu")
        val paints = styled.array("layers").map { it.jsonObject }.associate { it.text("id") to it["paint"] }
        val outside = requireNotNull(paints[OUTSIDE_LAYER]).jsonObject
        assertEquals("#FAFAFA", outside.text("fill-color"), "Poza terenem pada kolor tła")
        assertEquals(0.75, outside.number("fill-opacity"), "Krycie z tokenu teren_poza")
        val halo = requireNotNull(paints[EDGE_HALO_LAYER]).jsonObject
        assertEquals("#FFFFFF", halo.text("line-color"), "Obwódka w barwie granica_terenu_obwodka")
        assertEquals(5.0, halo.number("line-width"), "Obwódka 1,5 dp z każdej strony linii 2 dp")
        val line = requireNotNull(paints[EDGE_LAYER]).jsonObject
        assertEquals("#111111", line.text("line-color"), "Linia w barwie granica_terenu")
        assertEquals(2.0, line.number("line-width"), "Linia granicy ma 2 dp")
    }

    /**
     * Wiersz: V1, granica z pliku to wielokąt, także z kilku części i z otworem: każda część wycina otwór
     * w obszarze poza terenem (pierścień przeciwny do pierścienia świata, inaczej MapLibre nie robi otworu), otwór
     * w granicy też leży poza terenem, a linia biegnie po każdym pierścieniu.
     *
     * Wymagania: US1.7, BR-G5
     */
    @Test
    fun cutsEveryBoundaryPart() {
        val bounds = CampusBounds(west = 16.0, south = 59.0, east = 18.0, north = 61.0)
        val outer = square(west = 16.0, south = 59.0, size = 1.0)
        val inner = square(west = 16.4, south = 59.4, size = 0.2)
        val second = square(west = 17.5, south = 60.5, size = 0.5).reversed()
        val boundary = GeoMultiPolygon(listOf(GeoPolygon(listOf(outer, inner)), GeoPolygon(listOf(second))))
        val features = areaFeatures(boundary = boundary, bounds = bounds).map { it.geometry }
        val outside = features.first() as Polygon
        assertEquals(3, outside.coordinates.size, "Świat i otwory obu części")
        assertTrue(outside.coordinates.drop(1).all { signedArea(it) < 0 }, "Otwory biegną zgodnie z zegarem")
        assertTrue(signedArea(outside.coordinates.first()) > 0, "Pierścień świata biegnie przeciwnie do zegara")
        val rest = features.drop(1)
        assertEquals(1, rest.filterIsInstance<Polygon>().size, "Otwór granicy jest osobnym obszarem poza terenem")
        assertEquals(3, rest.filterIsInstance<LineString>().size, "Linia po każdym pierścieniu granicy")
    }

    /**
     * Wiersz: V1, podkładu spoza prostokąta terenu powiększonego o pół widoku mapy ani poniżej najmniejszego
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
     * Warstwy stoją w stałej kolejności. Warstwy zaznaczenia i terenu są zawsze; bez granic źródło terenu jest
     * puste, więc zmiana terenu nie wymaga nowego stylu. Wymagania: US1.7, US3.3, BR-G5
     */
    @Test
    fun ordersOptionalLayers() {
        val bare = Json.parseToJsonElement(styled(MapSelection()))
        val base = listOf("lad", "osm", BUILDINGS_LAYER, "kontury") + PLAN
        val marks = listOf(ENTRANCES_LAYER, BARRIER_WORDS_LAYER, BARRIERS_LAYER, "podpisy", CHOSEN_BARRIER_LAYER)
        val pin = listOf(ENTRANCE_PIN_LAYER, ENTRANCE_CAPTION_LAYER)
        val area = listOf(OUTSIDE_LAYER, EDGE_HALO_LAYER, EDGE_LAYER)
        val bareIds = bare.jsonObject.array("layers").map { it.jsonObject.text("id") }
        assertEquals(base + SELECTED_LAYER + area + marks + pin, bareIds)
        val sources =
            setOf("osm", "budynki", "podpisy", "wejscia", "bariery", AREA_SOURCE, CHOSEN_SOURCE, PLAN_SOURCE)
        val bareSources = bare.jsonObject.obj("sources")
        assertEquals(sources, bareSources.keys)
        val empty = bareSources.obj(AREA_SOURCE).obj("data").array("features")
        assertEquals(0, empty.size, "bez granic teren jest pusty")
        val entrance = SelectedEntrance(id = "w1", location = STYLE_ORIGIN, label = "Główne", buildingSymbol = "C-13")
        val full = Json.parseToJsonElement(styled(data.selection(building = "C-13", entrance = entrance)))
        val expected = base + SELECTED_LAYER + area + marks + pin
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

    private fun square(west: Double, south: Double, size: Double): List<GeoPoint> =
        listOf(west to south, west + size to south, west + size to south + size, west to south + size, west to south)
            .map { (longitude, latitude) -> GeoPoint(longitude = longitude, latitude = latitude) }

    private fun signedArea(ring: List<Position>): Double =
        ring.zipWithNext().sumOf { (from, to) -> from.longitude * to.latitude - to.longitude * from.latitude }
}

private val PLAN =
    listOf(PLAN_ROOMS_LAYER, PLAN_WALLS_LAYER, PLAN_OUTLINE_LAYER, PLAN_SIGNS_LAYER, PLAN_NUMBERS_LAYER)
