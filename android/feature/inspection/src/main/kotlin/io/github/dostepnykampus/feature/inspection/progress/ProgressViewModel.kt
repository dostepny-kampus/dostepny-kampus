/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.inspection.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.dostepnykampus.core.data.inspection.InspectionProgress
import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.model.inspection.BuildingProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/** Stan ekranu K1. Wymagania: US11.1, US11.2, NFR-50 */
sealed interface ProgressUiState {
    /** „Wczytujemy postęp…”. */
    data object Loading : ProgressUiState

    /**
     * Lista budynków z postępem.
     *
     * @property buildings budynki w kolejności odpowiedzi serwera
     * @property readAt chwila odczytu, pokazywana jako „Stan z …”
     * @property isRefreshing czy trwa odświeżanie; lista z poprzedniego odczytu zostaje widoczna
     */
    data class Ready(val buildings: List<BuildingProgress>, val readAt: Instant, val isRefreshing: Boolean = false) :
        ProgressUiState

    /** Bez internetu: postęp liczy serwer, więc bez sieci go nie widać. */
    data object Offline : ProgressUiState

    /** Serwer odpowiedział błędem albo treścią spoza kontraktu. */
    data object ServerError : ProgressUiState

    /** Serwer odmówił, bo osoba nie ma roli Koordynatora. */
    data object Forbidden : ProgressUiState
}

/**
 * K1 „Postęp inwentaryzacji”: odczyt postępu budynków przy otwarciu i na „Odśwież”, bez odświeżania w tle.
 *
 * Wymagania: US11.1, US11.2, NFR-50
 *
 * TODO(#2121): podłączyć do ekranu K1.
 *
 * @param progress źródło postępu inwentaryzacji
 * @param clock zegar chwili odczytu
 */
@HiltViewModel
class ProgressViewModel internal constructor(private val progress: InspectionProgress, private val clock: Clock) :
    ViewModel() {
    private val current = MutableStateFlow<ProgressUiState>(ProgressUiState.Loading)

    /** Stan listy; zmienia się tylko po otwarciu i po [refresh]. */
    val state: StateFlow<ProgressUiState> = current

    init {
        load()
    }

    @Inject
    constructor(progress: InspectionProgress) : this(progress, Clock.systemDefaultZone())

    /** „Odśwież” i „Spróbuj ponownie”: czyta postęp od nowa; w trakcie odczytu nie startuje drugiego. */
    fun refresh() {
        val shown = current.value
        val isBusy = shown == ProgressUiState.Loading || (shown as? ProgressUiState.Ready)?.isRefreshing == true
        if (!isBusy) {
            current.value = (shown as? ProgressUiState.Ready)?.copy(isRefreshing = true) ?: ProgressUiState.Loading
            load()
        }
    }

    private fun load() {
        viewModelScope.launch { current.value = progress.buildings().toState() }
    }

    private fun ServerReply<List<BuildingProgress>>.toState(): ProgressUiState =
        when (this) {
            is ServerReply.Ok -> ProgressUiState.Ready(buildings = value, readAt = clock.instant())
            ServerReply.Offline -> ProgressUiState.Offline
            ServerReply.Missing -> ProgressUiState.ServerError
            is ServerReply.Fault -> faultState(code)
        }

    private fun faultState(code: Int): ProgressUiState =
        if (code == FORBIDDEN) ProgressUiState.Forbidden else ProgressUiState.ServerError

    private companion object {
        const val FORBIDDEN = 403
    }
}
