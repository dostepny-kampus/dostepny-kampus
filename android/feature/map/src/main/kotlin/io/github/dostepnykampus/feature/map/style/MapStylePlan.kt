/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.style

import io.github.dostepnykampus.core.geo.toGeometry
import io.github.dostepnykampus.core.model.GeoMultiPolygon
import io.github.dostepnykampus.core.model.room.RoomCategory
import io.github.dostepnykampus.core.model.room.RoomList
import io.github.dostepnykampus.feature.map.campus.featureCollection
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.maplibre.spatialk.geojson.Feature

/** Źródło stylu z pomieszczeniami i ścianami wszystkich pięter budynku z planem. Wymagania: US16.1 */
internal const val PLAN_SOURCE = "plan"

/** Warstwa wypełnienia pomieszczeń wybranego piętra kolorem grupy funkcji. Wymagania: US16.1 */
internal const val PLAN_ROOMS_LAYER = "plan_pomieszczenia"

/** Warstwa ścian pomieszczeń wybranego piętra. Wymagania: US16.1 */
internal const val PLAN_WALLS_LAYER = "plan_sciany"

/** Warstwa ściany piętra z obrysu sumy jego pomieszczeń (R11). Wymagania: US16.1 */
internal const val PLAN_OUTLINE_LAYER = "plan_obrys"

private const val FLOOR = "floor"
private const val KIND = "kind"
private const val CATEGORY = "category"
private const val ROOM = "room"
private const val OUTLINE = "outline"
private const val WALL_WIDTH = 1.0
private const val OUTLINE_WIDTH = 3.0

/**
 * Kolory planu piętra z tokenów `mapa.kolory` jako `#RRGGBB`.
 *
 * @property classroom sala i sala wykładowa (`p_sala`)
 * @property corridor korytarz (`p_korytarz`)
 * @property other klatka schodowa, biuro i reszta (`p_inne`)
 * @property elevator winda (`p_winda`)
 * @property toilet toaleta (`p_wc`)
 * @property wall ściany pomieszczeń i piętra (`p_kontur`)
 */
data class PlanColors(
    val classroom: String,
    val corridor: String,
    val other: String,
    val elevator: String,
    val toilet: String,
    val wall: String,
)

/**
 * Obiekty planu pięter [floors] (identyfikator piętra → jego plan): pomieszczenie z właściwościami `floor`, `kind`
 * = `room` i `category`, ściana piętra z `floor` i `kind` = `outline`; piętro bez pomieszczeń nie ma ściany.
 *
 * Wymagania: US16.1
 */
internal fun planFeatures(floors: Map<String, RoomList>): JsonElement =
    featureCollection(
        floors.flatMap { (floor, plan) ->
            val walls = plan.floorOutline.takeUnless { it is GeoMultiPolygon && it.polygons.isEmpty() }
            val outline = listOfNotNull(walls?.let { Feature(it.toGeometry(), part(floor, OUTLINE)) })
            outline + plan.rooms.map { room -> Feature(room.geometry.toGeometry(), part(floor, ROOM, room.category)) }
        },
    )

/**
 * Warstwy planu w kolorach [colors]: pomieszczenia według grupy funkcji, ich ściany i grubsza ściana piętra.
 * Filtry nie przepuszczają niczego, dopóki widok mapy nie wybierze piętra.
 *
 * TODO(#1476): pokazanie pobranego planu i wybranego piętra w widoku mapy.
 *
 * Wymagania: US16.1, NFR-16
 */
internal fun planLayers(colors: PlanColors): List<JsonObject> {
    val none = planFilter(floor = null, kind = ROOM)
    val fill =
        styleLayer(id = PLAN_ROOMS_LAYER, type = "fill", source = PLAN_SOURCE) {
            put(key = "filter", element = none)
            paint("fill-color" to categoryColor(colors))
        }
    return listOf(
        fill,
        lineLayer(id = PLAN_WALLS_LAYER, source = PLAN_SOURCE, color = colors.wall, width = WALL_WIDTH, filter = none),
        lineLayer(
            id = PLAN_OUTLINE_LAYER,
            source = PLAN_SOURCE,
            color = colors.wall,
            width = OUTLINE_WIDTH,
            filter = planFilter(floor = null, kind = OUTLINE),
        ),
    )
}

/** Filtr obiektów rodzaju [kind] piętra [floor]; bez piętra nie przepuszcza niczego. */
internal fun planFilter(floor: String?, kind: String): JsonElement =
    expression(
        "all",
        expression("==", property(FLOOR), floor?.let(::JsonPrimitive) ?: JsonPrimitive(false)),
        expression("==", property(KIND), JsonPrimitive(kind)),
    )

private fun categoryColor(colors: PlanColors): JsonElement {
    val groups =
        RoomCategory.entries.groupBy { category ->
            when (category) {
                RoomCategory.CLASSROOM, RoomCategory.LECTURE_HALL -> colors.classroom
                RoomCategory.CORRIDOR -> colors.corridor
                RoomCategory.ELEVATOR -> colors.elevator
                RoomCategory.TOILET, RoomCategory.ACCESSIBLE_TOILET -> colors.toilet
                RoomCategory.STAIRCASE, RoomCategory.OFFICE, RoomCategory.OTHER -> colors.other
            }
        }
    val cases =
        groups.flatMap { (color, categories) ->
            listOf(JsonArray(categories.map { JsonPrimitive(it.name) }), JsonPrimitive(color))
        }
    return JsonArray(listOf(JsonPrimitive("match"), property(CATEGORY)) + cases + JsonPrimitive(colors.other))
}

private fun part(floor: String, kind: String, category: RoomCategory? = null): JsonObject =
    buildJsonObject {
        put(key = FLOOR, value = floor)
        put(key = KIND, value = kind)
        category?.let { put(key = CATEGORY, value = it.name) }
    }
