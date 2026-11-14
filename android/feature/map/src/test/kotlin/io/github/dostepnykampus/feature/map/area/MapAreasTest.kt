/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.area

import androidx.compose.runtime.mutableStateOf
import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.core.model.BuildingCatalog
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.feature.map.START_ZOOM
import io.github.dostepnykampus.feature.map.data.AreaChoice
import io.github.dostepnykampus.feature.map.data.START_BUILDING
import io.github.dostepnykampus.feature.map.data.campusMapData
import io.github.dostepnykampus.feature.map.data.selection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Tereny mapy V1 na konturach testowych „Budynek testowy” (nie dane PWr): teren startowy z dwoma budynkami,
 * „T-15” około 2,2 km na północ i „P-14” około 6,7 km na północ.
 */
class MapAreasTest {
    private val data =
        campusMapData(
            BuildingCatalog(
                buildings =
                    listOf(
                        building("P-14", south = 60.06),
                        building(START_BUILDING, south = 60.0),
                        building("T-15", south = 60.02),
                        building("A-2", south = 60.002),
                    ),
                entrances = emptyList(),
                attribution = "",
            ),
        )
    private val chosen = mutableStateOf<String?>(null)

    private val seen = mutableStateOf<String?>(null)

    private fun areas(symbol: String? = null) = MapAreas(data = data, symbol = symbol, chosen = chosen, seen = seen)

    /**
     * Wiersz: V1, budynek z innego terenu zmienia teren i daje napis „Pokazuję teren: Wittigowo”; napis znika,
     * gdy osoba ruszy mapę, i nie wraca dla tego terenu.
     *
     * Wymagania: US1.8
     */
    @Test
    fun us1_8_notice_after_building() {
        assertNull(areas().notice)
        assertNull(areas(symbol = START_BUILDING).notice)
        assertEquals("Wittigowo", areas(symbol = "T-15").notice)
        areas(symbol = "T-15").acknowledge()
        assertNull(areas(symbol = "T-15").notice)
        assertEquals("Kampus główny", areas(symbol = "A-2").notice)
    }

    /** Wiersz: V1, teren wybrany przez osobę w panelu nie daje napisu o zmianie terenu. Wymagania: US1.7, US1.8 */
    @Test
    fun us1_8_no_notice_for_own_choice() {
        areas().show("Wittigowo")
        assertNull(areas().notice)
        chosen.value = null
        assertNull(areas(symbol = "T-15").notice)
        assertEquals("Bartoszowice", areas(symbol = "P-14").notice)
    }

    /**
     * Wiersz: V1, lista terenów: wybrany pierwszy, pozostałe od najbliższego, z liczbą budynków i km.
     *
     * Wymagania: US1.7
     */
    @Test
    fun us1_7_choices_nearest_first() {
        val expected =
            listOf(
                AreaChoice(name = "Kampus główny", buildings = 2, kilometers = null),
                AreaChoice(name = "Wittigowo", buildings = 1, kilometers = 2),
                AreaChoice(name = "Bartoszowice", buildings = 1, kilometers = 7),
            )
        assertEquals(expected, areas().choices)
    }

    /** Wiersz: V1, odległość na liście liczy się od wybranego terenu, nie od startowego. Wymagania: US1.7 */
    @Test
    fun us1_7_distance_from_chosen() {
        chosen.value = "Wittigowo"
        assertEquals(listOf("Wittigowo", "Kampus główny", "Bartoszowice"), areas().choices.map(AreaChoice::name))
        assertEquals(listOf(null, 2, 4), areas().choices.map(AreaChoice::kilometers))
    }

    /**
     * Wiersz: V1, wybranie terenu przenosi granicę mapy na ten teren i daje jego widok startowy.
     *
     * Wymagania: US1.7, BR-G5
     */
    @Test
    fun us1_7_show_moves_to_area() {
        val camera = requireNotNull(areas().show("Wittigowo"))
        val area = data.areas.single { it.name == "Wittigowo" }
        assertEquals("Wittigowo", areas().current?.name)
        assertEquals(area.bounds, data.selection(area = areas().chosenName).bounds)
        assertEquals(START_ZOOM, camera.zoom)
        assertEquals(60.0205, camera.center.latitude, DEGREE_DELTA)
    }

    /** Wiersz: V1, teren spoza danych nie zmienia wyboru. Wymagania: US1.7 */
    @Test
    fun us1_7_unknown_area_ignored() {
        assertNull(areas().show("Nieznany"))
        assertNull(areas().chosenName)
    }

    /**
     * Wiersz: V1, bez wyboru terenem jest teren zaznaczonego budynku, a bez zaznaczenia startowy.
     *
     * Wymagania: US1.6, US1.8
     */
    @Test
    fun us1_8_area_follows_building() {
        assertEquals("Kampus główny", areas().current?.name)
        assertEquals("Bartoszowice", areas(symbol = "P-14").current?.name)
        chosen.value = "Wittigowo"
        assertEquals("Wittigowo", areas(symbol = "P-14").current?.name)
    }

    private fun building(symbol: String, south: Double): Building {
        val north = south + SIDE
        val corners = listOf(WEST to south, EAST to south, EAST to north, WEST to north, WEST to south)
        val outline = GeoPolygon(listOf(corners.map { (lon, lat) -> GeoPoint(longitude = lon, latitude = lat) }))
        return Building(symbol = symbol, name = "Budynek testowy", outline = outline)
    }

    private companion object {
        const val WEST = 17.0
        const val EAST = 17.001
        const val SIDE = 0.001
        const val DEGREE_DELTA = 1e-4
    }
}
