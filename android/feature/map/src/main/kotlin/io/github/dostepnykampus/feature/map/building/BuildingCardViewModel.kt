/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.dostepnykampus.core.data.buildings.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.buildings.ServerEntrances
import io.github.dostepnykampus.core.data.network.NetworkStatus
import io.github.dostepnykampus.core.model.BuildingCatalog
import io.github.dostepnykampus.core.model.Entrance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val STOP_TIMEOUT_MS = 5_000L

/** Stan karty budynku V2. Wymagania: US2 */
sealed interface BuildingCardUiState {
    /** Katalog jeszcze się wczytuje. */
    data object Loading : BuildingCardUiState

    /** Symbolu nie ma w katalogu zapisanym w telefonie. */
    data object Missing : BuildingCardUiState

    /**
     * Karta budynku z katalogu.
     *
     * Wymagania: BR-25
     *
     * @property symbol symbol budynku, np. `A-1`
     * @property name nazwa z danych; `null`, gdy danych nazwy nie ma
     * @property entrances wejścia odwiedzających, zalecane pierwsze
     */
    data class Ready(val symbol: String, val name: String?, val entrances: List<OrderedEntrance>) : BuildingCardUiState
}

/**
 * Karta budynku [symbol] z katalogu w telefonie. Katalog jest czytany, gdy ekran jest widoczny,
 * i jeszcze 5 s po zniknięciu, więc obrót ekranu nie czyta go od nowa. [retry] pyta serwer i czyta plik od nowa.
 *
 * Tekst karty i wejścia z pliku pokazują się od razu, bez czekania na sieć. Równolegle, w zasięgu
 * ViewModelu, karta pyta `serverEntrances` o wejścia z dniem i źródłem oznaczenia; niepusta lista
 * zastępuje wejścia z pliku. Pusta lista (brak sieci, błąd serwera, nic pobranego wcześniej) zostawia wejścia
 * z pliku bez komunikatu błędu. Zamknięcie karty przerywa pobieranie.
 *
 * Wymagania: US2, BR-25, NFR-12, US17.3, NFR-10
 */
@HiltViewModel(assistedFactory = BuildingCardViewModel.Factory::class)
class BuildingCardViewModel
    @AssistedInject
    constructor(
        @Assisted val symbol: String,
        repository: BuildingCatalogRepository,
        networkStatus: NetworkStatus,
        serverEntrances: ServerEntrances = ServerEntrances { emptyList() },
    ) : ViewModel() {
        private val attempts = MutableStateFlow(0)

        private val fromServer = MutableStateFlow(emptyList<Entrance>())

        /** Bieżący stan karty. */
        val uiState: StateFlow<BuildingCardUiState> =
            attempts
                .map { attempt -> buildingCard(catalogFor(repository, attempt), symbol) }
                .combine(fromServer, ::withServerEntrances)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), BuildingCardUiState.Loading)

        /** Czy jest internet; na start prawda, żeby linia „Bez internetu” nie mignęła. Wymagania: NFR-10, US2.8 */
        val online: StateFlow<Boolean> =
            networkStatus
                .isOnline()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), true)

        init {
            viewModelScope.launch { fromServer.value = serverEntrances.entrances(symbol) }
        }

        /** „Spróbuj ponownie”, gdy budynku nie ma w telefonie. Wymagania: NFR-10 */
        fun retry() {
            attempts.value++
        }

        private fun withServerEntrances(card: BuildingCardUiState, entrances: List<Entrance>): BuildingCardUiState =
            if (card is BuildingCardUiState.Ready && entrances.isNotEmpty()) {
                card.copy(entrances = orderedEntrances(entrances))
            } else {
                card
            }

        private suspend fun catalogFor(repository: BuildingCatalogRepository, attempt: Int): BuildingCatalog =
            if (attempt == 0) repository.catalog() else repository.refresh()

        /** Tworzy [BuildingCardViewModel] dla symbolu z trasy V2. */
        @AssistedFactory
        interface Factory {
            /** Karta budynku [symbol]. */
            fun create(symbol: String): BuildingCardViewModel
        }
    }
