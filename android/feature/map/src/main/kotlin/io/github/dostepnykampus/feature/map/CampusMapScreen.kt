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
import io.github.dostepnykampus.feature.map.data.CampusMapData
import io.github.dostepnykampus.feature.map.navigation.MapFocus

private const val TOP_STATE = "top"
private const val MAP_STATE = "map"

/**
 * Ekran V1 „Mapa kampusu” (US1): nagłówek z polem symbolu budynku (US1.1) i przełącznikiem „Mapa” i „Lista
 * budynków” (NFR-19), bez sieci linia „Bez internetu” (US9.1). Pod nimi lista budynków albo mapa: kontury z pliku
 * w aplikacji, przyciski powiększania i przesuwania (NFR-53) i podpis OSM (NFR-37). Przyciski i podpis są
 * w `WindowInsets.safeDrawing`. Budynek z listy otwiera V2 jako arkusz na mapie (decyzja 48).
 * Widok kamery jest stanem zapisywanym, więc wraca po „Wstecz” z V2 (decyzja 83).
 * Kamera to widok zapisany albo startowy; pod arkuszem V2 budynek z [focus] stoi w części nad arkuszem, w punkcie
 * podpisu, który zawsze leży wewnątrz konturu. Jeden trafiony budynek otwiera V2 od razu, a kilka czeka na wybór
 * w arkuszu „Który budynek?” (decyzja 85). Przyciski mapy są, gdy nie ma arkusza V2 ani klawiatury; stoją
 * w nim strzałki przesuwania w krzyżu, a pod nimi „Powiększ” i „Pomniejsz” (NFR-53, NFR-17, NFR-18, PW-8).
 * Każdy przycisk ma pole 48 dp ze znakiem, a TalkBack czyta tylko jego nazwę, np. „Przesuń mapę w górę”.
 * Podpis „© OpenStreetMap contributors” jest podkreślonym odnośnikiem na niskiej plakietce, jak w hi-fi V1; od
 * skali czcionki 1,5 skraca się do „© OpenStreetMap” (NFR-37). Pole dotyku ma 48 dp i wystaje poza plakietkę
 * (NFR-17). Gdy tło jest ciemne (jasność poniżej progu), element pływający dostaje obwódkę zamiast samego
 * cienia, bo cień słabo widać.
 *
 * @param onBuildingClick symbol budynku, gdy dotknięcie z tolerancją 24 dp trafiło jeden kontur, albo budynku
 *   wybranego w arkuszu „Który budynek?”, gdy trafiło kontur wspólny albo dwa kontury (US2, decyzja 85)
 * @param modifier wymiary ekranu nadane przez aplikację
 * @param focus zaznaczenie z trasy. Budynek dostaje obrys, np. po „Wstecz” z V2; sam obrys nie rusza kamery
 *   (US1.1). Budynek z poleceniem „Pokaż budynek na mapie” (US2.9): kamera staje na nim raz na polecenie,
 *   a dalsze przesuwanie mapy zostaje przy osobie (NFR-53); każde nowe polecenie, także dla tego samego budynku,
 *   znów przesuwa kamerę. Wejście dostaje pinezkę z podpisem, a kamera staje na nim, np. po „Pokaż wejście na
 *   mapie” (US3.3, decyzja 134); TalkBack czyta jego nazwę i budynek ekranu (NFR-18)
 * @param contentPadding część ekranu zasłonięta z zewnątrz przez arkusz V2 (decyzja 87). Podpis OSM stoi nad nią,
 *   a mapa dalej wypełnia cały ekran (NFR-37). Pod arkuszem nie ma przycisków mapy, bo wymagania V2 ich nie mają;
 *   wracają po „Wstecz” (NFR-53). Budynek z [focus] staje w środku części nad arkuszem, a po „Wstecz” wraca widok
 *   sprzed otwarcia V2 (decyzja 83)
 * @param onEmptyMapClick dotknięcie mapy, które nie trafiło żadnego konturu ani miejsca obok; aplikacja zamyka nim
 *   małą kartę V2 (US2.8)
 * @param onListBuildingClick symbol budynku dotkniętego na liście „Lista budynków”; aplikacja wybiera nim tryb
 *   karty V2 dla wejścia z listy (NFR-19)
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
 * też otwiera V2 jako arkusz na mapie (decyzja 48). Pod arkuszem widok kamery się nie zapisuje, więc po „Wstecz”
 * wraca widok sprzed V2 (decyzja 83). Przy otwartej klawiaturze mapa jest za niska na przyciski, więc znikają do
 * jej zamknięcia (decyzja 91).
 *
 * @param state katalog, gotowa geometria mapy, internet (bez niego pod polem symbolu stoi linia „Bez internetu”,
 *   US9.1, NFR-10) i ostatni wybór w przełączniku; bez wyboru „Mapa”, a przy skali czcionki co najmniej 1,5
 *   „Lista budynków” (NFR-19, NFR-20)
 * @param onListChange wybór w przełączniku: prawda dla „Lista budynków”
 * @param onBuildingClick budynek dotknięty na mapie, wybrany w arkuszu albo na liście (US2)
 * @param modifier wymiary ekranu nadane przez aplikację
 * @param focus zaznaczenie z trasy: budynek z obrysem (US1.1), także z poleceniem „Pokaż” (US2.9), albo wejście
 *   z pinezką (US3.3, decyzja 134)
 * @param contentPadding część ekranu zasłonięta przez arkusz V2 (decyzja 87)
 * @param onEmptyMapClick dotknięcie mapy poza konturami (US2.8)
 * @param onListBuildingClick budynek dotknięty na liście (NFR-19)
 * @param mapView widok mapy
 */
@Composable
fun CampusMapScreen(
    state: CampusMapUiState,
    onListChange: (Boolean) -> Unit,
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
                    isOnline = state.isOnline,
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
                CampusMap(
                    catalog = catalog,
                    layers = state.mapData,
                    focus = focus,
                    areas = areas,
                    contentPadding = contentPadding,
                    mapView = mapView,
                    onBuildingClick = onBuildingClick,
                    onEmptyMapClick = onEmptyMapClick,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun CampusMap(
    catalog: BuildingCatalog?,
    layers: CampusMapData?,
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
                selection = scene.selection,
                camera = camera,
                onCameraIdle = state::keep,
                onBuildingsTouch = { state.touch(it, onBuildingClick, onEmptyMapClick) },
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
                val chosen = catalog?.buildingsOf(state.choice).orEmpty()
                if (chosen.isNotEmpty()) {
                    val sheetModifier = Modifier.align(Alignment.BottomCenter).padding(Odstepy.s)
                    BuildingChoiceSheet(onClose = state::dismiss, modifier = sheetModifier) {
                        chosen.forEach { building ->
                            BuildingChoice(building) { state.choose(building.symbol, onBuildingClick) }
                        }
                    }
                }
            }
        }
    }
}

/** Punkt podpisu budynku o symbolu [symbol] z gotowych danych mapy; `null`, gdy dane go nie znają (#818). */
internal fun CampusMapData.pointOf(symbol: String?): GeoPoint? = symbol?.let(labelPointsBySymbol::get)

private fun BuildingCatalog.buildingsOf(symbols: List<String>): List<Building> =
    symbols.mapNotNull { symbol -> buildings.firstOrNull { it.symbol == symbol } }
