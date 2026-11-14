/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dostepnykampus.core.designsystem.LARGE_FONT_SCALE
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.core.model.BuildingCatalog
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.feature.map.area.MapAreas
import io.github.dostepnykampus.feature.map.area.rememberMapAreas
import io.github.dostepnykampus.feature.map.barrier.BarrierDescriptions
import io.github.dostepnykampus.feature.map.barrier.ChosenBarrierSheet
import io.github.dostepnykampus.feature.map.barrier.ShownBarriers
import io.github.dostepnykampus.feature.map.building.BuildingChoice
import io.github.dostepnykampus.feature.map.building.BuildingChoiceSheet
import io.github.dostepnykampus.feature.map.controls.MapStatusBar
import io.github.dostepnykampus.feature.map.controls.dataDay
import io.github.dostepnykampus.feature.map.data.CampusMapData
import io.github.dostepnykampus.feature.map.navigation.MapFocus

private const val TOP_STATE = "top"
private const val MAP_STATE = "map"

/**
 * Ekran V1 „Mapa kampusu”: nagłówek z polem symbolu i przełącznikiem „Mapa” i „Lista budynków”, bez sieci linia „Bez
 * internetu”, a pod nimi lista albo mapa z konturami z pliku, przyciskami mapy i podpisem OSM. Widok kamery jest stanem
 * zapisywanym; pod arkuszem V2 budynek z [focus] stoi nad arkuszem w punkcie podpisu. Jeden trafiony budynek otwiera
 * V2, kilka czeka na wybór w arkuszu „Który budynek?”. Przyciski mapy są bez arkusza V2 i klawiatury, każdy z polem 48
 * dp i nazwą dla TalkBack. Podpis „© OpenStreetMap contributors” jest odnośnikiem, od skali 1,5 krótszym; na ciemnym
 * tle elementy pływające mają obwódkę.
 *
 * Wymagania: US1, US1.1, NFR-19, US9.1, NFR-53, NFR-37, NFR-17, NFR-18, US2, US2.9, US3.3, US2.8
 *
 * @param onBuildingClick symbol budynku, gdy dotknięcie z tolerancją 24 dp trafiło jeden kontur, albo budynku
 *   wybranego w arkuszu „Który budynek?”, gdy trafiło kontur wspólny albo dwa kontury
 * @param modifier wymiary ekranu nadane przez aplikację
 * @param focus zaznaczenie z trasy. Budynek dostaje obrys, np. po „Wstecz” z V2; sam obrys nie rusza kamery.
 * Budynek z poleceniem „Pokaż budynek na mapie”: kamera staje na nim raz na polecenie,
 *   a dalsze przesuwanie mapy zostaje przy osobie; każde nowe polecenie, także dla tego samego budynku,
 *   znów przesuwa kamerę. Wejście dostaje pinezkę z podpisem, a kamera staje na nim, np. po „Pokaż wejście na
 *   mapie”; TalkBack czyta jego nazwę i budynek ekranu
 * @param contentPadding część ekranu zasłonięta z zewnątrz przez arkusz V2. Podpis OSM stoi nad nią,
 *   a mapa dalej wypełnia cały ekran. Pod arkuszem nie ma przycisków mapy, bo wymagania V2 ich nie mają;
 *   wracają po „Wstecz”. Budynek z [focus] staje w środku części nad arkuszem, a po „Wstecz” wraca widok
 *   sprzed otwarcia V2
 * @param onEmptyMapClick dotknięcie mapy, które nie trafiło żadnego konturu ani miejsca obok; aplikacja zamyka nim
 *   małą kartę V2
 * @param onListBuildingClick symbol budynku dotkniętego na liście „Lista budynków”; aplikacja wybiera nim tryb
 *   karty V2 dla wejścia z listy
 * @param mapView widok mapy; testy ekranu podają widok bez natywnej biblioteki
 * @param viewModel stan ekranu i zapis wyboru w przełączniku; w aplikacji z grafu Hilt, w testach z podróbkami źródeł
 */
