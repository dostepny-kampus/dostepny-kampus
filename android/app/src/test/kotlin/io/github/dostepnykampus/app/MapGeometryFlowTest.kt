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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

// Tylko ochrona przed zawieszeniem przy błędzie w kodzie; wynik testów nie zależy od czasu.
private const val HANG_GUARD_SECONDS = 60L
private const val MANY_OUTLINES = 40
private val FILE_LABELS = listOf("A-1", "B-2")
private val SERVER_LABELS = listOf("A-1")

/**
 * Geometria mapy V1 od odpowiedzi serwera i zamknięcia ekranu do stanu ViewModelu (NFR-10, NFR-11). Testy czekają na
 * zdarzenia (zatrzaski, wynik liczenia), nie na upływ czasu, więc wynik nie zależy od szybkości maszyny.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Timeout(HANG_GUARD_SECONDS)
class MapGeometryFlowTest {
    private val calls = AtomicInteger()
    private val screens = mutableListOf<Job>()

    @BeforeEach
    fun main() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun reset() {
        screens.forEach(Job::cancel)
        Dispatchers.resetMain()
    }

    // Wiersz: W3, plik, potem serwer → geometria raz na katalog, a stan ma ją z katalogu serwera; liczenie dla pliku,
    //   które jeszcze trwa, zostaje przerwane i nie wraca po serwerze, NFR-10, NFR-11.
    @Test
    fun nfr11_geometry_per_catalog() {
        val answer = CompletableDeferred<ApiResult<BuildingList>>()
        val fileRunning = CountDownLatch(1)
        val fileHeld = CountDownLatch(1)
        val fileFinished = CompletableDeferred<Boolean>()
        Executors.newFixedThreadPool(2).asCoroutineDispatcher().use { compute ->
            try {
                val geometry = { catalog: BuildingCatalog ->
                    calls.incrementAndGet()
                    if (catalog.buildings.size == FILE_LABELS.size) {
                        var isDone = false
                        try {
                            fileRunning.countDown()
                            fileHeld.await()
                            isDone = true
                        } finally {
                            fileFinished.complete(isDone)
                        }
                    }
                    campusMapData(catalog)
                }
                val viewModel = viewModelOf(repository { answer.await() }, compute, geometry)
                viewModel.show()
                fileRunning.await()
                answer.complete(ApiResult.Success(SERVER_LIST))
                val state = viewModel.waitFor { it.catalog?.buildings?.size == 1 && it.mapData != null }
                assertEquals(SERVER_LABELS, state.mapData?.labels, "geometria nie z serwera")
                assertFalse(runBlocking { fileFinished.await() }, "liczenie dla pliku doszło do końca po serwerze")
                assertEquals(2, calls.get(), "geometria liczona inaczej niż raz na katalog")
            } finally {
                fileHeld.countDown()
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
        ViewModelProvider.create(store, factoryOf(repository, UnconfinedTestDispatcher()))[CampusMapViewModel::class]
            .show()
        runBlocking { started.await() }
        store.clear()
        runBlocking { cancelled.await() }
        assertTrue(started.isCompleted && cancelled.isCompleted, "pobranie nie zaczęło się albo trwa po zamknięciu")
    }

    // Wiersz: zamknięcie ekranu w trakcie liczenia geometrii → liczenie czekające w kolejce nie rusza po zamknięciu.
    @Test
    fun closing_map_cancels_compute() {
        val queued = ArrayDeque<Runnable>()
        val compute =
            object : CoroutineDispatcher() {
                override fun dispatch(context: CoroutineContext, block: Runnable) {
                    queued.addLast(block)
                }
            }
        val store = ViewModelStore()
        ViewModelProvider.create(store, factoryOf(repository(), compute))[CampusMapViewModel::class].show()
        assertTrue(queued.isNotEmpty(), "geometria nie trafiła do kontekstu obliczeń")
        store.clear()
        while (queued.isNotEmpty()) queued.removeFirst().run()
        assertEquals(0, calls.get(), "liczenie ruszyło po zamknięciu ekranu")
    }

    // Wiersz: zamknięcie ekranu w trakcie liczenia geometrii katalogu z wieloma konturami → liczenie nie dochodzi do
    //   końca katalogu, NFR-11.
    @Test
    fun closing_map_stops_running() {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val finished = CompletableDeferred<Boolean>()
        val store = ViewModelStore()
        Executors.newSingleThreadExecutor().asCoroutineDispatcher().use { compute ->
            try {
                val geometry = { catalog: BuildingCatalog ->
                    var isDone = false
                    try {
                        started.countDown()
                        release.await()
                        campusMapData(catalog).also { isDone = true }
                    } finally {
                        finished.complete(isDone)
                    }
                }
                val factory =
                    viewModelFactory { initializer { viewModelOf(repository(json = MANY_JSON), compute, geometry) } }
                ViewModelProvider.create(store, factory)[CampusMapViewModel::class].show()
                started.await()
                store.clear()
                release.countDown()
                assertFalse(runBlocking { finished.await() }, "liczenie doszło do końca katalogu po zamknięciu ekranu")
            } finally {
                release.countDown()
            }
        }
    }

    // Wiersz: serwer odpowiada, gdy liczy się katalog z pliku → liczenie katalogu z serwera rusza, zanim skończy się
    //   liczenie pliku, NFR-11.
    @Test
    fun server_compute_starts_early() {
        val answer = CompletableDeferred<ApiResult<BuildingList>>()
        val fileRunning = CountDownLatch(1)
        val serverStarted = CountDownLatch(1)
        val hold = CountDownLatch(1)
        val fileFinished = CompletableDeferred<Boolean>()
        Executors.newFixedThreadPool(2).asCoroutineDispatcher().use { compute ->
            try {
                val geometry = { catalog: BuildingCatalog ->
                    if (catalog.buildings.size == SERVER_LABELS.size) {
                        serverStarted.countDown()
                    } else {
                        var isDone = false
                        try {
                            fileRunning.countDown()
                            hold.await()
                            isDone = true
                        } finally {
                            fileFinished.complete(isDone)
                        }
                    }
                    campusMapData(catalog)
                }
                viewModelOf(repository { answer.await() }, compute, geometry).show()
                fileRunning.await()
                answer.complete(ApiResult.Success(SERVER_LIST))
                serverStarted.await()
                assertFalse(runBlocking { fileFinished.await() }, "serwer czekał na koniec liczenia pliku")
            } finally {
                hold.countDown()
            }
        }
    }

    private fun repository(json: String = ASSET_JSON, list: (suspend () -> ApiResult<BuildingList>)? = null) =
        BuildingCatalogRepository(ioContext = EmptyCoroutineContext, listBuildings = list) { json }

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

    // Widoczny ekran: subskrypcja stanu trwa do końca testu, więc źródła działają aż do zamknięcia ViewModelu.
    private fun CampusMapViewModel.show() {
        screens += CoroutineScope(Dispatchers.Unconfined).launch { uiState.collect {} }
    }

    private fun CampusMapViewModel.waitFor(until: (CampusMapUiState) -> Boolean): CampusMapUiState =
        runBlocking { uiState.first(until) }

    private companion object {
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

        val MANY_FEATURES =
            (1..MANY_OUTLINES).joinToString(separator = ",") { i ->
                """{"type":"Feature","properties":{"kind":"building","symbols":["M-$i"],"osm_id":"way/$i"},""" +
                    """"geometry":{"type":"Polygon","coordinates":[[[$i,0],[${i + 1},0],[${i + 1},1],[$i,0]]]}}"""
            }

        val MANY_JSON = """{"type":"FeatureCollection","attribution":"© OSM","features":[$MANY_FEATURES]}"""

        const val ASSET_JSON =
            """{"type":"FeatureCollection","attribution":"© OpenStreetMap contributors","features":[""" +
                """{"type":"Feature","properties":{"kind":"building","symbols":["A-1"],"name":"Z pliku",""" +
                """"osm_id":"way/1"},"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}},""" +
                """{"type":"Feature","properties":{"kind":"building","symbols":["B-2"],"name":"Drugi",""" +
                """"osm_id":"way/2"},"geometry":{"type":"Polygon","coordinates":[[[2,0],[3,0],[3,1],[2,0]]]}}]}"""
    }
}
