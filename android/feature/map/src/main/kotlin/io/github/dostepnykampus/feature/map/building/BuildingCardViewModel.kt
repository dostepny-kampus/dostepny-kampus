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
import io.github.dostepnykampus.core.model.building.EvacuationExit
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
     * @property evacuationExits wyjścia ewakuacyjne z planów PWr; pusta lista, gdy dane ich nie podają
     */
    data class Ready(
        val symbol: String,
        val name: String?,
        val entrances: List<OrderedEntrance>,
        val barriers: List<AcceptedBarrier>? = null,
        val floors: FloorFacts? = null,
        val isBasic: Boolean = false,
        val evacuationExits: List<EvacuationExit> = emptyList(),
    ) : BuildingCardUiState
}

/**
 * Karta budynku [symbol] z katalogu w telefonie. Katalog jest czytany, gdy ekran jest widoczny, i jeszcze 5 s po
 * zniknięciu, więc obrót ekranu nie czyta go od nowa. [retry] pyta serwer i czyta plik od nowa. Pierwszy stan ma
 * już to, co telefon zna bez czekania: tekst i wejścia z pliku, wejścia i wyjścia z karty serwera pobranej wcześniej
 * i zgłoszenia przyjęte z ostatniego pobrania. Równolegle karta pyta raz `serverEntrances` o wejścia z dniem
 * i źródłem oznaczenia i o wyjścia ewakuacyjne; niepusta lista wejść zastępuje wejścia z pliku, pusta zostawia je bez
 * błędu. Zgłoszenia przyjęte pobiera tak samo. Fakty o piętrach bierze najpierw z kopii planu w telefonie, a po
 * pobraniu planu z serwera z nowej kopii. Zamknięcie karty przerywa pobieranie.
 *
 * Wymagania: US2, BR-25, NFR-12, US17.3, NFR-10, US2.2, US2.4, US2.7, NFR-56, US16.3, US2.6
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

        private val known = serverEntrances.knownCard(symbol)

        private val fromServer = MutableStateFlow(known?.entrances.orEmpty())

        private val barriers = MutableStateFlow(acceptedBarriers.known()?.let(::ofBuilding))

        private val floors = MutableStateFlow<FloorFacts?>(null)

        private val exits = MutableStateFlow(known?.evacuationExits.orEmpty())

        private val first =
            repository.current()?.let { catalog ->
                withExits(
                    withBarriers(withServerEntrances(buildingCard(catalog, symbol), fromServer.value), barriers.value),
                    exits.value,
                )
            } ?: BuildingCardUiState.Loading

        /** Bieżący stan karty. */
        val uiState: StateFlow<BuildingCardUiState> =
            attempts
                .map { attempt -> buildingCard(catalogFor(repository, attempt), symbol) }
                .combine(fromServer, ::withServerEntrances)
                .combine(barriers, ::withBarriers)
                .combine(floors, ::withFloors)
                .combine(exits, ::withExits)
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
            viewModelScope.launch {
                floors.value = storedFloorFacts(plans, symbol)
                if (plans.download(symbol)) floors.value = storedFloorFacts(plans, symbol)
            }
            viewModelScope.launch {
                val card = serverEntrances.cardEntrances(symbol)
                fromServer.value = card.entrances
                exits.value = card.evacuationExits
            }
            viewModelScope.launch {
                val accepted = acceptedBarriers.barriers()
                barriers.value = acceptedBarriers.syncedAt()?.let { ofBuilding(accepted) }
            }
        }

        private fun ofBuilding(accepted: List<AcceptedBarrier>): List<AcceptedBarrier> =
            accepted.filter { barrier -> barrier.buildingSymbol == symbol }

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
            if (card is BuildingCardUiState.Ready) {
                card.copy(
                    barriers = accepted,
                    entrances = orderedEntrances(card.entrances.map { it.entrance }, accepted.orEmpty()),
                )
            } else {
                card
            }

        private fun withFloors(card: BuildingCardUiState, facts: FloorFacts?): BuildingCardUiState =
            if (card is BuildingCardUiState.Ready) card.copy(floors = facts) else card

        private fun withExits(card: BuildingCardUiState, found: List<EvacuationExit>): BuildingCardUiState =
            if (card is BuildingCardUiState.Ready) card.copy(evacuationExits = found) else card

        private suspend fun catalogFor(repository: BuildingCatalogRepository, attempt: Int): BuildingCatalog =
            if (attempt == 0) repository.catalog() else repository.refresh()

        /** Tworzy [BuildingCardViewModel] dla symbolu z trasy V2. */
        @AssistedFactory
        interface Factory {
            /** Karta budynku [symbol]. */
            fun create(symbol: String): BuildingCardViewModel
        }
    }

/** Fakty o piętrach budynku [symbol] z kopii planu w [plans], bez pobierania; `null`, gdy kopii nie ma. */
internal suspend fun storedFloorFacts(plans: FloorPlans, symbol: String): FloorFacts? {
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
