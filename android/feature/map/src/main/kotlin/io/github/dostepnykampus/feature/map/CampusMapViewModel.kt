/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.dostepnykampus.core.data.barrier.AcceptedBarrier
import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.di.ComputeContext
import io.github.dostepnykampus.core.data.map.MapListChoice
import io.github.dostepnykampus.core.data.network.NetworkStatus
import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.geo.building.buildingBeforeRoom
import io.github.dostepnykampus.core.geo.building.searchBuildings
import io.github.dostepnykampus.core.model.building.Building
import io.github.dostepnykampus.core.model.building.BuildingCatalog
import io.github.dostepnykampus.core.model.path.AccessiblePath
import io.github.dostepnykampus.feature.map.building.BuildingSearch
import io.github.dostepnykampus.feature.map.building.MissingRoom
import io.github.dostepnykampus.feature.map.building.isRoomNumberOnly
import io.github.dostepnykampus.feature.map.campus.CampusMapData
import io.github.dostepnykampus.feature.map.campus.CampusMapDataCache
import io.github.dostepnykampus.feature.map.campus.MapServer
import io.github.dostepnykampus.feature.map.campus.NO_SERVER
import io.github.dostepnykampus.feature.map.campus.withBarriers
import io.github.dostepnykampus.feature.map.path.withPaths
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

private const val STOP_TIMEOUT_MS = 5_000L

/**
 * Stan ekranu V1 „Mapa kampusu”.
 *
 * Wymagania: US1, NFR-10, NFR-11, US9.1, NFR-19, US1.3, NFR-18
 *
 * @property catalog katalog budynków; `null`, dopóki plik konturów się wczytuje, potem katalog z pliku, a gdy serwer
 *   odpowie niepustą listą, katalog z serwera
 * @property isOnline czy jest internet; na start prawda, żeby linia „Bez internetu” nie mignęła, zanim przyjdzie stan
 *   bieżący
 * @property isListSaved ostatni widok: prawda dla listy, fałsz dla mapy, `null`, gdy osoba
 *   jeszcze nie wybierała
 * @property mapData gotowe dane warstw mapy, policzone poza wątkiem interfejsu raz na katalog; `null`, dopóki pierwsza
 *   geometria nie jest gotowa, a po zmianie katalogu poprzednie dane, aż policzy się nowa. Dane mają
 *   bariery ze zgłoszeń przyjętych i ścieżki dostosowane, gdy serwer albo kopia w telefonie je podały
 * @property barriers bariery pokazane na mapie, do nazw znaków dla TalkBack
 * @property syncedAt chwila pobrania najstarszych danych z serwera pokazanych na mapie: listy budynków albo barier,
 *   do daty w linii „Bez internetu”; `null`, gdy na mapie są tylko dane z pakietu aplikacji
 * @property search wyniki wyszukiwania budynku dla ostatniego zapytania z [CampusMapViewModel.search], policzone
 *   poza wątkiem interfejsu
 */
data class CampusMapUiState(
    val catalog: BuildingCatalog? = null,
    val isOnline: Boolean = true,
    val isListSaved: Boolean? = null,
    val mapData: CampusMapData? = null,
    val barriers: List<AcceptedBarrier> = emptyList(),
    val syncedAt: Instant? = null,
    val search: BuildingSearch = BuildingSearch(),
)

/**
 * Stan V1 z katalogu budynków (najpierw plik, potem serwer), stanu internetu, barier i zapamiętanego wyboru „Mapa” albo
 * „Lista budynków”. Źródła są obserwowane, gdy ekran jest widoczny, i 5 s po zniknięciu. Geometrię liczy [geometry] w
 * [computeContext] raz na katalog; nowszy katalog i zamknięcie ekranu przerywają liczenie przerwaniem wątku, a do czasu
 * nowej mapa zostaje na poprzedniej. O bariery i ścieżki dostosowane ekran pyta na starcie i po każdej zmianie stanu
 * sieci; do pierwszej odpowiedzi mapa nie ma znaków barier ani ścieżek. Dane mapy powstają od nowa tylko po zmianie
 * katalogu, barier albo ścieżek.
 *
 * Wymagania: US1, US9.1, NFR-19, NFR-11, US1.3, US1.4, NFR-10
 *
 * @param repository katalog budynków: najpierw plik, potem serwer
 * @param networkStatus stan internetu
 * @param mapListChoice zapamiętany wybór „Mapa” albo „Lista budynków”
 * @property computeContext kontekst obliczeń geometrii; w aplikacji `Dispatchers.Default` z grafu Hilt
 * @property geometry dane mapy z katalogu, przerywalne przerwaniem wątku; w aplikacji pamięć `CampusMapDataCache`
 * @param server bariery ze zgłoszeń przyjętych, ścieżki dostosowane i wyszukiwanie sal; w testach bez nich
 */
