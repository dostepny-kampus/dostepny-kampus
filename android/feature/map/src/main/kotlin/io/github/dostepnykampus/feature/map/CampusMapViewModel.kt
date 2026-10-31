/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.dostepnykampus.core.data.buildings.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.map.MapListChoice
import io.github.dostepnykampus.core.data.network.NetworkStatus
import io.github.dostepnykampus.core.model.BuildingCatalog
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L

/**
 * Stan ekranu V1 „Mapa kampusu” (US1).
 *
 * @property catalog katalog budynków; `null`, dopóki plik konturów się wczytuje
 * @property isOnline czy jest internet; na start prawda, żeby linia „Bez internetu” nie mignęła, zanim przyjdzie stan
 *   bieżący (US9.1)
 * @property isListSaved ostatni wybór w przełączniku: prawda dla „Lista budynków”, fałsz dla „Mapa”, `null`, gdy osoba
 *   jeszcze nie wybierała (NFR-19)
 */
data class CampusMapUiState(
    val catalog: BuildingCatalog? = null,
    val isOnline: Boolean = true,
    val isListSaved: Boolean? = null,
)

/**
 * Stan V1 z katalogu budynków, stanu internetu i zapamiętanego wyboru „Mapa” albo „Lista budynków” (US1, US9.1,
 * NFR-19). Źródła są obserwowane, gdy ekran jest widoczny, i jeszcze 5 s po zniknięciu, więc obrót ekranu nie
 * uruchamia ich od nowa, a monitor sieci nie działa w tle.
 */
@HiltViewModel
class CampusMapViewModel
    @Inject
    constructor(
        repository: BuildingCatalogRepository,
        networkStatus: NetworkStatus,
        private val mapListChoice: MapListChoice,
    ) : ViewModel() {
        /** Bieżący stan ekranu. */
        val uiState: StateFlow<CampusMapUiState> =
            combine(
                flow = flow<BuildingCatalog?> { emit(repository.catalog()) }.onStart { emit(null) },
                flow2 = networkStatus.isOnline(),
                flow3 = mapListChoice.savedChoice,
                transform = ::CampusMapUiState,
            ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), CampusMapUiState())

        /** Zapamiętuje wybór w przełączniku: „Lista budynków” przy [isList], inaczej „Mapa” (NFR-19). */
        fun chooseList(isList: Boolean) {
            viewModelScope.launch { mapListChoice.choose(isList) }
        }
    }
