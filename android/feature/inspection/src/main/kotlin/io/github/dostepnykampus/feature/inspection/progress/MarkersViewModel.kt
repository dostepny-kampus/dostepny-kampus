/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.inspection.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.dostepnykampus.core.data.building.BuildingFloors
import io.github.dostepnykampus.core.data.inspection.InspectionProgress
import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.model.inspection.BuildingProgress
import io.github.dostepnykampus.core.model.inspection.InspectionCoverage
import io.github.dostepnykampus.core.model.inspection.MarkerPlace
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Czemu cofnięcie znacznika się nie udało. Wymagania: US11.3, NFR-50 */
enum class RevokeFailure {
    /** Brak internetu: Koordynator cofa znacznik tylko przez serwer. */
    OFFLINE,

    /** Serwer odpowiedział błędem albo odmówił. */
    SERVER,
}

/** Stan ekranu C5 w wariancie Koordynatora. Wymagania: US11.3, BR-20 */
sealed interface MarkersUiState {
    /** „Wczytujemy postęp…”. */
    data object Loading : MarkersUiState

    /**
     * Budynek z jego znacznikami.
     *
     * @property building postęp budynku, po cofnięciu pomniejszony o cofnięty znacznik
     * @property asking identyfikator znacznika, o którego cofnięcie pyta aplikacja, albo null
     * @property failure czemu ostatnie cofnięcie się nie udało, albo null
     * @property floorLabels etykiety pięter z danych PWr według identyfikatora; bez etykiety piętra nie ma go w mapie
     */
    data class Ready(
        val building: BuildingProgress,
        val asking: String? = null,
        val failure: RevokeFailure? = null,
        val floorLabels: Map<String, String> = emptyMap(),
    ) : MarkersUiState

    /** Bez internetu. */
    data object Offline : MarkersUiState

    /** Serwer odpowiedział błędem, odmówił albo nie zna budynku. */
    data object ServerError : MarkersUiState
}

/**
 * Znaczniki budynku [symbol] z autorami i „Cofnij znacznik” z potwierdzeniem. Cofnięcie dociera do serwera
 * dopiero po potwierdzeniu; „Zostaw znacznik” niczego nie zmienia.
 *
 * Wymagania: US11.3, BR-20
 */
@HiltViewModel(assistedFactory = MarkersViewModel.Factory::class)
class MarkersViewModel
    @AssistedInject
    constructor(
        @Assisted private val symbol: String,
        private val progress: InspectionProgress,
        private val floors: BuildingFloors,
    ) : ViewModel() {
        private val current = MutableStateFlow<MarkersUiState>(MarkersUiState.Loading)

        /** Stan ekranu. */
        val state: StateFlow<MarkersUiState> = current

        init {
            viewModelScope.launch { current.value = opened() }
        }

        /** „Spróbuj ponownie” po błędzie odczytu: czyta budynek od nowa. */
        fun retry() {
            current.value = MarkersUiState.Loading
            viewModelScope.launch { current.value = opened() }
        }

        /** „Cofnij znacznik”: pyta o potwierdzenie, nic jeszcze nie zmienia. */
        fun ask(markerId: String) {
            update { it.copy(asking = markerId, failure = null) }
        }

        /** „Zostaw znacznik”: znacznik zostaje, pytanie znika. */
        fun keep() {
            update { it.copy(asking = null) }
        }

        /** „Cofnij znacznik” w potwierdzeniu: serwer cofa znacznik, a postęp budynku się zmniejsza. */
        fun confirm() {
            val shown = current.value as? MarkersUiState.Ready ?: return
            val markerId = shown.asking ?: return
            viewModelScope.launch { current.value = revoked(shown, markerId) }
        }

        private suspend fun opened(): MarkersUiState {
            val reply = progress.buildings()
            val building = (reply as? ServerReply.Ok)?.run { value.firstOrNull { it.buildingSymbol == symbol } }
            return when {
                building != null -> MarkersUiState.Ready(building, floorLabels = labels())
                reply == ServerReply.Offline -> MarkersUiState.Offline
                else -> MarkersUiState.ServerError
            }
        }

        private suspend fun labels(): Map<String, String> =
            floors.floors(symbol)?.floors.orEmpty().associate { floor -> floor.id to floor.label }

        private suspend fun revoked(shown: MarkersUiState.Ready, markerId: String): MarkersUiState =
            when (progress.revoke(markerId)) {
                is ServerReply.Ok -> shown.copy(building = without(shown.building, markerId), asking = null)
                ServerReply.Offline -> shown.copy(asking = null, failure = RevokeFailure.OFFLINE)
                ServerReply.Missing, is ServerReply.Fault -> shown.copy(asking = null, failure = RevokeFailure.SERVER)
            }

        private fun update(change: (MarkersUiState.Ready) -> MarkersUiState.Ready) {
            (current.value as? MarkersUiState.Ready)?.let { current.value = change(it) }
        }

        private fun without(building: BuildingProgress, markerId: String): BuildingProgress {
            val removed = building.markers.firstOrNull { it.marker.id == markerId }?.run { marker.place }
            return building.copy(
                markers = building.markers.filterNot { it.marker.id == markerId },
                coverage = coverageWithout(building.coverage, removed),
            )
        }

        private fun coverageWithout(coverage: InspectionCoverage, place: MarkerPlace?): InspectionCoverage {
            val floors = coverage as? InspectionCoverage.Floors
            val shrunk = floors?.copy(floorsInspected = floors.floorsInspected - 1)
            return when {
                shrunk != null && place is MarkerPlace.Floor -> shrunk
                place == MarkerPlace.WholeBuilding -> InspectionCoverage.Whole(isInspected = false)
                else -> coverage
            }
        }

        /** Tworzy [MarkersViewModel] dla symbolu z trasy C5. */
        @AssistedFactory
        interface Factory {
            /** Znaczniki budynku [symbol]. */
            fun create(symbol: String): MarkersViewModel
        }
    }
