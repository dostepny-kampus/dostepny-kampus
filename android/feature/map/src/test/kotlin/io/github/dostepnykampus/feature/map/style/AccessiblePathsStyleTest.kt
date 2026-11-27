/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.style

import io.github.dostepnykampus.core.designsystem.MapaSciezka
import io.github.dostepnykampus.core.map.style.PATHS_SOURCE
import io.github.dostepnykampus.core.map.style.PATH_EDGE_LAYER
import io.github.dostepnykampus.core.map.style.PATH_INSIDE_LAYER
import io.github.dostepnykampus.core.map.style.PathColors
import io.github.dostepnykampus.core.map.style.basemapNameLayers
import io.github.dostepnykampus.core.map.style.basemapShapeLayers
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.path.AccessiblePath
import io.github.dostepnykampus.feature.map.campus.campusMapData
import io.github.dostepnykampus.feature.map.campus.selection
import io.github.dostepnykampus.feature.map.path.withPaths
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Ścieżki dostosowane na mapie V1: dwie warstwy linii pod budynkami, w barwach i grubościach z tokenów. */
class AccessiblePathsStyleTest {
    private val colors = styleColors().copy(path = PathColors(edge = "#3D7C88", inside = "#EFE2C4"))
    private val data = campusMapData(styleCatalog()).withPaths(listOf(PATH))
    private val style = Json.parseToJsonElement(campusMapStyle(data, data.selection(), colors)).jsonObject

    /** Wstęga ścieżki leży nad kształtami podkładu, pod jego nazwami, budynkami PWr i znakami. Wymagania: US1.4 */
    @Test
    fun drawsPathsUnderNames() {
        val ids = style.getValue("layers").jsonArray.map { it.jsonObject.text("id") }
        val edge = ids.indexOf(PATH_EDGE_LAYER)
        val palette = checkNotNull(colors.basemap)
        val shapes = basemapShapeLayers(palette, minZoom = 0.0).map { it.text("id") }
        val names = basemapNameLayers(palette, minZoom = 0.0, textSize = 1.0).map { it.text("id") }
        assertEquals(ids.indexOf(shapes.last()) + 1, edge, "brzeg zaraz nad kształtami podkładu")
        val next = ids.subList(fromIndex = edge + 1, toIndex = edge + 2 + names.size + 1)
        assertEquals(listOf(PATH_INSIDE_LAYER) + names + BUILDINGS_LAYER, next, "wnętrze, nazwy podkładu, budynki")
    }

    /** Brzeg i wnętrze w barwach `sciezka_brzeg` i `sciezka_wnetrze`, od powiększenia z tokenu. Wymagania: NFR-16 */
    @Test
    fun colorsPathsFromTokens() {
        val layers = listOf(PATH_EDGE_LAYER, PATH_INSIDE_LAYER).map(style::layerById)
        val lineColors = layers.map { it.paintText("line-color") }
        assertEquals(listOf("\"#3D7C88\"", "\"#EFE2C4\""), lineColors, "barwy brzegu i wnętrza")
        val minZooms = layers.map { it.getValue("minzoom").jsonPrimitive.double }
        assertEquals(List(2) { MapaSciezka.odPowiekszenia }, minZooms, "od powiększenia ścieżek")
        assertEquals(List(2) { PATHS_SOURCE }, layers.map { it.text("source") }, "źródło ścieżek")
    }

    /** Szerokość wstęgi i wnętrza rośnie z powiększeniem według progów z tokenów. Wymagania: NFR-16, NFR-21 */
    @Test
    fun widensPathsWithZoom() {
        assertEquals(MapaSciezka.wstegaDp.map { it.zoom to it.dp }, widthStops(PATH_EDGE_LAYER), "szerokość wstęgi")
        assertEquals(MapaSciezka.wnetrzeDp.map { it.zoom to it.dp }, widthStops(PATH_INSIDE_LAYER), "wnętrze")
    }

    /** Szerokość zależy od wyrażenia `["zoom"]`; sam napis `"zoom"` MapLibre odrzuca razem z warstwą. */
    @Test
    fun interpolatesByZoomExpression() {
        val head = Json.parseToJsonElement("""["interpolate",["exponential",$LINE_GROWTH],["zoom"]]""")
        listOf(PATH_EDGE_LAYER, PATH_INSIDE_LAYER).forEach { layer ->
            val width = style.layerById(layer).obj("paint").array("line-width")
            assertEquals(head, JsonArray(width.take(WIDTH_HEAD)), layer)
        }
    }

    /** Odcinek trafia do źródła [PATHS_SOURCE] jako linia z identyfikatorem. Wymagania: US1.4 */
    @Test
    fun putsPathsIntoSource() {
        val source = Json.parseToJsonElement(data.sourceTexts().getValue(PATHS_SOURCE)).jsonObject
        val feature = source.array("features").single().jsonObject
        assertEquals("LineString", feature.obj("geometry").text("type"), "linia")
        assertEquals("p-1", feature.obj("properties").text("id"), "identyfikator odcinka")
    }

    private fun widthStops(layer: String): List<Pair<Double, Double>> {
        val width = style.layerById(layer).obj("paint").array("line-width")
        val values = width.subList(fromIndex = WIDTH_HEAD, toIndex = width.size).map { it.jsonPrimitive.double }
        return values.chunked(2) { (zoom, dp) -> zoom to dp }
    }

    private fun JsonObject.obj(key: String): JsonObject = getValue(key).jsonObject

    private fun JsonObject.array(key: String): JsonArray = getValue(key).jsonArray

    private fun JsonObject.text(key: String): String = getValue(key).jsonPrimitive.content

    private companion object {
        const val WIDTH_HEAD = 3
        val PATH =
            AccessiblePath(
                id = "p-1",
                line = listOf(GeoPoint(longitude = 0.1, latitude = 0.1), GeoPoint(longitude = 0.2, latitude = 0.2)),
            )
    }
}
