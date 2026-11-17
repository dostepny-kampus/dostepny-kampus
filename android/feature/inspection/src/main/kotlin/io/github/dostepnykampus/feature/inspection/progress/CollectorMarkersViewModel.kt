/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.inspection.progress

import android.database.sqlite.SQLiteException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.dostepnykampus.core.data.inspection.FloorMarkers
import io.github.dostepnykampus.core.data.inspection.PendingMarkerChange
import io.github.dostepnykampus.core.data.inspection.ServerMarkers
import io.github.dostepnykampus.core.data.room.FloorPlans
import io.github.dostepnykampus.core.model.building.Floor
import io.github.dostepnykampus.core.model.inspection.InspectionMarker
import io.github.dostepnykampus.core.model.inspection.MarkerPlace
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Wiersz C5 Zbierającego: piętro budynku szczegółowego albo cały budynek.
 *
 * @property place piętro albo cały budynek
 * @property label etykieta piętra z danych PWr; `null` dla całego budynku
 * @property markerId identyfikator znacznika; `null`, gdy wiersz jest nieobejrzany
 * @property isPending czy znacznik czeka w telefonie na wysłanie
 * @property isMine czy znacznik postawiła osoba z telefonu
 */
data class FloorRow(
    val place: MarkerPlace,
    val label: String?,
    val markerId: String? = null,
    val isPending: Boolean = false,
    val isMine: Boolean = true,
)

/** Stan ekranu C5 w wariancie Zbierającego. Wymagania: US15.1, US15.3, NFR-7 */
sealed interface CollectorMarkersUiState {
    /** Piętra budynku jeszcze się wczytują. */
    data object Loading : CollectorMarkersUiState

    /**
     * Piętra budynku z ich stanem.
     *
     * @property rows piętra od najniższego albo jeden wiersz całego budynku
     * @property unsaved miejsce, którego znacznika nie udało się zapisać w telefonie, albo `null`
     */
    data class Ready(val rows: List<FloorRow>, val unsaved: MarkerPlace? = null) : CollectorMarkersUiState
}

/**
 * Piętra budynku [symbol] dla Zbierającego i „Oznacz jako obejrzane”. Znacznik trafia najpierw do kolejki w telefonie,
 * która wysyła go na serwer, gdy jest sieć. Obok kolejki stoją znaczniki z serwera, także innych Zbierających,
 * pobierane przy otwarciu ekranu. Budynek bez pięter w kopii planów ma jeden wiersz całego budynku.
 *
 * Wymagania: US15.1, US15.3, BR-20, BR-G3, NFR-7
 */
@HiltViewModel(assistedFactory = CollectorMarkersViewModel.Factory::class)
class CollectorMarkersViewModel
    @AssistedInject
    constructor(
        @Assisted private val symbol: String,
        private val plans: FloorPlans,
        private val markers: FloorMarkers,
        private val server: ServerMarkers,
    ) : ViewModel() {
        private val unsaved = MutableStateFlow<MarkerPlace?>(null)

        /** Stan ekranu; zmienia się po każdym zapisie w kolejce telefonu. */
        val state: StateFlow<CollectorMarkersUiState> =
            combine(
                flow = flow { emit(floors()) },
                flow2 = markers.pending(),
                flow3 = server.building(symbol),
                flow4 = unsaved,
            ) { floors, pending, saved, failed ->
                CollectorMarkersUiState.Ready(rows = rows(floors, pending, saved), unsaved = failed)
            }.stateIn(viewModelScope, SharingStarted.Eagerly, CollectorMarkersUiState.Loading)

        init {
            viewModelScope.launch { server.refresh() }
        }

        /** „Oznacz jako obejrzane” i „Spróbuj ponownie” po błędzie zapisu: zapisuje znacznik [place] w telefonie. */
        fun mark(place: MarkerPlace) {
            unsaved.value = null
            viewModelScope.launch {
                try {
                    markers.mark(buildingSymbol = symbol, floorId = (place as? MarkerPlace.Floor)?.floorId)
                } catch (_: IOException) {
                    unsaved.value = place
                } catch (_: SQLiteException) {
                    unsaved.value = place
                }
            }
        }

        private suspend fun floors(): List<Floor> =
            (plans.floors(symbol) ?: plans.download(symbol).takeIf { it }?.let { plans.floors(symbol) })
                ?.floors
                .orEmpty()
                .sortedBy { it.order }

        private fun rows(
            floors: List<Floor>,
            pending: List<PendingMarkerChange>,
            saved: List<InspectionMarker>,
        ): List<FloorRow> {
            val queued = pending.filter { it.buildingSymbol == symbol }
            val own = queued.filterNot { it.isRevoke }.associateBy { it.floorId }
            val revoked = queued.filter { it.isRevoke }.map { it.markerId }.toSet()
            val onServer =
                saved
                    .filter { it.id !in revoked }
                    .sortedBy { it.isMine }
                    .associateBy(keySelector = { (it.place as? MarkerPlace.Floor)?.floorId }, valueTransform = { it })
            val places =
                floors
                    .map<Floor, Pair<MarkerPlace, String?>> { MarkerPlace.Floor(it.id) to it.label }
                    .ifEmpty { listOf(MarkerPlace.WholeBuilding to null) }
            return places.map { (place, label) ->
                val floorId = (place as? MarkerPlace.Floor)?.floorId
                val queuedId = own[floorId]?.markerId
                val server = onServer[floorId]
                FloorRow(
                    place = place,
                    label = label,
                    markerId = queuedId ?: server?.id,
                    isPending = queuedId != null,
                    isMine = queuedId != null || server?.isMine != false,
                )
            }
        }

        /** Tworzy [CollectorMarkersViewModel] dla symbolu z trasy C5. */
        @AssistedFactory
        interface Factory {
            /** Piętra budynku [symbol]. */
            fun create(symbol: String): CollectorMarkersViewModel
        }
    }
