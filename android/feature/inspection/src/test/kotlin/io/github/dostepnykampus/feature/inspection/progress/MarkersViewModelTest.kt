/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.inspection.progress

import io.github.dostepnykampus.core.data.building.BuildingFloors
import io.github.dostepnykampus.core.data.inspection.InspectionProgress
import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.model.building.Floor
import io.github.dostepnykampus.core.model.building.FloorList
import io.github.dostepnykampus.core.model.inspection.BuildingProgress
import io.github.dostepnykampus.core.model.inspection.InspectionCoverage
import io.github.dostepnykampus.core.model.inspection.InspectionMarker
import io.github.dostepnykampus.core.model.inspection.MarkerPlace
import io.github.dostepnykampus.core.model.inspection.ProgressMarker
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
import java.time.Instant

/**
 * Znaczniki budynku w C5 Koordynatora: pytanie o potwierdzenie, cofnięcie po potwierdzeniu i znacznik, który zostaje.
 *
 * Wymagania: US11.3, BR-20
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MarkersViewModelTest {
    /**
     * Potwierdzenie cofa znacznik na serwerze i zmniejsza postęp budynku.
     * Scenariusz: postep-inwentaryzacji.feature: Koordynator cofa znacznik dowolnego Zbierającego po potwierdzeniu
     *
     * Wymagania: US11.3
     */
    @Test
    fun confirmRevokesShrinksProgress() =
        viewModelTest { viewModel, revoked ->
            viewModel.ask(MARKER_ID)
            assertEquals(MARKER_ID, (viewModel.state.value as MarkersUiState.Ready).asking, "pytanie o potwierdzenie")
            assertEquals(emptyList<String>(), revoked, "przed potwierdzeniem serwer nic nie dostaje")
            viewModel.confirm()
            runCurrent()
            val floors = InspectionCoverage.Floors(floorsTotal = 5, floorsInspected = 3)
            val expected = MarkersUiState.Ready(BUILDING.copy(coverage = floors, markers = emptyList()))
            assertEquals(expected, viewModel.state.value, "o jedno piętro mniej")
            assertEquals(listOf(MARKER_ID), revoked, "serwer dostał cofnięcie")
        }

    /**
     * „Zostaw znacznik” niczego nie zmienia i nie wysyła cofnięcia.
     * Scenariusz: postep-inwentaryzacji.feature: Bez potwierdzenia znacznik zostaje
     *
     * Wymagania: US11.3
     */
    @Test
    fun keepChangesNothing() =
        viewModelTest { viewModel, revoked ->
            viewModel.ask(MARKER_ID)
            viewModel.keep()
            runCurrent()
            assertEquals(MarkersUiState.Ready(BUILDING), viewModel.state.value, "znacznik zostaje")
            assertEquals(emptyList<String>(), revoked, "serwer nic nie dostał")
        }

    /** Brak internetu przy cofnięciu: znacznik zostaje, a ekran mówi dlaczego. Wymagania: US11.3, NFR-50 */
    @Test
    fun offlineKeepsMarker() =
        viewModelTest(revokeReply = ServerReply.Offline) { viewModel, _ ->
            viewModel.ask(MARKER_ID)
            viewModel.confirm()
            runCurrent()
            val expected = MarkersUiState.Ready(BUILDING, failure = RevokeFailure.OFFLINE)
            assertEquals(expected, viewModel.state.value, "znacznik zostaje, bez internetu")
        }

    /** Odmowa serwera przy cofnięciu: znacznik zostaje. Wymagania: US11.3 */
    @Test
    fun serverFaultKeepsMarker() =
        viewModelTest(revokeReply = ServerReply.Fault(FORBIDDEN)) { viewModel, _ ->
            viewModel.ask(MARKER_ID)
            viewModel.confirm()
            runCurrent()
            val expected = MarkersUiState.Ready(BUILDING, failure = RevokeFailure.SERVER)
            assertEquals(expected, viewModel.state.value, "znacznik zostaje, błąd serwera")
        }

    /** Budynek bez postępu na serwerze: błąd zamiast pustego ekranu. Wymagania: US11.3 */
    @Test
    fun unknownBuildingIsError() =
        viewModelTest(symbol = "Z-9") { viewModel, _ ->
            runCurrent()
            assertEquals(MarkersUiState.ServerError, viewModel.state.value, "nieznany budynek")
        }

    /**
     * „Spróbuj ponownie” po błędzie odczytu czyta budynek od nowa.
     *
     * Wymagania: US11.3, NFR-50
     */
    @Test
    fun retryReadsAgain() =
        viewModelTest(symbol = "Z-9") { viewModel, _ ->
            viewModel.retry()
            assertEquals(MarkersUiState.Loading, viewModel.state.value, "w trakcie odczytu")
            runCurrent()
            assertEquals(MarkersUiState.ServerError, viewModel.state.value, "budynku nadal nie ma")
        }

    /** Etykieta piętra z danych PWr trafia do stanu i zostaje po cofnięciu znacznika. Wymagania: US11.3 */
    @Test
    fun carriesFloorLabels() =
        viewModelTest(floors = { FloorList("PWr", listOf(Floor(id = "2", label = "Parter", order = 0))) }) { vm, _ ->
            val expected = mapOf("2" to "Parter")
            assertEquals(expected, (vm.state.value as MarkersUiState.Ready).floorLabels, "etykieta po otwarciu")
            vm.ask(MARKER_ID)
            vm.confirm()
            runCurrent()
            assertEquals(expected, (vm.state.value as MarkersUiState.Ready).floorLabels, "etykieta po cofnięciu")
        }

    private fun viewModelTest(
        symbol: String = "A-1",
        revokeReply: ServerReply<Unit> = ServerReply.Ok(Unit),
        floors: BuildingFloors = BuildingFloors { null },
        block: suspend TestScope.(MarkersViewModel, MutableList<String>) -> Unit,
    ) {
        val revoked = mutableListOf<String>()
        val source =
            object : InspectionProgress {
                override suspend fun buildings(): ServerReply<List<BuildingProgress>> = ServerReply.Ok(listOf(BUILDING))

                override suspend fun revoke(markerId: String): ServerReply<Unit> {
                    revoked.add(markerId)
                    return revokeReply
                }
            }
        val dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
        try {
            runTest(dispatcher) {
                val viewModel = MarkersViewModel(symbol, source, floors)
                runCurrent()
                block(viewModel, revoked)
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    private companion object {
        const val MARKER_ID = "7f0c1c1e-0000-4000-8000-000000000001"
        const val FORBIDDEN = 403
        val MARKER =
            InspectionMarker(
                id = MARKER_ID,
                buildingSymbol = "A-1",
                place = MarkerPlace.Floor(floorId = "2"),
                createdAt = Instant.parse("2026-11-12T10:00:00Z"),
                isRevoked = false,
                isMine = false,
            )
        val BUILDING =
            BuildingProgress(
                buildingSymbol = "A-1",
                coverage = InspectionCoverage.Floors(floorsTotal = 5, floorsInspected = 4),
                pendingSubmissions = 3,
                acceptedSubmissions = 4,
                entrancesTotal = 6,
                entrancesCheckedOnSite = 1,
                markers = listOf(ProgressMarker(marker = MARKER, authorPseudonym = "Autor 14")),
            )
    }
}
