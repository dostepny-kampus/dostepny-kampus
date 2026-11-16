/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import io.github.dostepnykampus.core.geo.PwrAreaBoundaries
import io.github.dostepnykampus.core.geo.allRings
import io.github.dostepnykampus.core.model.GeoArea
import io.github.dostepnykampus.core.model.GeoMultiPolygon
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.core.model.building.Building
import io.github.dostepnykampus.core.model.building.BuildingCatalog
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.camera.MapLimits
import io.github.dostepnykampus.feature.map.camera.PanDirection
import io.github.dostepnykampus.feature.map.camera.moved
import io.github.dostepnykampus.feature.map.campus.CampusBounds
import io.github.dostepnykampus.feature.map.campus.START_BUILDING
import io.github.dostepnykampus.feature.map.campus.boundsOf
import io.github.dostepnykampus.feature.map.campus.campusMapData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import kotlin.math.cos

/** Teren mapy V1 i granice widoku na konturach testowych „Budynek testowy” (nie dane PWr). */
class CampusBoundsTest {
    private val bounds = CampusBounds(west = 17.0, south = 60.0, east = 17.04, north = 60.02)
    private val limits = MapLimits(bounds, widthDp = 400.0, heightDp = 800.0)

    /**
     * Wiersz: V1, mapa zostaje na wybranym terenie: teren spoza pliku granic ma za granicę kontury budynków, więc
     * prostokąt z konturami budynków tego terenu, powiększony o 100 m z każdej strony, nie tylko przy budynku
     * startowym. Na 60° szerokości 100 m na wschód to dwa razy więcej stopni niż 100 m na północ.
     *
     * Wymagania: US1.7, BR-G5, NFR-39
     */
    @Test
    fun addsMarginAroundArea() {
        val start = rectangle(west = 17.0, south = 60.0, east = 17.01, north = 60.004)
        val near = GeoMultiPolygon(listOf(rectangle(west = 17.012, south = 60.006, east = 17.02, north = 60.01)))
        val expected = listOf(17.0 - 2 * LAT_100_M, 60.0 - LAT_100_M, 17.02 + 2 * LAT_100_M, 60.01 + LAT_100_M)
        assertBounds(expected, campusMapData(catalog(START_BUILDING to start, "T-1" to near)).bounds)
    }

    /**
     * Wiersz: V1, prostokąt terenu z jego granicy z pliku, większej niż kontury, powiększonej o 100 m; granica, której
     * prostokąt nie obejmuje budynków terenu (np. budynek dodany na serwerze), zostaje pominięta i liczą się kontury.
     *
     * Wymagania: BR-G5, NFR-39
     */
    @Test
    fun takesBoundsFromBoundary() {
        val start = rectangle(west = 17.0, south = 60.0, east = 17.01, north = 60.004)
        val boundary = rectangle(west = 16.99, south = 59.99, east = 17.02, north = 60.01)
        val wider = listOf(16.99 - 2 * LAT_100_M, 59.99 - LAT_100_M, 17.02 + 2 * LAT_100_M, 60.01 + LAT_100_M)
        val catalog = catalog(START_BUILDING to start)
        val bounded = campusMapData(catalog, mapOf("Kampus główny" to boundary))
        assertBounds(wider, bounded.bounds)
        assertEquals(boundary, bounded.areas.single().boundary, "Teren ma granicę z pliku")
        val aside = rectangle(west = 17.005, south = 60.0, east = 17.02, north = 60.004)
        val outlines = listOf(17.0 - 2 * LAT_100_M, 60.0 - LAT_100_M, 17.01 + 2 * LAT_100_M, 60.004 + LAT_100_M)
        val skipped = campusMapData(catalog, mapOf("Kampus główny" to aside))
        assertBounds(outlines, skipped.bounds)
        assertNull(skipped.areas.single().boundary, "Pominięta granica nie trafia na mapę")
    }

    /**
     * Wiersz: V1, teren „T-9 Atol” z danymi PWr: prostokąt opisany na granicy z pliku granic terenów, powiększony
     * o 100 m z każdej strony.
     *
     * Wymagania: BR-G5, NFR-39
     */
    @Test
    fun readsAtolFromPwrBoundary() {
        val points = PwrAreaBoundaries.getValue("T-9 Atol").allRings().flatten()
        val (west, east) = points.minOf { it.longitude } to points.maxOf { it.longitude }
        val (south, north) = points.minOf { it.latitude } to points.maxOf { it.latitude }
        val inside = rectangle(west = west, south = south, east = west + 1e-5, north = south + 1e-5)
        val eastward = LAT_100_M / cos(Math.toRadians((south + north) / 2))
        val expected = listOf(west - eastward, south - LAT_100_M, east + eastward, north + LAT_100_M)
        assertBounds(expected, campusMapData(catalog("T-9" to inside)).bounds)
    }

