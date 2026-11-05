/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import io.github.dostepnykampus.core.data.buildings.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.map.MapListChoice
import io.github.dostepnykampus.core.model.BuildingCatalog
import io.github.dostepnykampus.feature.map.data.CampusMapData
import io.github.dostepnykampus.feature.map.data.campusMapData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

private const val WAIT_MS = 1_000L
private const val COMPUTE_THREAD = "map-geometry-test"

/** Gdzie i ile razy ViewModel V1 liczy geometrię mapy: na podróbce funkcji liczącej, bez pomiaru czasu (NFR-11). */
@OptIn(ExperimentalCoroutinesApi::class)
class CampusMapGeometryTest {
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
    private val calls = AtomicInteger()
    private val thread = AtomicReference<String>()

    @BeforeEach
    fun main() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun reset() {
        Dispatchers.resetMain()
    }

    private fun counting(catalog: BuildingCatalog): CampusMapData {
        calls.incrementAndGet()
        thread.set(Thread.currentThread().name)
        return campusMapData(catalog)
    }

    private fun viewModelOf(compute: CoroutineContext) =
        CampusMapViewModel(
            repository = repository,
            networkStatus = { online },
            mapListChoice = choice,
            computeContext = compute,
            geometry = ::counting,
        )

    private fun CampusMapViewModel.ready(): CampusMapUiState =
        runBlocking { withTimeoutOrNull(WAIT_MS) { uiState.first { it.mapData != null } } ?: uiState.value }

    // Wiersz: W3, O5, katalog z pliku → geometria liczona w kontekście obliczeń, nie w wątku wywołującym, a stan niesie
    //   gotowe dane mapy, NFR-11.
    @Disabled("geometria mapy liczona w kompozycji, poprawka w następnym PR")
    @Test
    fun nfr11_geometry_in_compute() {
        Executors.newSingleThreadExecutor { Thread(it, COMPUTE_THREAD) }.asCoroutineDispatcher().use { compute ->
            val viewModel = viewModelOf(compute)
            val state = viewModel.ready()
            assertEquals(COMPUTE_THREAD, thread.get(), "geometria liczona poza kontekstem obliczeń")
            assertNotEquals(Thread.currentThread().name, thread.get())
            assertEquals(state.catalog?.let(::campusMapData), state.mapData, "stan bez gotowych danych mapy")
        }
    }

    // Wiersz: W3, jeden katalog, potem zmiana internetu i wyboru „Lista” → ta sama geometria i jedno wywołanie
    //   funkcji liczącej, nie przeliczenie przy każdej zmianie stanu, NFR-11.
    @Disabled("geometria mapy liczona w kompozycji, poprawka w następnym PR")
    @Test
    fun nfr11_geometry_once_catalog() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = viewModelOf(UnconfinedTestDispatcher())
            backgroundScope.launch { viewModel.uiState.collect {} }
            val first = viewModel.ready()
            online.value = false
            viewModel.chooseList(isList = true)
            val later = viewModel.uiState.value
            assertEquals(false to true, later.isOnline to later.isListSaved)
            assertSame(first.mapData, later.mapData, "geometria policzona od nowa")
            assertEquals(1, calls.get(), "geometria liczona częściej niż raz na katalog")
        }
}
