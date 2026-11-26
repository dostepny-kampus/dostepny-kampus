/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

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
import io.github.dostepnykampus.feature.map.ASSET_JSON
import io.github.dostepnykampus.feature.map.TestViewModels
import io.github.dostepnykampus.feature.map.entranceFeature
import io.github.dostepnykampus.feature.map.unitSquareCatalog
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

private const val COMPUTE_THREAD = "karta-obliczenia"

/**
 * Składanie stanu karty V2 w jednym `combine` poza wątkiem głównym: zachowanie tej samej listy wejść przy zmianie
 * samych pięter albo wyjść oraz pomijanie powtórzonych stanów.
 *
 * Wymagania: NFR-56, US2
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CardStateCombineTest {
    private val viewModels = TestViewModels()

    @BeforeEach
    fun main() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun reset() {
        viewModels.resetMain()
    }

    @Test
    fun keepsEntrancesOnFloorsChange() =
        runTest {
            val downloaded = CompletableDeferred<Boolean>()
            val isStored = AtomicBoolean(false)
            val plans =
                object : FloorPlans {
                    override suspend fun download(symbol: String): Boolean {
                        val isReady = downloaded.await()
                        isStored.set(isReady)
                        return isReady
                    }

                    override suspend fun floors(symbol: String): FloorList? =
                        FloorList(
                            source = "PWr",
                            floors = listOf(Floor(id = "P0", label = "Parter", order = 0)),
                        ).takeIf { isStored.get() }

                    override suspend fun rooms(symbol: String, floor: String): RoomList? = null
                }
            val card = cardViewModel(plans = plans)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { card.uiState.collect {} }
            val before = card.uiState.value as BuildingCardUiState.Ready
            assertNull(before.floors, "Przed pobraniem planu brak faktów o piętrach")
            downloaded.complete(true)
            val after = card.uiState.value as BuildingCardUiState.Ready
            assertNotNull(after.floors, "Po pobraniu planu pojawiają się fakty o piętrach")
            assertSame(before.entrances, after.entrances, "Zmiana samych pięter zostawia ten sam obiekt listy wejść")
        }

    @Test
    fun keepsEntrancesOnExitsChange() =
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
            val warmCollector =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { warm.uiState.collect {} }
            warmCollector.cancel()
            viewModels.cancelAndJoin()

            val card = cardViewModel(repository = repository, serverEntrances = entrances)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { card.uiState.collect {} }
            val before = card.uiState.value as BuildingCardUiState.Ready
            assertEquals(emptyList<EvacuationExit>(), before.evacuationExits, "Przed odpowiedzią brak wyjść")
            secondAnswer.complete(ServerReply.Ok(SERVER_CARD.copy(evacuationExits = listOf(EXIT))))
            val after = card.uiState.value as BuildingCardUiState.Ready
            assertEquals(listOf(EXIT), after.evacuationExits, "Wyjście ewakuacyjne weszło do stanu karty")
            assertSame(before.entrances, after.entrances, "Zmiana samych wyjść zostawia ten sam obiekt listy wejść")
        }

    @Test
    fun reordersEntrancesWithBarriers() =
        runTest {
            val fetchedBarriers = CompletableDeferred<List<AcceptedBarrier>>()
            val barriers =
                object : AcceptedBarriers {
                    override suspend fun barriers(): List<AcceptedBarrier> = fetchedBarriers.await()

                    override fun syncedAt(): Instant = SYNCED_AT
                }
            val card = cardViewModel(acceptedBarriers = barriers)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { card.uiState.collect {} }
            val before = card.uiState.value as BuildingCardUiState.Ready
            assertEquals(
                "node/1",
                before.recommendedEntrance()?.run { entrance.id },
                "Przed barierą główne jest zalecane",
            )
            fetchedBarriers.complete(listOf(BLOCKING_ON_MAIN))
            val after = card.uiState.value as BuildingCardUiState.Ready
            assertNotSame(before.entrances, after.entrances, "Nowa bariera buduje nową listę wejść")
            assertEquals("node/2", after.recommendedEntrance()?.run { entrance.id }, "Blokada zmienia wejście zalecane")
        }

    @Test
    fun computesStateOffMainThread() =
        runTest {
            val callerThread = Thread.currentThread().name
            val readThread = AtomicReference<String>()
            val repository =
                BuildingCatalogRepository(ioContext = EmptyCoroutineContext) {
                    readThread.set(Thread.currentThread().name)
                    ASSET_JSON
                }
            withWorker { worker ->
                val card =
                    viewModels.keep(
                        BuildingCardViewModel(
                            symbol = "A-1",
                            repository = repository,
                            networkStatus = { flowOf(true) },
                            sources = sources(computeContext = worker),
                        ),
                    )
                val ready = card.uiState.first { it is BuildingCardUiState.Ready } as BuildingCardUiState.Ready
                val workerName = readThread.get().substringBefore(" @coroutine")
                assertEquals("A-1", ready.symbol, "Karta wczytała budynek A-1")
                assertEquals(COMPUTE_THREAD, workerName, "Stan liczony w przekazanym wątku obliczeń")
                assertNotEquals(callerThread, readThread.get(), "Stan liczony z flowOn poza wątkiem głównym")
            }
        }

    @Test
    fun skipsDuplicateOwnBarriers() =
        runTest {
            val own = MutableStateFlow(listOf(ownSubmission(id = "z1", state = SendState.LOCAL)))
            val repository = catalog()
            repository.catalog()
            val card =
                viewModels.keep(
                    BuildingCardViewModel(
                        symbol = "A-1",
                        repository = repository,
                        networkStatus = { flowOf(true) },
                        sources = sources(ownSubmissions = OwnSubmissions { own }),
                    ),
                )
            val emitted = mutableListOf<BuildingCardUiState>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { card.uiState.collect { emitted += it } }
            val countBefore = emitted.size
            val before = card.uiState.value as BuildingCardUiState.Ready
            own.value = listOf(ownSubmission(id = "z1", state = SendState.SENT))
            val after = card.uiState.value as BuildingCardUiState.Ready
            assertEquals(countBefore, emitted.size, "Powtórzony zbiór własnych zgłoszeń nie emituje stanu")
            assertSame(before.entrances, after.entrances, "Powtórzone własne zgłoszenia nie przebudowują wejść")
        }

    private suspend fun withWorker(block: suspend (CoroutineDispatcher) -> Unit) {
        val pool = Executors.newSingleThreadExecutor { runnable -> Thread(runnable, COMPUTE_THREAD) }
        try {
            pool.asCoroutineDispatcher().use { worker ->
                block(worker)
                viewModels.cancelAndJoin()
            }
        } finally {
            pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)
        }
    }

    private fun cardViewModel(
        repository: BuildingCatalogRepository = catalog(),
        serverEntrances: ServerEntrances = ServerEntrances { emptyList() },
        acceptedBarriers: AcceptedBarriers = AcceptedBarriers { emptyList() },
        plans: FloorPlans = NoFloorPlans,
    ): BuildingCardViewModel =
        viewModels.keep(
            BuildingCardViewModel(
                symbol = "A-1",
                repository = repository,
                networkStatus = { flowOf(true) },
                serverEntrances = serverEntrances,
                acceptedBarriers = acceptedBarriers,
                plans = plans,
            ),
        )

    private fun sources(
        ownSubmissions: OwnSubmissions = OwnSubmissions { flowOf(emptyList()) },
        computeContext: CoroutineContext = EmptyCoroutineContext,
    ): BuildingCardSources =
        BuildingCardSources(
            serverEntrances = ServerEntrances { emptyList() },
            acceptedBarriers = AcceptedBarriers { emptyList() },
            plans = NoFloorPlans,
            ownSubmissions = ownSubmissions,
            computeContext = computeContext,
        )

    private fun catalog(): BuildingCatalogRepository =
        unitSquareCatalog(
            symbol = "A-1",
            entrances =
                listOf(
                    entranceFeature(
                        symbol = "A-1",
                        id = "node/1",
                        tags = """"entrance":"main","wheelchair":"yes","ref":"Główne"""",
                    ),
                    entranceFeature(
                        symbol = "A-1",
                        id = "node/2",
                        tags = """"entrance":"yes","wheelchair":"yes","ref":"Boczne"""",
                    ),
                ),
        )

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
    }
}
