/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.dostepnykampus.core.data.buildings.BuildingCatalogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private const val STOP_TIMEOUT_MS = 5_000L

/** Stan karty budynku V2 (US2). */
sealed interface BuildingCardUiState {
    /** Katalog jeszcze się wczytuje. */
    data object Loading : BuildingCardUiState

    /** Symbolu nie ma w katalogu zapisanym w telefonie. */
    data object Missing : BuildingCardUiState

    /**
     * Karta budynku z katalogu.
     *
     * @property symbol symbol budynku, np. `A-1`
     * @property name nazwa z danych; `null`, gdy danych nazwy nie ma
     * @property entrances wejścia odwiedzających, zalecane pierwsze (BR-25)
     */
    data class Ready(val symbol: String, val name: String?, val entrances: List<OrderedEntrance>) : BuildingCardUiState
}

/**
 * Karta budynku [symbol] z katalogu w telefonie (US2, BR-25). Katalog jest czytany, gdy ekran jest widoczny,
 * i jeszcze 5 s po zniknięciu, więc obrót ekranu nie czyta go od nowa. [retry] czyta katalog ponownie.
 */
@HiltViewModel(assistedFactory = BuildingCardViewModel.Factory::class)
class BuildingCardViewModel
    @AssistedInject
    constructor(
        @Assisted val symbol: String,
        private val repository: BuildingCatalogRepository,
    ) : ViewModel() {
        private val attempts = MutableStateFlow(0)

        /** Bieżący stan karty. */
        val uiState: StateFlow<BuildingCardUiState> =
            attempts
                .map { buildingCard(repository.catalog(), symbol) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), BuildingCardUiState.Loading)

        /** „Spróbuj ponownie”, gdy budynku nie ma w telefonie (NFR-10). */
        fun retry() {
            attempts.value++
        }

        /** Tworzy [BuildingCardViewModel] dla symbolu z trasy V2. */
        @AssistedFactory
        interface Factory {
            /** Karta budynku [symbol]. */
            fun create(symbol: String): BuildingCardViewModel
        }
    }
