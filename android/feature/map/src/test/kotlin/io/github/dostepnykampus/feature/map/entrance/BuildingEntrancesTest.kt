/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.entrance

import androidx.lifecycle.viewModelScope
import io.github.dostepnykampus.core.data.building.ServerEntrances
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.building.AssessmentSource
import io.github.dostepnykampus.core.model.building.BuildingCatalog
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceAssessment
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.feature.map.campus.selectedEntrance
import io.github.dostepnykampus.feature.map.navigation.MapFocus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Wejścia budynku z serwera w arkuszu wejścia i w pinezce na V1, te same co na karcie V2; bez nich wejścia z pliku.
 *
 * Wymagania: US2.1, US3.3, BR-25, NFR-10
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BuildingEntrancesTest {
    /**
     * Wiersz: wejście `pwr/1` tylko z planów PWr, pobrane z serwera dla A-1 → arkusz wejścia je znajduje,
     *   z nazwą, oznaczeniem i źródłem z serwera.
     *
     * Wymagania: US3.3, BR-27
     */
    @Test
    fun opensServerOnlyEntrance() {
        val shown = catalogWith(listOf(SERVER_ONLY)).shownEntrance("pwr/1")
        assertEquals(SERVER_ONLY, shown?.entrance, "arkusz wejścia z planów PWr")
    }

    /**
     * Wiersz: wejście z parą w pliku konturów (`node/1`) → arkusz ma oznaczenie i nazwę z serwera, nie z pliku;
     *   wejścia innych budynków zostają z pliku.
     *
     * Wymagania: US2.1, US17.3, BR-25
     */
    @Test
    fun replacesFileEntrancesOfBuilding() {
        val catalog = catalogWith(listOf(SERVER_MATCH, SERVER_ONLY))
        assertEquals(SERVER_MATCH, catalog.shownEntrance("node/1")?.entrance, "wejście A-1 z serwera")
        assertEquals(OTHER_BUILDING, catalog.shownEntrance("node/9")?.entrance, "wejście C-13 z pliku")
        assertEquals(3, catalog.entrances.size, "dwa wejścia A-1 z serwera i jedno C-13")
    }

    /**
     * Wiersz: „Pokaż wejście na mapie” dla wejścia `pwr/1` → pinezka stoi w punkcie wejścia z serwera.
     *
     * Wymagania: US3.3
     */
    @Test
    fun pinsServerLocation() {
        val catalog = catalogWith(listOf(SERVER_ONLY))
        val pin = selectedEntrance(catalog = catalog, id = "pwr/1", buildingSymbol = "A-1", unnamed = "")
        assertEquals(SERVER_ONLY.location, pin?.location, "pinezka w punkcie z serwera")
    }

    /**
     * Wiersz: serwer nie podał wejść (brak sieci, nic nie pobrano) albo nie ma budynku → wejścia z pliku konturów,
     *   jak na karcie V2.
     *
     * Wymagania: NFR-10
     */
    @Test
    fun keepsFileWithoutServer() {
        val empty = BuildingEntrances(symbol = "A-1", entrances = emptyList())
        assertEquals(CATALOG, CATALOG.withBuildingEntrances(empty), "pusta odpowiedź serwera")
        assertEquals(CATALOG, CATALOG.withBuildingEntrances(null), "brak odpowiedzi serwera")
    }

    /**
     * Wiersz: zaznaczenie budynku A-1 albo wejścia budynku A-1 → wejścia budynku A-1; bez zaznaczenia brak budynku.
     *
     * Wymagania: US3.3
     */
    @Test
    fun picksFocusedBuilding() {
        assertEquals("A-1", focusedBuilding(MapFocus.Building(symbol = "A-1")), "budynek")
        assertEquals("A-1", focusedBuilding(MapFocus.Entrance(id = "pwr/1", buildingSymbol = "A-1")), "wejście")
        assertEquals(null, focusedBuilding(null), "bez zaznaczenia")
    }

    /**
     * Wiersz: budynek A-1 wybrany na V1 → model widoku pyta serwer o wejścia A-1; bez budynku nic nie podaje.
     *
     * Wymagania: US3.3, NFR-10
     */
    @Test
    fun fetchesChosenBuilding() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val asked = mutableListOf<String>()
            val model =
                BuildingEntrancesViewModel(
                    ServerEntrances { symbol ->
                        asked += symbol
                        listOf(SERVER_ONLY)
                    },
                )
            val collector = backgroundScope.launch { model.shown.collect {} }
            model.show("A-1")
            advanceUntilIdle()
            val fetched = model.shown.value
            model.show(null)
            advanceUntilIdle()
            val cleared = model.shown.value
            collector.cancel()
            model.viewModelScope.cancel()
            advanceUntilIdle()
            Dispatchers.resetMain()
            assertEquals(BuildingEntrances(symbol = "A-1", entrances = listOf(SERVER_ONLY)), fetched, "wejścia A-1")
            assertEquals(listOf("A-1"), asked, "jedno pytanie o A-1")
            assertEquals(null, cleared, "bez budynku bez wejść")
        }

    private fun catalogWith(entrances: List<Entrance>): BuildingCatalog =
        CATALOG.withBuildingEntrances(BuildingEntrances(symbol = "A-1", entrances))

    private companion object {
        val DAY: LocalDate = LocalDate.of(2024, 10, 12)

        val FILE_MATCH = entrance(id = "node/1", symbol = "A-1", name = null, marking = EntranceMarking.NO_DATA)
        val OTHER_BUILDING =
            entrance(id = "node/9", symbol = "C-13", name = "Główne", marking = EntranceMarking.NO_DATA)
        val SERVER_MATCH =
            entrance(id = "node/1", symbol = "A-1", name = "Wejście Cesarskie", marking = EntranceMarking.ACCESSIBLE)
                .copy(assessment = EntranceAssessment(DAY, AssessmentSource.PWR_PLANS), doorWidthCm = 120)
        val SERVER_ONLY =
            entrance(id = "pwr/1", symbol = "A-1", name = "Wejście", marking = EntranceMarking.INACCESSIBLE)
                .copy(
                    location = GeoPoint(longitude = 17.06, latitude = 51.11),
                    assessment = EntranceAssessment(DAY, AssessmentSource.PWR_PLANS),
                    approach = "Do wejścia prowadzą schody.",
                )

        val CATALOG =
            BuildingCatalog(buildings = emptyList(), entrances = listOf(FILE_MATCH, OTHER_BUILDING), attribution = "©")

        fun entrance(id: String, symbol: String, name: String?, marking: EntranceMarking): Entrance =
            Entrance(
                id = id,
                buildingSymbol = symbol,
                name = name,
                isMain = false,
                type = null,
                location = GeoPoint(longitude = 17.0, latitude = 51.0),
                marking = marking,
            )
    }
}
