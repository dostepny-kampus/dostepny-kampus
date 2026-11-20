/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.style

import io.github.dostepnykampus.core.geo.toPosition
import io.github.dostepnykampus.feature.map.campus.featureCollection
import io.github.dostepnykampus.feature.map.campus.RoomStepMarker
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.Point

/** Warstwa wypełnienia wyróżnionej sali na planie (`p_wybrana`). Wymagania: US18.1 */
internal const val PLAN_CHOSEN_LAYER = "plan_sala_wybrana"

/** Warstwa konturu wyróżnionej sali na planie (`p_wybrana_kontur`). Wymagania: US18.1, NFR-21 */
internal const val PLAN_CHOSEN_OUTLINE_LAYER = "plan_sala_wybrana_kontur"

/** Grubość konturu wyróżnionej sali w dp: grubszy od ściany piętra, więc salę widać nie tylko po kolorze. */
internal const val PLAN_CHOSEN_OUTLINE_WIDTH = 3.0

private const val ROOM_KIND = "room"

/**
 * Warstwy wyróżnionej sali [room] na piętrze [floor] w kolorach [colors]: wypełnienie i kontur. Bez piętra albo bez
 * sali filtry nie przepuszczają niczego.
 *
 * Wymagania: US18.1, NFR-21
 */
internal fun chosenRoomLayers(colors: PlanColors, floor: String?, room: String?): List<JsonObject> {
    val filter = chosenRoomFilter(floor = floor, room = room)
    val fill =
        styleLayer(id = PLAN_CHOSEN_LAYER, type = "fill", source = PLAN_SOURCE) {
            put(key = "filter", element = filter)
            paint("fill-color" to JsonPrimitive(colors.chosen))
        }
    val outline =
        lineLayer(
            id = PLAN_CHOSEN_OUTLINE_LAYER,
            source = PLAN_SOURCE,
            color = colors.chosenOutline,
            width = PLAN_CHOSEN_OUTLINE_WIDTH,
            filter = filter,
        )
    return listOf(fill, outline)
}

/**
 * Pokazuje w stylu z [chosenRoomLayers] salę [room] na piętrze [floor]. Zmienia tylko filtry dwóch warstw,
 * niezależnie od liczby pomieszczeń planu. Wymagania: US18.1, NFR-10
 */
internal fun MapStyleTarget.showChosenRoom(floor: String?, room: String?) {
    val filter = chosenRoomFilter(floor = floor, room = room)
    filter(layer = PLAN_CHOSEN_LAYER, filter = filter)
    filter(layer = PLAN_CHOSEN_OUTLINE_LAYER, filter = filter)
}

/**
 * Filtr pomieszczenia [room] piętra [floor] planu: identyfikator sali z danych trafia do filtra jako literał;
 * bez sali filtr nie przepuszcza niczego.
 */
internal fun chosenRoomFilter(floor: String?, room: String?): JsonElement =
    expression(
        "all",
        planFilter(floor = floor, kind = ROOM_KIND),
        expression(
            "==",
            property(PLAN_ROOM_ID),
            room?.let { expression("literal", JsonPrimitive(it)) } ?: JsonPrimitive(false),
        ),
    )

internal const val ROOM_STEPS_SOURCE = "kroki_sali"
internal const val ROOM_STEPS_LAYER = "plan_kroki_sali"
internal const val ROOM_STEP_INDEX = "step"
private const val ROOM_STEP_IMAGE = "image"
internal const val ROOM_STEP_OFFSET = "room_step_offset"
private const val ROOM_LABEL_CLEARANCE_DP = 30.0

/** Nazwa obrazu dla [index] kroku i jego stanu [selected]. Wymagania: US18.2 */
internal fun roomStepImage(index: Int, selected: Boolean): String = "krok: $index/$selected"

/** Punkty [steps] widoczne na aktualnym piętrze. Wymagania: US18.2 */
internal fun roomStepsFeatures(steps: List<RoomStepMarker>): JsonElement =
    featureCollection(
        steps.map { step ->
            val properties =
                buildJsonObject {
                    put(key = ROOM_STEP_INDEX, value = step.index.toString())
                    put(key = ROOM_STEP_IMAGE, value = roomStepImage(index = step.index, selected = step.selected))
                    if (step.isDestination) {
                        put(key = ROOM_STEP_OFFSET, element = numbers(0.0, -ROOM_LABEL_CLEARANCE_DP))
                    }
                }
            Feature(Point(step.point.toPosition()), properties)
        },
    )

/** Numery kroków rysowane nad znakami planu. Wymagania: US18.2 */
internal fun roomStepsLayer(): JsonObject =
    symbolLayer(
        id = ROOM_STEPS_LAYER,
        source = ROOM_STEPS_SOURCE,
        image = property(ROOM_STEP_IMAGE),
        layout =
            arrayOf(
                "icon-allow-overlap" to JsonPrimitive(true),
                "icon-ignore-placement" to JsonPrimitive(true),
                "icon-offset" to offsetOr(key = ROOM_STEP_OFFSET, fallback = numbers(0.0, 0.0)),
            ),
    )
