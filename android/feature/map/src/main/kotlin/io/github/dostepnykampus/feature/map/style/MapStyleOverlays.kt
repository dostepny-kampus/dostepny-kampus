/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.style

import io.github.dostepnykampus.core.geo.toPosition
import io.github.dostepnykampus.core.map.style.OVERLAP
import io.github.dostepnykampus.core.map.style.numbers
import io.github.dostepnykampus.core.map.style.property
import io.github.dostepnykampus.core.map.style.symbolLayer
import io.github.dostepnykampus.feature.map.campus.BARRIER_BUILDING
import io.github.dostepnykampus.feature.map.campus.BARRIER_FLOOR
import io.github.dostepnykampus.feature.map.campus.BARRIER_ID
import io.github.dostepnykampus.feature.map.campus.BARRIER_OFFSET
import io.github.dostepnykampus.feature.map.campus.CampusMapData
import io.github.dostepnykampus.feature.map.campus.MapSelection
import io.github.dostepnykampus.feature.map.campus.SelectedEntrance
import io.github.dostepnykampus.feature.map.campus.featureCollection
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.Geometry
import org.maplibre.spatialk.geojson.Point

/** Źródło stylu z punktem zaznaczonym pinezką. Wymagania: US3.3, US1.5 */
internal const val CHOSEN_SOURCE = "wejscie"

private val CAPTION_OFFSET_DP = ENTRANCE_MARK_DP / 2 + 2
internal const val BARRIER_CAPTION_OFFSET = "caption_offset"
internal data class ChosenPoint(val data: JsonObject, val barrierCaptionOffset: Double? = null)

private fun entrancePoint(chosen: SelectedEntrance): JsonObject {
    val properties =
        buildJsonObject {
            put(key = "label", value = chosen.label)
            put(key = "image", value = pointCaptionImage(chosen.label))
        }
    val point = Point(chosen.location.toPosition())
    val features: List<Feature<Geometry, JsonObject?>> = listOf(Feature(point, properties))
    return featureCollection(features).jsonObject.getValue("features").jsonArray.single().jsonObject
}

/** Zaznaczony punkt z [selection] i odpowiadający mu podpis. Wymagania: US3.3, US1.5 */
internal fun CampusMapData.chosenPoint(selection: MapSelection): ChosenPoint {
    val point = selection.entrance?.let { ChosenPoint(entrancePoint(it)) } ?: barrierPoint(selection)
    val collection =
        buildJsonObject {
            put("type", "FeatureCollection")
            put("features", JsonArray(listOfNotNull(point?.data)))
        }
    return ChosenPoint(collection, point?.barrierCaptionOffset)
}

private fun CampusMapData.barrierPoint(selection: MapSelection): ChosenPoint? {
    val id = selection.barrier ?: return null
    val label = selection.barrierCaption ?: return null
    val point =
        barriers.jsonObject.getValue("features").jsonArray.firstOrNull {
            it.jsonObject.getValue("properties").jsonObject[BARRIER_ID]?.jsonPrimitive?.content == id
        }?.jsonObject ?: return null
    val properties = point.getValue("properties").jsonObject
    val floor = properties[BARRIER_FLOOR]?.jsonPrimitive?.content
    val building = properties[BARRIER_BUILDING]?.jsonPrimitive?.content
    val onPlan = selection.plan != null && selection.plan.building == building
    if (onPlan && floor != null && floor != selection.plan?.floor) return null
    val height = if (onPlan && floor == null) BARRIER_NO_FLOOR_DP else BARRIER_SIGN_DP
    val offset = properties[BARRIER_OFFSET]?.jsonArray
    val gap = height / 2.0 + 2
    val x = offset?.get(0)?.jsonPrimitive?.double ?: 0.0
    val y = offset?.get(1)?.jsonPrimitive?.double ?: 0.0
    val caption = numbers(x, y + gap)
    val captionProperties =
        properties + mapOf(
            "label" to JsonPrimitive(label),
            "image" to JsonPrimitive(pointCaptionImage(label)),
            BARRIER_CAPTION_OFFSET to caption,
        )
    return ChosenPoint(JsonObject(point + ("properties" to JsonObject(captionProperties))), gap)
}

/** Warstwy zaznaczenia punktu: pinezka czubkiem w znaku i podpis pod znakiem. Wymagania: US3.3, US1.5 */
internal fun chosenLayers(look: SelectionLook): List<JsonObject> {
    val pin = JsonPrimitive(ENTRANCE_PIN_IMAGE)
    val caption = property("image")
    val below = "icon-anchor" to JsonPrimitive("bottom")
    val above = "icon-anchor" to JsonPrimitive("top")
    return listOf(
        symbolLayer(
            id = ENTRANCE_PIN_LAYER,
            source = CHOSEN_SOURCE,
            image = pin,
            below,
            OVERLAP,
            "icon-offset" to pinOffset(look),
        ),
        symbolLayer(
            id = ENTRANCE_CAPTION_LAYER,
            source = CHOSEN_SOURCE,
            image = caption,
            above,
            OVERLAP,
            "icon-offset" to captionOffset(look),
        ),
    )
}

internal fun pinOffset(look: SelectionLook): JsonElement =
    if (look.chosenPoint.barrierCaptionOffset != null) look.besideEntrance.signOffset() else numbers(0.0, 0.0)

internal fun captionOffset(look: SelectionLook): JsonElement =
    look.chosenPoint.barrierCaptionOffset?.let(look.besideEntrance::captionOffset)
        ?: numbers(0.0, CAPTION_OFFSET_DP.toDouble())