@HiltViewModel
class CampusMapViewModel(
    repository: BuildingCatalogRepository,
    networkStatus: NetworkStatus,
    private val mapListChoice: MapListChoice,
    internal val computeContext: CoroutineContext,
    internal val geometry: (BuildingCatalog) -> CampusMapData,
    private val server: MapServer = NO_SERVER,
) : ViewModel() {
    private val queries = MutableStateFlow("")

    private val shown: StateFlow<CampusMapUiState> =
        combine(
            flow =
                repository
                    .catalogUpdates()
                    .map<BuildingCatalog, BuildingCatalog?> { it }
                    .onStart { emit(null) }
                    .withGeometry()
                    .combine(networkStatus.isOnline().onNetworkChange(server)) { (catalog, data), fetched ->
                        Triple(catalog, data?.run { withBarriers(fetched.barriers).withPaths(fetched.paths) }, fetched)
                    },
            flow2 = networkStatus.isOnline(),
            flow3 = mapListChoice.savedChoice,
        ) { (catalog, data, fetched), isOnline, isListSaved ->
            CampusMapUiState(
                catalog = catalog,
                isOnline = isOnline,
                isListSaved = isListSaved,
                mapData = data,
                barriers = fetched.barriers,
                syncedAt = listOfNotNull(repository.syncedAt(), fetched.syncedAt).minOrNull(),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), CampusMapUiState())

    /**
     * Bieżący stan ekranu. Wyszukiwanie idzie osobno od katalogu i mapy, więc trwające liczenie wyniku nie
     * wstrzymuje nowego katalogu ani geometrii.
     */
    val uiState: StateFlow<CampusMapUiState> =
        combine(flow = shown, flow2 = shown.searches()) { state, search -> state.copy(search = search) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), CampusMapUiState())

    @Inject
    constructor(
        repository: BuildingCatalogRepository,
        networkStatus: NetworkStatus,
        mapListChoice: MapListChoice,
        server: MapServer,
        @ComputeContext computeContext: CoroutineContext,
        cache: CampusMapDataCache,
    ) : this(repository, networkStatus, mapListChoice, computeContext, cache::dataFor, server)

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun Flow<Boolean>.onNetworkChange(server: MapServer): Flow<Fetched> =
        distinctUntilChanged()
            .mapLatest { server.fetched() }
            .onStart { emit(Fetched(barriers = emptyList(), syncedAt = null, paths = emptyList())) }
            .distinctUntilChanged()

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

    /**
     * Szuka budynku po symbolu [query] w [computeContext], a gdy go nie ma i tekst ma cyfry, sali na serwerze; sam
     * numer sali bez budynku daje prośbę o budynek zamiast pytania serwera. Wynik przychodzi w
     * [CampusMapUiState.search].
     *
     * Wymagania: US1.3, US18.1
     */
    fun search(query: String) {
        queries.value = query
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun Flow<CampusMapUiState>.searches(): Flow<BuildingSearch> {
        val catalogs = map { it.catalog }.distinctUntilChanged()
        val pairs = combine(flow = catalogs, flow2 = queries) { catalog, query -> catalog to query }
        val results =
            pairs.mapLatest { (catalog, query) ->
                val found = catalog?.let { withContext(computeContext) { searchBuildings(query, it.buildings) } }
                val isNoBuilding = found?.isEmpty() == true && query.any(Char::isDigit)
                val isBuildingMissing = isNoBuilding && isRoomNumberOnly(query)
                val reply = if (isNoBuilding && !isBuildingMissing) server.rooms.search(query.trim()) else null
                val rooms = (reply as? ServerReply.Ok)?.value.orEmpty()
                val isRoomAbsent = reply is ServerReply.Ok && rooms.isEmpty()
                val missing = catalog?.takeIf { isRoomAbsent }?.let { missingRoomOf(query, it.buildings) }
                BuildingSearch(
                    query = query,
                    buildings = found,
                    rooms = rooms,
                    isBuildingMissing = isBuildingMissing,
                    missingRoom = missing,
                )
            }
        return results.onStart { emit(BuildingSearch()) }
    }

    private fun missingRoomOf(query: String, buildings: List<Building>): MissingRoom? {
        val building = buildingBeforeRoom(query, buildings) ?: return null
        return MissingRoom(building = building, number = query.trim().substringAfterLast(' '))
    }

    private suspend fun MapServer.fetched(): Fetched {
        val shown = barriers.barriers()
        return Fetched(barriers = shown, syncedAt = barriers.syncedAt(), paths = paths.paths())
    }

    /** Bariery i ścieżki dostosowane z jednego pytania źródeł razem z chwilą pobrania barier z serwera. */
    private data class Fetched(
        val barriers: List<AcceptedBarrier>,
        val syncedAt: Instant?,
        val paths: List<AccessiblePath>,
    )

    /** Zapamiętuje widok: listę przy [isList], inaczej mapę. Wymagania: NFR-19 */
    fun chooseList(isList: Boolean) {
        viewModelScope.launch { mapListChoice.choose(isList) }
    }
}
