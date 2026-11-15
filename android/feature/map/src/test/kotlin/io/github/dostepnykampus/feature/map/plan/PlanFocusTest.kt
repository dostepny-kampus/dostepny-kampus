/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.plan

import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.core.model.building.Building
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.math.cos

/**
 * Budynek z planem po przybliżeniu: próg 17, histereza 16,5, 60 m od środka i przejście przy przewadze ponad 10 m.
 * Budynki „A” i „B” stoją 70 m od siebie (nie dane PWr).
 *
 * Wymagania: US16.1, BR-G1
 */
class PlanFocusTest {
    @Test
    fun showsPlanFromZoom17() {
        val inside = GeoPoint(longitude = 17.00025, latitude = MIDDLE)
        val focus =
            listOf(17.0, 16.9).map { zoom ->
                planFocus(center = inside, zoom = zoom, detailed = BUILDINGS, shown = null)
            }

        assertEquals(listOf("A", null), focus, "Plan nie pojawia się od powiększenia 17 albo jest przy mniejszym")
    }

    @Test
    fun keepsPlanDownToZoom16And5() {
        val inside = GeoPoint(longitude = 17.00025, latitude = MIDDLE)
        val focus =
            listOf(16.6, 16.4).map { zoom ->
                planFocus(center = inside, zoom = zoom, detailed = BUILDINGS, shown = "A")
            }

        assertEquals(listOf("A", null), focus, "Plan nie zostaje do powiększenia 16,5 albo zostaje niżej")
    }

    @Test
    fun ignoresBuildingsBeyond60m() {
        val focus =
            listOf(50.0, 70.0).map { meters ->
                val north = GeoPoint(longitude = 17.00025, latitude = TOP + meters / METERS_PER_LATITUDE)
                planFocus(center = north, zoom = 18.0, detailed = BUILDINGS, shown = null)
            }

        assertEquals(listOf("A", null), focus, "Plan pomija budynek do 60 m albo bierze dalszy")
    }

    @Test
    fun switchesOnlyWhen10mCloser() {
        val focus =
            listOf(38.0, 48.0).map { fromA ->
                val between = GeoPoint(longitude = A_EAST + fromA / metersPerLongitude(), latitude = MIDDLE)
                planFocus(center = between, zoom = 18.0, detailed = BUILDINGS, shown = "A")
            }

        assertEquals(listOf("A", "B"), focus, "Plan zmienia budynek bez przewagi 10 m albo wcale")
    }

    @Test
    fun skipsBasicBuilding() {
        val insideB = GeoPoint(longitude = 17.00175, latitude = MIDDLE)

        val focus = planFocus(center = insideB, zoom = 18.0, detailed = BUILDINGS.take(1), shown = null)

        assertEquals(null, focus, "Budynek podstawowy dostał plan piętra")
    }

    private companion object {
        const val SOUTH = 51.0
        const val TOP = 51.0003
        const val MIDDLE = 51.00015
        const val A_EAST = 17.0005
        const val METERS_PER_LATITUDE = 111_320.0
        val BUILDINGS = listOf(square("A", west = 17.0), square("B", west = 17.0015))

        fun metersPerLongitude(): Double = METERS_PER_LATITUDE * cos(Math.toRadians(MIDDLE))

        fun square(symbol: String, west: Double): Building {
            val east = west + 0.0005
            val ring = listOf(west to SOUTH, east to SOUTH, east to TOP, west to TOP, west to SOUTH)
            val outline = GeoPolygon(listOf(ring.map { (lon, lat) -> GeoPoint(longitude = lon, latitude = lat) }))
            return Building(symbol = symbol, name = null, outline = outline)
        }
    }
}
