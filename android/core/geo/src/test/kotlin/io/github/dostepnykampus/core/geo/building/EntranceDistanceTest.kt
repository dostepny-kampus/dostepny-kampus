/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.geo.building

import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceMarking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Najbliższe wejście do punktu pomiaru z odległością w metrach. Wymagania: US7.4 */
class EntranceDistanceTest {
    private val point = GeoPoint(longitude = LON, latitude = LAT)

    @Test
    fun picksNearerOfTwoEntrances() {
        val far = entrance(id = "far", latitudeMeters = 30.0)
        val near = entrance(id = "near", latitudeMeters = 3.0)

        val result = requireNotNull(nearestEntrance(point, listOf(far, near)))

        assertEquals("near", result.entrance.id)
        assertEquals(3.0, result.meters, TOLERANCE_METERS)
    }

    @Test
    fun givesNothingWithoutEntrances() {
        assertNull(nearestEntrance(point, emptyList()))
    }

    private fun entrance(id: String, latitudeMeters: Double): Entrance =
        Entrance(
            id = id,
            buildingSymbol = "A-1",
            name = null,
            isMain = false,
            type = null,
            location = GeoPoint(longitude = LON, latitude = LAT + latitudeMeters / METERS_PER_DEGREE),
            marking = EntranceMarking.NO_DATA,
        )

    private companion object {
        const val LON = 17.06209
        const val LAT = 51.10785
        const val METERS_PER_DEGREE = 111_195.0
        const val TOLERANCE_METERS = 0.05
    }
}
