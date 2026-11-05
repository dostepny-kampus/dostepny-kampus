/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import io.github.dostepnykampus.core.data.buildings.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.map.MapListChoice
import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.core.model.BuildingCatalog
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.core.network.ApiBuilding
import io.github.dostepnykampus.core.network.ApiResult
import io.github.dostepnykampus.core.network.BuildingList
import io.github.dostepnykampus.core.network.DetailLevel
import io.github.dostepnykampus.feature.map.CampusMapViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

private const val WAIT_MS = 1_000L
private const val SETTLE_MS = 300L

/** Kontury mapy V1 od odpowiedzi serwera (wstrzymanej w teście) do stanu ekranu (US1, NFR-10, NFR-11). */
@OptIn(ExperimentalCoroutinesApi::class)
class MapCatalogFromFileTest {
    private val fileCatalog = runBlocking { BuildingCatalogRepository(EmptyCoroutineContext) { ASSET_JSON }.catalog() }

    @BeforeEach
    fun main() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun reset() {
        Dispatchers.resetMain()
    }

    // Wiersz: W1, serwer jeszcze nie odpowiedział → stan mapy ma kontury z pliku, NFR-11.
    @Test
    fun nfr11_file_without_server() {
        val screen = openScreen { CompletableDeferred<ApiResult<BuildingList>>().await() }
        assertEquals(fileCatalog, screen.waitFor { it == fileCatalog }, "mapa czeka na serwer")
    }

    // Wiersz: W2, serwer odpowiada niepustą listą → najpierw kontury z pliku, potem katalog z serwera, NFR-10, US1.
    @Test
    fun nfr10_server_replaces_file() {
        val answer = CompletableDeferred<ApiResult<BuildingList>>()
        val screen = openScreen { answer.await() }
        assertEquals(fileCatalog, screen.waitFor { it == fileCatalog }, "mapa czeka na serwer")
        answer.complete(ApiResult.Success(SERVER_LIST))
        val shown = screen.waitFor { it != fileCatalog }
        assertEquals(listOf(SERVER_BUILDING), shown?.buildings, "kontury z serwera nie zastąpiły pliku")
        assertEquals(SERVER_LIST.source, shown?.attribution, "atrybucja nie jest z serwera")
    }

    // Wiersz: W2, brak sieci albo błąd serwera → zostają kontury z pliku, bez null, błędu i pustego katalogu, NFR-10.
    @Test
    fun nfr10_failure_keeps_file() {
        listOf(ApiResult.NetworkError(IOException("brak sieci")), ApiResult.ServerError(code = SERVER_ERROR))
            .forEach { failure -> assertKeepsFile(failure) }
    }

    // Wiersz: W2, serwer odpowiada pustą listą → zostają kontury z pliku, bez pustego katalogu, NFR-10.
    @Test
    fun nfr10_empty_list_keeps_file() {
        assertKeepsFile(ApiResult.Success(BuildingList(source = "Serwer", buildings = emptyList())))
    }

    private fun assertKeepsFile(result: ApiResult<BuildingList>) {
        val answer = CompletableDeferred<ApiResult<BuildingList>>()
        val screen = openScreen { answer.await() }
        assertEquals(fileCatalog, screen.waitFor { it == fileCatalog }, "mapa czeka na serwer: $result")
        answer.complete(result)
        val seen = screen.watch()
        assertTrue(seen.isNotEmpty() && seen.all { it == fileCatalog }, "kontury zniknęły: $result")
    }

    // Wiersz: Kolejność, odczyt pliku wstrzymany, a serwer odpowiada niepustą listą → po zwolnieniu odczytu stan
    //   zostaje przy katalogu z serwera; późniejszy plik go nie nadpisuje, NFR-10.
    @Test
    fun nfr10_late_file_keeps_server() {
        val fileRead = CountDownLatch(1)
        Executors.newSingleThreadExecutor().asCoroutineDispatcher().use { reader ->
            val screen =
                openScreen(
                    ioContext = reader,
                    readGeoJson = {
                        fileRead.await(WAIT_MS * 2, TimeUnit.MILLISECONDS)
                        ASSET_JSON
                    },
                    listBuildings = { ApiResult.Success(SERVER_LIST) },
                )
            fileRead.countDown()
            screen.waitFor { it.buildings == listOf(SERVER_BUILDING) }
            val seen = screen.watch()
            assertEquals(listOf(SERVER_BUILDING), seen.lastOrNull()?.buildings, "plik nadpisał katalog z serwera")
        }
    }

    private fun openScreen(
        ioContext: CoroutineContext = EmptyCoroutineContext,
        readGeoJson: () -> String = { ASSET_JSON },
        listBuildings: suspend () -> ApiResult<BuildingList>,
    ): Screen {
        val repository =
            BuildingCatalogRepository(
                ioContext = ioContext,
                listBuildings = listBuildings,
                readGeoJson = readGeoJson,
            )
        val choice =
            object : MapListChoice {
                override val savedChoice = flowOf<Boolean?>(null)

                override suspend fun choose(isList: Boolean) = Unit
            }
        val viewModel = CampusMapViewModel(repository, { flowOf(true) }, choice)
        return Screen(viewModel)
    }

    private class Screen(val viewModel: CampusMapViewModel) {
        /** Katalogi widoczne na ekranie przez [SETTLE_MS] po odpowiedzi serwera. */
        fun watch(): List<BuildingCatalog?> {
            val seen = mutableListOf<BuildingCatalog?>()
            runBlocking { withTimeoutOrNull(SETTLE_MS) { viewModel.uiState.collect { seen += it.catalog } } }
            return seen
        }

        fun waitFor(until: (BuildingCatalog) -> Boolean): BuildingCatalog? =
            runBlocking {
                withTimeoutOrNull(WAIT_MS) { viewModel.uiState.first { it.catalog?.let(until) == true }.catalog }
            }
    }

    private companion object {
        const val SERVER_ERROR = 500

        val SERVER_BUILDING = Building(symbol = "A-1", name = "Z serwera", outline = GeoPolygon(emptyList()))

        val SERVER_LIST =
            BuildingList(source = "Serwer", buildings = listOf(ApiBuilding(SERVER_BUILDING, DetailLevel.BASIC)))

        const val ASSET_JSON =
            """{"type":"FeatureCollection","attribution":"© OpenStreetMap contributors","features":[""" +
                """{"type":"Feature","properties":{"kind":"building","symbols":["A-1"],"name":"Z pliku",""" +
                """"osm_id":"way/1"},"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}},""" +
                """{"type":"Feature","properties":{"kind":"building","symbols":["B-2"],"name":"Drugi",""" +
                """"osm_id":"way/2"},"geometry":{"type":"Polygon","coordinates":[[[2,0],[3,0],[3,1],[2,0]]]}}]}"""
    }
}