@Composable
fun CampusMapScreen(
    onBuildingClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    focus: MapFocus? = null,
    contentPadding: PaddingValues = PaddingValues(),
    onEmptyMapClick: () -> Unit = {},
    onListBuildingClick: (String) -> Unit = onBuildingClick,
    mapView: CampusMapView = MapLibreMapView,
    viewModel: CampusMapViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CampusMapScreen(
        state = state,
        onListChange = viewModel::chooseList,
        onSearch = viewModel::search,
        onBuildingClick = onBuildingClick,
        modifier = modifier,
        focus = focus,
        contentPadding = contentPadding,
        onEmptyMapClick = onEmptyMapClick,
        onListBuildingClick = onListBuildingClick,
        mapView = mapView,
    )
}

/**
 * Ekran V1 ze stanem [state] z zewnątrz, bez ViewModel; parametry jak w [CampusMapScreen] z ViewModel.
 *
 * Góra V1 i mapa znikają pod arkuszem V2 albo przy liście; ich stan, np. przewinięcie listy i widok kamery, czeka
 * na powrót. Pod arkuszem V2 nagłówka nie ma, jak w hi-fi V2: mapa sięga wtedy pod pasek stanu. Budynek z listy
 * też otwiera V2 jako arkusz na mapie. Pod arkuszem widok kamery się nie zapisuje, więc po „Wstecz”
 * wraca widok sprzed V2. Przy otwartej klawiaturze mapa jest za niska na przyciski, więc znikają do
 * jej zamknięcia.
 *
 * Wymagania: US9.1, NFR-10, NFR-19, NFR-20, US2, US1.1, US2.9, US3.3, US2.8
 *
 * @param state katalog, gotowa geometria mapy, internet (bez niego pod polem symbolu stoi linia „Bez internetu”)
 *  i ostatni wybór w przełączniku; bez wyboru „Mapa”, a przy skali czcionki co najmniej 1,5
 *   „Lista budynków”
 * @param onListChange wybór w przełączniku: prawda dla „Lista budynków”
 * @param onSearch zapytanie z pola symbolu do policzenia w modelu widoku; wynik wraca w [CampusMapUiState.search]
 * @param onBuildingClick budynek dotknięty na mapie, wybrany w arkuszu albo na liście
 * @param modifier wymiary ekranu nadane przez aplikację
 * @param focus zaznaczenie z trasy: budynek z obrysem, także z poleceniem „Pokaż”, albo wejście
 *   z pinezką
 * @param contentPadding część ekranu zasłonięta przez arkusz V2
 * @param onEmptyMapClick dotknięcie mapy poza konturami
 * @param onListBuildingClick budynek dotknięty na liście
 * @param mapView widok mapy
 */
@Composable
fun CampusMapScreen(
    state: CampusMapUiState,
    onListChange: (Boolean) -> Unit,
    onSearch: (String) -> Unit,
    onBuildingClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    focus: MapFocus? = null,
    contentPadding: PaddingValues = PaddingValues(),
    onEmptyMapClick: () -> Unit = {},
    onListBuildingClick: (String) -> Unit = onBuildingClick,
    mapView: CampusMapView = MapLibreMapView,
) {
    val catalog = state.catalog
    val isCovered = contentPadding.calculateBottomPadding() > 0.dp
    val isList = state.isListSaved ?: (LocalDensity.current.fontScale >= LARGE_FONT_SCALE)
    val states = rememberSaveableStateHolder()
    val areas = rememberMapAreas(state.mapData, focus)
    Column(modifier.fillMaxSize()) {
        if (!isCovered) {
            states.SaveableStateProvider(TOP_STATE) {
                CampusSearch(
                    catalog = catalog,
                    search = state.search,
                    onSearch = onSearch,
                    isOnline = state.isOnline,
                    syncedAt = state.syncedAt,
                    isList = isList,
                    areas = areas.order,
                    onListChange = onListChange,
                    onBuildingClick = if (isList) onListBuildingClick else onBuildingClick,
                    modifier = if (isList) Modifier.weight(1f) else Modifier,
                )
            }
        }
        if (!isList || isCovered) {
            states.SaveableStateProvider(MAP_STATE) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    val shown = ShownBarriers(state.barriers.takeUnless { isCovered }.orEmpty())
                    CampusMap(
                        catalog = catalog,
                        layers = state.mapData,
                        barriers = shown,
                        focus = focus,
                        areas = areas,
                        contentPadding = contentPadding,
                        mapView = mapView,
                        onBuildingClick = onBuildingClick,
                        onEmptyMapClick = onEmptyMapClick,
                        modifier = Modifier.fillMaxSize(),
                    )
                    BarrierDescriptions(shown)
                }
            }
        }
    }
}

