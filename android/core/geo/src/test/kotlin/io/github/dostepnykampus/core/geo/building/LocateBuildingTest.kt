/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.geo.building

import io.github.dostepnykampus.core.model.GeoMultiPolygon
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.core.model.building.Building
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Podpowiedź budynku na małych ręcznych konturach. Współrzędne leżą na siatce o kroku 1/4096 stopnia
 * (około 17 m), dokładnie zapisywalnej w `Double`, więc punkt na krawędzi leży na niej bez błędu zaokrąglenia.
 */
class LocateBuildingTest {
    private fun courtyardBuilding(): Building =
        Building(
            symbol = "A-1",
            name = "Budynek z dziedzińcem",
            outline = GeoPolygon(listOf(square(x = 0, y = 0, size = 8), square(x = 3, y = 3, size = 2))),
        )

    private fun twoPartBuilding(): Building =
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

    private fun compoundCourtyardBuilding(): Building =
        Building(
            symbol = "C-1",
            name = "Budynek złożony z dziedzińcem w drugiej części",
            outline =
                GeoMultiPolygon(
                    listOf(
                        GeoPolygon(listOf(square(x = 20, y = 0, size = 2))),
                        GeoPolygon(listOf(square(x = 20, y = 6, size = 8), square(x = 23, y = 9, size = 2))),
                    ),
                ),
        )

    private fun triangleBuilding(): Building =
        Building(
            symbol = "D-1",
            name = "Budynek z ukośną krawędzią",
            outline =
                GeoPolygon(
                    listOf(
                        listOf(point(x = 30, y = 0), point(x = 38, y = 0), point(x = 30, y = 8), point(x = 30, y = 0)),
                    ),
                ),
        )

    private fun buildings(): List<Building> = listOf(courtyardBuilding(), twoPartBuilding())

    @Test
    fun findsPointInsideOutline() {
        assertEquals(
            LocateResult.InsideOutline(courtyardBuilding()),
            locateBuilding(point(x = 1, y = 1), buildings()),
            "punkt w obrysie leży wewnątrz budynku",
        )
    }

    @Test
    fun findsPointInMultipolygonPart() {
        assertEquals(
            LocateResult.InsideOutline(twoPartBuilding()),
            locateBuilding(point(x = 13, y = 7), buildings()),
            "punkt w części wielokąta złożonego leży wewnątrz",
        )
    }

    /** Wiersz: G1 — punkt na zewnętrznej krawędzi jest poza budynkiem, z jego propozycją. Wymagania: US6.2, BR-G2 */
    @Test
    fun leavesOutlineEdgeOutside() {
        assertEquals(
            LocateResult.OutsideBuilding(courtyardBuilding()),
            locateBuilding(point(x = 8, y = 4), buildings()),
            "punkt na krawędzi obrysu leży na zewnątrz",
        )
    }

    /** Wiersz: G2 — punkt na krawędzi dziedzińca jest poza budynkiem, z jego propozycją. Wymagania: US6.2, BR-G2 */
    @Test
    fun leavesCourtyardEdgeOutside() {
        assertEquals(
            LocateResult.OutsideBuilding(courtyardBuilding()),
            locateBuilding(point(x = 3, y = 4), buildings()),
            "punkt na krawędzi dziedzińca leży na zewnątrz",
        )
    }

    /** Wiersz: G1 — każdy z boków i wierzchołek konturu jest poza budynkiem. Wymagania: US6.2, BR-G2 */
    @Test
    fun leavesEverySideCornerOutside() {
        listOf(0 to 4, 4 to 0, 4 to 8, 0 to 0, 8 to 8).forEach { (x, y) ->
            assertEquals(
                LocateResult.OutsideBuilding(courtyardBuilding()),
                locateBuilding(point(x = x, y = y), buildings()),
                "punkt ($x, $y) na konturze",
            )
        }
    }

    /**
     * Wiersz: G2 — krawędź dziedzińca w drugiej części wielokąta złożonego, także poza budynkiem.
     *
     * Wymagania: BR-G2
     */
    @Test
    fun leavesCompoundHoleEdgeOutside() {
        assertEquals(
            LocateResult.OutsideBuilding(compoundCourtyardBuilding()),
            locateBuilding(point(x = 23, y = 10), listOf(compoundCourtyardBuilding())),
            "punkt na krawędzi otworu złożonego obrysu leży na zewnątrz",
        )
    }

