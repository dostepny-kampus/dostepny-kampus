/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.geo

import io.github.dostepnykampus.core.model.GeoPoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.math.PI

/** Odległość pokazywana osobie: co 5 m, najmniej 5 m. Wymagania: US3.3, US18.2 */
class RoundedMetersTest {
    @Test
    fun roundsToFiveMeters() {
        val shown = listOf(12.0, 13.0, 47.0, 103.0).map { meters -> START.roundedMetersTo(north(meters = meters)) }
        assertEquals(listOf(10, 15, 45, 105), shown, "Odległość nie jest zaokrąglona do 5 m")
    }

    @Test
    fun showsAtLeastFiveMeters() {
        val shown = listOf(0.0, 2.0).map { meters -> START.roundedMetersTo(north(meters = meters)) }
        assertEquals(listOf(5, 5), shown, "Bliska odległość nie jest pokazana jako 5 m")
    }

    private companion object {
        /** Punkt na kampusie Grunwaldzkim, od którego mierzymy. */
        val START = GeoPoint(longitude = 17.06, latitude = 51.11)

        /** Metry na stopień szerokości przy promieniu Ziemi, którego używa Turf. */
        const val METERS_PER_DEGREE = 6_371_008.8 * PI / 180

        /** Punkt [meters] metrów na północ od [START]. */
        fun north(meters: Double): GeoPoint =
            GeoPoint(longitude = START.longitude, latitude = START.latitude + meters / METERS_PER_DEGREE)
    }
}
