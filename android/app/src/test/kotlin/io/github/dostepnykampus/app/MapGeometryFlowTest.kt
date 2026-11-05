/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.dostepnykampus.core.data.buildings.BuildingCatalogRepository
import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.core.model.BuildingCatalog
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.core.network.ApiBuilding
import io.github.dostepnykampus.core.network.ApiResult
import io.github.dostepnykampus.core.network.BuildingList
import io.github.dostepnykampus.core.network.DetailLevel
import io.github.dostepnykampus.feature.map.CampusMapUiState
import io.github.dostepnykampus.feature.map.CampusMapViewModel
import io.github.dostepnykampus.feature.map.data.CampusMapData
import io.github.dostepnykampus.feature.map.data.campusMapData
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.awaitCancellation
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

private const val WAIT_MS = 1_000L
private const val SETTLE_MS = 300L
private const val SUBSCRIBE_MS = 50L
private val FILE_LABELS = listOf("A-1", "B-2")
private val SERVER_LABELS = listOf("A-1")

/** Geometria mapy V1 od odpowiedzi serwera i zamknięcia ekranu do stanu ViewModelu (NFR-10, NFR-11). */
@OptIn(ExperimentalCoroutinesApi::class)
class MapGeometryFlowTest {
    private val calls = AtomicInteger()

    @BeforeEach
    fun main() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun reset() {
        Dispatchers.resetMain()
    }

    // Wiersz: W3, plik, potem serwer → geometria najwyżej raz na katalog, a ostatni stan ma ją z katalogu serwera;
    //   wolna geometria z pliku nie wraca po niej, NFR-10, NFR-11.
    @Test
    fun nfr11_geometry_per_catalog() {
        val answer = CompletableDeferred<ApiResult<BuildingList>>()
        val slowFile = CountDownLatch(1)
        Executors.newFixedThreadPool(2).asCoroutineDispatcher().use { compute ->
            try {
                val geometry = { catalog: BuildingCatalog ->
                    calls.incrementAndGet()
                    if (catalog.buildings.size == FILE_LABELS.size) slowFile.await(WAIT_MS, TimeUnit.MILLISECONDS)
                    campusMapData(catalog).also { slowFile.countDown() }
                }
                val viewModel = viewModelOf(repository { answer.await() }, compute, geometry)
                viewModel.subscribe()
                answer.complete(ApiResult.Success(SERVER_LIST))
                viewModel.waitFor { state -> state.catalog?.run { buildings.size } == 1 && state.mapData != null }
                runBlocking { withTimeoutOrNull(SETTLE_MS) { awaitCancellation() } }
                val shown = viewModel.uiState.value.mapData
                assertEquals(SERVER_LABELS, shown?.labels, "geometria nie z serwera")
                assertTrue(calls.get() in 1..2, "geometria liczona częściej niż raz na katalog: ${calls.get()}")
            } finally {
                slowFile.countDown()
            }
        }
    }

    // Wiersz: zamknięcie ekranu w trakcie pobierania z serwera → wywołanie przerwane, bez działającej pracy.
    @Test
    fun closing_map_cancels_call() {
        val started = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()
        val store = ViewModelStore()
        val repository =
            repository {
                started.complete(Unit)
                try {
                    awaitCancellation()
                } finally {
                    cancelled.complete(Unit)
                }
            }
        val viewModel = ViewModelProvider.create(store, factoryOf(repository, UnconfinedTestDispatcher()))
        viewModel[CampusMapViewModel::class].subscribe()
        runBlocking { withTimeoutOrNull(WAIT_MS) { started.await() } }
        store.clear()
        runBlocking { withTimeoutOrNull(WAIT_MS) { cancelled.await() } }
        assertTrue(started.isCompleted && cancelled.isCompleted, "pobranie nie zaczęło się albo trwa po zamknięciu")
    }

    // Wiersz: zamknięcie ekranu w trakcie liczenia geometrii → liczenie czekające w kolejce nie rusza po zamknięciu.
    @Test
    fun closing_map_cancels_compute() {
        val busy = CountDownLatch(1)
        val pool = Executors.newSingleThreadExecutor()
        val queued = CompletableDeferred<Unit>()
        val compute =
            object : CoroutineDispatcher() {
                override fun dispatch(context: CoroutineContext, block: Runnable) {
                    queued.complete(Unit)
                    pool.execute(block)
                }
            }
        val store = ViewModelStore()
        try {
            pool.execute { busy.await(WAIT_MS * 3, TimeUnit.MILLISECONDS) }
            ViewModelProvider.create(store, factoryOf(repository(), compute))[CampusMapViewModel::class].subscribe()
            runBlocking { withTimeoutOrNull(WAIT_MS) { queued.await() } }
            assertTrue(queued.isCompleted, "geometria nie trafiła do kontekstu obliczeń")
            store.clear()
            busy.countDown()
            pool.submit {}.get(WAIT_MS, TimeUnit.MILLISECONDS)
            assertEquals(0, calls.get(), "liczenie ruszyło po zamknięciu ekranu")
        } finally {
            busy.countDown()
            pool.shutdownNow()
        }
    }

    private fun repository(list: (suspend () -> ApiResult<BuildingList>)? = null) =
        BuildingCatalogRepository(ioContext = EmptyCoroutineContext, listBuildings = list) { ASSET_JSON }

    private fun viewModelOf(
        repository: BuildingCatalogRepository,
        compute: CoroutineContext,
        geometry: (BuildingCatalog) -> CampusMapData = { catalog ->
            calls.incrementAndGet()
            campusMapData(catalog)
        },
    ) = CampusMapViewModel(
        repository = repository,
        networkStatus = { flowOf(true) },
        mapListChoice = TestMapListChoice(),
        computeContext = compute,
        geometry = geometry,
    )

    private fun factoryOf(repository: BuildingCatalogRepository, compute: CoroutineContext) =
        viewModelFactory { initializer { viewModelOf(repository, compute) } }

    // Jedna subskrypcja startuje źródła; po jej końcu działają jeszcze przez 5 s, aż do zamknięcia ViewModelu.
    private fun CampusMapViewModel.subscribe() {
        runBlocking { withTimeoutOrNull(SUBSCRIBE_MS) { uiState.first { false } } }
    }

    private fun CampusMapViewModel.waitFor(until: (CampusMapUiState) -> Boolean): CampusMapUiState? =
        runBlocking { withTimeoutOrNull(WAIT_MS * 2) { uiState.first(until) } }

    private companion object {
        val SERVER_OUTLINE =
            GeoPolygon(listOf(listOf(GeoPoint(0.0, 0.0), GeoPoint(1.0, 0.0), GeoPoint(1.0, 1.0), GeoPoint(0.0, 0.0))))

        val SERVER_BUILDING = Building(symbol = "A-1", name = "Z serwera", outline = SERVER_OUTLINE)

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
