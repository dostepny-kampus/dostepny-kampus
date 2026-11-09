/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.geo

import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Podpowiedź budynku po pomiarze z progiem 50 m od konturu (US6.2, BR-G4); punkty na północ od budynku A-1.
 *
 * `gridBuilding`: Siatka o kroku 1/4096 stopnia (około 17 m), dokładnie zapisywalna w `Double`: punkt na krawędzi leży
 * na niej.
 */
class BuildingHintTest {
    private val building =
        Building(
            symbol = "A-1",
            name = "Gmach Główny",
            outline =
                GeoPolygon(
                    listOf(
                        listOf(
                            GeoPoint(longitude = LON, latitude = LAT),
                            GeoPoint(longitude = LON + SIDE, latitude = LAT),
                            GeoPoint(longitude = LON + SIDE, latitude = LAT + SIDE),
                            GeoPoint(longitude = LON, latitude = LAT + SIDE),
                            GeoPoint(longitude = LON, latitude = LAT),
                        ),
                    ),
                ),
        )

    private val gridBuilding =
        Building(
            symbol = "A-1",
            name = "Budynek z dziedzińcem",
            outline = GeoPolygon(listOf(gridSquare(origin = 0, size = 8), gridSquare(origin = 3, size = 2))),
        )

    @Test
    fun us6_2_inside_outline() {
        val hint = hintBuilding(GeoPoint(longitude = LON + SIDE / 2, latitude = LAT + SIDE / 2), listOf(building))
        assertEquals(BuildingHint(building, distanceMeters = 0.0, isInside = true), hint)
        assertTrue(hint?.isInside == true)
    }

    /**
     * Wiersz: G1, decyzja 137, US6.2, BR-G2 — punkt na krawędzi konturu: podpowiedź budynku, 0 m, ale nie w konturze.
     */
    @Test
    fun us6_2_edge_hint_not_inside() {
        val hint = checkNotNull(hintBuilding(gridPoint(x = 8.0, y = 4.0), listOf(gridBuilding)))
        assertEquals(gridBuilding, hint.building)
        assertEquals(0.0, hint.distanceMeters, EXACT)
        assertFalse(hint.isInside)
    }

    /** Wiersz: G2, decyzja 137, BR-G2 — to samo dla krawędzi dziedzińca. */
    @Test
    fun br_g2_courtyard_hint_not_in() {
        val hint = checkNotNull(hintBuilding(gridPoint(x = 3.0, y = 4.0), listOf(gridBuilding)))
        assertEquals(gridBuilding, hint.building)
        assertEquals(0.0, hint.distanceMeters, EXACT)
        assertFalse(hint.isInside)
    }

    /** Wiersz: G3, US6.2 — punkt ściśle w konturze tuż przy krawędzi: ten budynek, 0 m, w konturze. */
    @Test
    fun us6_2_near_edge_hint_inside() {
        val hint = checkNotNull(hintBuilding(gridPoint(x = 7.9375, y = 4.0), listOf(gridBuilding)))
        assertEquals(gridBuilding, hint.building)
        assertEquals(0.0, hint.distanceMeters, EXACT)
        assertTrue(hint.isInside)
    }

    /** Wiersze: 30 m i 50 m od konturu → budynek do potwierdzenia. */
    @Test
    fun br_g4_hint_within_50_m() {
        listOf(30.0, 49.0).forEach { meters ->
            assertEquals(meters, checkNotNull(hintBuilding(north(meters), listOf(building))).distanceMeters, METER)
        }
    }

    /** Wiersz: 60 m od konturu → bez podpowiedzi, zgłoszenie „poza budynkiem”. */
    @Test
    fun br_g4_no_hint_beyond_50_m() {
        assertNull(hintBuilding(north(meters = 60.0), listOf(building)))
    }

    @Test
    fun br_g2_no_buildings_no_hint() {
        assertNull(hintBuilding(north(meters = 1.0), emptyList()))
    }

    private fun gridSquare(origin: Int, size: Int): List<GeoPoint> =
        listOf(0 to 0, size to 0, size to size, 0 to size, 0 to 0).map { (dx, dy) ->
            gridPoint(x = (origin + dx).toDouble(), y = (origin + dy).toDouble())
        }

    private fun gridPoint(x: Double, y: Double): GeoPoint =
        GeoPoint(longitude = GRID_LON + x * GRID_STEP, latitude = GRID_LAT + y * GRID_STEP)

    private fun north(meters: Double): GeoPoint =
        GeoPoint(longitude = LON + SIDE / 2, latitude = LAT + SIDE + meters / METERS_PER_DEGREE)

    private companion object {
        const val LON = 17.06
        const val LAT = 51.11
        const val SIDE = 0.001
        const val METERS_PER_DEGREE = 111_195.0
        const val METER = 0.5
        const val EXACT = 1e-6
        const val GRID_LON = 17.0625
        const val GRID_LAT = 51.109375
        const val GRID_STEP = 1.0 / 4096
    }
}
