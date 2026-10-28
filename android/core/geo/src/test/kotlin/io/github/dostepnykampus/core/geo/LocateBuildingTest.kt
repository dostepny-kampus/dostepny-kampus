/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.geo

import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.core.model.GeoMultiPolygon
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Podpowiedź budynku na małych ręcznych konturach. Współrzędne leżą na siatce o kroku 1/4096 stopnia
 * (około 17 m), dokładnie zapisywalnej w `Double`, więc punkt na krawędzi leży na niej bez błędu zaokrąglenia.
 */
class LocateBuildingTest {
    private val courtyardBuilding =
        Building(
            symbol = "A-1",
            name = "Budynek z dziedzińcem",
            outline = GeoPolygon(listOf(square(x = 0, y = 0, size = 8), square(x = 3, y = 3, size = 2))),
        )

    private val twoPartBuilding =
        Building(
            symbol = "B-1",
            name = "Budynek z dwóch części",
            outline =
                GeoMultiPolygon(
                    listOf(
                        GeoPolygon(listOf(square(x = 12, y = 0, size = 2))),
                        GeoPolygon(listOf(square(x = 12, y = 6, size = 2))),
                    ),
                ),
        )

    private val buildings = listOf(courtyardBuilding, twoPartBuilding)

    @Test
    fun us6_2_point_inside_outline() {
        assertEquals(LocateResult.InsideOutline(courtyardBuilding), locateBuilding(point(x = 1, y = 1), buildings))
    }

    @Test
    fun us6_2_inside_multipolygon_part() {
        assertEquals(LocateResult.InsideOutline(twoPartBuilding), locateBuilding(point(x = 13, y = 7), buildings))
    }

    @Test
    fun us6_2_on_outline_edge_inside() {
        assertEquals(LocateResult.InsideOutline(courtyardBuilding), locateBuilding(point(x = 8, y = 4), buildings))
    }

    @Test
    fun us6_2_on_courtyard_edge_inside() {
        assertEquals(LocateResult.InsideOutline(courtyardBuilding), locateBuilding(point(x = 3, y = 4), buildings))
    }

    @Test
    fun br_g2_courtyard_outside() {
        assertEquals(LocateResult.OutsideBuilding(courtyardBuilding), locateBuilding(point(x = 4, y = 4), buildings))
    }

    @Test
    fun br_g2_outside_nearest_building() {
        assertEquals(LocateResult.OutsideBuilding(twoPartBuilding), locateBuilding(point(x = 11, y = 7), buildings))
    }

    @Test
    fun br_g2_no_buildings_outside() {
        assertEquals(LocateResult.OutsideBuilding(null), locateBuilding(point(x = 1, y = 1), emptyList()))
    }

    private fun square(x: Int, y: Int, size: Int): List<GeoPoint> =
        listOf(0 to 0, size to 0, size to size, 0 to size, 0 to 0).map { (dx, dy) -> point(x = x + dx, y = y + dy) }

    private fun point(x: Int, y: Int): GeoPoint =
        GeoPoint(longitude = BASE_LONGITUDE + x * STEP, latitude = BASE_LATITUDE + y * STEP)

    private companion object {
        const val BASE_LONGITUDE = 17.0625
        const val BASE_LATITUDE = 51.109375
        const val STEP = 1.0 / 4096
    }
}
