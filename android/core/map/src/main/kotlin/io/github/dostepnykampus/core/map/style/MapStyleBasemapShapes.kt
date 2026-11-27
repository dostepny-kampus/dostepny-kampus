/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.map.style

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.put

private const val SOURCE_LAYER = "source-layer"
private const val STREET_ZOOM_NEAR = 16.0
private const val CLOSE_ZOOM = 19.0

/** Podstawa wykładniczego wzrostu grubości linii między progami powiększenia: ulice i ścieżki dostosowane. */
public const val LINE_GROWTH: Double = 1.6

private const val RIVER_NEAR_WIDTH = 4.0
private const val PATH_NEAR_WIDTH = 4.0
private const val RAIL_WIDTH = 1.5
private val ROAD_CLASSES = listOf("motorway", "trunk", "primary", "secondary", "tertiary", "minor", "service")
private val MINOR_ROADS = listOf("minor", "service")

/**
 * Grubość ulicy osiedlowej [minor] i głównej [major] przy jednym powiększeniu.
 *
 * @param minor grubość ulic klas `minor` i `service` w dp
 * @param major grubość pozostałych ulic w dp
 */
public class RoadWidth(private val minor: Double, private val major: Double) {
    /** Grubość zależna od klasy ulicy. */
    public fun byClass(): JsonArray =
        expression(
            "match",
            property(CLASS),
            JsonArray(MINOR_ROADS.map { JsonPrimitive(it) }),
            JsonPrimitive(minor),
            JsonPrimitive(major),
        )
}

/** Grubość obrzeża ulic przy powiększeniu miasta, ulicy i budynku: szersza od wypełnienia o obrzeże. */
public val CASING_WIDTHS: List<RoadWidth>
    get() =
        listOf(
            RoadWidth(minor = 2.3, major = 3.0),
            RoadWidth(minor = 5.0, major = 7.0),
            RoadWidth(minor = 18.0, major = 28.0),
        )

/** Grubość wypełnienia ulic przy powiększeniu miasta, ulicy i budynku. */
public val ROAD_WIDTHS: List<RoadWidth>
    get() =
        listOf(
            RoadWidth(minor = 0.3, major = 1.0),
            RoadWidth(minor = 3.0, major = 5.0),
            RoadWidth(minor = 16.0, major = 26.0),
        )

/** Wypełnienie obszarów warstwy [layer] źródła podkładu barwą [color]; [filter] zawęża je do klas. */
public fun basemapFill(id: String, layer: String, color: String, filter: JsonElement? = null): JsonObject =
    styleLayer(id, type = "fill", source = BASEMAP) {
        put(key = SOURCE_LAYER, value = layer)
        filter?.let { put(key = "filter", element = it) }
        paint("fill-color" to JsonPrimitive(color))
    }

/** Linie warstwy [layer] źródła podkładu barwą [color] i grubością [width]. */
public fun basemapLine(id: String, layer: String, color: String, width: JsonElement): JsonObject =
    styleLayer(id, type = "line", source = BASEMAP) {
        put(key = SOURCE_LAYER, value = layer)
        paint("line-color" to JsonPrimitive(color), "line-width" to width)
    }

/** Ulice barwą [color], z zaokrąglonymi końcami i grubością [widths] przy kolejnych powiększeniach. */
public fun basemapRoads(id: String, color: String, widths: List<RoadWidth>): JsonObject {
    val line = basemapLine(id = id, layer = TRANSPORTATION, color = color, width = roadWidth(widths))
    val round = JsonObject(mapOf("line-join" to JsonPrimitive("round"), "line-cap" to JsonPrimitive("round")))
    return JsonObject(line + ("filter" to classIn(ROAD_CLASSES)) + ("layout" to round))
}

/** Budynki spoza PWr z konturem, w barwach `budynek` i `budynek_kontur`. */
public fun cityBuildings(id: String, colors: BasemapColors): JsonObject =
    styleLayer(id, type = "fill", source = BASEMAP) {
        put(key = SOURCE_LAYER, value = "building")
        val outline = "fill-outline-color" to JsonPrimitive(colors.buildingOutline)
        paint("fill-color" to JsonPrimitive(colors.building), outline)
    }

/** Grubość rzeki: 1 dp przy powiększeniu miasta, 4 dp przy powiększeniu budynku. */
public fun riverWidth(): JsonArray = linear(fromZoom = CITY_ZOOM, toZoom = CLOSE_ZOOM, toWidth = RIVER_NEAR_WIDTH)

/** Grubość chodników: 1 dp od ich powiększenia, 4 dp przy powiększeniu budynku. */
public fun pathWidth(): JsonArray = linear(fromZoom = STREET_ZOOM, toZoom = CLOSE_ZOOM, toWidth = PATH_NEAR_WIDTH)

/** Grubość torów w dp. */
public fun railWidth(): JsonPrimitive = JsonPrimitive(RAIL_WIDTH)

private fun linear(fromZoom: Double, toZoom: Double, toWidth: Double): JsonArray =
    expression(
        "interpolate",
        expression("linear"),
        expression("zoom"),
        JsonPrimitive(fromZoom),
        JsonPrimitive(1.0),
        JsonPrimitive(toZoom),
        JsonPrimitive(toWidth),
    )

private fun roadWidth(widths: List<RoadWidth>): JsonArray {
    val stops =
        listOf(CITY_ZOOM, STREET_ZOOM_NEAR, CLOSE_ZOOM).zip(widths).flatMap { (zoom, width) ->
            listOf(JsonPrimitive(zoom), width.byClass())
        }
    return growingWidth(stops)
}

/** Szerokość linii rosnąca wykładniczo o [LINE_GROWTH] między progami [stops]: na przemian powiększenie i szerokość. */
public fun growingWidth(stops: List<JsonElement>): JsonArray {
    val growth = expression("exponential", JsonPrimitive(LINE_GROWTH))
    val head = listOf(JsonPrimitive("interpolate"), growth, expression("zoom"))
    return JsonArray(head + stops)
}
