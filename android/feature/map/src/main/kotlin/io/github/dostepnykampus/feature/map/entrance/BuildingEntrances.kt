/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.entrance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.dostepnykampus.core.data.building.ServerEntrances
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.feature.map.CampusMapUiState
import io.github.dostepnykampus.feature.map.navigation.MapFocus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L

/**
 * Wejścia jednego budynku z serwera.
 *
 * Wymagania: US2.1, US3.3
 *
 * @property symbol symbol budynku
 * @property entrances wejścia budynku z serwera; pusta lista, gdy serwer ich nie podał
 */
data class BuildingEntrances(val symbol: String, val entrances: List<Entrance>)

/**
 * Stan V1, w którym wejścia budynku [fetched] zastępują w katalogu wejścia tego budynku z pliku konturów, tak jak na
 * karcie V2; arkusz wejścia i pinezka biorą je z katalogu. Bez katalogu, bez [fetched] albo przy pustej liście stan
 * zostaje bez zmian. Dane warstw mapy się nie zmieniają.
 *
 * Wymagania: US2.1, US3.3, BR-25, NFR-10
 */
fun CampusMapUiState.withBuildingEntrances(fetched: BuildingEntrances?): CampusMapUiState {
    val shown = catalog
    val server = fetched?.run { entrances.filter(Entrance::isForVisitors) }.orEmpty()
    if (fetched == null || shown == null || server.isEmpty()) return this
    val others = shown.entrances.filter { it.buildingSymbol != fetched.symbol }
    return copy(catalog = shown.copy(entrances = others + server))
}

/** Budynek zaznaczenia [focus]: budynek albo budynek wejścia; `null` bez zaznaczenia. Wymagania: US2.1, US3.3 */
fun focusedBuilding(focus: MapFocus?): String? =
    when (focus) {
        is MapFocus.Building -> focus.symbol
        is MapFocus.Entrance -> focus.buildingSymbol
        null -> null
    }

/**
 * Wejścia z serwera budynku wybranego na V1 przez [show]. Pytanie idzie, gdy ktoś obserwuje [shown], i 5 s po
 * zniknięciu ekranu; nowy budynek przerywa pytanie o poprzedni.
 *
 * Wymagania: US2.1, US3.3, NFR-10
 *
 * @param entrances wejścia budynku z serwera z pamięcią ostatniej odpowiedzi
 */
@HiltViewModel
class BuildingEntrancesViewModel
    @Inject
    constructor(
        private val entrances: ServerEntrances,
    ) : ViewModel() {
        private val symbols = MutableStateFlow<String?>(null)

        /** Wejścia wybranego budynku; `null` bez budynku i do pierwszej odpowiedzi. */
        @OptIn(ExperimentalCoroutinesApi::class)
        val shown: StateFlow<BuildingEntrances?> =
            symbols
                .mapLatest { symbol -> symbol?.let { BuildingEntrances(it, entrances.entrances(it)) } }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

        /** Wybiera budynek [symbol], którego wejścia podaje [shown]; `null` bez budynku. */
        fun show(symbol: String?) {
            symbols.value = symbol
        }
    }
