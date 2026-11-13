/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import io.github.dostepnykampus.core.geo.toPosition
import io.github.dostepnykampus.feature.map.data.CampusBounds
import io.github.dostepnykampus.feature.map.data.SelectedEntrance
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.Geometry
import org.maplibre.spatialk.geojson.LineString
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Polygon
import org.maplibre.spatialk.geojson.Position

/** Źródło stylu z geometrią terenu: przyciemnienie i linia granicy. Wymagania: US1.7, BR-G5 */
internal const val AREA_SOURCE = "teren"

/** Źródło stylu z punktem zaznaczonego wejścia. Wymagania: US3.3 */
internal const val CHOSEN_SOURCE = "wejscie"

private const val CAPTION_OFFSET_DP = ENTRANCE_MARK_DP / 2 + 2
private const val EDGE_WIDTH = 2.0
private const val DAY_DIM = "#3B4950"
private const val DAY_DIM_OPACITY = 0.5
private const val NIGHT_DIM = "#000000"
private const val NIGHT_DIM_OPACITY = 0.6
private val WORLD_RING = listOf(-180.0 to -85.0, 180.0 to -85.0, 180.0 to 85.0, -180.0 to 85.0, -180.0 to -85.0)

/** Geometria terenu [bounds]: wielokąt świata z otworem o brzegach terenu i linia po tych brzegach. Wymagania: US1.7 */
internal fun areaFeatures(bounds: CampusBounds): List<Feature<Geometry, JsonObject?>> {
    val corners = with(bounds) { listOf(west to south, east to south, east to north, west to north, west to south) }
    val edge = corners.map { (longitude, latitude) -> Position(longitude = longitude, latitude = latitude) }
    val world = WORLD_RING.map { (longitude, latitude) -> Position(longitude = longitude, latitude = latitude) }
    return listOf(Feature(Polygon(listOf(world, edge.reversed())), null), Feature(LineString(edge), null))
}

/** Warstwy terenu: przyciemnienie poza granicą, osobne dla motywu ciemnego, i linia granicy. Wymagania: US1.7, BR-G5 */
internal fun areaLayers(colors: MapStyleColors): List<JsonObject> {
    val shape = expression("geometry-type")
    val outside =
        styleLayer(id = OUTSIDE_LAYER, type = "fill", source = AREA_SOURCE) {
            put(key = "filter", element = expression("==", shape, JsonPrimitive("Polygon")))
            paint(
                "fill-color" to JsonPrimitive(if (colors.isNight) NIGHT_DIM else DAY_DIM),
                "fill-opacity" to JsonPrimitive(if (colors.isNight) NIGHT_DIM_OPACITY else DAY_DIM_OPACITY),
            )
        }
    val line = expression("==", shape, JsonPrimitive("LineString"))
    val edge = lineLayer(id = EDGE_LAYER, source = AREA_SOURCE, color = colors.ink, width = EDGE_WIDTH, filter = line)
    return listOf(outside, edge)
}

/** Punkt zaznaczonego wejścia [chosen] z nazwą i obrazem podpisu. Wymagania: US3.3 */
internal fun chosenFeatures(chosen: SelectedEntrance): List<Feature<Geometry, JsonObject?>> {
    val properties =
        buildJsonObject {
            put(key = "label", value = chosen.label)
            put(key = "image", value = entranceCaptionImage(chosen.label))
        }
    val point = Point(chosen.location.toPosition())
    return listOf(Feature(point, properties))
}

/** Warstwy zaznaczonego wejścia: pinezka czubkiem w punkcie i podpis pod znakiem. Wymagania: US3.3 */
internal fun chosenLayers(): List<JsonObject> {
    val pin = JsonPrimitive(ENTRANCE_PIN_IMAGE)
    val caption = property("image")
    val below = "icon-anchor" to JsonPrimitive("bottom")
    val above = "icon-anchor" to JsonPrimitive("top")
    val offset = "icon-offset" to numbers(0.0, CAPTION_OFFSET_DP.toDouble())
    return listOf(
        symbolLayer(id = ENTRANCE_PIN_LAYER, source = CHOSEN_SOURCE, image = pin, below, OVERLAP),
        symbolLayer(id = ENTRANCE_CAPTION_LAYER, source = CHOSEN_SOURCE, image = caption, above, offset, OVERLAP),
    )
}
