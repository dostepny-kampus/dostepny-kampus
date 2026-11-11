/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.core.model.BuildingCatalog
import io.github.dostepnykampus.core.model.GeoArea
import io.github.dostepnykampus.core.model.GeoMultiPolygon
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.feature.map.data.CampusBounds
import io.github.dostepnykampus.feature.map.data.START_BUILDING
import io.github.dostepnykampus.feature.map.data.boundsOf
import io.github.dostepnykampus.feature.map.data.campusMapData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.math.log2

/** Teren mapy V1 i granice widoku na konturach testowych „Budynek testowy” (nie dane PWr). */
class CampusBoundsTest {
    private val bounds = CampusBounds(west = 17.0, south = 60.0, east = 17.04, north = 60.02)
    private val limits = MapLimits(bounds, widthDp = 400.0, heightDp = 800.0)

    /**
     * Wiersz: V1, mapa zostaje na wybranym terenie: w prostokącie z konturami budynków tego terenu, powiększonym
     * o 500 m z każdej strony (US1.7, BR-G5, NFR-39), nie tylko przy budynku startowym. Na 60° szerokości 500 m
     * na wschód to dwa razy więcej stopni niż 500 m na północ.
     */
    @Test
    fun nfr39_area_plus_500_m() {
        val start = rectangle(west = 17.0, south = 60.0, east = 17.01, north = 60.004)
        val near = GeoMultiPolygon(listOf(rectangle(west = 17.012, south = 60.006, east = 17.02, north = 60.01)))
        val expected = listOf(17.0 - 2 * LAT_500_M, 60.0 - LAT_500_M, 17.02 + 2 * LAT_500_M, 60.01 + LAT_500_M)
        assertBounds(expected, campusMapData(catalog(START_BUILDING to start, "T-1" to near)).bounds)
    }

    /**
     * Wiersz: V1, budynek innego terenu, dalej niż 600 m od budynków terenu z budynkiem startowym, nie powiększa
     * prostokąta mapy, a mapa pokazana na tym budynku ma prostokąt jego terenu (US1.8, BR-G5, NFR-39).
     */
    @Test
    fun br_g5_other_area_left_out() {
        val start = rectangle(west = 17.0, south = 60.0, east = 17.01, north = 60.004)
        val far = rectangle(west = 17.05, south = 60.03, east = 17.06, north = 60.04)
        val expected = listOf(17.0 - 2 * LAT_500_M, 60.0 - LAT_500_M, 17.01 + 2 * LAT_500_M, 60.004 + LAT_500_M)
        val data = campusMapData(catalog("T-1" to far, START_BUILDING to start))
        assertBounds(expected, data.bounds)
        val other = listOf(17.05 - 2 * LAT_500_M, 60.03 - LAT_500_M, 17.06 + 2 * LAT_500_M, 60.04 + LAT_500_M)
        assertBounds(other, data.boundsOf(symbol = "T-1"))
        assertBounds(expected, data.boundsOf(symbol = null))
    }

    /**
     * Wiersz: V1, prostokąt obszaru kampusu wynika z konturów w danych aplikacji, inne kontury dają inny prostokąt
     * (NFR-39).
     */
    @Test
    fun nfr39_bounds_from_data() {
        val outline = rectangle(west = -0.01, south = -0.02, east = 0.0, north = 0.0)
        val expected = listOf(-0.01 - LAT_500_M, -0.02 - LAT_500_M, LAT_500_M, LAT_500_M)
        assertBounds(expected, campusMapData(catalog("T-0" to outline)).bounds)
    }

    /**
     * Wiersz: V1, strzałki przesuwania zatrzymują się na granicy obszaru kampusu bez komunikatu; środek widoku nie
     * wychodzi poza prostokąt (NFR-53, NFR-39).
     */
    @Test
    fun nfr53_arrows_stop_at_edge() {
        val start = MapCamera(center = GeoPoint(longitude = 17.02, latitude = 60.01), zoom = 15.0)
        val edges =
            mapOf(
                PanDirection.RIGHT to GeoPoint(longitude = 17.04, latitude = 60.01),
                PanDirection.LEFT to GeoPoint(longitude = 17.0, latitude = 60.01),
                PanDirection.UP to GeoPoint(longitude = 17.02, latitude = 60.02),
                PanDirection.DOWN to GeoPoint(longitude = 17.02, latitude = 60.0),
            )
        edges.forEach { (direction, edge) ->
            val far = generateSequence(start) { limits.clamp(it.moved(direction)) }.elementAt(index = 100)
            assertEquals(start.copy(center = edge), far, "strzałka $direction")
        }
    }

    /**
     * Wiersz: V1, najmniejsze powiększenie to takie, przy którym cały prostokąt mieści się w widoku; bardziej
     * oddalić mapy się nie da (NFR-39). Na równiku pas 360/2^14 stopnia ma przy powiększeniu 14 dokładnie 512 dp.
     */
    @Test
    fun nfr39_min_zoom_whole_campus() {
        val span = 360.0 / 16_384
        val wide = CampusBounds(west = 0.0, south = -span / 8, east = span, north = span / 8)
        val tall = CampusBounds(west = 0.0, south = -span / 2, east = span / 8, north = span / 2)
        assertEquals(14.0 + log2(1.5), MapLimits(wide, widthDp = 768.0, heightDp = 1024.0).minZoom, ZOOM_DELTA)
        assertEquals(14.0 + log2(1.5), MapLimits(tall, widthDp = 1024.0, heightDp = 768.0).minZoom, ZOOM_DELTA)
        val farthest = limits.clamp(MapCamera(center = GeoPoint(longitude = 17.02, latitude = 60.01), zoom = 3.0))
        assertEquals(limits.minZoom, farthest.zoom)
    }

    private fun catalog(vararg outlines: Pair<String, GeoArea>): BuildingCatalog =
        BuildingCatalog(
            buildings =
                outlines.map { (symbol, outline) ->
                    Building(symbol = symbol, name = "Budynek testowy", outline = outline)
                },
            entrances = emptyList(),
            attribution = "",
        )

    private fun rectangle(west: Double, south: Double, east: Double, north: Double): GeoPolygon {
        val corners = listOf(west to south, east to south, east to north, west to north, west to south)
        return GeoPolygon(listOf(corners.map { (lon, lat) -> GeoPoint(longitude = lon, latitude = lat) }))
    }

    private fun assertBounds(expected: List<Double>, actual: CampusBounds?) {
        val edges = actual?.run { listOf(west, south, east, north) }.orEmpty()
        assertEquals(expected.size, edges.size, "obszar kampusu")
        expected.zip(edges).forEach { (edge, found) -> assertEquals(edge, found, DEGREE_DELTA) }
    }

    private companion object {
        const val LAT_500_M = 500.0 / 111_195.0
        const val DEGREE_DELTA = 5e-5
        const val ZOOM_DELTA = 1e-3
    }
}
