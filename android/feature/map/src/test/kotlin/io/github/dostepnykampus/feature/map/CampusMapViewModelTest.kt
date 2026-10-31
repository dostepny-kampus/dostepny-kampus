/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import io.github.dostepnykampus.core.data.buildings.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.map.MapListChoice
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
    private val repository =
        BuildingCatalogRepository(EmptyCoroutineContext) {
            """{"type":"FeatureCollection","attribution":"© OpenStreetMap contributors","features":[""" +
                """{"type":"Feature","properties":{"kind":"building","symbols":["C-13"],"osm_id":"way/1"},""" +
                """"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}}]}"""
        }

    @BeforeEach
    fun main() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun reset() {
        Dispatchers.resetMain()
    }

    private fun viewModel(catalog: BuildingCatalogRepository = repository) =
        CampusMapViewModel(catalog, { online }, choice)

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
}
