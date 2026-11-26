/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
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
import io.github.dostepnykampus.core.data.inspection.ServerFloorMarkers
import io.github.dostepnykampus.core.data.network.NetworkStatus
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
 * @property isMine czy znacznik postawił bieżący Zbierający
 */
data class FloorRow(
    val place: MarkerPlace,
    val label: String?,
    val markerId: String? = null,
    val isPending: Boolean = false,
    val isMine: Boolean = true,
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
     * @property isOnline czy telefon ma internet
     */
    data class Ready(
        val rows: List<FloorRow>,
        val asking: String? = null,
        val unsaved: MarkerPlace? = null,
        val isOnline: Boolean = true,
    ) : CollectorMarkersUiState
}

/**
 * Piętra budynku [symbol] dla Zbierającego: „Oznacz jako obejrzane” i „Cofnij znacznik” z potwierdzeniem. Łączy
 * znaczniki pobrane z serwera (własne i innych Zbierających) ze zmianami czekającymi w kolejce telefonu. Budynek
 * bez pięter w kopii planów ma jeden wiersz całego budynku.
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
        private val serverMarkers: ServerFloorMarkers,
        private val network: NetworkStatus,
    ) : ViewModel() {
        private val asking = MutableStateFlow<String?>(null)
        private val unsaved = MutableStateFlow<MarkerPlace?>(null)

        init {
            viewModelScope.launch { serverMarkers.pull() }
        }

        /** Stan ekranu; zmienia się po każdym zapisie w kolejce telefonu i pobraniu z serwera. */
        val state: StateFlow<CollectorMarkersUiState> =
            combine(
                flow = flow { emit(floors()) },
                flow2 = markers.pending(),
                flow3 = serverMarkers.markers(symbol),
                flow4 = asking,
                flow5 = unsaved,
            ) { floors, pending, synced, asked, failed ->
                val list = rows(floors, pending, synced)
                val activeAsk = asked?.takeIf { id -> list.any { it.markerId == id && it.isMine } }
                CollectorMarkersUiState.Ready(rows = list, asking = activeAsk, unsaved = failed)
            }.combine(network.isOnline()) { ready, online -> ready.copy(isOnline = online) }
                .stateIn(viewModelScope, SharingStarted.Eagerly, CollectorMarkersUiState.Loading)

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
            val row = askedRow() ?: return
            val markerId = row.markerId ?: return
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

        private fun askedRow(): FloorRow? {
            val ready = state.value as? CollectorMarkersUiState.Ready
            return ready?.asking?.let { id -> ready.rows.firstOrNull { it.markerId == id && it.isMine } }
        }

        private suspend fun floors(): List<Floor> =
            (plans.floors(symbol) ?: plans.download(symbol).takeIf { it }?.let { plans.floors(symbol) })
                ?.floors
                .orEmpty()
                .sortedBy { it.order }

        private fun rows(
            floors: List<Floor>,
            pending: List<PendingMarkerChange>,
            synced: List<InspectionMarker>,
        ): List<FloorRow> {
            val revoked = pending.filter { it.buildingSymbol == symbol && it.isRevoke }.map { it.markerId }.toSet()
            val ownPending =
                pending
                    .filter { it.buildingSymbol == symbol && !it.isRevoke && it.markerId !in revoked }
                    .associateBy { it.floorId }
            val byFloor =
                synced
                    .filter { it.id !in revoked }
                    .groupBy { (it.place as? MarkerPlace.Floor)?.floorId }
            val places =
                floors
                    .map<Floor, Pair<MarkerPlace, String?>> { MarkerPlace.Floor(it.id) to it.label }
                    .ifEmpty { listOf(MarkerPlace.WholeBuilding to null) }
            return places.map { (place, label) ->
                rowFor(place = place, label = label, ownPending = ownPending, synced = byFloor)
            }
        }

        private fun rowFor(
            place: MarkerPlace,
            label: String?,
            ownPending: Map<String?, PendingMarkerChange>,
            synced: Map<String?, List<InspectionMarker>>,
        ): FloorRow {
            val floorId = (place as? MarkerPlace.Floor)?.floorId
            val queued = ownPending[floorId]
            val serverList = synced[floorId].orEmpty()
            val fromServer = serverList.firstOrNull { it.isMine } ?: serverList.firstOrNull()
            return if (queued != null) {
                FloorRow(place = place, label = label, markerId = queued.markerId, isPending = true, isMine = true)
            } else if (fromServer != null) {
                FloorRow(
                    place = place,
                    label = label,
                    markerId = fromServer.id,
                    isPending = false,
                    isMine = fromServer.isMine,
                )
            } else {
                FloorRow(place = place, label = label)
            }
        }

        /** Tworzy [CollectorMarkersViewModel] dla symbolu z trasy C5. */
        @AssistedFactory
        interface Factory {
            /** Piętra budynku [symbol]. */
            fun create(symbol: String): CollectorMarkersViewModel
        }
    }
