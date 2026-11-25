/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import androidx.lifecycle.viewModelScope
import io.github.dostepnykampus.core.data.barrier.AcceptedBarrier
import io.github.dostepnykampus.core.data.barrier.AcceptedBarriers
import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.building.ServerEntrances
import io.github.dostepnykampus.core.data.building.ServerEntrancesRepository
import io.github.dostepnykampus.core.data.room.FloorPlans
import io.github.dostepnykampus.core.data.server.ServerCard
import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.data.submission.own.OwnSubmission
import io.github.dostepnykampus.core.data.submission.own.OwnSubmissions
import io.github.dostepnykampus.core.data.submission.own.SendState
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.core.model.building.Building
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.core.model.building.EvacuationExit
import io.github.dostepnykampus.core.model.building.Floor
import io.github.dostepnykampus.core.model.building.FloorList
import io.github.dostepnykampus.core.model.room.RoomList
import io.github.dostepnykampus.core.model.submission.Severity
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Składanie stanu karty V2 w jednym `combine` poza wątkiem głównym: zachowanie tej samej listy wejść przy zmianie
 * samych pięter albo wyjść oraz pomijanie powtórzonych stanów.
 *
 * Wymagania: NFR-56, US2
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CardStateCombineTest {
    @BeforeEach
    fun main() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterEach
    fun reset() {
        Dispatchers.resetMain()
    }

    @Test
    fun keepsEntrancesOnFloorsChange() {
        runTest {
            val downloaded = CompletableDeferred<Boolean>()
            val isStored = AtomicBoolean(false)
            val plans =
                object : FloorPlans {
                    override suspend fun download(symbol: String): Boolean =
                        downloaded.await().also(isStored::set)

                    override suspend fun floors(symbol: String): FloorList? =
                        FloorList("PWr", listOf(Floor(id = "P0", label = "Parter", order = 0)))
                            .takeIf { isStored.get() }

                    override suspend fun rooms(symbol: String, floor: String): RoomList? = null
                }
            val card = cardViewModel(plans = plans)
            val collector = backgroundScope.launch { card.uiState.collect {} }
            advanceUntilIdle()
            val before = card.uiState.value as BuildingCardUiState.Ready
            assertNull(before.floors, "Przed pobraniem planu brak faktów o piętrach")
            downloaded.complete(true)
            advanceUntilIdle()
            val after = card.uiState.value as BuildingCardUiState.Ready
            assertNotNull(after.floors, "Po pobraniu planu pojawiają się fakty o piętrach")
            assertSame(before.entrances, after.entrances, "Zmiana samych pięter zostawia ten sam obiekt listy wejść")
            finish(card, collector)
        }
    }

    @Test
    fun keepsEntrancesOnExitsChange() {
        runTest {
            val repository = catalog()
            repository.catalog()
            val secondAnswer = CompletableDeferred<ServerReply<ServerCard>>()
            val answers =
                ArrayDeque<suspend () -> ServerReply<ServerCard>>(
                    listOf({ ServerReply.Ok(SERVER_CARD) }, { secondAnswer.await() }),
                )
            val entrances = ServerEntrancesRepository { answers.removeFirst()() }
            val warm = cardViewModel(repository = repository, serverEntrances = entrances)
            val warmCollector = backgroundScope.launch { warm.uiState.collect {} }
            advanceUntilIdle()
            finish(warm, warmCollector)

            val card = cardViewModel(repository = repository, serverEntrances = entrances)
            val collector = backgroundScope.launch { card.uiState.collect {} }
            advanceUntilIdle()
            val before = card.uiState.value as BuildingCardUiState.Ready
            assertEquals(emptyList<EvacuationExit>(), before.evacuationExits, "Przed odpowiedzią brak wyjść")
            secondAnswer.complete(ServerReply.Ok(SERVER_CARD.copy(evacuationExits = listOf(EXIT))))
            advanceUntilIdle()
            val after = card.uiState.value as BuildingCardUiState.Ready
            assertEquals(listOf(EXIT), after.evacuationExits, "Wyjście ewakuacyjne weszło do stanu karty")
            assertSame(before.entrances, after.entrances, "Zmiana samych wyjść zostawia ten sam obiekt listy wejść")
            finish(card, collector)
        }
    }

    @Test
    fun reordersEntrancesWithBarriers() {
        runTest {
            val fetchedBarriers = CompletableDeferred<List<AcceptedBarrier>>()
            val barriers =
                object : AcceptedBarriers {
                    override suspend fun barriers(): List<AcceptedBarrier> = fetchedBarriers.await()

                    override fun syncedAt(): Instant = SYNCED_AT
                }
            val card = cardViewModel(acceptedBarriers = barriers)
            val collector = backgroundScope.launch { card.uiState.collect {} }
            advanceUntilIdle()
            val before = card.uiState.value as BuildingCardUiState.Ready
            assertEquals("node/1", before.recommendedEntrance()?.entrance?.id, "Przed barierą główne jest zalecane")
            fetchedBarriers.complete(listOf(BLOCKING_ON_MAIN))
            advanceUntilIdle()
            val after = card.uiState.value as BuildingCardUiState.Ready
            assertNotSame(before.entrances, after.entrances, "Nowa bariera buduje nową listę wejść")
            assertEquals("node/2", after.recommendedEntrance()?.entrance?.id, "Blokada zmienia wejście zalecane")
            finish(card, collector)
        }
    }

    @Test
    fun computesStateOffMainThread() {
        runBlocking {
            val callerThread = Thread.currentThread()
            val worker = Executors.newSingleThreadExecutor { Thread(it, "karta-obliczenia") }.asCoroutineDispatcher()
            Dispatchers.setMain(Dispatchers.Unconfined)
            try {
                val readThreads = mutableListOf<Thread>()
                val repository =
                    BuildingCatalogRepository(EmptyCoroutineContext) {
                        readThreads += Thread.currentThread()
                        ASSET_JSON
                    }
                val card =
                    BuildingCardViewModel(
                        symbol = "A-1",
                        repository = repository,
                        networkStatus = { flowOf(true) },
                        sources = sources(computeContext = worker),
                    )
                val ready = card.uiState.first { it is BuildingCardUiState.Ready } as BuildingCardUiState.Ready
                card.viewModelScope.cancel()
                assertEquals("A-1", ready.symbol, "Karta wczytała budynek A-1")
                assertNotEquals(callerThread, readThreads.single(), "Stan liczony z flowOn poza wątkiem głównym")
            } finally {
                worker.close()
            }
        }
    }

    @Test
    fun skipsDuplicateOwnBarriers() {
        runTest {
            val own = MutableStateFlow(listOf(ownSubmission(id = "z1", state = SendState.LOCAL)))
            val repository = catalog()
            repository.catalog()
            val card =
                BuildingCardViewModel(
                    symbol = "A-1",
                    repository = repository,
                    networkStatus = { flowOf(true) },
                    sources = sources(ownSubmissions = OwnSubmissions { own }),
                )
            val emitted = mutableListOf<BuildingCardUiState>()
            val collector = backgroundScope.launch { card.uiState.collect { emitted += it } }
            advanceUntilIdle()
            val countBefore = emitted.size
            val before = card.uiState.value as BuildingCardUiState.Ready
            own.value = listOf(ownSubmission(id = "z1", state = SendState.SENT))
            advanceUntilIdle()
            val after = card.uiState.value as BuildingCardUiState.Ready
            assertEquals(countBefore, emitted.size, "Powtórzony zbiór własnych zgłoszeń nie emituje stanu")
            assertSame(before.entrances, after.entrances, "Powtórzone własne zgłoszenia nie przebudowują wejść")
            finish(card, collector)
        }
    }

    private fun TestScope.finish(card: BuildingCardViewModel, collector: Job) {
        collector.cancel()
        card.viewModelScope.cancel()
        advanceUntilIdle()
    }

    private fun cardViewModel(
        repository: BuildingCatalogRepository = catalog(),
        serverEntrances: ServerEntrances = ServerEntrances { emptyList() },
        acceptedBarriers: AcceptedBarriers = AcceptedBarriers { emptyList() },
        plans: FloorPlans = NoPlans,
    ): BuildingCardViewModel =
        BuildingCardViewModel(
            symbol = "A-1",
            repository = repository,
            networkStatus = { flowOf(true) },
            serverEntrances = serverEntrances,
            acceptedBarriers = acceptedBarriers,
            plans = plans,
        )

    private fun sources(
        ownSubmissions: OwnSubmissions = OwnSubmissions { flowOf(emptyList()) },
        computeContext: CoroutineContext = EmptyCoroutineContext,
    ): BuildingCardSources =
        BuildingCardSources(
            serverEntrances = ServerEntrances { emptyList() },
            acceptedBarriers = AcceptedBarriers { emptyList() },
            plans = NoPlans,
            ownSubmissions = ownSubmissions,
            computeContext = computeContext,
        )

    private fun catalog(): BuildingCatalogRepository = BuildingCatalogRepository(EmptyCoroutineContext) { ASSET_JSON }

    private fun ownSubmission(id: String, state: SendState): OwnSubmission =
        OwnSubmission(
            id = id,
            savedAt = SYNCED_AT,
            barrierType = null,
            buildingSymbol = "A-1",
            floorNumber = null,
            state = state,
            refusal = null,
        )

    private object NoPlans : FloorPlans {
        override suspend fun download(symbol: String): Boolean = false

        override suspend fun floors(symbol: String): FloorList? = null

        override suspend fun rooms(symbol: String, floor: String): RoomList? = null
    }

    private companion object {
        val SYNCED_AT: Instant = Instant.parse("2026-11-12T10:00:00Z")
        val POINT = GeoPoint(longitude = 0.0, latitude = 0.0)
        val SERVER_ENTRANCE =
            Entrance(
                id = "a1-main",
                buildingSymbol = "A-1",
                name = "Główne",
                isMain = true,
                type = null,
                location = POINT,
                marking = EntranceMarking.ACCESSIBLE,
            )
        val SERVER_CARD =
            ServerCard(
                building = Building(symbol = "A-1", name = "Gmach", outline = GeoPolygon(emptyList())),
                entrances = listOf(SERVER_ENTRANCE),
            )
        val EXIT =
            EvacuationExit(
                id = "pwr/9",
                location = POINT,
                description = "Wyjście ewakuacyjne",
                doorWidthCm = 90,
            )
        val BLOCKING_ON_MAIN =
            AcceptedBarrier(
                id = "z1",
                location = POINT,
                severity = Severity.BLOCKING,
                type = null,
                buildingSymbol = "A-1",
                verifiedAt = SYNCED_AT,
                entranceId = "node/1",
            )

        const val ASSET_JSON =
            """{"type":"FeatureCollection","attribution":"© OpenStreetMap contributors","features":[""" +
                """{"type":"Feature","properties":{"kind":"building","symbols":["A-1"],"display_name":"Gmach",""" +
                """"osm_id":"way/1"},"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}},""" +
                """{"type":"Feature","properties":{"kind":"entrance","symbols":["A-1"],"osm_id":"node/1",""" +
                """"entrance":"main","wheelchair":"yes","ref":"Główne"},""" +
                """"geometry":{"type":"Point","coordinates":[0,0]}},""" +
                """{"type":"Feature","properties":{"kind":"entrance","symbols":["A-1"],"osm_id":"node/2",""" +
                """"entrance":"yes","wheelchair":"yes","ref":"Boczne"},""" +
                """"geometry":{"type":"Point","coordinates":[1,0]}}]}"""
    }
}
