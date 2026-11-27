/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import io.github.dostepnykampus.core.model.CampusBounds
import io.github.dostepnykampus.feature.map.camera.CameraMotion
import io.github.dostepnykampus.feature.map.camera.FlightArea
import io.github.dostepnykampus.feature.map.camera.StyleArea
import io.github.dostepnykampus.feature.map.campus.MapSelection
import io.github.dostepnykampus.feature.map.style.Basemap
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Teren i podkład w stylu na czas przelotu na inny teren na prostokątach testowych (nie dane PWr). */
class FlightAreaTest {
    private val main = areaOf(CampusBounds(west = 0.0, south = 0.0, east = 0.01, north = 0.01), minZoom = 14.5)
    private val other = areaOf(CampusBounds(west = 0.03, south = 0.0, east = 0.04, north = 0.01), minZoom = 15.0)
    private val areas = FlightArea(main)

    /**
     * Wiersz: V1, zmiana terenu: przez cały przelot styl ma teren startowy z podkładem rysowanym przy każdym
     * powiększeniu, więc mapa w przelocie nie jest pusta; po przelocie nowy teren z jego podkładem.
     *
     * Wymagania: US1.8, NFR-39
     */
    @Test
    fun drawsBasemapThroughFlight() {
        areas.start(CameraMotion.FLIGHT)
        areas.area(other)
        areas.zoom(16.0)
        assertEquals(main.area, areas.shown.area, "pierwsza klatka przelotu: stary teren")
        areas.zoom(12.0)
        assertEquals(main.area, areas.shown.area, "w środku przelotu stary teren")
        assertEquals(0.0, areas.shown.hiddenBelow, "podkład przy każdym powiększeniu")
        areas.endFlight()
        assertEquals(other, areas.shown, "po przelocie nowy teren")
    }

    /**
     * Wiersz: V1, zmiana terenu: w przelocie kafelki podkładu tylko w prostokątach starego i nowego terenu,
     * po przelocie tylko nowego.
     *
     * Wymagania: NFR-39
     */
    @Test
    fun limitsTilesToBothAreasInFlight() {
        val tiles = mutableListOf<List<CampusBounds>>()
        val flight = FlightArea(first = main, onTileAreas = { tiles += it })
        flight.start(CameraMotion.FLIGHT)
        flight.area(other)
        assertEquals(listOf(main.area.bounds, other.area.bounds), tiles.last(), "w przelocie oba tereny")
        flight.endFlight()
        assertEquals(listOf(other.area.bounds), tiles.last(), "po przelocie nowy teren")
    }

    /** Wiersz: V1, bez przelotu nowy teren i podkład są w stylu od razu, a po spoczynku mapy zostaje sam nowy teren. */
    @Test
    fun showsAreaAtOnceWithoutFlight() {
        areas.zoom(10.0)
        areas.area(other)
        assertEquals(other.area, areas.shown.area, "nowy teren od razu")
        areas.endFlight()
        assertEquals(other, areas.shown, "po spoczynku sam nowy teren")
    }

    /**
     * Wiersz: V1, zmiana terenu: teren zmienia się tuż przed startem przelotu. Podkład starego terenu zostaje na
     * ekranie, a kafelki wolno pobierać w prostokątach obu terenów.
     *
     * Wymagania: US1.8, NFR-39, NFR-56
     */
    @Test
    fun keepsBasemapOnEarlyChange() {
        val tiles = mutableListOf<List<CampusBounds>>()
        val flight = FlightArea(first = main, onTileAreas = { tiles += it })
        flight.zoom(14.6)
        flight.area(other)
        assertTrue(flight.isBasemapKept, "podkład starego terenu przy bieżącym powiększeniu")
        assertEquals(14.5, flight.shown.hiddenBelow, "podkład od mniejszego powiększenia")
        flight.start(CameraMotion.FLIGHT)
        assertTrue(flight.isBasemapKept, "start przelotu nie chowa podkładu")
        assertEquals(listOf(main.area.bounds, other.area.bounds), tiles.last(), "w przelocie oba tereny")
    }

    /** Wiersz: V1, przelot przerwany przed oddaleniem mapy kończy się terenem wybranym jako ostatni. */
    @Test
    fun showsLatestAfterCancel() {
        areas.start(CameraMotion.FLIGHT)
        areas.area(other)
        areas.endFlight()
        assertEquals(other, areas.shown, "po przerwaniu nowy teren")
    }

    /**
     * Wiersz: V1, krótki przelot na inny teren nie oddala mapy poniżej powiększenia podkładu: teren zmienia się
     * przy lądowaniu, a podkład starego terenu zostaje na ekranie, zamiast chować się do wczytania nowego.
     *
     * Wymagania: US1.8, NFR-56
     */
    @Test
    fun isBasemapKeptAfterShortFlight() {
        areas.zoom(16.0)
        areas.start(CameraMotion.FLIGHT)
        areas.area(other)
        areas.zoom(15.2)
        areas.endFlight()
        assertEquals(other, areas.shown, "po przelocie nowy teren")
        assertTrue(areas.isBasemapKept, "podkład starego terenu zostaje do wczytania nowego")
    }

    /** Wiersz: V1, przelot oddalił mapę i zszedł do celu: podkład był na ekranie przez cały lot, więc zostaje. */
    @Test
    fun keepsBasemapAfterDeepFlight() {
        areas.zoom(16.0)
        areas.start(CameraMotion.FLIGHT)
        areas.area(other)
        areas.zoom(12.0)
        areas.zoom(16.0)
        areas.endFlight()
        assertEquals(other, areas.shown, "po przelocie nowy teren")
        assertTrue(areas.isBasemapKept, "podkład rysowany przez cały lot")
    }

    /** Wiersz: V1, przelot przerwany gestem daleko od celu: podkład nowego terenu tam nie jest rysowany. */
    @Test
    fun hidesBasemapAfterFlightCutLow() {
        areas.zoom(16.0)
        areas.start(CameraMotion.FLIGHT)
        areas.area(other)
        areas.zoom(12.0)
        areas.endFlight()
        assertEquals(other, areas.shown, "po przerwaniu nowy teren")
        assertFalse(areas.isBasemapKept, "widok poniżej podkładu nowego terenu")
    }

    /** Wiersz: V1, skok na inny teren bez animacji: stary podkład nie leży pod nowym widokiem. */
    @Test
    fun hidesBasemapAfterJump() {
        areas.zoom(16.0)
        areas.area(other)
        areas.start(CameraMotion.NONE)
        assertEquals(other, areas.shown, "po skoku nowy teren")
        assertFalse(areas.isBasemapKept, "kafelki nowego widoku nie są wczytane")
    }

    /** Wiersz: V1, przesunięcie na bliski teren: podkład zostaje na ekranie. */
    @Test
    fun isBasemapKeptOnShift() {
        areas.zoom(16.0)
        areas.start(CameraMotion.EASE)
        areas.area(other)
        assertTrue(areas.isBasemapKept, "przesunięcie bez skoku: podkład zostaje")
    }

    /** Wiersz: V1, widok poniżej podkładu nowego terenu: w spoczynku podkład nie jest rysowany, więc czeka ukryty. */
    @Test
    fun hidesBelowNewBasemapZoom() {
        areas.zoom(14.8)
        areas.area(other)
        areas.endFlight()
        assertFalse(areas.isBasemapKept, "nowy podkład od wyższego powiększenia")
    }

    private fun areaOf(bounds: CampusBounds, minZoom: Double) =
        StyleArea(area = MapSelection(bounds = bounds), basemap = Basemap(minZoom = minZoom))
}
