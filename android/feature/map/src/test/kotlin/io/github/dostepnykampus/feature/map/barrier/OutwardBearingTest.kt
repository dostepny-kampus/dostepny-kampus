/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.barrier

import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.math.cos
import kotlin.math.sin

/** Kierunek na zewnątrz konturu „Budynek testowy” w kształcie litery L (nie dane PWr). Wymagania: US2.2, NFR-17 */
class OutwardBearingTest {
    private val building =
        GeoPolygon(
            listOf(
                listOf(
                    point(17.0, 51.0),
                    point(17.5, 51.0),
                    point(17.5, 51.25),
                    point(17.25, 51.25),
                    point(17.25, 51.5),
                    point(17.0, 51.5),
                    point(17.0, 51.0),
                ),
            ),
        )

    /** Wiersz: wejście na ścianie w zagłębieniu konturu → kierunek na wschód, poza kontur. Wymagania: NFR-17 */
    @Test
    fun pointsOutOfNotchOfConcaveOutline() {
        assertEquals(
            listOf(1.0, 0.0),
            direction(building.outwardBearing(point(17.25, 51.4))),
            "Kierunek z zagłębienia konturu nie wskazuje wschodu",
        )
    }

    /** Wiersz: wejście na południowej ścianie → kierunek na południe. Wymagania: NFR-17 */
    @Test
    fun pointsAwayFromOuterWall() {
        assertEquals(
            listOf(0.0, -1.0),
            direction(building.outwardBearing(point(17.1, 51.0))),
            "Kierunek od południowej ściany nie wskazuje południa",
        )
    }

    private fun direction(degrees: Double?): List<Double> {
        val radians = Math.toRadians(checkNotNull(degrees))
        return listOf(sin(radians), cos(radians)).map { Math.round(it * PRECISION) / PRECISION + 0.0 }
    }

    private fun point(longitude: Double, latitude: Double) = GeoPoint(longitude = longitude, latitude = latitude)

    private companion object {
        const val PRECISION = 100.0
    }
}
