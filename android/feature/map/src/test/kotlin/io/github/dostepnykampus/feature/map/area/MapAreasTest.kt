/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.area

import androidx.compose.runtime.mutableStateOf
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.core.model.building.Building
import io.github.dostepnykampus.core.model.building.BuildingCatalog
import io.github.dostepnykampus.feature.map.campus.CampusBounds
import io.github.dostepnykampus.feature.map.campus.START_BUILDING
import io.github.dostepnykampus.feature.map.campus.campusMapData
import io.github.dostepnykampus.feature.map.campus.selection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
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

    private val followed = mutableStateOf<String?>(null)

    private val announced = mutableStateOf<String?>(null)

    private val onScreen = mutableStateOf(false)

    private fun areas(symbol: String? = null) =
        MapAreas(
            data = data,
            symbol = symbol,
            chosen = chosen,
            seen = seen,
            followed = followed,
            noticeMemory = NoticeMemory(announced = announced, onScreen = onScreen),
        )

    /**
     * Wiersz: V1, budynek z innego terenu zmienia teren i daje napis „Pokazuję teren: Wittigowo · 2 km na północ”
     * z odległością i kierunkiem od terenu, o którym osoba wiedziała; napis znika, gdy osoba ruszy mapę, i nie wraca
     * dla tego terenu.
     * Scenariusz (częściowo): przegladanie-mapy.feature: Wyszukanie budynku z innego terenu zmienia teren
     *
     * Wymagania: US1.8, NFR-56
     */
    @Test
    fun showsNoticeAfterBuilding() {
        assertNull(areas().notice)
        assertNull(areas(symbol = START_BUILDING).notice)
        assertEquals(
            AreaShift(name = "Wittigowo", kilometers = 2, direction = CompassDirection.North),
            areas(symbol = "T-15").notice,
        )
        areas(symbol = "T-15").noticeShown(isShown = true)
        areas(symbol = "T-15").acknowledge()
        assertNull(areas(symbol = "T-15").notice)
        assertEquals(
            AreaShift(name = "Kampus główny", kilometers = 2, direction = CompassDirection.South),
            areas(symbol = "A-2").notice,
        )
    }

    /**
     * Wiersz: V1, ruch mapy gasi napis o zmianie terenu tylko wtedy, gdy napis był w tej chwili na ekranie.
     * Scenariusz (częściowo): przegladanie-mapy.feature: Ruch mapy gasi widoczny napis o zmianie terenu
     *
     * Wymagania: US1.8, NFR-23
     */
    @Test
    fun keepsHiddenNoticeOnMove() {
        areas(symbol = "T-15").acknowledge()
        assertEquals("Wittigowo", areas(symbol = "T-15").notice?.name)
        areas(symbol = "T-15").noticeShown(isShown = true)
        areas(symbol = "T-15").acknowledge()
        assertNull(areas(symbol = "T-15").notice)
    }

    /**
     * Wiersz: V1, TalkBack czyta napis o zmianie terenu raz: po pierwszym pokazaniu ten sam napis nie jest nowy.
     * Scenariusz (częściowo): przegladanie-mapy.feature: TalkBack czyta napis o zmianie terenu raz, po nagłówku karty
     *
     * Wymagania: US1.8, NFR-18
     */
    @Test
    fun announcesNoticeOnce() {
        assertTrue(areas(symbol = "T-15").isNoticeNew)
        areas(symbol = "T-15").noticeShown(isShown = true)
        areas(symbol = "T-15").noticeShown(isShown = false)
        assertFalse(areas(symbol = "T-15").isNoticeNew)
    }

    /**
     * Wiersz: V2, napis o zmianie terenu jest znany już w chwili wyboru budynku, zanim mapa przejdzie na jego teren,
     * więc kamera może zostawić na niego miejsce od razu.
     *
     * Wymagania: US1.8, US2.8
     */
    @Test
    fun predictsSelectionNotice() {
        val shift = AreaShift(name = "Wittigowo", kilometers = 2, direction = CompassDirection.North)
        assertNull(areas().selectionNotice)
        assertEquals(shift, areas(symbol = "T-15").selectionNotice)
        areas(symbol = "T-15").followSelection()
        assertEquals(shift, areas(symbol = "T-15").selectionNotice)
        assertNull(areas(symbol = START_BUILDING).selectionNotice)
    }

    /** Wiersz: V1, teren wybrany przez osobę w panelu nie daje napisu o zmianie terenu. Wymagania: US1.7, US1.8 */
    @Test
    fun skipsNoticeForOwnChoice() {
        areas().show("Wittigowo")
        assertNull(areas().notice)
        chosen.value = null
        assertNull(areas(symbol = "T-15").notice)
        assertEquals(
            AreaShift(name = "Bartoszowice", kilometers = 4, direction = CompassDirection.North),
            areas(symbol = "P-14").notice,
        )
    }

    /**
     * Wiersz: V1, lista terenów: wybrany pierwszy, pozostałe od najbliższego, z liczbą budynków i km.
     * Scenariusz (częściowo): przegladanie-mapy.feature: Lista terenów podaje nazwę, liczbę budynków i odległość
     *
     * Wymagania: US1.7
     */
    @Test
    fun listsNearestFirst() {
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
    fun measuresFromChosen() {
        chosen.value = "Wittigowo"
        assertEquals(listOf("Wittigowo", "Kampus główny", "Bartoszowice"), areas().choices.map(AreaChoice::name))
        assertEquals(listOf(null, 2, 4), areas().choices.map(AreaChoice::kilometers))
    }

    /**
     * Wiersz: V1, wybranie terenu przenosi granicę mapy na ten teren i daje prostokąt jego budynków.
     * Scenariusz (częściowo): przegladanie-mapy.feature: Wybór terenu z listy pokazuje jego mapę
     *
     * Wymagania: US1.7, BR-G5
     */
    @Test
    fun movesToArea() {
        val buildings = requireNotNull(areas().show("Wittigowo"))
        val area = data.areas.single { it.name == "Wittigowo" }
        assertEquals("Wittigowo", areas().current?.name)
        assertEquals(area.bounds, data.selection(area = areas().chosenName).bounds)
        assertEquals(60.0205, (buildings.south + buildings.north) / 2, DEGREE_DELTA)
    }

    /**
     * Wiersz: V1, po wyborze terenu mapa pokazuje wszystkie budynki terenu, a nie środek jego prostokąta.
     * Scenariusz (częściowo): przegladanie-mapy.feature: Po wyborze terenu widać wszystkie jego budynki
     *
     * Wymagania: US1.7, BR-G5
     */
    @Test
    fun showsAllBuildingsOfArea() {
        val start = data.areas.single { START_BUILDING in it.symbols }
        val buildings = requireNotNull(areas().show(start.name))
        val expected = CampusBounds(west = WEST, south = 60.0, east = EAST, north = 60.002 + SIDE)
        assertEquals(setOf(START_BUILDING, "A-2"), start.symbols.toSet())
        assertEquals(expected, buildings)
    }

    /** Wiersz: V1, teren spoza danych nie zmienia wyboru. Wymagania: US1.7 */
    @Test
    fun ignoresUnknownArea() {
        assertNull(areas().show("Nieznany"))
        assertNull(areas().chosenName)
    }

    /**
     * Wiersz: V1, bez wyboru terenem jest teren zaznaczonego budynku, a bez zaznaczenia startowy.
     * Scenariusz (częściowo): przegladanie-mapy.feature: Mapa otwiera się na terenie „Kampus główny”
     *
     * Wymagania: US1.6, US1.8
     */
    @Test
    fun followsBuildingArea() {
        assertEquals("Kampus główny", areas().current?.name)
        assertEquals("Bartoszowice", areas(symbol = "P-14").current?.name)
        chosen.value = "Wittigowo"
        assertEquals("Wittigowo", areas(symbol = "P-14").current?.name)
    }

    /**
     * Wiersz: V1, wybór budynku z innego terenu ustawia teren raz; zamknięcie karty zostawia ten teren i napis
     * o zmianie.
     * Scenariusz: przegladanie-mapy.feature: Zamknięcie karty budynku z innego terenu zostawia ten teren
     *
     * Wymagania: US1.8, BR-G5
     */
    @Test
    fun keepsAreaWithoutSelection() {
        areas(symbol = "T-15").followSelection()
        assertEquals("Wittigowo", chosen.value)
        areas().followSelection()
        assertEquals("Wittigowo", areas().current?.name)
        assertEquals(
            AreaShift(name = "Wittigowo", kilometers = 2, direction = CompassDirection.North),
            areas().notice,
        )
        assertNull(followed.value)
    }

    /**
     * Wiersz: V1, teren wybrany przez osobę po wyborze budynku zostaje, także gdy zaznaczenie znika albo wraca
     * to samo.
     * Scenariusz: przegladanie-mapy.feature: Teren wybrany przez osobę zostaje po zamknięciu karty
     *
     * Wymagania: US1.8, BR-G5
     */
    @Test
    fun keepsOwnChoice() {
        areas(symbol = "T-15").followSelection()
        areas(symbol = "T-15").show("Kampus główny")
        areas(symbol = "T-15").followSelection()
        assertEquals("Kampus główny", areas(symbol = "T-15").current?.name)
        areas().followSelection()
        assertEquals("Kampus główny", areas().current?.name)
        assertNull(areas().notice)
    }

    /** Wiersz: V1, nowy budynek z innego terenu po wyborze osoby znów ustawia teren raz. Wymagania: US1.8 */
    @Test
    fun setsAreaFromNewBuilding() {
        areas(symbol = "T-15").followSelection()
        areas(symbol = "T-15").show("Kampus główny")
        areas(symbol = "P-14").followSelection()
        assertEquals("Bartoszowice", areas().current?.name)
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
