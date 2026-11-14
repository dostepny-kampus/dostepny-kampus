/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import io.github.dostepnykampus.core.data.barriers.AcceptedBarrier
import io.github.dostepnykampus.core.data.barriers.AcceptedBarriers
import io.github.dostepnykampus.core.data.buildings.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.map.MapListChoice
import io.github.dostepnykampus.core.data.testing.ServerBuildings
import io.github.dostepnykampus.core.data.testing.ServerReply
import io.github.dostepnykampus.core.data.testing.serverBuildingCatalog
import io.github.dostepnykampus.core.geo.parseGeoArea
import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.feature.map.data.campusMapData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.coroutines.EmptyCoroutineContext

/** Stan V1 w ViewModel na podróbkach źródeł: katalog z pliku testowego, internet i wybór „Mapa” albo „Lista”. */
@OptIn(ExperimentalCoroutinesApi::class)
class CampusMapViewModelTest {
    private val online = MutableStateFlow(true)
    private val saved = MutableStateFlow<Boolean?>(null)
    private val choice =
        object : MapListChoice {
            override val savedChoice = saved

            override suspend fun choose(isList: Boolean) {
                saved.value = isList
            }
        }
    private val geoJson =
        """{"type":"FeatureCollection","attribution":"© OpenStreetMap contributors","features":[""" +
            """{"type":"Feature","properties":{"kind":"building","symbols":["C-13"],"osm_id":"way/1"},""" +
            """"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}}]}"""
    private val repository = BuildingCatalogRepository(EmptyCoroutineContext) { geoJson }

    @BeforeEach
    fun main() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun reset() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        catalog: BuildingCatalogRepository = repository,
        barriers: AcceptedBarriers = AcceptedBarriers { emptyList() },
    ) = CampusMapViewModel(
        repository = catalog,
        networkStatus = { online },
        mapListChoice = choice,
        computeContext = EmptyCoroutineContext,
        geometry = ::campusMapData,
        barriers = barriers,
    )

    /** Wiersz: bariery pobrane przed listą budynków → linia „Bez internetu” z dniem pobrania barier. */
    @Test
    fun nfr10_older_barriers_date() =
        runTest(UnconfinedTestDispatcher()) {
            val outline = parseGeoArea("""{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}""")
            val server =
                ServerBuildings(
                    source = "serwer",
                    buildings = listOf(Building(symbol = "C-13", name = "Serwer C-13", outline = outline)),
                )
            val catalog =
                serverBuildingCatalog(
                    ioContext = EmptyCoroutineContext,
                    list = { ServerReply.Ok(server) },
                    nowMillis = { BUILDINGS_SYNCED },
                    readGeoJson = { geoJson },
                )
            val barriers =
                object : AcceptedBarriers {
                    override suspend fun barriers(): List<AcceptedBarrier> = emptyList()

                    override fun syncedAt(): Instant = Instant.ofEpochMilli(BARRIERS_SYNCED)
                }
            val viewModel = viewModel(catalog, barriers)
            backgroundScope.launch { viewModel.uiState.collect {} }
            online.value = false
            assertEquals(Instant.ofEpochMilli(BARRIERS_SYNCED), viewModel.uiState.value.syncedAt)
        }

    /** Wiersz: dane tylko z pakietu aplikacji → linia „Bez internetu” bez daty. */
    @Test
    fun nfr10_no_date_for_bundled_data() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = viewModel()
            backgroundScope.launch { viewModel.uiState.collect {} }
            online.value = false
            assertNull(viewModel.uiState.value.syncedAt)
        }

    @Test
    fun us1_catalog_loaded() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = viewModel()
            backgroundScope.launch { viewModel.uiState.collect {} }
            val catalog = viewModel.uiState.value.catalog
            assertEquals(listOf("C-13"), catalog?.buildings.orEmpty().map { it.symbol })
        }

    @Test
    fun us9_1_offline_line() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = viewModel()
            backgroundScope.launch { viewModel.uiState.collect {} }
            online.value = false
            assertFalse(viewModel.uiState.value.isOnline)
        }

    @Test
    fun us9_1_offline_while_loading() =
        runTest(UnconfinedTestDispatcher()) {
            val stopped = StandardTestDispatcher(TestCoroutineScheduler())
            val viewModel = viewModel(BuildingCatalogRepository(stopped) { error("plik nie jest czytany") })
            backgroundScope.launch { viewModel.uiState.collect {} }
            online.value = false
            assertNull(viewModel.uiState.value.catalog)
            assertFalse(viewModel.uiState.value.isOnline)
        }

    @Test
    fun nfr19_choice_saved() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = viewModel()
            backgroundScope.launch { viewModel.uiState.collect {} }
            assertNull(viewModel.uiState.value.isListSaved)
            viewModel.chooseList(isList = true)
            assertEquals(true, viewModel.uiState.value.isListSaved)
        }

    private companion object {
        const val BARRIERS_SYNCED = 1_000L
        const val BUILDINGS_SYNCED = 2_000L
    }
}
