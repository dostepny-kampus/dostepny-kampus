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
import io.github.dostepnykampus.core.data.submission.own.OwnSubmissions
import io.github.dostepnykampus.core.model.building.BuildingCatalog
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EvacuationExit
import io.github.dostepnykampus.core.model.building.FloorList
import io.github.dostepnykampus.core.model.room.RoomList
import io.github.dostepnykampus.feature.map.camera.MapTrace
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

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
     * @property ownBarrierIds identyfikatory własnych zgłoszeń zalogowanego konta
     */
    data class Ready(
        val symbol: String,
        val name: String?,
        val entrances: List<OrderedEntrance>,
        val barriers: List<AcceptedBarrier>? = null,
        val floors: FloorFacts? = null,
        val isBasic: Boolean = false,
        val evacuationExits: List<EvacuationExit> = emptyList(),
        val ownBarrierIds: Set<String> = emptySet(),
    ) : BuildingCardUiState
}

/** Źródła danych serwera i własnych zgłoszeń dla karty budynku V2. Wymagania: US2, US17.3, US16.3, BR-3, BR-18 */
data class BuildingCardSources
    @Inject
    constructor(
        internal val serverEntrances: ServerEntrances,
        internal val acceptedBarriers: AcceptedBarriers,
        internal val plans: FloorPlans,
        internal val ownSubmissions: OwnSubmissions,
    )

/**
 * Karta budynku [symbol] z katalogu w telefonie, czytanego, gdy ekran jest widoczny, i jeszcze 5 s po zniknięciu.
 * [retry] pyta serwer i czyta plik od nowa. Pierwszy stan ma to, co telefon już zna: wejścia z pliku, wejścia
 * i wyjścia z karty serwera pobranej wcześniej i zgłoszenia przyjęte z ostatniego pobrania. Równolegle karta pyta raz
 * `serverEntrances` i zgłoszenia przyjęte; niepusta lista wejść z serwera zastępuje wejścia z pliku, pusta je zostawia.
 * Fakty o piętrach bierze najpierw z kopii planu w telefonie, a po pobraniu planu z nowej kopii. Zamknięcie karty
 * przerywa pobieranie.
 *
 * Wymagania: US2, BR-25, NFR-12, US17.3, NFR-10, US2.2, US2.4, US2.7, NFR-56, US16.3, US2.6, BR-3, BR-18
 */
@HiltViewModel(assistedFactory = BuildingCardViewModel.Factory::class)
class BuildingCardViewModel
    @AssistedInject
    constructor(
        @Assisted val symbol: String,
        repository: BuildingCatalogRepository,
        networkStatus: NetworkStatus,
        sources: BuildingCardSources,
    ) : ViewModel() {
        private val attempts = MutableStateFlow(0)

        private val known = sources.serverEntrances.knownCard(symbol)

        private val fromServer = MutableStateFlow(known?.entrances.orEmpty())

        private val barriers = MutableStateFlow(sources.acceptedBarriers.known()?.let(::ofBuilding))

        private val floors = MutableStateFlow<FloorFacts?>(null)

        private val exits = MutableStateFlow(known?.evacuationExits.orEmpty())

        private val ownIds = sources.ownSubmissions.observe().map { rows -> rows.mapTo(mutableSetOf()) { it.id } }

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
                .map { attempt -> catalogFor(repository, attempt) }
                .map(::cardOf)
                .combine(fromServer, ::withServerEntrances)
                .combine(barriers, ::withBarriers)
                .combine(floors, ::withFloors)
                .combine(exits, ::withExits)
                .combine(ownIds, ::withOwnBarriers)
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
                floors.value = storedFloorFacts(sources.plans, symbol)
                if (sources.plans.download(symbol)) floors.value = storedFloorFacts(sources.plans, symbol)
            }
            viewModelScope.launch {
                val card = sources.serverEntrances.cardEntrances(symbol)
                fromServer.value = card.entrances
                exits.value = card.evacuationExits
            }
            viewModelScope.launch {
                val accepted = sources.acceptedBarriers.barriers()
                barriers.value = sources.acceptedBarriers.syncedAt()?.let { ofBuilding(accepted) }
            }
        }

        /** Wariant dla testów z pojedynczymi źródłami bez [BuildingCardSources]. */
        constructor(
            symbol: String,
            repository: BuildingCatalogRepository,
            networkStatus: NetworkStatus,
            serverEntrances: ServerEntrances = ServerEntrances { emptyList() },
            acceptedBarriers: AcceptedBarriers = AcceptedBarriers { emptyList() },
            plans: FloorPlans = NoFloorPlans,
        ) : this(
            symbol = symbol,
            repository = repository,
            networkStatus = networkStatus,
            sources =
                BuildingCardSources(
                    serverEntrances = serverEntrances,
                    acceptedBarriers = acceptedBarriers,
                    plans = plans,
                    ownSubmissions = OwnSubmissions { flowOf(emptyList()) },
                ),
        )

        private fun ofBuilding(accepted: List<AcceptedBarrier>): List<AcceptedBarrier> =
            accepted.filter { barrier -> barrier.buildingSymbol == symbol }

        /** „Spróbuj ponownie”, gdy budynku nie ma w telefonie. Wymagania: NFR-10 */
        fun retry() {
            attempts.value++
        }

        private fun cardOf(catalog: BuildingCatalog): BuildingCardUiState =
            MapTrace.section(name = MapTrace.CARD_STATE_SECTION) { buildingCard(catalog, symbol) }

        private fun withServerEntrances(card: BuildingCardUiState, entrances: List<Entrance>): BuildingCardUiState =
            MapTrace.section(name = MapTrace.CARD_STATE_SECTION) {
                if (card is BuildingCardUiState.Ready && entrances.isNotEmpty()) {
                    card.copy(entrances = orderedEntrances(entrances))
                } else {
                    card
                }
            }

        private fun withBarriers(card: BuildingCardUiState, accepted: List<AcceptedBarrier>?): BuildingCardUiState =
            MapTrace.section(name = MapTrace.CARD_STATE_SECTION) {
                if (card is BuildingCardUiState.Ready) {
                    card.copy(
                        barriers = accepted,
                        entrances = orderedEntrances(card.entrances.map { it.entrance }, accepted.orEmpty()),
                    )
                } else {
                    card
                }
            }

        private fun withFloors(card: BuildingCardUiState, facts: FloorFacts?): BuildingCardUiState =
            if (card is BuildingCardUiState.Ready) card.copy(floors = facts) else card

        private fun withExits(card: BuildingCardUiState, found: List<EvacuationExit>): BuildingCardUiState =
            if (card is BuildingCardUiState.Ready) card.copy(evacuationExits = found) else card

        private fun withOwnBarriers(card: BuildingCardUiState, ids: Set<String>): BuildingCardUiState =
            if (card is BuildingCardUiState.Ready) card.copy(ownBarrierIds = ids) else card

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
    val facts = MapTrace.section(name = MapTrace.CARD_FLOORS_SECTION) { floorFacts(floors, rooms) }
    return facts.takeIf { floors.isNotEmpty() }
}

/** Plany pięter budynku bez planu: nic nie pobiera i nic nie ma. */
private object NoFloorPlans : FloorPlans {
    override suspend fun download(symbol: String): Boolean = false

    override suspend fun floors(symbol: String): FloorList? = null

    override suspend fun rooms(symbol: String, floor: String): RoomList? = null
}
