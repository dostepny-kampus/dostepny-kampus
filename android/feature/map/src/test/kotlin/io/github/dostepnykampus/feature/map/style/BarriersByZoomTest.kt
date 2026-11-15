/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.style

import io.github.dostepnykampus.core.data.barrier.AcceptedBarrier
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.submission.Severity
import io.github.dostepnykampus.feature.map.barrier.withBarriers
import io.github.dostepnykampus.feature.map.campus.campusMapData
import io.github.dostepnykampus.feature.map.campus.selection
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Bariery na mapie V1 według uciążliwości i powiększenia, na konturze „Budynek testowy” C-13 (nie dane PWr).
 *
 * Wymagania: US1.3, US10.6
 */
class BarriersByZoomTest {
    private val data = campusMapData(styleCatalog()).withBarriers(BARRIERS)

    @Test
    fun showsOnlyBlockingBelowZoom16() {
        assertEquals(
            listOf("C-13 BLOCKING", "A-1 BLOCKING"),
            shown(building = null, zoom = 15.0),
            "Przy powiększeniu 15 bez zaznaczenia mapa pokazała inną barierę niż „Blokada”",
        )
    }

    @Test
    fun showsEverySeverityFromZoom16() {
        listOf(16.0, 17.0).forEach { zoom ->
            assertEquals(
                BARRIERS.map { it.id },
                shown(building = null, zoom = zoom),
                "Przy powiększeniu $zoom mapa nie pokazała wszystkich barier",
            )
        }
    }

    @Test
    fun showsSelectedAtAnyZoom() {
        assertEquals(
            listOf("C-13 HINDRANCE", "C-13 ONLY_WITH_HELP", "C-13 BLOCKING", "A-1 BLOCKING"),
            shown(building = "C-13", zoom = 15.0),
            "Przy zaznaczonym C-13 i powiększeniu 15 mapa pominęła jego barierę albo pokazała lżejszą z A-1",
        )
    }

    @Test
    fun showsAllSelectedFromZoom16() {
        listOf(16.0, 17.0).forEach { zoom ->
            assertEquals(
                BARRIERS.map { it.id },
                shown(building = "C-13", zoom = zoom),
                "Przy zaznaczonym C-13 i powiększeniu $zoom mapa nie pokazała wszystkich barier",
            )
        }
    }

    private fun shown(building: String?, zoom: Double): List<String> {
        val selection = data.selection(building = building)
        val filter = selectionLook(data = data, selection = selection, colors = styleColors()).barriersFilter
        val features = data.barriers.jsonObject.getValue("features").jsonArray
        return features
            .map { it.jsonObject.getValue("properties").jsonObject }
            .filter { passes(filter = filter, properties = it, zoom = zoom) }
            .map { it.getValue("id").jsonPrimitive.content }
    }

    private companion object {
        val BARRIERS =
            listOf("C-13", "A-1").flatMap { symbol ->
                Severity.entries.map { severity ->
                    AcceptedBarrier(
                        id = "$symbol ${severity.name}",
                        location = GeoPoint(longitude = 0.5, latitude = 0.5),
                        severity = severity,
                        type = null,
                        buildingSymbol = symbol,
                        verifiedAt = null,
                    )
                }
            }
    }
}
