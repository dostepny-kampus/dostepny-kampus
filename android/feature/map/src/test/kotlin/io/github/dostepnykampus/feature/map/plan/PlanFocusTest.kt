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
 * Budynek z planem po przybliżeniu: próg 17, histereza 16,5, 60 m od środka, przejście przy przewadze ponad 10 m,
 * wybrany budynek oraz budynek podstawowy z zapasem 5 m (syntetyczne kontury).
 *
 * Wymagania: US16.1, US16.2, BR-G1
 */
class PlanFocusTest {
    @Test
    fun showsPlanFromZoom17() {
        val inside = GeoPoint(longitude = 17.00025, latitude = MIDDLE)
        val focus =
            listOf(17.0, 16.9).map { zoom ->
                planFocus(center = inside, zoom = zoom, buildings = PlanBuildings(BUILDINGS), shown = null)
            }

        assertEquals(listOf("A", null), focus, "Plan nie pojawia się od powiększenia 17 albo jest przy mniejszym")
    }

    @Test
    fun keepsPlanDownToZoom16And5() {
        val inside = GeoPoint(longitude = 17.00025, latitude = MIDDLE)
        val focus =
            listOf(16.6, 16.4).map { zoom ->
                planFocus(center = inside, zoom = zoom, buildings = PlanBuildings(BUILDINGS), shown = "A")
            }

        assertEquals(listOf("A", null), focus, "Plan nie zostaje do powiększenia 16,5 albo zostaje niżej")
    }

    @Test
    fun ignoresBuildingsBeyond60m() {
        val focus =
            listOf(50.0, 70.0).map { meters ->
                val north = GeoPoint(longitude = 17.00025, latitude = TOP + meters / METERS_PER_LATITUDE)
                planFocus(center = north, zoom = 18.0, buildings = PlanBuildings(BUILDINGS), shown = null)
            }

        assertEquals(listOf("A", null), focus, "Plan pomija budynek do 60 m albo bierze dalszy")
    }

    @Test
    fun switchesOnlyWhen10mCloser() {
        val focus =
            listOf(38.0, 48.0).map { fromA ->
                val between = GeoPoint(longitude = A_EAST + fromA / metersPerLongitude(), latitude = MIDDLE)
                planFocus(center = between, zoom = 18.0, buildings = PlanBuildings(BUILDINGS), shown = "A")
            }

        assertEquals(listOf("A", "B"), focus, "Plan zmienia budynek bez przewagi 10 m albo wcale")
    }

    @Test
    fun skipsBasicBuilding() {
        val insideB = GeoPoint(longitude = 17.00175, latitude = MIDDLE)

        val focus = planFocus(center = insideB, zoom = 18.0, buildings = PlanBuildings(BUILDINGS.take(1)), shown = null)

        assertEquals(null, focus, "Budynek podstawowy dostał plan piętra")
    }

    /**
     * Scenariusz: budynki-bez-szczegolow.feature: Budynek podstawowy blisko budynku szczegółowego
     *   nie pokazuje planu sąsiada.
     */
    @Test
    fun hidesNeighborInsideBasic() {
        val buildings = PlanBuildings(detailed = listOf(A_1), basic = listOf(B_8))
        val insideB8 = GeoPoint(longitude = 17.00085, latitude = MIDDLE)
        val edge4m = GeoPoint(longitude = B8_EAST + 4.0 / metersPerLongitude(), latitude = MIDDLE)
        val edge6m = GeoPoint(longitude = B8_EAST + 6.0 / metersPerLongitude(), latitude = MIDDLE)
        val focus =
            listOf(
                planFocus(center = insideB8, zoom = 18.0, buildings = buildings, shown = "A-1", selected = "B-8"),
                planFocus(center = insideB8, zoom = 18.0, buildings = buildings, shown = "A-1"),
                planFocus(center = edge4m, zoom = 18.0, buildings = buildings, shown = null),
                planFocus(center = edge6m, zoom = 18.0, buildings = buildings, shown = null),
            )
        assertEquals(listOf(null, null, null, "A-1"), focus, "Budynek podstawowy i histereza 5 m przy B-8")
    }

    @Test
    fun showsDetailedInsideAdjacent() {
        val adjacent = PlanBuildings(detailed = listOf(A_1), basic = listOf(square("B-8", west = A_EAST)))
        val insideA1 = GeoPoint(longitude = A_EAST - 1.5 / metersPerLongitude(), latitude = MIDDLE)
        val focus = planFocus(center = insideA1, zoom = 18.0, buildings = adjacent, shown = null)
        assertEquals("A-1", focus, "Budynek szczegółowy przy ścianie podstawowego nie pokazał planu")
    }

    @Test
    fun switchesToSelectedBuilding() {
        val insideA1 = GeoPoint(longitude = 17.00025, latitude = MIDDLE)
        val close = PlanBuildings(detailed = listOf(A_1, square("B-1", west = 17.0006)))
        val switched =
            listOf(18.0 to "A-1", 16.7 to "A-1", 16.7 to null, 16.4 to "B-1").map { (zoom, shown) ->
                planFocus(center = insideA1, zoom = zoom, buildings = close, shown = shown, selected = "B-1")
            }
        assertEquals(listOf("B-1", "B-1", null, null), switched, "Wybrany budynek szczegółowy przy progach")
    }

    @Test
    fun hidesFarSelectedBuilding() {
        val insideA = GeoPoint(longitude = 17.00025, latitude = MIDDLE)
        val focus =
            planFocus(center = insideA, zoom = 18.0, buildings = PlanBuildings(BUILDINGS), shown = "B", selected = "B")
        assertEquals(null, focus, "Wybrany budynek dalej niż 60 m pokazał plan swój albo sąsiada")
    }

    private companion object {
        const val SOUTH = 51.0
        const val TOP = 51.0003
        const val MIDDLE = 51.00015
        const val A_EAST = 17.0005
        const val B8_EAST = 17.0011
        const val METERS_PER_LATITUDE = 111_320.0
        val A_1 = square("A-1", west = 17.0)
        val B_8 = square("B-8", west = 17.0006)
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
