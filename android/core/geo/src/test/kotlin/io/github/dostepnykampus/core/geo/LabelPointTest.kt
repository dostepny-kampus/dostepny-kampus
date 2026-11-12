/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.geo

import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.core.model.GeoMultiPolygon
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/** Punkt podpisu na kształtach testowych „Budynek testowy” (nie dane PWr) i na pliku konturów z repozytorium. */
class LabelPointTest {
    private val square = ring(0.0 to 0.0, 4.0 to 0.0, 4.0 to 4.0, 0.0 to 4.0)

    @Test
    fun us1_square_label_near_center() {
        val point = labelPoint(GeoPolygon(listOf(square)))
        assertEquals(2.0, point.longitude, CELL)
        assertEquals(2.0, point.latitude, CELL)
    }

    @Test
    fun us1_label_not_in_courtyard() {
        val courtyard = ring(1.0 to 1.0, 3.0 to 1.0, 3.0 to 3.0, 1.0 to 3.0)
        val point = labelPoint(GeoPolygon(listOf(square, courtyard)))
        val isInCourtyard = point.longitude in 1.0..3.0 && point.latitude in 1.0..3.0
        assertTrue(!isInCourtyard && point.longitude in 0.0..4.0 && point.latitude in 0.0..4.0)
    }

    @Test
    fun us1_l_shape_label_inside() {
        val shape = ring(0.0 to 0.0, 4.0 to 0.0, 4.0 to 1.0, 1.0 to 1.0, 1.0 to 4.0, 0.0 to 4.0)
        val point = labelPoint(GeoPolygon(listOf(shape)))
        assertTrue(point.longitude < 1.0 || point.latitude < 1.0)
    }

    @Test
    fun us1_multipolygon_largest_part() {
        val small = ring(10.0 to 0.0, 11.0 to 0.0, 11.0 to 1.0, 10.0 to 1.0)
        val point = labelPoint(GeoMultiPolygon(listOf(GeoPolygon(listOf(small)), GeoPolygon(listOf(square)))))
        assertTrue(point.longitude < 4.0)
    }

    /** US1, #820: punkt podpisu każdego konturu z repozytorium leży w tym konturze według [locateBuilding]. */
    @Test
    fun us1_real_labels_inside_outline() {
        val real = parseBuildingCatalog(File(System.getProperty("dostepnykampus.buildings")).readText())
        val outside =
            real.buildings.filter { building ->
                locateBuilding(labelPoint(building.outline), listOf(building)) !is LocateResult.InsideOutline
            }
        assertEquals(emptyList<String>(), outside.map(Building::symbol))
    }

    private fun ring(vararg corners: Pair<Double, Double>): List<GeoPoint> =
        (corners.toList() + corners.first()).map { (x, y) -> GeoPoint(longitude = x, latitude = y) }

    private companion object {
        const val CELL = 4.0 / 32
    }
}
