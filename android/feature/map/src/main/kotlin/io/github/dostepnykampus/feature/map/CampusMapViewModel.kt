/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.dostepnykampus.core.data.barriers.AcceptedBarrier
import io.github.dostepnykampus.core.data.barriers.AcceptedBarriers
import io.github.dostepnykampus.core.data.buildings.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.map.MapListChoice
import io.github.dostepnykampus.core.data.network.NetworkStatus
import io.github.dostepnykampus.core.geo.searchBuildings
import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.core.model.BuildingCatalog
import io.github.dostepnykampus.feature.map.data.CampusMapData
import io.github.dostepnykampus.feature.map.data.campusMapData
import io.github.dostepnykampus.feature.map.data.withBarriers
import kotlinx.coroutines.Dispatchers
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
 *   geometria nie jest gotowa, a po zmianie katalogu poprzednie dane, aż policzy się nowa (NFR-11). Dane mają
 *   bariery ze zgłoszeń przyjętych, gdy serwer je podał (US1.3)
 * @property barriers bariery pokazane na mapie, do nazw znaków dla TalkBack (NFR-18)
 * @property search wyniki wyszukiwania budynku dla ostatniego zapytania z [CampusMapViewModel.search], policzone
 *   poza wątkiem interfejsu (NFR-11, #1053)
 */
data class CampusMapUiState(
    val catalog: BuildingCatalog? = null,
    val isOnline: Boolean = true,
    val isListSaved: Boolean? = null,
    val mapData: CampusMapData? = null,
    val barriers: List<AcceptedBarrier> = emptyList(),
    val search: BuildingSearch = BuildingSearch(),
)

/**
 * Wynik wyszukiwania budynku po symbolu (US1.3).
 *
 * @property query zapytanie, dla którego policzono wynik; ekran pokazuje wynik tylko dla zapytania z pola
 * @property buildings budynki o tym symbolu w kolejności katalogu; `null`, dopóki katalog się wczytuje
 */
@Immutable
data class BuildingSearch(val query: String = "", val buildings: List<Building>? = null)

/** Wynik dla zapytania [shown] z pola; `null`, gdy wynik jest dla innego zapytania albo katalog się wczytuje. */
fun BuildingSearch.foundFor(shown: String): List<Building>? = buildings.takeIf { query == shown }

/**
 * Stan V1 z katalogu budynków (najpierw z pliku, potem z serwera), stanu internetu i zapamiętanego wyboru „Mapa” albo
 * „Lista budynków” (US1, US9.1, NFR-19). Źródła są obserwowane, gdy ekran jest widoczny, i jeszcze 5 s po zniknięciu,
 * więc obrót ekranu nie uruchamia ich od nowa, a monitor sieci nie działa w tle. Geometrię mapy liczy [geometry]
 * w [computeContext], raz na katalog. Zamknięcie ekranu i nowszy katalog przerywają trwające liczenie przerwaniem
 * wątku, więc [geometry] musi je respektować (NFR-11).
 * Katalog trafia do stanu od razu, a jego geometria z kontekstu obliczeń: raz na katalog. Nowszy katalog i
 * zamknięcie ekranu przerywają liczenie przerwaniem wątku (`runInterruptible`), więc nowe nie czeka na koniec
 * starego. Do czasu policzenia nowej mapa zostaje na poprzedniej, więc kontury nie znikają przy zmianie katalogu
 * (NFR-11).
 * O bariery ze zgłoszeń przyjętych ekran pyta źródło na starcie i od nowa po każdej zmianie stanu sieci: z internetem
 * źródło daje odpowiedź serwera, a bez niego ostatnią kopię z telefonu (US1.3, NFR-10). Do pierwszej odpowiedzi mapa
 * działa bez znaków barier (NFR-11). Dane mapy powstają od nowa tylko po zmianie katalogu albo barier, nie przy
 * każdej zmianie stanu ekranu ani wtedy, gdy źródło dało te same bariery.
 *
 * @param repository katalog budynków: najpierw plik, potem serwer
 * @param networkStatus stan internetu
 * @param mapListChoice zapamiętany wybór „Mapa” albo „Lista budynków”
 * @property computeContext kontekst obliczeń geometrii; w aplikacji `Dispatchers.Default`
 * @property geometry funkcja licząca dane mapy z katalogu, przerywalna przerwaniem wątku; w aplikacji `campusMapData`
 * @param barriers bariery ze zgłoszeń przyjętych; w testach bez barier
 */
@HiltViewModel
class CampusMapViewModel(
    repository: BuildingCatalogRepository,
    networkStatus: NetworkStatus,
    private val mapListChoice: MapListChoice,
    internal val computeContext: CoroutineContext,
    internal val geometry: (BuildingCatalog) -> CampusMapData,
    barriers: AcceptedBarriers = AcceptedBarriers { emptyList() },
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
                    .combine(networkStatus.isOnline().onNetworkChange(barriers)) { (catalog, data), shown ->
                        Triple(catalog, data?.withBarriers(shown), shown)
                    },
            flow2 = networkStatus.isOnline(),
            flow3 = mapListChoice.savedChoice,
        ) { (catalog, data, shown), isOnline, isListSaved ->
            CampusMapUiState(
                catalog = catalog,
                isOnline = isOnline,
                isListSaved = isListSaved,
                mapData = data,
                barriers = shown,
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
        barriers: AcceptedBarriers,
    ) : this(repository, networkStatus, mapListChoice, Dispatchers.Default, ::campusMapData, barriers)

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun Flow<Boolean>.onNetworkChange(barriers: AcceptedBarriers): Flow<List<AcceptedBarrier>> =
        distinctUntilChanged()
            .mapLatest { barriers.barriers() }
            .onStart { emit(emptyList()) }
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

    /** Szuka budynku po symbolu [query] w [computeContext]; wynik przychodzi w [CampusMapUiState.search] (#1053). */
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
                BuildingSearch(query = query, buildings = found)
            }
        return results.onStart { emit(BuildingSearch()) }
    }

    /** Zapamiętuje wybór w przełączniku: „Lista budynków” przy [isList], inaczej „Mapa” (NFR-19). */
    fun chooseList(isList: Boolean) {
        viewModelScope.launch { mapListChoice.choose(isList) }
    }
}
