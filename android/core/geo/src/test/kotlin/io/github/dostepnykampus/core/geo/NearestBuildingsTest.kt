/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.geo

import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Sekcja „Wybierz budynek” na C1: budynki najbliżej punktu, od najbliższego, z odległością. Wymagania: US6.2, NFR-4 */
class NearestBuildingsTest {
    private val near = square(symbol = "A-1", south = LAT)
    private val middle = square(symbol = "A-2", south = LAT + 2 * SIDE)
    private val far = square(symbol = "A-3", south = LAT + 4 * SIDE)
    private val farthest = square(symbol = "A-4", south = LAT + 6 * SIDE)

    /** Punkt na południe od budynków: trzy najbliższe, od najbliższego, z odległością od krawędzi w metrach. */
    @Test
    fun us6_2_nearest_sorted() {
        val point = GeoPoint(longitude = LON + SIDE / 2, latitude = LAT - SIDE / 10)
        val hints = buildingHints(point, listOf(farthest, far, near, middle)).nearby

        assertEquals(listOf("A-1", "A-2", "A-3"), hints.map { it.building.symbol })
        assertEquals(SIDE / 10 * METERS_PER_DEGREE, hints.first().distanceMeters, METER)
        assertEquals((2 + 1.0 / 10) * SIDE * METERS_PER_DEGREE, hints[1].distanceMeters, METER)
        assertFalse(hints.any { it.isInside })
    }

    /** Punkt w konturze: ten budynek pierwszy, 0 m i w konturze; pozostałe dalej z odległością. */
    @Test
    fun us6_2_nearest_inside_first() {
        val point = GeoPoint(longitude = LON + SIDE / 2, latitude = LAT + 2.4 * SIDE)
        val hints = buildingHints(point, listOf(near, middle, far), limit = 2).nearby

        assertEquals(listOf("A-2", "A-1"), hints.map { it.building.symbol })
        assertEquals(0.0, hints.first().distanceMeters)
        assertTrue(hints.first().isInside)
    }

    private fun square(symbol: String, south: Double): Building =
        Building(
            symbol = symbol,
            name = "Budynek $symbol",
            outline =
                GeoPolygon(
                    listOf(
                        listOf(
                            GeoPoint(longitude = LON, latitude = south),
                            GeoPoint(longitude = LON + SIDE, latitude = south),
                            GeoPoint(longitude = LON + SIDE, latitude = south + SIDE),
                            GeoPoint(longitude = LON, latitude = south + SIDE),
                            GeoPoint(longitude = LON, latitude = south),
                        ),
                    ),
                ),
        )

    private companion object {
        const val LON = 17.06
        const val LAT = 51.11
        const val SIDE = 0.001
        const val METERS_PER_DEGREE = 111_195.0
        const val METER = 0.5
    }
}
