/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.style

import io.github.dostepnykampus.core.geo.toPosition
import io.github.dostepnykampus.feature.map.campus.SelectedEntrance
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.Geometry
import org.maplibre.spatialk.geojson.Point

/** Źródło stylu z punktem zaznaczonego wejścia. Wymagania: US3.3 */
internal const val CHOSEN_SOURCE = "wejscie"

private val CAPTION_OFFSET_DP = ENTRANCE_MARK_DP / 2 + 2

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
