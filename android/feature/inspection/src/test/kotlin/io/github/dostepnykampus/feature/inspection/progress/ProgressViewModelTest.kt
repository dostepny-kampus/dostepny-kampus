/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.inspection.progress

import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.inspection.InspectionProgress
import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.model.inspection.BuildingProgress
import io.github.dostepnykampus.core.model.inspection.InspectionCoverage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Stan listy K1: wczytywanie, lista z chwilą odczytu, bez sieci, błąd serwera, brak uprawnień i odświeżanie.
 *
 * Wymagania: US11.1, US11.2, NFR-50
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProgressViewModelTest {
    @Test
    fun showsLoadingThenList() =
        viewModelTest(ServerReply.Ok(listOf(DETAILED))) { viewModel ->
            assertEquals(ProgressUiState.Loading, viewModel.state.value, "przed odpowiedzią serwera")
            runCurrent()
            val expected = ProgressUiState.Ready(buildings = listOf(DETAILED), readAt = READ_AT, names = NAMES)
            assertEquals(expected, viewModel.state.value, "lista z chwilą odczytu")
        }

    @Test
    fun carriesNamesFromCatalog() =
        viewModelTest(ServerReply.Ok(listOf(DETAILED, BASIC))) { viewModel ->
            runCurrent()
            val state = viewModel.state.value as ProgressUiState.Ready
            assertEquals(mapOf("A-1" to "Gmach A"), state.names, "nazwa z katalogu tylko dla budynku, który ją ma")
        }

    @Test
    fun showsOffline() =
        viewModelTest(ServerReply.Offline) { viewModel ->
            runCurrent()
            assertEquals(ProgressUiState.Offline, viewModel.state.value, "bez internetu")
        }

    @Test
    fun showsServerError() =
        viewModelTest(ServerReply.Fault(SERVER_ERROR)) { viewModel ->
            runCurrent()
            assertEquals(ProgressUiState.ServerError, viewModel.state.value, "błąd serwera")
        }

    @Test
    fun showsForbidden() =
        viewModelTest(ServerReply.Fault(FORBIDDEN)) { viewModel ->
            runCurrent()
            assertEquals(ProgressUiState.Forbidden, viewModel.state.value, "brak uprawnień")
        }

    @Test
    fun refreshKeepsList() =
        viewModelTest(ServerReply.Ok(listOf(DETAILED)), ServerReply.Ok(listOf(BASIC))) { viewModel ->
            runCurrent()
            viewModel.refresh()
            val state = viewModel.state
            val busy =
                ProgressUiState.Ready(
                    buildings = listOf(DETAILED),
                    readAt = READ_AT,
                    isRefreshing = true,
                    names = NAMES,
                )
            assertEquals(busy, state.value, "stara lista w trakcie")
            runCurrent()
            val fresh = ProgressUiState.Ready(listOf(BASIC), READ_AT, names = NAMES)
            assertEquals(fresh, state.value, "nowa lista po odświeżeniu")
        }

    @Test
    fun noSecondReadWhileBusy() {
        val reads = AtomicInteger()
        val source =
            object : InspectionProgress {
                override suspend fun buildings(): ServerReply<List<BuildingProgress>> {
                    reads.incrementAndGet()
                    return ServerReply.Offline
                }

                override suspend fun revoke(markerId: String): ServerReply<Unit> = ServerReply.Missing
            }
        runViewModelTest(source) { viewModel ->
            viewModel.refresh()
            viewModel.refresh()
            runCurrent()
            assertEquals(1, reads.get(), "jeden odczyt, mimo trzech wywołań w trakcie wczytywania")
        }
    }

    private fun viewModelTest(
        vararg replies: ServerReply<List<BuildingProgress>>,
        block: suspend TestScope.(ProgressViewModel) -> Unit,
    ) {
        val queue = replies.iterator()
        val source =
            object : InspectionProgress {
                override suspend fun buildings(): ServerReply<List<BuildingProgress>> = queue.next()

                override suspend fun revoke(markerId: String): ServerReply<Unit> = ServerReply.Missing
            }
        runViewModelTest(source, block)
    }

    private fun runViewModelTest(source: InspectionProgress, block: suspend TestScope.(ProgressViewModel) -> Unit) {
        val dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
        try {
            runTest(dispatcher) { block(ProgressViewModel(source, catalog(), Clock.fixed(READ_AT, ZoneOffset.UTC))) }
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun catalog() = BuildingCatalogRepository(EmptyCoroutineContext) { FILE }

    private companion object {
        const val SERVER_ERROR = 500
        const val FORBIDDEN = 403
        val READ_AT: Instant = Instant.parse("2026-11-12T10:42:00Z")
        val NAMES = mapOf("A-1" to "Gmach A")
        const val FILE =
            """{"type":"FeatureCollection","attribution":"© plik","features":[""" +
                """{"type":"Feature","properties":{"kind":"building","symbols":["A-1"],"display_name":"Gmach A",""" +
                """"osm_id":"way/1"},"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}},""" +
                """{"type":"Feature","properties":{"kind":"building","symbols":["B-4"],"osm_id":"way/2"},""" +
                """"geometry":{"type":"Polygon","coordinates":[[[2,2],[3,2],[3,3],[2,2]]]}}]}"""
        val DETAILED =
            BuildingProgress(
                buildingSymbol = "A-1",
                coverage = InspectionCoverage.Floors(floorsTotal = 5, floorsInspected = 4),
                pendingSubmissions = 3,
                acceptedSubmissions = 4,
                entrancesTotal = 6,
                entrancesCheckedOnSite = 1,
                markers = emptyList(),
            )
        val BASIC =
            BuildingProgress(
                buildingSymbol = "B-4",
                coverage = InspectionCoverage.Whole(isInspected = true),
                pendingSubmissions = 0,
                acceptedSubmissions = 1,
                entrancesTotal = 2,
                entrancesCheckedOnSite = 0,
                markers = emptyList(),
            )
    }
}