@Composable
private fun CampusMap(
    catalog: BuildingCatalog?,
    layers: CampusMapData?,
    barriers: ShownBarriers,
    focus: MapFocus?,
    areas: MapAreas,
    contentPadding: PaddingValues,
    mapView: CampusMapView,
    onBuildingClick: (String) -> Unit,
    onEmptyMapClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cover = contentPadding.calculateBottomPadding()
    val isKeyboardOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val topInset = WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
    var size by remember { mutableStateOf(IntSize.Zero) }
    var barrierId by rememberSaveable { mutableStateOf<String?>(null) }
    val state =
        rememberMapSceneState(
            layers = layers,
            catalog = catalog,
            focus = focus,
            area = areas.chosenName,
            cover = cover,
            size = size,
        )
    Box(modifier.consumeWindowInsets(topInset).onSizeChanged { size = it }) {
        val scene = state.scene
        val camera = state.camera
        if (scene != null && camera != null && state.limits != null) {
            mapView.Show(
                data = scene.data,
                selection = scene.selection.copy(barrier = barrierId),
                camera = camera,
                onCameraIdle = state::keep,
                onBuildingsTouch = { state.touch(it, onBuildingClick, onEmptyMapClick).also { barrierId = null } },
                onBarrierTouch = { barrierId = it.also { state.dismiss() } },
                modifier = Modifier.fillMaxSize(),
            )
            scene.selection.entrance?.let { EntranceDescription(it) }
            MapStatusBar(topInset, isShown = state.isCovered)
            val underBar = if (state.isCovered) topInset.asPaddingValues() else PaddingValues()
            val aboveSheet = Modifier.padding(underBar).padding(contentPadding)
            MapOverlay(
                camera = camera,
                hasControls = state.hasControls && !isKeyboardOpen,
                dataDay = catalog?.dataDay(),
                onCameraChange = state::move,
                areas = areas,
                onAreaShow = state::keep,
                modifier = Modifier.fillMaxSize().safeDrawingPadding().then(aboveSheet),
                hasPanels = !state.isCovered && !isKeyboardOpen,
            ) {
                val sheetModifier = Modifier.align(Alignment.BottomCenter).padding(Odstepy.s)
                ChosenBarrierSheet(
                    id = barrierId,
                    barriers = barriers,
                    catalog = catalog,
                    onClose = { barrierId = null },
                    onBuildingClick = onBuildingClick,
                    modifier = sheetModifier,
                )
                BuildingChoices(catalog = catalog, state = state, onChoose = onBuildingClick, modifier = sheetModifier)
            }
        }
    }
}

@Composable
private fun BuildingChoices(
    catalog: BuildingCatalog?,
    state: MapSceneState,
    onChoose: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val chosen = catalog?.buildingsOf(state.choice).orEmpty()
    if (chosen.isNotEmpty()) {
        BuildingChoiceSheet(onClose = state::dismiss, modifier = modifier) {
            chosen.forEach { building ->
                BuildingChoice(building) { state.choose(building.symbol, onChoose) }
            }
        }
    }
}

/** Punkt podpisu budynku o symbolu [symbol] z gotowych danych mapy; `null`, gdy dane go nie znają. */
internal fun CampusMapData.pointOf(symbol: String?): GeoPoint? = symbol?.let(labelPointsBySymbol::get)

private fun BuildingCatalog.buildingsOf(symbols: List<String>): List<Building> =
    symbols.mapNotNull { symbol -> buildings.firstOrNull { it.symbol == symbol } }
