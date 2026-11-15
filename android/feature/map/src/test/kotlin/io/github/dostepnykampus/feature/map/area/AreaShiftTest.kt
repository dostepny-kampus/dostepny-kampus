/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.area

import io.github.dostepnykampus.feature.map.campus.CampusBounds
import io.github.dostepnykampus.feature.map.campus.MapArea
import org.junit.Assert.assertEquals
import org.junit.Test

/** Zmiana terenu do napisu po przelocie na terenach testowych (nie dane PWr) wokół punktu 17° E, 51° N. */
class AreaShiftTest {
    /**
     * Wiersz: V1, napis po zmianie terenu podaje jeden z ośmiu kierunków świata od terenu, o którym osoba wiedziała.
     *
     * Wymagania: US1.8, NFR-56
     */
    @Test
    fun namesEightDirections() {
        val start = square(east = 0.0, north = 0.0)
        val expected =
            mapOf(
                square(east = 0.0, north = STEP) to CompassDirection.North,
                square(east = STEP, north = STEP) to CompassDirection.NorthEast,
                square(east = STEP, north = 0.0) to CompassDirection.East,
                square(east = STEP, north = -STEP) to CompassDirection.SouthEast,
                square(east = 0.0, north = -STEP) to CompassDirection.South,
                square(east = -STEP, north = -STEP) to CompassDirection.SouthWest,
                square(east = -STEP, north = 0.0) to CompassDirection.West,
                square(east = -STEP, north = STEP) to CompassDirection.NorthWest,
            )
        assertEquals(
            "kierunki od terenu startowego",
            expected.values.toList(),
            expected.keys.map { start.directionTo(it) },
        )
    }

    /**
     * Wiersz: V1, napis po zmianie terenu podaje nazwę nowego terenu i odległość w pełnych kilometrach, jak lista.
     *
     * Wymagania: US1.8, US1.7
     */
    @Test
    fun givesNameAndFullKilometers() {
        val start = MapArea(name = "Start", symbols = listOf("S-1"), bounds = square(east = 0.0, north = 0.0))
        val other = MapArea(name = "Inny", symbols = listOf("I-1"), bounds = square(east = STEP, north = 0.0))
        assertEquals(
            "zmiana na teren na wschód",
            AreaShift(name = "Inny", kilometers = 1, direction = CompassDirection.East),
            start.shiftTo(other),
        )
    }

    private fun square(east: Double, north: Double): CampusBounds {
        val west = LONGITUDE + east
        val south = LATITUDE + north
        return CampusBounds(west = west, south = south, east = west + SIDE, north = south + SIDE)
    }

    private companion object {
        const val LONGITUDE = 17.0
        const val LATITUDE = 51.0
        const val STEP = 0.01
        const val SIDE = 0.002
    }
}
