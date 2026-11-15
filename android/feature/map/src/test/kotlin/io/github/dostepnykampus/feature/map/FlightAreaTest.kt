/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import io.github.dostepnykampus.feature.map.camera.FlightArea
import io.github.dostepnykampus.feature.map.camera.StyleArea
import io.github.dostepnykampus.feature.map.campus.CampusBounds
import io.github.dostepnykampus.feature.map.campus.MapSelection
import io.github.dostepnykampus.feature.map.style.Basemap
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Teren i podkład w stylu na czas przelotu na inny teren na prostokątach testowych (nie dane PWr). */
class FlightAreaTest {
    private val main = areaOf(CampusBounds(west = 0.0, south = 0.0, east = 0.01, north = 0.01), minZoom = 14.5)
    private val other = areaOf(CampusBounds(west = 0.03, south = 0.0, east = 0.04, north = 0.01), minZoom = 15.0)
    private val areas = FlightArea(main)

    /**
     * Wiersz: V1, zmiana terenu: na starcie przelotu podkład obejmuje stary teren; zamiana na nowy teren przychodzi,
     * gdy mapa oddali się poniżej powiększenia podkładu obu terenów, więc przy zejściu do celu wraca podkład nowego.
     *
     * Wymagania: US1.8, NFR-39
     */
    @Test
    fun keepsOldAreaAtFlightStart() {
        areas.startFlight()
        areas.area(other)
        areas.zoom(16.0)
        assertEquals(main, areas.shown, "pierwsza klatka przelotu: stary teren")
        areas.zoom(14.7)
        assertEquals(main, areas.shown, "podkład starego terenu jeszcze rysowany")
        areas.zoom(14.0)
        assertEquals(other, areas.shown, "oba podkłady ukryte: nowy teren")
        areas.endFlight()
        assertEquals(other, areas.shown, "po przelocie nowy teren")
    }

    /** Wiersz: V1, bez przelotu nowy teren i podkład są w stylu od razu. */
    @Test
    fun showsAreaAtOnceWithoutFlight() {
        areas.zoom(10.0)
        areas.area(other)
        assertEquals(other, areas.shown, "nowy teren od razu")
    }

    /** Wiersz: V1, przelot przerwany przed oddaleniem mapy kończy się terenem wybranym jako ostatni. */
    @Test
    fun showsLatestAfterCancel() {
        areas.startFlight()
        areas.area(other)
        areas.endFlight()
        assertEquals(other, areas.shown, "po przerwaniu nowy teren")
    }

    private fun areaOf(bounds: CampusBounds, minZoom: Double) =
        StyleArea(area = MapSelection(bounds = bounds), basemap = Basemap(bounds = bounds, minZoom = minZoom))
}
