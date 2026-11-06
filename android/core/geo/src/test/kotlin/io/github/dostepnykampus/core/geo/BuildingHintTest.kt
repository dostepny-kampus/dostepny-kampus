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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Podpowiedź budynku po pomiarze z progiem 50 m od konturu (US6.2, BR-G4); punkty na północ od budynku A-1. */
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

    @Test
    fun us6_2_inside_outline() {
        val hint = hintBuilding(GeoPoint(longitude = LON + SIDE / 2, latitude = LAT + SIDE / 2), listOf(building))
        assertEquals(BuildingHint(building, distanceMeters = 0.0), hint)
        assertTrue(hint?.isInside == true)
    }

    // Wiersze: 30 m i 50 m od konturu → budynek do potwierdzenia.
    @Test
    fun br_g4_hint_within_50_m() {
        listOf(30.0, 49.0).forEach { meters ->
            assertEquals(meters, checkNotNull(hintBuilding(north(meters), listOf(building))).distanceMeters, METER)
        }
    }

    // Wiersz: 60 m od konturu → bez podpowiedzi, zgłoszenie „poza budynkiem”.
    @Test
    fun br_g4_no_hint_beyond_50_m() {
        assertNull(hintBuilding(north(meters = 60.0), listOf(building)))
    }

    @Test
    fun br_g2_no_buildings_no_hint() {
        assertNull(hintBuilding(north(meters = 1.0), emptyList()))
    }

    private fun north(meters: Double): GeoPoint =
        GeoPoint(longitude = LON + SIDE / 2, latitude = LAT + SIDE + meters / METERS_PER_DEGREE)

    private companion object {
        const val LON = 17.06
        const val LAT = 51.11
        const val SIDE = 0.001
        const val METERS_PER_DEGREE = 111_195.0
        const val METER = 0.5
    }
}