    /** Wiersz: G2 — krawędź zewnętrzna tej samej części złożonej jest poza, a ścisłe wnętrze w. Wymagania: BR-G2 */
    @Test
    fun splitsCompoundEdgeAndBody() {
        val compound = listOf(compoundCourtyardBuilding())
        val outside = LocateResult.OutsideBuilding(compoundCourtyardBuilding())
        val inside = LocateResult.InsideOutline(compoundCourtyardBuilding())
        assertEquals(
            outside,
            locateBuilding(point(x = 28, y = 10), compound),
            "krawędź złożonego obrysu leży na zewnątrz",
        )
        assertEquals(inside, locateBuilding(point(x = 21, y = 7), compound), "wnętrze złożonego obrysu: wewnątrz")
    }

    /**
     * Wiersz: G3 — punkt ściśle w konturze tuż przy krawędzi i tuż przy dziedzińcu nadal jest w budynku.
     *
     * Wymagania: US6.2
     */
    @Test
    fun findsPointNearEdgesInside() {
        listOf(7.9375 to 4.0, 0.0625 to 4.0, 3.0 to 2.9375, 3.0 to 5.0625).forEach { (x, y) ->
            assertEquals(
                LocateResult.InsideOutline(courtyardBuilding()),
                locateBuilding(point(x = x, y = y), buildings()),
                "punkt ($x, $y) w konturze",
            )
        }
    }

    /**
     * Wiersz: G1 — punkt dokładnie na ukośnej krawędzi (30,8)-(38,0) jest poza budynkiem.
     *
     * Wymagania: US6.2, BR-G2
     */
    @Test
    fun leavesDiagonalEdgeOutside() {
        assertEquals(
            LocateResult.OutsideBuilding(triangleBuilding()),
            locateBuilding(point(x = 34, y = 4), listOf(triangleBuilding())),
            "punkt na ukośnej krawędzi leży na zewnątrz",
        )
    }

    /** Wiersz: G3, O5 — punkt ściśle w trójkącie, ale w prostokącie ograniczającym ukośną krawędź. */
    @Test
    fun findsPointInDiagonalBox() {
        assertEquals(
            LocateResult.InsideOutline(triangleBuilding()),
            locateBuilding(point(x = 32, y = 2), listOf(triangleBuilding())),
            "punkt w ukośnym pudełku leży wewnątrz",
        )
    }

    /**
     * Wiersz: G3, O5 — punkt na przedłużeniu poziomej krawędzi dziedzińca, poza jej odcinkiem, jest w
     * budynku (korpus między krawędzią zewnętrzną a dziedzińcem).
     */
    @Test
    fun findsPointOnEdgeExtension() {
        assertEquals(
            LocateResult.InsideOutline(courtyardBuilding()),
            locateBuilding(point(x = 2, y = 3), buildings()),
            "punkt na przedłużeniu krawędzi leży wewnątrz",
        )
    }

    /** Wiersz: G3, O5 — punkt około 4 mm w głąb od krawędzi (1/4096 kroku siatki) jest w budynku. */
    @Test
    fun findsPointFourMmInside() {
        val almostOnEdge = point(x = 8.0 - 1.0 / 4096, y = 4.0)
        assertEquals(
            LocateResult.InsideOutline(courtyardBuilding()),
            locateBuilding(almostOnEdge, buildings()),
            "punkt cztery milimetry od krawędzi leży wewnątrz",
        )
    }

    @Test
    fun leavesCourtyardOutside() {
        assertEquals(
            LocateResult.OutsideBuilding(courtyardBuilding()),
            locateBuilding(point(x = 4, y = 4), buildings()),
            "punkt w dziedzińcu leży na zewnątrz",
        )
    }

    @Test
    fun givesNearestBuildingOutside() {
        assertEquals(
            LocateResult.OutsideBuilding(twoPartBuilding()),
            locateBuilding(point(x = 11, y = 7), buildings()),
            "punkt poza budynkami wskazuje najbliższy budynek",
        )
    }

    @Test
    fun staysOutsideWithoutBuildings() {
        assertEquals(
            LocateResult.OutsideBuilding(null),
            locateBuilding(point(x = 1, y = 1), emptyList()),
            "bez budynków punkt leży poza każdym budynkiem",
        )
    }

    private fun square(x: Int, y: Int, size: Int): List<GeoPoint> =
        listOf(0 to 0, size to 0, size to size, 0 to size, 0 to 0).map { (dx, dy) -> point(x = x + dx, y = y + dy) }

    private fun point(x: Int, y: Int): GeoPoint = point(x = x.toDouble(), y = y.toDouble())

    private fun point(x: Double, y: Double): GeoPoint =
        GeoPoint(longitude = BASE_LONGITUDE + x * STEP, latitude = BASE_LATITUDE + y * STEP)

    private companion object {
        const val BASE_LONGITUDE = 17.0625
        const val BASE_LATITUDE = 51.109375
        const val STEP = 1.0 / 4096
    }
}
