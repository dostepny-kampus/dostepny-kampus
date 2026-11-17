/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.designsystem.LARGE_FONT_SCALE
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.model.building.Building
import io.github.dostepnykampus.core.model.building.BuildingCatalog
import io.github.dostepnykampus.feature.map.area.AreaEdgeAnnouncement
import io.github.dostepnykampus.feature.map.area.MapAreas
import io.github.dostepnykampus.feature.map.area.rememberMapAreas
import io.github.dostepnykampus.feature.map.area.rememberNoticeHeight
import io.github.dostepnykampus.feature.map.barrier.BarrierDescriptions
import io.github.dostepnykampus.feature.map.barrier.ChosenBarrierSheet
import io.github.dostepnykampus.feature.map.barrier.ShownBarriers
import io.github.dostepnykampus.feature.map.building.BuildingChoice
import io.github.dostepnykampus.feature.map.building.BuildingChoiceSheet
import io.github.dostepnykampus.feature.map.camera.CampusMapView
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.camera.MapLimits
import io.github.dostepnykampus.feature.map.camera.MapMotion
import io.github.dostepnykampus.feature.map.campus.CampusMapData
import io.github.dostepnykampus.feature.map.campus.selection
import io.github.dostepnykampus.feature.map.navigation.MapFocus
import io.github.dostepnykampus.feature.map.overlay.MapOverlay
import io.github.dostepnykampus.feature.map.overlay.MapStatusBar
import io.github.dostepnykampus.feature.map.overlay.dataDay
import io.github.dostepnykampus.feature.map.plan.FloorColumn
import io.github.dostepnykampus.feature.map.plan.ShownPlan

private const val TOP_STATE = "top"
private const val MAP_STATE = "map"

