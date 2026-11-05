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
import io.github.dostepnykampus.feature.map.data.CampusMapData
import io.github.dostepnykampus.feature.map.data.campusMapData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

private const val STOP_TIMEOUT_MS = 5_000L

/**
 * Stan ekranu V1 „Mapa kampusu” (US1).
 *
 * @property catalog katalog budynków; `null`, dopóki plik konturów się wczytuje, potem katalog z pliku, a gdy serwer
 *   odpowie niepustą listą, katalog z serwera (NFR-10, NFR-11)
 * @property isOnline czy jest internet; na start prawda, żeby linia „Bez internetu” nie mignęła, zanim przyjdzie stan
 *   bieżący (US9.1)
 * @property isListSaved ostatni wybór w przełączniku: prawda dla „Lista budynków”, fałsz dla „Mapa”, `null`, gdy osoba
 *   jeszcze nie wybierała (NFR-19)
 * @property mapData gotowe dane warstw mapy, policzone poza wątkiem interfejsu raz na katalog; `null`, dopóki pierwsza
 *   geometria nie jest gotowa, a po zmianie katalogu poprzednie dane, aż policzy się nowa (NFR-11)
 */
data class CampusMapUiState(
    val catalog: BuildingCatalog? = null,
    val isOnline: Boolean = true,
    val isListSaved: Boolean? = null,
    val mapData: CampusMapData? = null,
)

/**
 * Stan V1 z katalogu budynków (najpierw z pliku, potem z serwera), stanu internetu i zapamiętanego wyboru „Mapa” albo
 * „Lista budynków” (US1, US9.1, NFR-19). Źródła są obserwowane, gdy ekran jest widoczny, i jeszcze 5 s po zniknięciu,
 * więc obrót ekranu nie uruchamia ich od nowa, a monitor sieci nie działa w tle. Geometrię mapy liczy [geometry]
 * w [computeContext], raz na katalog. Zamknięcie ekranu i nowszy katalog przerywają trwające liczenie przerwaniem
 * wątku, więc [geometry] musi je respektować (NFR-11).
 *
 * @param repository katalog budynków: najpierw plik, potem serwer
 * @param networkStatus stan internetu
 * @param mapListChoice zapamiętany wybór „Mapa” albo „Lista budynków”
 * @property computeContext kontekst obliczeń geometrii; w aplikacji `Dispatchers.Default`
 * @property geometry funkcja licząca dane mapy z katalogu, przerywalna przerwaniem wątku; w aplikacji `campusMapData`
 */
@HiltViewModel
class CampusMapViewModel(
    repository: BuildingCatalogRepository,
    networkStatus: NetworkStatus,
    private val mapListChoice: MapListChoice,
    internal val computeContext: CoroutineContext,
    internal val geometry: (BuildingCatalog) -> CampusMapData,
) : ViewModel() {
    /** Bieżący stan ekranu. */
    val uiState: StateFlow<CampusMapUiState> =
        combine(
            flow =
                repository
                    .catalogUpdates()
                    .map<BuildingCatalog, BuildingCatalog?> { it }
                    .onStart { emit(null) }
                    .withGeometry(),
            flow2 = networkStatus.isOnline(),
            flow3 = mapListChoice.savedChoice,
        ) { (catalog, data), isOnline, isListSaved ->
            CampusMapUiState(catalog = catalog, isOnline = isOnline, isListSaved = isListSaved, mapData = data)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), CampusMapUiState())

    @Inject
    constructor(
        repository: BuildingCatalogRepository,
        networkStatus: NetworkStatus,
        mapListChoice: MapListChoice,
    ) : this(repository, networkStatus, mapListChoice, Dispatchers.Default, ::campusMapData)

    // Katalog od razu, jego geometria z kontekstu obliczeń: raz na katalog. Nowszy katalog i zamknięcie ekranu
    // przerywają liczenie przerwaniem wątku (runInterruptible), więc nowe nie czeka na koniec starego.
    // Do czasu policzenia nowej mapa zostaje na poprzedniej, więc kontury nie znikają przy zmianie katalogu (NFR-11).
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun Flow<BuildingCatalog?>.withGeometry(): Flow<Pair<BuildingCatalog?, CampusMapData?>> =
        flow {
            var shown: CampusMapData? = null
            emitAll(
                transformLatest { catalog ->
                    emit(catalog to shown)
                    if (catalog != null) {
                        shown = runInterruptible(computeContext) { geometry(catalog) }
                        emit(catalog to shown)
                    }
                },
            )
        }

    /** Zapamiętuje wybór w przełączniku: „Lista budynków” przy [isList], inaczej „Mapa” (NFR-19). */
    fun chooseList(isList: Boolean) {
        viewModelScope.launch { mapListChoice.choose(isList) }
    }
}