    /**
     * Wiersz: V1, budynek innego terenu, dalej niż 600 m od budynków terenu z budynkiem startowym, nie powiększa
     * prostokąta mapy, a mapa pokazana na tym budynku ma prostokąt jego terenu.
     *
     * Wymagania: US1.8, BR-G5, NFR-39
     */
    @Test
    fun leavesOtherAreaOut() {
        val start = rectangle(west = 17.0, south = 60.0, east = 17.01, north = 60.004)
        val far = rectangle(west = 17.05, south = 60.03, east = 17.06, north = 60.04)
        val expected = listOf(17.0 - 2 * LAT_100_M, 60.0 - LAT_100_M, 17.01 + 2 * LAT_100_M, 60.004 + LAT_100_M)
        val data = campusMapData(catalog("T-1" to far, START_BUILDING to start))
        assertBounds(expected, data.bounds)
        val other = listOf(17.05 - 2 * LAT_100_M, 60.03 - LAT_100_M, 17.06 + 2 * LAT_100_M, 60.04 + LAT_100_M)
        assertBounds(other, data.boundsOf(symbol = "T-1"))
        assertBounds(expected, data.boundsOf(symbol = null))
    }

    /**
     * Wiersz: V1, prostokąt obszaru kampusu wynika z konturów w danych aplikacji, inne kontury dają inny prostokąt.
     *
     * Wymagania: NFR-39
     */
    @Test
    fun takesBoundsFromData() {
        val outline = rectangle(west = -0.01, south = -0.02, east = 0.0, north = 0.0)
        val expected = listOf(-0.01 - LAT_100_M, -0.02 - LAT_100_M, LAT_100_M, LAT_100_M)
        assertBounds(expected, campusMapData(catalog("T-0" to outline)).bounds)
    }

    /**
     * Wiersz: V1, strzałki przesuwania zatrzymują się bez komunikatu, gdy brzeg widoku dojdzie do brzegu prostokąta
     * terenu; środek stoi wtedy pół widoku od brzegu.
     * Scenariusz (częściowo): przegladanie-mapy.feature: Mapa nie wychodzi poza wybrany teren
     *
     * Wymagania: NFR-53, NFR-39
     */
    @Test
    fun stopsArrowsAtEdge() {
        val start = MapCamera(center = GeoPoint(longitude = 17.02, latitude = 60.01), zoom = 15.0)
        val box = limits.centerBounds(zoom = 15.0)
        val edges =
            mapOf(
                PanDirection.RIGHT to GeoPoint(longitude = box.east, latitude = 60.01),
                PanDirection.LEFT to GeoPoint(longitude = box.west, latitude = 60.01),
                PanDirection.UP to GeoPoint(longitude = 17.02, latitude = box.north),
                PanDirection.DOWN to GeoPoint(longitude = 17.02, latitude = box.south),
            )
        edges.forEach { (direction, edge) ->
            val far = generateSequence(start) { limits.clamp(it.moved(direction)) }.elementAt(index = 100)
            assertEquals(start.copy(center = edge), far, "strzałka $direction")
        }
    }

    /**
     * Wiersz: V1, najmniejsze powiększenie to takie, przy którym prostokąt wypełnia widok w krótszym wymiarze;
     * bardziej oddalić mapy się nie da. Na równiku pas 360/2^14 stopnia ma przy powiększeniu 14 dokładnie 512 dp:
     * pas 512 × 128 dp wypełnia 1024 dp wysokości przy powiększeniu 17, a pas 64 × 512 dp — 1024 dp szerokości
     * przy 18.
     * Scenariusz (częściowo): przegladanie-mapy.feature:
     *   Przy najmniejszym powiększeniu teren wypełnia mapę w krótszym wymiarze
     *
     * Wymagania: NFR-39
     */
    @Test
    fun fillsShorterSideAtMinZoom() {
        val span = 360.0 / 16_384
        val wide = CampusBounds(west = 0.0, south = -span / 8, east = span, north = span / 8)
        val tall = CampusBounds(west = 0.0, south = -span / 2, east = span / 8, north = span / 2)
        assertEquals(17.0, MapLimits(wide, widthDp = 768.0, heightDp = 1024.0).minZoom, ZOOM_DELTA)
        assertEquals(18.0, MapLimits(tall, widthDp = 1024.0, heightDp = 768.0).minZoom, ZOOM_DELTA)
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
        const val LAT_100_M = 100.0 / 111_195.0
        const val DEGREE_DELTA = 5e-5
        const val ZOOM_DELTA = 1e-3
    }
}
