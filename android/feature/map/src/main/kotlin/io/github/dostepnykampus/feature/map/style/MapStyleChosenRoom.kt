/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.style

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

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
