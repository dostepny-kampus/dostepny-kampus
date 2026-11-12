/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import io.github.dostepnykampus.core.data.buildings.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.map.MapListChoice
import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.core.model.BuildingCatalog
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.core.network.ApiBuilding
import io.github.dostepnykampus.core.network.ApiResult
import io.github.dostepnykampus.core.network.BuildingList
import io.github.dostepnykampus.core.network.DetailLevel
import io.github.dostepnykampus.feature.map.data.campusMapData
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Kontury mapy V1 od odpowiedzi serwera (wstrzymanej w teście) do stanu ekranu (US1, NFR-10, NFR-11). Odczyt pliku
 * i liczenie geometrii idą na dyspozytorze testowym, więc stan jest sprawdzany po zakończeniu całej zaplanowanej pracy,
 * nie po upływie czasu.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MapCatalogFromFileTest {
    private val fileCatalog = runBlocking { BuildingCatalogRepository(EmptyCoroutineContext) { ASSET_JSON }.catalog() }
    private val scheduler = TestCoroutineScheduler()
    private val background = StandardTestDispatcher(scheduler)
    private val screens = mutableListOf<Job>()

    @BeforeEach
    fun main() {
        Dispatchers.setMain(UnconfinedTestDispatcher(scheduler))
    }

    @AfterEach
    fun reset() {
        screens.forEach(Job::cancel)
        Dispatchers.resetMain()
    }

    /** Wiersz: W1, serwer jeszcze nie odpowiedział → stan mapy ma kontury z pliku, NFR-11. */
    @Test
    fun nfr11_file_without_server() {
        val screen = openScreen { CompletableDeferred<ApiResult<BuildingList>>().await() }
        assertEquals(fileCatalog, screen.settled(), "mapa czeka na serwer")
    }

    /** Wiersz: W2, serwer odpowiada niepustą listą → najpierw kontury z pliku, potem katalog z serwera, NFR-10, US1. */
    @Test
    fun nfr10_server_replaces_file() {
        val answer = CompletableDeferred<ApiResult<BuildingList>>()
        val screen = openScreen { answer.await() }
        assertEquals(fileCatalog, screen.settled(), "mapa czeka na serwer")
        answer.complete(ApiResult.Success(SERVER_LIST))
        val shown = screen.settled()
        assertEquals(listOf(SERVER_BUILDING), shown?.buildings, "kontury z serwera nie zastąpiły pliku")
        assertEquals(SERVER_LIST.source, shown?.attribution, "atrybucja nie jest z serwera")
    }

    /**
     * Wiersz: W2, brak sieci albo błąd serwera → zostają kontury z pliku, bez null, błędu i pustego katalogu, NFR-10.
     */
    @Test
    fun nfr10_failure_keeps_file() {
        listOf(ApiResult.NetworkError(IOException("brak sieci")), ApiResult.ServerError(code = SERVER_ERROR))
            .forEach { failure -> assertKeepsFile(failure) }
    }

    /** Wiersz: W2, serwer odpowiada pustą listą → zostają kontury z pliku, bez pustego katalogu, NFR-10. */
    @Test
    fun nfr10_empty_list_keeps_file() {
        assertKeepsFile(ApiResult.Success(BuildingList(source = "Serwer", buildings = emptyList())))
    }

    private fun assertKeepsFile(result: ApiResult<BuildingList>) {
        val answer = CompletableDeferred<ApiResult<BuildingList>>()
        val screen = openScreen { answer.await() }
        assertEquals(fileCatalog, screen.settled(), "mapa czeka na serwer: $result")
        answer.complete(result)
        screen.settled()
        val fromFile = screen.seen.dropWhile { it != fileCatalog }
        assertTrue(fromFile.isNotEmpty() && fromFile.all { it == fileCatalog }, "kontury zniknęły: $result")
    }

    /**
     * Wiersz: Kolejność, odczyt pliku wstrzymany, a serwer odpowiada niepustą listą → po zwolnieniu odczytu stan
     *   zostaje przy katalogu z serwera; późniejszy plik go nie nadpisuje, NFR-10.
     */
    @Test
    fun nfr10_late_file_keeps_server() {
        val screen = openScreen { ApiResult.Success(SERVER_LIST) }
        assertTrue(screen.seen.all { it == null }, "plik wczytany, zanim odczyt ruszył")
        screen.settled()
        val fromServer = screen.seen.dropWhile { it?.buildings != listOf(SERVER_BUILDING) }
        assertTrue(
            fromServer.isNotEmpty() && fromServer.all { it?.buildings == listOf(SERVER_BUILDING) },
            "plik nadpisał katalog z serwera: $fromServer",
        )
    }

    private fun openScreen(listBuildings: suspend () -> ApiResult<BuildingList>): Screen {
        val repository =
            BuildingCatalogRepository(
                ioContext = background,
                listBuildings = listBuildings,
                readGeoJson = { ASSET_JSON },
            )
        val choice =
            object : MapListChoice {
                override val savedChoice = flowOf<Boolean?>(null)

                override suspend fun choose(isList: Boolean) = Unit
            }
        val viewModel =
            CampusMapViewModel(
                repository = repository,
                networkStatus = { flowOf(true) },
                mapListChoice = choice,
                computeContext = background,
                geometry = ::campusMapData,
            )
        return Screen(viewModel)
    }

    /** Widoczny ekran: zbiera każdy katalog ze stanu do końca testu. */
    private inner class Screen(val viewModel: CampusMapViewModel) {
        val seen = mutableListOf<BuildingCatalog?>()

        init {
            screens +=
                CoroutineScope(UnconfinedTestDispatcher(scheduler)).launch {
                    viewModel.uiState.collect { seen += it.catalog }
                }
        }

        /** Wykonuje całą zaplanowaną pracę (odczyt pliku, odpowiedź, geometria) i zwraca katalog na ekranie. */
        fun settled(): BuildingCatalog? {
            scheduler.advanceUntilIdle()
            return viewModel.uiState.value.catalog
        }
    }

    private companion object {
        const val SERVER_ERROR = 500

        val SERVER_OUTLINE =
            GeoPolygon(
                listOf(
                    listOf(
                        GeoPoint(longitude = 0.0, latitude = 0.0),
                        GeoPoint(longitude = 1.0, latitude = 0.0),
                        GeoPoint(longitude = 1.0, latitude = 1.0),
                        GeoPoint(longitude = 0.0, latitude = 0.0),
                    ),
                ),
            )

        val SERVER_BUILDING = Building(symbol = "A-1", name = "Z serwera", outline = SERVER_OUTLINE)

        val SERVER_LIST =
            BuildingList(source = "Serwer", buildings = listOf(ApiBuilding(SERVER_BUILDING, DetailLevel.BASIC)))

        const val ASSET_JSON =
            """{"type":"FeatureCollection","attribution":"© OpenStreetMap contributors","features":[""" +
                """{"type":"Feature","properties":{"kind":"building","symbols":["A-1"],"display_name":"Z pliku",""" +
                """"osm_id":"way/1"},"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}},""" +
                """{"type":"Feature","properties":{"kind":"building","symbols":["B-2"],"display_name":"Drugi",""" +
                """"osm_id":"way/2"},"geometry":{"type":"Polygon","coordinates":[[[2,0],[3,0],[3,1],[2,0]]]}}]}"""
    }
}
