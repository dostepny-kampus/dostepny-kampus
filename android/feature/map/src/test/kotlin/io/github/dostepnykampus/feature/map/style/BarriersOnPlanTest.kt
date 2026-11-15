/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.style

import io.github.dostepnykampus.core.data.barrier.AcceptedBarrier
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.submission.Severity
import io.github.dostepnykampus.core.model.submission.SubmissionFloor
import io.github.dostepnykampus.feature.map.barrier.withBarriers
import io.github.dostepnykampus.feature.map.campus.campusMapData
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Bariery na planie piętra: tylko piętro planu, bez piętra na każdym, inne budynki bez zmian. Wymagania: US16.1 */
class BarriersOnPlanTest {
    @Test
    fun keepsBarriersOfChosenFloor() {
        assertEquals(
            listOf("z-p1", "z-bez", "z-inny"),
            shown(building = "A-1", floor = "P1"),
            "Plan 1 piętra A-1 nie pokazał bariery swojego piętra, bariery bez piętra albo bariery innego budynku",
        )
    }

    @Test
    fun hidesBarrierOfOtherFloor() {
        assertEquals(
            listOf("z-p0", "z-bez", "z-inny"),
            shown(building = "A-1", floor = "P0"),
            "Plan parteru A-1 pokazał barierę 1 piętra",
        )
    }

    @Test
    fun showsAllWithoutPlan() {
        assertEquals(
            listOf("z-p1", "z-p0", "z-bez", "z-inny"),
            shown(building = null, floor = null),
            "Bez planu mapa nie pokazała wszystkich barier",
        )
    }

    private fun shown(building: String?, floor: String?): List<String> {
        val filter = barriersOnPlan(building = building, floor = floor)
        val barriers = campusMapData(styleCatalog()).withBarriers(BARRIERS).barriers
        val features = barriers.jsonObject.getValue("features").jsonArray
        return features
            .asSequence()
            .map { it.jsonObject.getValue("properties").jsonObject }
            .filter { evaluate(expression = filter, properties = it).jsonPrimitive.boolean }
            .map { it.getValue("id").jsonPrimitive.content }
            .toList()
    }

    private fun evaluate(expression: JsonElement, properties: JsonObject): JsonElement {
        if (expression !is JsonArray) return expression
        val name = expression.first().jsonPrimitive.content
        val arguments = expression.drop(1)

        fun value(index: Int) = evaluate(expression = arguments[index], properties = properties)

        return when (name) {
            "get" -> properties[arguments[0].jsonPrimitive.content] ?: JsonNull
            "has" -> JsonPrimitive(arguments[0].jsonPrimitive.content in properties)
            "!" -> JsonPrimitive(!value(0).jsonPrimitive.boolean)
            "==" -> JsonPrimitive(value(0) == value(1))
            "!=" -> JsonPrimitive(value(0) != value(1))
            "any" -> JsonPrimitive(arguments.indices.any { value(it).jsonPrimitive.boolean })
            else -> error("Wyrażenie $name poza filtrem barier")
        }
    }

    private companion object {
        val BARRIERS =
            listOf(
                barrier(id = "z-p1", symbol = "A-1", floor = "P1"),
                barrier(id = "z-p0", symbol = "A-1", floor = "P0"),
                barrier(id = "z-bez", symbol = "A-1", floor = null),
                barrier(id = "z-inny", symbol = "C-13", floor = "P1"),
            )

        fun barrier(id: String, symbol: String, floor: String?) =
            AcceptedBarrier(
                id = id,
                location = GeoPoint(longitude = 17.0, latitude = 51.0),
                severity = Severity.BLOCKING,
                type = null,
                buildingSymbol = symbol,
                verifiedAt = null,
                floor = floor?.let { SubmissionFloor(floorId = it) },
            )
    }
}
