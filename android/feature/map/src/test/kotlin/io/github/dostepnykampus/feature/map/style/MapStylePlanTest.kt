/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.style

import io.github.dostepnykampus.core.model.GeoMultiPolygon
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.core.model.room.Room
import io.github.dostepnykampus.core.model.room.RoomCategory
import io.github.dostepnykampus.core.model.room.RoomList
import io.github.dostepnykampus.feature.map.campus.campusMapData
import io.github.dostepnykampus.feature.map.campus.selection
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Plan piętra w stylu mapy V1: obiekty pięter, kolory grup funkcji z tokenów i filtr piętra. Wymagania: US16.1 */
class MapStylePlanTest {
    @Test
    fun marksFeaturesWithFloor() {
        val features = planFeatures(mapOf("P0" to plan(RoomCategory.ELEVATOR, RoomCategory.CORRIDOR), "P1" to EMPTY))
        val parts = features.jsonObject.items("features").map { it.jsonObject.getValue("properties") }

        val expected =
            listOf(
                """{"floor":"P0","kind":"outline"}""",
                """{"floor":"P0","kind":"room","category":"ELEVATOR"}""",
                """{"floor":"P0","kind":"room","category":"CORRIDOR"}""",
            )
        assertEquals(expected, parts.map(JsonElement::toString), "Obiekty planu nie mają piętra i rodzaju")
    }

    @Test
    fun colorsRoomsByCategory() {
        val layers = layers()
        val fill = layers.paint(PLAN_ROOMS_LAYER).items("fill-color")
        val colors = RoomCategory.entries.associateWith { colorOf(fill, it) }

        val expected =
            mapOf(
                RoomCategory.CLASSROOM to "#D2EAF3",
                RoomCategory.LECTURE_HALL to "#D2EAF3",
                RoomCategory.CORRIDOR to "#FFFFFF",
                RoomCategory.STAIRCASE to "#E1E6E8",
                RoomCategory.ELEVATOR to "#A7C0C9",
                RoomCategory.ACCESSIBLE_TOILET to "#F2F5F6",
                RoomCategory.TOILET to "#F2F5F6",
                RoomCategory.OFFICE to "#E1E6E8",
                RoomCategory.OTHER to "#E1E6E8",
            )
        assertEquals(expected, colors, "Kolor grupy funkcji nie pochodzi z tokenów")
        assertEquals("#6A7579", layers.paint(PLAN_OUTLINE_LAYER).text("line-color"), "Obrys nie ma koloru ściany")
    }

    @Test
    fun hidesPlanUntilFloorChosen() {
        val hidden = planFilter(floor = null, kind = "room")
        val layers = layers()

        val filters = listOf(PLAN_ROOMS_LAYER, PLAN_WALLS_LAYER).map { layers.getValue(it).getValue("filter") }
        assertEquals(listOf(hidden, hidden), filters, "Plan widać bez wybranego piętra")
    }

    private fun layers(): Map<String, JsonObject> {
        val colors = styleColors().copy(plan = TOKENS)
        val data = campusMapData(styleCatalog())
        val style = Json.parseToJsonElement(campusMapStyle(data, data.selection(), colors)).jsonObject
        return style.items("layers").associate { it.jsonObject.text("id") to it.jsonObject }
    }

    private fun colorOf(match: List<JsonElement>, category: RoomCategory): String {
        val pairs = match.drop(2).dropLast(1).chunked(2)
        val hit = pairs.firstOrNull { (names, _) -> names.jsonArray.any { it.jsonPrimitive.content == category.name } }
        return (hit?.get(1) ?: match.last()).jsonPrimitive.content
    }

    private fun plan(vararg categories: RoomCategory): RoomList =
        RoomList(
            source = "Politechnika Wrocławska",
            floorOutline = SQUARE,
            rooms =
                categories.mapIndexed { index, category ->
                    Room(
                        id = "$index",
                        number = null,
                        function = category.name,
                        category = category,
                        geometry = SQUARE,
                        labelPoint = STYLE_ORIGIN,
                        isLabelled = false,
                    )
                },
        )

    private fun JsonObject.text(key: String): String = getValue(key).jsonPrimitive.content

    private fun JsonObject.items(key: String): List<JsonElement> = getValue(key).jsonArray

    private fun Map<String, JsonObject>.paint(layer: String): JsonObject = getValue(layer).getValue("paint").jsonObject

    private companion object {
        val RING =
            listOf(STYLE_ORIGIN, STYLE_ORIGIN.copy(longitude = 1.0), STYLE_ORIGIN.copy(latitude = 1.0), STYLE_ORIGIN)
        val SQUARE = GeoMultiPolygon(listOf(GeoPolygon(listOf(RING))))
        val EMPTY = RoomList(source = "PWr", floorOutline = GeoMultiPolygon(emptyList()), rooms = emptyList())
        val TOKENS =
            PlanColors(
                classroom = "#D2EAF3",
                corridor = "#FFFFFF",
                other = "#E1E6E8",
                elevator = "#A7C0C9",
                toilet = "#F2F5F6",
                wall = "#6A7579",
            )
    }
}
