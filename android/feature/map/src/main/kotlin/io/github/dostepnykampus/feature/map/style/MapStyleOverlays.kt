/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.style

import io.github.dostepnykampus.core.geo.toPosition
import io.github.dostepnykampus.core.map.style.OVERLAP
import io.github.dostepnykampus.core.map.style.expression
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
import io.github.dostepnykampus.feature.map.campus.ShownPlan
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
private const val FEATURES = "features"
private const val PROPERTIES = "properties"
private const val LABEL = "label"
private const val IMAGE = "image"
private const val ICON_OFFSET = "icon-offset"
private const val LITERAL = "literal"
internal const val BARRIER_CAPTION_OFFSET = "caption_offset"

internal data class ChosenPoint(val data: JsonObject, val barrierCaptionOffset: Double? = null)

private fun entrancePoint(chosen: SelectedEntrance): JsonObject {
    val properties =
        buildJsonObject {
            put(key = LABEL, value = chosen.label)
            put(key = IMAGE, value = pointCaptionImage(chosen.label))
        }
    val point = Point(chosen.location.toPosition())
    val features: List<Feature<Geometry, JsonObject?>> = listOf(Feature(point, properties))
    return featureCollection(features).points().single().jsonObject
}

/** Zaznaczony punkt z [selection] i odpowiadający mu podpis. Wymagania: US3.3, US1.5 */
internal fun CampusMapData.chosenPoint(selection: MapSelection): ChosenPoint {
    val point = selection.entrance?.let { ChosenPoint(entrancePoint(it)) } ?: barrierPoint(selection)
    val collection =
        buildJsonObject {
            put("type", "FeatureCollection")
            put(FEATURES, JsonArray(listOfNotNull(point?.data)))
        }
    return ChosenPoint(collection, point?.barrierCaptionOffset)
}

private fun CampusMapData.barrierPoint(selection: MapSelection): ChosenPoint? {
    val id = selection.barrier
    val label = selection.barrierCaption
    if (id == null || label == null) return null
    val point = barriers.points().firstOrNull { it.jsonObject.properties()[BARRIER_ID] == JsonPrimitive(id) }
    return point?.jsonObject?.withCaption(label, selection.plan)
}

private fun JsonElement.points(): JsonArray = jsonObject.getValue(FEATURES).jsonArray

private fun JsonObject.properties(): JsonObject = getValue(PROPERTIES).jsonObject

private fun JsonObject.withCaption(label: String, plan: ShownPlan?): ChosenPoint? {
    val properties = properties()
    val floor = properties[BARRIER_FLOOR]?.jsonPrimitive?.content
    val building = properties[BARRIER_BUILDING]?.jsonPrimitive?.content
    val onPlan = plan != null && plan.building == building
    if (onPlan && floor != null && floor != plan.floor) return null
    val height = if (onPlan && floor == null) BARRIER_NO_FLOOR_DP else BARRIER_SIGN_DP
    val offset = properties[BARRIER_OFFSET]?.jsonArray
    val gap = height / 2.0 + 2
    val x = offset?.get(0)?.jsonPrimitive?.double ?: 0.0
    val y = offset?.get(1)?.jsonPrimitive?.double ?: 0.0
    val caption = numbers(x, y + gap)
    val captionProperties = buildJsonObject {
        properties.forEach { (key, value) -> put(key, value) }
        put(LABEL, label)
        put(IMAGE, pointCaptionImage(label))
        put(BARRIER_CAPTION_OFFSET, caption)
    }
    return ChosenPoint(JsonObject(this + (PROPERTIES to captionProperties)), gap)
}

/** Warstwy zaznaczenia punktu: pinezka czubkiem w znaku i podpis pod znakiem. Wymagania: US3.3, US1.5 */
internal fun chosenLayers(look: SelectionLook): List<JsonObject> {
    val pin = JsonPrimitive(ENTRANCE_PIN_IMAGE)
    val caption = property(IMAGE)
    val below = "icon-anchor" to JsonPrimitive("bottom")
    val above = "icon-anchor" to JsonPrimitive("top")
    return listOf(
        symbolLayer(
            id = ENTRANCE_PIN_LAYER,
            source = CHOSEN_SOURCE,
            image = pin,
            below,
            OVERLAP,
            ICON_OFFSET to pinOffset(look),
        ),
        symbolLayer(
            id = ENTRANCE_CAPTION_LAYER,
            source = CHOSEN_SOURCE,
            image = caption,
            above,
            OVERLAP,
            ICON_OFFSET to captionOffset(look),
        ),
    )
}

internal fun pinOffset(look: SelectionLook): JsonElement =
    if (look.chosenPoint.barrierCaptionOffset != null) {
        look.besideEntrance.signOffset()
    } else {
        expression(LITERAL, numbers(0.0, 0.0))
    }

internal fun captionOffset(look: SelectionLook): JsonElement =
    look.chosenPoint.barrierCaptionOffset?.let(look.besideEntrance::captionOffset)
        ?: expression(LITERAL, numbers(0.0, CAPTION_OFFSET_DP.toDouble()))
