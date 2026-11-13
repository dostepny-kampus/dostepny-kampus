/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

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
import io.github.dostepnykampus.feature.map.data.CampusMapData
import io.github.dostepnykampus.feature.map.data.campusMapData
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

private const val MANY_OUTLINES = 40
private val FILE_LABELS = listOf("A-1", "B-2")
private val SERVER_LABELS = listOf("A-1")

/**
 * Geometria mapy V1 od odpowiedzi serwera i zamknięcia ekranu do stanu ViewModelu. Testy czekają na zdarzenia z funkcji
 * liczącej, nie na upływ czasu; zawieszenie przy błędzie kończy limit `runTest`. `holdFile` zgłasza start liczenia i
 * czeka na zwolnienie albo przerwanie wątku, `withPool` daje wątki liczące, na które czeka na końcu testu, a `show`
 * trzyma subskrypcję stanu do końca testu.
 *
 * Wymagania: NFR-10, NFR-11
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MapGeometryFlowTest {
    private val calls = AtomicInteger()
    private val viewModels = TestViewModels()

    @BeforeEach
    fun main() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun reset() {
        viewModels.resetMain()
    }

    /**
     * Wiersz: W3, plik, potem serwer → geometria raz na katalog, a stan ma ją z katalogu serwera; liczenie dla pliku,
     *   które jeszcze trwa, zostaje przerwane i nie wraca po serwerze.
     *
     * Wymagania: NFR-10, NFR-11
     */
    @Test
    fun nfr11_geometry_per_catalog() =
        runTest {
            val answer = CompletableDeferred<ApiResult<BuildingList>>()
            val fileRunning = CompletableDeferred<Unit>()
            val fileHeld = CountDownLatch(1)
            val fileFinished = CompletableDeferred<Boolean>()
            withPool(threads = 2) { compute ->
                try {
                    val geometry = { catalog: BuildingCatalog ->
                        calls.incrementAndGet()
                        if (catalog.buildings.size == FILE_LABELS.size) {
                            holdFile(fileRunning, fileHeld, fileFinished)
                        }
                        campusMapData(catalog)
                    }
                    val viewModel = viewModelOf(repository { answer.await() }, compute, geometry)
                    show(viewModel)
                    fileRunning.await()
                    answer.complete(ApiResult.Success(SERVER_LIST))
                    val state =
                        viewModel.uiState.first { it.catalog?.run { buildings.size } == 1 && it.mapData != null }
                    assertEquals(SERVER_LABELS, state.mapData?.labels, "geometria nie z serwera")
                    assertFalse(fileFinished.await(), "liczenie dla pliku doszło do końca po serwerze")
                    assertEquals(2, calls.get(), "geometria liczona inaczej niż raz na katalog")
                } finally {
                    fileHeld.countDown()
                }
            }
        }

    /** Wiersz: zamknięcie ekranu w trakcie pobierania z serwera → wywołanie przerwane, bez działającej pracy. */
    @Test
    fun closing_map_cancels_call() =
        runTest {
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
            val factory = factoryOf(repository, UnconfinedTestDispatcher(testScheduler))
            show(ViewModelProvider.create(store, factory)[CampusMapViewModel::class])
            started.await()
            store.clear()
            cancelled.await()
            assertTrue(started.isCompleted && cancelled.isCompleted, "pobranie nie zaczęło się albo trwa po zamknięciu")
        }

    /**
     * Wiersz: zamknięcie ekranu w trakcie liczenia geometrii → liczenie czekające w kolejce nie rusza po zamknięciu.
     */
    @Test
    fun closing_map_cancels_compute() =
        runTest {
            val queued = ArrayDeque<Runnable>()
            val compute =
                object : CoroutineDispatcher() {
                    override fun dispatch(context: CoroutineContext, block: Runnable) {
                        queued.addLast(block)
                    }
                }
            val store = ViewModelStore()
            show(ViewModelProvider.create(store, factoryOf(repository(), compute))[CampusMapViewModel::class])
            assertTrue(queued.isNotEmpty(), "geometria nie trafiła do kontekstu obliczeń")
            store.clear()
            while (queued.isNotEmpty()) queued.removeFirst().run()
            assertEquals(0, calls.get(), "liczenie ruszyło po zamknięciu ekranu")
        }

    /**
     * Wiersz: zamknięcie ekranu w trakcie liczenia geometrii katalogu z wieloma konturami → liczenie nie dochodzi do
     *   końca katalogu.
     *
     * Wymagania: NFR-11
     */
    @Test
    fun closing_map_stops_running() =
        runTest {
            val started = CompletableDeferred<Unit>()
            val release = CountDownLatch(1)
            val finished = CompletableDeferred<Boolean>()
            val store = ViewModelStore()
            withPool(threads = 1) { compute ->
                try {
                    val geometry = { catalog: BuildingCatalog ->
                        var isDone = false
                        try {
                            started.complete(Unit)
                            release.await()
                            campusMapData(catalog).also { isDone = true }
                        } finally {
                            finished.complete(isDone)
                        }
                    }
                    val many = repository(json = MANY_JSON)
                    val factory = viewModelFactory { initializer { viewModelOf(many, compute, geometry) } }
                    show(ViewModelProvider.create(store, factory)[CampusMapViewModel::class])
                    started.await()
                    store.clear()
                    release.countDown()
                    assertFalse(finished.await(), "liczenie doszło do końca katalogu po zamknięciu ekranu")
                } finally {
                    release.countDown()
                }
            }
        }

    /**
     * Wiersz: serwer odpowiada, gdy liczy się katalog z pliku → liczenie katalogu z serwera rusza, zanim skończy się
     *   liczenie pliku.
     *
     * Wymagania: NFR-11
     */
    @Test
    fun server_compute_starts_early() =
        runTest {
            val answer = CompletableDeferred<ApiResult<BuildingList>>()
            val fileRunning = CompletableDeferred<Unit>()
            val serverStarted = CompletableDeferred<Unit>()
            val hold = CountDownLatch(1)
            val fileFinished = CompletableDeferred<Boolean>()
            withPool(threads = 2) { compute ->
                try {
                    val geometry = { catalog: BuildingCatalog ->
                        if (catalog.buildings.size == SERVER_LABELS.size) {
                            serverStarted.complete(Unit)
                        } else {
                            holdFile(fileRunning, hold, fileFinished)
                        }
                        campusMapData(catalog)
                    }
                    show(viewModelOf(repository { answer.await() }, compute, geometry))
                    fileRunning.await()
                    answer.complete(ApiResult.Success(SERVER_LIST))
                    serverStarted.await()
                    assertFalse(fileFinished.await(), "serwer czekał na koniec liczenia pliku")
                } finally {
                    hold.countDown()
                }
            }
        }

    private fun holdFile(
        running: CompletableDeferred<Unit>,
        held: CountDownLatch,
        finished: CompletableDeferred<Boolean>,
    ) {
        var isDone = false
        try {
            running.complete(Unit)
            held.await()
            isDone = true
        } finally {
            finished.complete(isDone)
        }
    }

    private suspend inline fun withPool(threads: Int, block: (CoroutineDispatcher) -> Unit) {
        val pool = Executors.newFixedThreadPool(threads)
        try {
            pool.asCoroutineDispatcher().use { compute ->
                try {
                    block(compute)
                } finally {
                    withContext(NonCancellable) { viewModels.cancelAndJoin() }
                }
            }
        } finally {
            pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)
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
    ) = viewModels.keep(
        CampusMapViewModel(
            repository = repository,
            networkStatus = { flowOf(true) },
            mapListChoice = TestMapListChoice(),
            computeContext = compute,
            geometry = geometry,
        ),
    )

    private fun factoryOf(repository: BuildingCatalogRepository, compute: CoroutineContext) =
        viewModelFactory { initializer { viewModelOf(repository, compute) } }

    private fun TestScope.show(viewModel: CampusMapViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

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
                """{"type":"Feature","properties":{"kind":"building","symbols":["A-1"],"display_name":"Z pliku",""" +
                """"osm_id":"way/1"},"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}},""" +
                """{"type":"Feature","properties":{"kind":"building","symbols":["B-2"],"display_name":"Drugi",""" +
                """"osm_id":"way/2"},"geometry":{"type":"Polygon","coordinates":[[[2,0],[3,0],[3,1],[2,0]]]}}]}"""
    }
}
