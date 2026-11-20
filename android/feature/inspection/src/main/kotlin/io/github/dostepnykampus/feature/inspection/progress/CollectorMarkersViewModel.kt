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
import io.github.dostepnykampus.core.data.room.FloorPlans
import io.github.dostepnykampus.core.model.building.Floor
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
 * @property markerId identyfikator własnego znacznika; `null`, gdy wiersz jest nieobejrzany
 * @property isPending czy znacznik czeka w telefonie na wysłanie
 */
data class FloorRow(
    val place: MarkerPlace,
    val label: String?,
    val markerId: String? = null,
    val isPending: Boolean = false,
)

/** Stan ekranu C5 w wariancie Zbierającego. Wymagania: US15.1, US15.2, US15.3, NFR-7 */
sealed interface CollectorMarkersUiState {
    /** Piętra budynku jeszcze się wczytują. */
    data object Loading : CollectorMarkersUiState

    /**
     * Piętra budynku z ich stanem.
     *
     * @property rows piętra od najniższego albo jeden wiersz całego budynku
     * @property asking identyfikator własnego znacznika, o którego cofnięcie pyta aplikacja, albo `null`
     * @property unsaved miejsce, którego zmiany nie udało się zapisać w telefonie, albo `null`
     */
    data class Ready(
        val rows: List<FloorRow>,
        val asking: String? = null,
        val unsaved: MarkerPlace? = null,
    ) : CollectorMarkersUiState
}

/**
 * Piętra budynku [symbol] dla Zbierającego: „Oznacz jako obejrzane” i „Cofnij znacznik” z potwierdzeniem. Zmiany
 * trafiają najpierw do kolejki w telefonie, która wysyła je na serwer, gdy jest sieć. Budynek bez pięter w kopii
 * planów ma jeden wiersz całego budynku.
 *
 * TODO(#2787): znaczniki zapisane na serwerze, także innych Zbierających.
 *
 * Wymagania: US15.1, US15.2, US15.3, BR-20, BR-G3, NFR-7
 */
@HiltViewModel(assistedFactory = CollectorMarkersViewModel.Factory::class)
class CollectorMarkersViewModel
    @AssistedInject
    constructor(
        @Assisted private val symbol: String,
        private val plans: FloorPlans,
        private val markers: FloorMarkers,
    ) : ViewModel() {
        private val asking = MutableStateFlow<String?>(null)
        private val unsaved = MutableStateFlow<MarkerPlace?>(null)

        /** Stan ekranu; zmienia się po każdym zapisie w kolejce telefonu. */
        val state: StateFlow<CollectorMarkersUiState> =
            combine(flow { emit(floors()) }, markers.pending(), asking, unsaved) { floors, pending, asked, failed ->
                val list = rows(floors, pending)
                val activeAsk = asked?.takeIf { id -> list.any { it.markerId == id } }
                CollectorMarkersUiState.Ready(rows = list, asking = activeAsk, unsaved = failed)
            }.stateIn(viewModelScope, SharingStarted.Eagerly, CollectorMarkersUiState.Loading)

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

        /** „Cofnij znacznik”: pyta o potwierdzenie, nic jeszcze nie zmienia. */
        fun askRevoke(markerId: String) {
            unsaved.value = null
            asking.value = markerId
        }

        /** „Zostaw znacznik”: znacznik zostaje, pytanie znika. */
        fun keepMarker() {
            asking.value = null
        }

        /** „Cofnij znacznik” w potwierdzeniu: zapisuje cofnięcie własnego znacznika w telefonie. */
        fun confirmRevoke() {
            val ready = state.value as? CollectorMarkersUiState.Ready ?: return
            val markerId = ready.asking ?: return
            val row = ready.rows.firstOrNull { it.markerId == markerId } ?: return
            asking.value = null
            unsaved.value = null
            viewModelScope.launch {
                try {
                    markers.revoke(
                        markerId = markerId,
                        buildingSymbol = symbol,
                        floorId = (row.place as? MarkerPlace.Floor)?.floorId,
                    )
                } catch (_: IOException) {
                    unsaved.value = row.place
                } catch (_: SQLiteException) {
                    unsaved.value = row.place
                }
            }
        }

        private suspend fun floors(): List<Floor> =
            (plans.floors(symbol) ?: plans.download(symbol).takeIf { it }?.let { plans.floors(symbol) })
                ?.floors
                .orEmpty()
                .sortedBy { it.order }

        private fun rows(floors: List<Floor>, pending: List<PendingMarkerChange>): List<FloorRow> {
            val revoked = pending.filter { it.buildingSymbol == symbol && it.isRevoke }.map { it.markerId }.toSet()
            val own =
                pending
                    .filter { it.buildingSymbol == symbol && !it.isRevoke && it.markerId !in revoked }
                    .associateBy { it.floorId }
            val places =
                floors
                    .map<Floor, Pair<MarkerPlace, String?>> { MarkerPlace.Floor(it.id) to it.label }
                    .ifEmpty { listOf(MarkerPlace.WholeBuilding to null) }
            return places.map { (place, label) ->
                val marker = own[(place as? MarkerPlace.Floor)?.floorId]
                FloorRow(place = place, label = label, markerId = marker?.markerId, isPending = marker != null)
            }
        }

        /** Tworzy [CollectorMarkersViewModel] dla symbolu z trasy C5. */
        @AssistedFactory
        interface Factory {
            /** Piętra budynku [symbol]. */
            fun create(symbol: String): CollectorMarkersViewModel
        }
    }
