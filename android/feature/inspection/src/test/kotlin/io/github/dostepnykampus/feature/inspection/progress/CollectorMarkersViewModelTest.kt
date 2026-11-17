/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.inspection.progress

import io.github.dostepnykampus.core.data.inspection.FloorMarkers
import io.github.dostepnykampus.core.data.inspection.PendingMarkerChange
import io.github.dostepnykampus.core.data.inspection.ServerMarkers
import io.github.dostepnykampus.core.data.room.FloorPlans
import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.model.building.Floor
import io.github.dostepnykampus.core.model.building.FloorList
import io.github.dostepnykampus.core.model.inspection.InspectionMarker
import io.github.dostepnykampus.core.model.inspection.MarkerPlace
import io.github.dostepnykampus.core.model.room.RoomList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.Instant

/**
 * C5 Zbierającego: piętra budynku od najniższego, cały budynek bez planu, znacznik w kolejce telefonu i błąd zapisu.
 *
 * Wymagania: US15.1, US15.3, BR-G3, NFR-7
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CollectorMarkersViewModelTest {
    @Test
    fun listsFloorsFromLowest() =
        viewModelTest(FakePlans(stored = A1_FLOORS)) { viewModel, _ ->
            assertEquals(CollectorMarkersUiState.Ready(listOf(GROUND, FIRST)), viewModel.state.value, "piętra A-1")
        }

    /** Przykład „jedno z 5 pięter jako obejrzane”: znacznik czeka w telefonie, a wiersz jest obejrzany. */
    @Test
    fun marksFloorInPhone() =
        viewModelTest(FakePlans(stored = A1_FLOORS)) { viewModel, markers ->
            viewModel.mark(MarkerPlace.Floor("p1"))
            runCurrent()
            val marked = FIRST.copy(markerId = "m-1", isPending = true)
            assertEquals(CollectorMarkersUiState.Ready(listOf(GROUND, marked)), viewModel.state.value, "piętro 1")
            assertEquals(listOf("A-1" to "p1"), markers.saved, "kolejka w telefonie")
        }

    /** Przykład „cały budynek jako obejrzany”: budynek bez pięter w kopii i bez sieci ma jeden wiersz. */
    @Test
    fun marksWholeBuilding() =
        viewModelTest(FakePlans(stored = null, downloaded = null)) { viewModel, markers ->
            viewModel.mark(MarkerPlace.WholeBuilding)
            runCurrent()
            val row = FloorRow(place = MarkerPlace.WholeBuilding, label = null, markerId = "m-1", isPending = true)
            assertEquals(CollectorMarkersUiState.Ready(listOf(row)), viewModel.state.value, "cały budynek")
            assertEquals(listOf("A-1" to null), markers.saved, "znacznik bez piętra")
        }

    @Test
    fun downloadsMissingFloors() =
        viewModelTest(FakePlans(stored = null, downloaded = A1_FLOORS)) { viewModel, _ ->
            assertEquals(CollectorMarkersUiState.Ready(listOf(GROUND, FIRST)), viewModel.state.value, "pobrane piętra")
        }

    @Test
    fun retriesFailedSave() =
        viewModelTest(FakePlans(stored = A1_FLOORS), FakeMarkers(failures = 1)) { viewModel, _ ->
            viewModel.mark(MarkerPlace.Floor("p0"))
            runCurrent()
            val failed = CollectorMarkersUiState.Ready(listOf(GROUND, FIRST), unsaved = MarkerPlace.Floor("p0"))
            assertEquals(failed, viewModel.state.value, "błąd zapisu")
            viewModel.mark(MarkerPlace.Floor("p0"))
            runCurrent()
            val marked = GROUND.copy(markerId = "m-1", isPending = true)
            assertEquals(CollectorMarkersUiState.Ready(listOf(marked, FIRST)), viewModel.state.value, "po ponowieniu")
        }

    /** Przykład „znacznik innej osoby”: znaczniki z serwera pobrane przy otwarciu ekranu, własny i cudzy. */
    @Test
    fun showsServerMarkers() {
        val mine = serverMarker(id = "s-1", floor = "p0", isMine = true)
        val server = FakeServer(mine, serverMarker(id = "s-2", floor = "p1"))
        viewModelTest(FakePlans(stored = A1_FLOORS), server = server) { viewModel, _ ->
            val rows = listOf(GROUND.copy(markerId = "s-1"), FIRST.copy(markerId = "s-2", isMine = false))
            assertEquals(CollectorMarkersUiState.Ready(rows), viewModel.state.value, "znaczniki z serwera")
            assertEquals(1, server.refreshes, "pobranie przy otwarciu")
        }
    }

    /** Cofnięcie czekające w telefonie ukrywa znacznik z serwera. */
    @Test
    fun hidesRevokedServerMarker() {
        val server = FakeServer(serverMarker(id = "s-1", floor = "p0", isMine = true))
        viewModelTest(FakePlans(stored = A1_FLOORS), server = server) { viewModel, markers ->
            markers.revoke(markerId = "s-1", buildingSymbol = "A-1", floorId = "p0")
            runCurrent()
            assertEquals(CollectorMarkersUiState.Ready(listOf(GROUND, FIRST)), viewModel.state.value, "po cofnięciu")
        }
    }

    private fun viewModelTest(
        plans: FloorPlans,
        markers: FakeMarkers = FakeMarkers(),
        server: FakeServer = FakeServer(),
        block: suspend TestScope.(CollectorMarkersViewModel, FakeMarkers) -> Unit,
    ) {
        val dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
        try {
            runTest(dispatcher) {
                val viewModel =
                    CollectorMarkersViewModel(symbol = "A-1", plans = plans, markers = markers, server = server)
                runCurrent()
                block(viewModel, markers)
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    private companion object {
        val A1_FLOORS =
            FloorList(
                source = "PWr",
                floors =
                    listOf(
                        Floor(id = "p1", label = "Piętro 1", order = 1),
                        Floor(id = "p0", label = "Parter", order = 0),
                    ),
            )
        val GROUND = FloorRow(MarkerPlace.Floor("p0"), label = "Parter")
        val FIRST = FloorRow(MarkerPlace.Floor("p1"), label = "Piętro 1")

        fun serverMarker(id: String, floor: String, isMine: Boolean = false): InspectionMarker =
            InspectionMarker(
                id = id,
                buildingSymbol = "A-1",
                place = MarkerPlace.Floor(floor),
                createdAt = Instant.parse("2026-11-17T10:00:00Z"),
                isRevoked = false,
                isMine = isMine,
            )
    }
}

/** Kopia planów z piętrami w telefonie [stored] i piętrami, które daje pobranie z serwera [downloaded]. */
private class FakePlans(private var stored: FloorList?, private val downloaded: FloorList? = null) : FloorPlans {
    override suspend fun download(symbol: String): Boolean {
        stored = downloaded
        return downloaded != null
    }

    override suspend fun floors(symbol: String): FloorList? = stored

    override suspend fun rooms(symbol: String, floor: String): RoomList? = null
}

/** Kolejka znaczników w telefonie; pierwsze [failures] zapisy kończą się błędem dysku. */
private class FakeMarkers(private var failures: Int = 0) : FloorMarkers {
    private val queue = MutableStateFlow(emptyList<PendingMarkerChange>())
    val saved = mutableListOf<Pair<String, String?>>()

    override fun pending(): Flow<List<PendingMarkerChange>> = queue

    override suspend fun mark(buildingSymbol: String, floorId: String?): String {
        if (failures-- > 0) throw IOException("brak miejsca")
        val markerId = "m-${saved.size + 1}"
        saved += buildingSymbol to floorId
        queue.value +=
            PendingMarkerChange(
                markerId = markerId,
                buildingSymbol = buildingSymbol,
                floorId = floorId,
                isRevoke = false,
            )
        return markerId
    }

    override suspend fun revoke(markerId: String, buildingSymbol: String, floorId: String?) {
        queue.value +=
            PendingMarkerChange(
                markerId = markerId,
                buildingSymbol = buildingSymbol,
                floorId = floorId,
                isRevoke = true,
            )
    }
}

private class FakeServer(vararg saved: InspectionMarker) : ServerMarkers {
    private val markers = MutableStateFlow(saved.toList())
    var refreshes = 0

    override fun building(buildingSymbol: String): Flow<List<InspectionMarker>> = markers

    override suspend fun refresh(): ServerReply<Unit> {
        refreshes++
        return ServerReply.Ok(Unit)
    }
}
