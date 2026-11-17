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
import io.github.dostepnykampus.core.data.barrier.AcceptedBarrier
import io.github.dostepnykampus.core.data.barrier.AcceptedBarriers
import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.building.ServerEntrances
import io.github.dostepnykampus.core.data.network.NetworkStatus
import io.github.dostepnykampus.core.data.room.FloorPlans
import io.github.dostepnykampus.core.model.building.BuildingCatalog
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.FloorList
import io.github.dostepnykampus.core.model.room.RoomList
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
     * @property barriers zgłoszenia przyjęte tego budynku; pusta lista, gdy dane o barierach są, a budynek nie ma
     *   zgłoszeń; `null`, gdy telefon nie ma danych o barierach
     * @property floors fakty o piętrach z planów PWr; `null`, gdy budynek nie ma planu w telefonie
     * @property isBasic czy serwer podał budynek jako podstawowy, bez planu pięter; fałsz także wtedy, gdy telefon
     *   nie ma jeszcze listy budynków z serwera
     */
    data class Ready(
        val symbol: String,
        val name: String?,
        val entrances: List<OrderedEntrance>,
        val barriers: List<AcceptedBarrier>? = null,
        val floors: FloorFacts? = null,
        val isBasic: Boolean = false,
    ) : BuildingCardUiState
}

/**
 * Karta budynku [symbol] z katalogu w telefonie. Katalog jest czytany, gdy ekran jest widoczny,
 * i jeszcze 5 s po zniknięciu, więc obrót ekranu nie czyta go od nowa. [retry] pyta serwer i czyta plik od nowa.
 *
 * Tekst karty i wejścia z pliku pokazują się bez czekania na sieć, a z katalogu, który telefon już ma, od pierwszej
 * klatki. Równolegle karta pyta `serverEntrances` o wejścia z dniem i źródłem oznaczenia; niepusta lista zastępuje
 * wejścia z pliku, a pusta (brak sieci, błąd serwera) zostawia je bez komunikatu błędu. Zgłoszenia przyjęte budynku
 * pobiera tak samo, a plan pięter pobiera raz i czyta z kopii w telefonie. Zamknięcie karty przerywa pobieranie.
 *
 * Wymagania: US2, BR-25, NFR-12, US17.3, NFR-10, US2.2, US2.4, US2.7, NFR-56, US16.3
 */
@HiltViewModel(assistedFactory = BuildingCardViewModel.Factory::class)
class BuildingCardViewModel
    @AssistedInject
    constructor(
        @Assisted val symbol: String,
        repository: BuildingCatalogRepository,
        networkStatus: NetworkStatus,
        serverEntrances: ServerEntrances = ServerEntrances { emptyList() },
        acceptedBarriers: AcceptedBarriers = AcceptedBarriers { emptyList() },
        plans: FloorPlans = NoFloorPlans,
    ) : ViewModel() {
        private val attempts = MutableStateFlow(0)

        private val fromServer = MutableStateFlow(emptyList<Entrance>())

        private val barriers = MutableStateFlow<List<AcceptedBarrier>?>(null)

        private val floors = MutableStateFlow<FloorFacts?>(null)

        private val first = repository.current()?.let { buildingCard(it, symbol) } ?: BuildingCardUiState.Loading

        /** Bieżący stan karty. */
        val uiState: StateFlow<BuildingCardUiState> =
            attempts
                .map { attempt -> buildingCard(catalogFor(repository, attempt), symbol) }
                .combine(fromServer, ::withServerEntrances)
                .combine(barriers, ::withBarriers)
                .combine(floors, ::withFloors)
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                    initialValue = first,
                )

        /** Czy jest internet; na start prawda, żeby linia „Bez internetu” nie mignęła. Wymagania: NFR-10, US2.8 */
        val online: StateFlow<Boolean> =
            networkStatus
                .isOnline()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), true)

        init {
            viewModelScope.launch { floors.value = floorFactsOrNull(plans, symbol) }
            viewModelScope.launch { fromServer.value = serverEntrances.entrances(symbol) }
            viewModelScope.launch {
                val accepted = acceptedBarriers.barriers()
                barriers.value =
                    acceptedBarriers.syncedAt()?.let {
                        accepted.filter { barrier -> barrier.buildingSymbol == symbol }
                    }
            }
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

        private fun withBarriers(card: BuildingCardUiState, accepted: List<AcceptedBarrier>?): BuildingCardUiState =
            if (card is BuildingCardUiState.Ready) card.copy(barriers = accepted) else card

        private fun withFloors(card: BuildingCardUiState, facts: FloorFacts?): BuildingCardUiState =
            if (card is BuildingCardUiState.Ready) card.copy(floors = facts) else card

        private suspend fun catalogFor(repository: BuildingCatalogRepository, attempt: Int): BuildingCatalog =
            if (attempt == 0) repository.catalog() else repository.refresh()

        /** Tworzy [BuildingCardViewModel] dla symbolu z trasy V2. */
        @AssistedFactory
        interface Factory {
            /** Karta budynku [symbol]. */
            fun create(symbol: String): BuildingCardViewModel
        }
    }

/** Fakty o piętrach budynku [symbol] z planu w [plans]; `null`, gdy budynek nie ma planu ani jego kopii. */
internal suspend fun floorFactsOrNull(plans: FloorPlans, symbol: String): FloorFacts? {
    plans.download(symbol)
    val floors = plans.floors(symbol)?.floors.orEmpty()
    val rooms = floors.mapNotNull { floor -> plans.rooms(symbol, floor.id)?.let { floor.id to it } }.toMap()
    return floorFacts(floors, rooms).takeIf { floors.isNotEmpty() }
}

/** Plany pięter budynku bez planu: nic nie pobiera i nic nie ma. */
private object NoFloorPlans : FloorPlans {
    override suspend fun download(symbol: String): Boolean = false

    override suspend fun floors(symbol: String): FloorList? = null

    override suspend fun rooms(symbol: String, floor: String): RoomList? = null
}