/**
 * Ekran V1 „Mapa kampusu”: nagłówek z przejściem między mapą a listą i polem symbolu, bez sieci linia „Bez
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
 * @param motion zapis ruchów mapy do budynku nad kartą V2, z których karta bierze czas; `null` bez zapisu
 * @param viewModel stan ekranu i zapis ostatniego widoku; w aplikacji z grafu Hilt, w testach z podróbkami źródeł
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
    motion: MapMotion? = null,
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
        motion = motion,
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
 *  i ostatni widok; bez wyboru mapa, a przy skali czcionki co najmniej 1,5
 *   „Lista budynków”
 * @param onListChange wybór widoku: prawda dla listy, fałsz dla mapy
 * @param onSearch zapytanie z pola symbolu do policzenia w modelu widoku; wynik wraca w [CampusMapUiState.search]
 * @param onBuildingClick budynek dotknięty na mapie, wybrany w arkuszu albo na liście
 * @param modifier wymiary ekranu nadane przez aplikację
 * @param focus zaznaczenie z trasy: budynek z obrysem, także z poleceniem „Pokaż”, albo wejście
 *   z pinezką
 * @param contentPadding część ekranu zasłonięta przez arkusz V2
 * @param onEmptyMapClick dotknięcie mapy poza konturami
 * @param onListBuildingClick budynek dotknięty na liście
 * @param mapView widok mapy
 * @param motion zapis ruchów mapy do budynku nad kartą V2
 * @param plan plan piętra budynku przy środku mapy; `null`, gdy planu nie widać
 * @param onCamera widok mapy po geście osoby i widok, do którego zmierza ruch kamery wykonany przez aplikację
 * @param onFloorChoose piętro wybrane w kolumnie pięter: symbol budynku i identyfikator piętra
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
    motion: MapMotion? = null,
    plan: ShownPlan? = null,
    onCamera: (MapCamera) -> Unit = {},
    onFloorChoose: (String, String) -> Unit = { _, _ -> },
) {
    val catalog = state.catalog
    val isCovered = contentPadding.calculateBottomPadding() > 0.dp
    val isList = state.isListSaved ?: (LocalDensity.current.fontScale >= LARGE_FONT_SCALE)
    val states = rememberSaveableStateHolder()
    val areas = rememberMapAreas(state.mapData, focus)
    val barExit = rememberTopBarExit(isCovered = isCovered, motion = motion)
    var barHeight by remember { mutableIntStateOf(0) }
    var headerHeight by remember { mutableIntStateOf(0) }
    val barTop = with(LocalDensity.current) { barHeight.toDp() }
    val headerTop = with(LocalDensity.current) { headerHeight.toDp() }
    Box(modifier.fillMaxSize()) {
        if (!isList || isCovered) {
            states.SaveableStateProvider(MAP_STATE) {
                Box(Modifier.fillMaxSize()) {
                    val shown = ShownBarriers(state.barriers.takeUnless { isCovered }.orEmpty())
                    CampusMap(
                        catalog = catalog,
                        layers = state.mapData,
                        barriers = shown,
                        focus = focus,
                        areas = areas,
                        top = mapTop(isCovered = isCovered, bar = barTop),
                        cameraTop = mapTop(isCovered = isCovered, bar = headerTop),
                        contentPadding = contentPadding,
                        mapView = mapView,
                        motion = motion,
                        plan = plan,
                        onCamera = onCamera,
                        onFloorChoose = onFloorChoose,
                        onBuildingClick = onBuildingClick,
                        onEmptyMapClick = onEmptyMapClick,
                        modifier = Modifier.fillMaxSize().background(DostepnyKampusTheme.mapColors.lad),
                    )
                    BarrierDescriptions(shown)
                }
            }
        }
        if (barExit.isBarShown(isCovered = isCovered, isList = isList)) {
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
                    onBarHeight = { headerHeight = it },
                    modifier =
                        (if (isList) Modifier.fillMaxSize() else Modifier)
                            .slideUp(barExit)
                            .onSizeChanged { barHeight = it.height },
                )
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
    top: Dp,
    cameraTop: Dp,
    contentPadding: PaddingValues,
    mapView: CampusMapView,
    motion: MapMotion?,
    plan: ShownPlan?,
    onCamera: (MapCamera) -> Unit,
    onFloorChoose: (String, String) -> Unit,
    onBuildingClick: (String) -> Unit,
    onEmptyMapClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cover = contentPadding.calculateBottomPadding()
    val isKeyboardOpen = rememberKeyboardOpenFlag()
    val topInset = WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
    var size by remember { mutableStateOf(IntSize.Zero) }
    var barrierId by rememberSaveable { mutableStateOf<String?>(null) }
    val width = with(LocalDensity.current) { size.width.toDp() }
    val notice = rememberNoticeHeight(shift = areas.selectionNotice, width = width)
    val state =
        rememberMapSceneState(
            layers = layers,
            catalog = catalog,
            focus = focus,
            area = areas.chosenName,
            cover = cover,
            size = size,
            top = cameraTop,
            motion = motion,
            notice = notice,
        )
    Box(modifier.consumeWindowInsets(topInset).onSizeChanged { size = it }) {
        val scene = state.scene
        val move = state.move
        if (scene != null && move != null && state.limits != null) {
            mapView.Show(
                data = scene.data,
                selection = scene.selection.copy(barrier = barrierId, plan = plan),
                move = move,
                inset = state.inset,
                onCameraIdle = { view ->
                    areas.acknowledge()
                    onCamera(view.also { state.keep(it) })
                },
                onBuildingsTouch = { state.touch(it, onBuildingClick, onEmptyMapClick).also { barrierId = null } },
                onBarrierTouch = { barrierId = it.also { state.dismiss() } },
                modifier = Modifier.fillMaxSize(),
            )
            scene.selection.entrance?.let { EntranceDescription(it) }
            FollowCamera(camera = move.camera, limits = state.limits, area = areas.current?.name, onCamera = onCamera)
            MapStatusBar(topInset, isShown = state.isCovered)
            val underBar = if (state.isCovered) topInset.asPaddingValues() else PaddingValues(top = top)
            MapOverlay(
                camera = move.camera,
                hasControls = state.hasControls && !isKeyboardOpen,
                dataDay = catalog?.dataDay(),
                onCameraChange = state::move,
                areas = areas,
                onAreaShow = state::land,
                modifier = Modifier.overlayArea(underBar = underBar, sheet = contentPadding),
                hasPanels = !state.isCovered && !isKeyboardOpen,
                hasNotice = !isKeyboardOpen,
                motion = motion,
                start = { FloorColumn(plan = plan, onChoose = onFloorChoose) },
            ) {
                val barrier = ChosenBarrier(id = barrierId, barriers = barriers, onClose = { barrierId = null })
                MapSheets(barrier = barrier, catalog = catalog, state = state, onBuildingClick = onBuildingClick)
            }
        }
    }
}

private fun Modifier.overlayArea(underBar: PaddingValues, sheet: PaddingValues): Modifier =
    fillMaxSize()
        .safeDrawingPadding()
        .padding(underBar)
        .padding(sheet)

/** Zaznaczona przeszkoda [id] spośród [barriers] i zamknięcie jej karty [onClose]. */
private data class ChosenBarrier(val id: String?, val barriers: ShownBarriers, val onClose: () -> Unit)

@Composable
private fun FollowCamera(camera: MapCamera, limits: MapLimits, area: String?, onCamera: (MapCamera) -> Unit) {
    val report by rememberUpdatedState(onCamera)
    DisposableEffect(camera) {
        report(camera)
        onDispose {}
    }
    AreaEdgeAnnouncement(camera = camera, limits = limits, area = area)
}

@Composable
private fun BoxScope.MapSheets(
    barrier: ChosenBarrier,
    catalog: BuildingCatalog?,
    state: MapSceneState,
    onBuildingClick: (String) -> Unit,
) {
    val modifier = Modifier.align(Alignment.BottomCenter).padding(Odstepy.s)
    ChosenBarrierSheet(
        id = barrier.id,
        barriers = barrier.barriers,
        catalog = catalog,
        onClose = barrier.onClose,
        onBuildingClick = onBuildingClick,
        modifier = modifier,
    )
    BuildingChoices(catalog = catalog, state = state, onChoose = onBuildingClick, modifier = modifier)
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

private fun BuildingCatalog.buildingsOf(symbols: List<String>): List<Building> =
    symbols.mapNotNull { symbol -> buildings.firstOrNull { it.symbol == symbol } }

@Composable
private fun rememberKeyboardOpenFlag(): Boolean {
    val ime = WindowInsets.ime
    val density = LocalDensity.current
    val isOpen by remember(ime, density) { derivedStateOf { ime.getBottom(density) > 0 } }
    return isOpen
}
