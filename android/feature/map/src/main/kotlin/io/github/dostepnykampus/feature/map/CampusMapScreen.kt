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
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.designsystem.LARGE_FONT_SCALE
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.model.building.Building
import io.github.dostepnykampus.core.model.building.BuildingCatalog
import io.github.dostepnykampus.core.model.room.FoundRoom
import io.github.dostepnykampus.feature.map.area.AreaEdgeAnnouncement
import io.github.dostepnykampus.feature.map.area.MapAreas
import io.github.dostepnykampus.feature.map.area.rememberMapAreas
import io.github.dostepnykampus.feature.map.area.rememberNoticeHeight
import io.github.dostepnykampus.feature.map.barrier.BarrierDescriptions
import io.github.dostepnykampus.feature.map.barrier.ShownBarriers
import io.github.dostepnykampus.feature.map.building.BuildingChoiceSheet
import io.github.dostepnykampus.feature.map.building.ChoiceLink
import io.github.dostepnykampus.feature.map.camera.CampusMapView
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.camera.MapLimits
import io.github.dostepnykampus.feature.map.camera.MapMotion
import io.github.dostepnykampus.feature.map.campus.selection
import io.github.dostepnykampus.feature.map.entrance.EntranceActions
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
 *   wracają po „Wstecz”. Budynek z [focus] staje w środku części nad arkuszem, a po zamknięciu arkusza mapa
 *   stoi na ekranie w widoku z chwili zamknięcia
 * @param onEmptyMapClick dotknięcie mapy, które nie trafiło żadnego konturu ani miejsca obok; aplikacja zamyka nim
 *   małą kartę V2
 * @param onListBuildingClick symbol budynku dotkniętego na liście „Lista budynków”; aplikacja wybiera nim tryb
 *   karty V2 dla wejścia z listy
 * @param mapView widok mapy; testy ekranu podają widok bez natywnej biblioteki
 * @param motion zapis ruchów mapy do budynku nad kartą V2, z których karta bierze czas; `null` bez zapisu
 * @param entranceActions „Zgłoś barierę przy wejściu” z arkusza wejścia
 * @param onCardReturn powrót z arkusza znaku do karty budynku o podanym symbolu, którą arkusz zastąpił
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
    entranceActions: EntranceActions = EntranceActions(),
    onCardReturn: (String) -> Unit = onBuildingClick,
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
        entranceActions = entranceActions,
        mapView = mapView,
        motion = motion,
        onCardReturn = onCardReturn,
    )
}

/**
 * Ekran V1 ze stanem [state] z zewnątrz, bez ViewModel; parametry jak w [CampusMapScreen] z ViewModel.
 *
 * Góra V1 i mapa znikają pod arkuszem V2 albo przy liście; ich stan, np. przewinięcie listy i widok kamery, czeka
 * na powrót. Pod arkuszem V2 nagłówka nie ma, jak w hi-fi V2: mapa sięga wtedy pod pasek stanu. Budynek z listy
 * też otwiera V2 jako arkusz na mapie. Po zamknięciu arkusza mapa stoi na ekranie w widoku z chwili zamknięcia.
 * Przy otwartej klawiaturze mapa jest za niska na przyciski, więc znikają do jej zamknięcia; dotknięcie mapy
 * zamyka klawiaturę i wyniki pola „Szukaj”, a tekst w polu zostaje.
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
 * @param entranceActions czynności arkusza wejścia
 * @param mapView widok mapy
 * @param motion zapis ruchów mapy do budynku nad kartą V2
 * @param plan plan piętra budynku przy środku mapy; `null`, gdy planu nie widać
 * @param onCamera widok mapy po geście osoby i widok, do którego zmierza ruch kamery wykonany przez aplikację
 * @param onFloorChoose piętro wybrane w kolumnie pięter: symbol budynku i identyfikator piętra
 * @param onMapHide mapa zeszła z ekranu, np. pod listę budynków; jej plan piętra przestaje obowiązywać
 * @param onCardReturn powrót z arkusza znaku do karty budynku, którą arkusz zastąpił
 * @param sheetCover zasłonięty dół okna w px w bieżącej klatce, za którym przyciski mapy i podpis OSM idą razem
 *   z krawędzią arkusza V2 w ruchu; czytany tylko w fazie układu; `null` stawia je nad [contentPadding]
 * @param onRoomClick sala wybrana w wynikach pola szukaj
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
    entranceActions: EntranceActions = EntranceActions(),
    mapView: CampusMapView = MapLibreMapView,
    motion: MapMotion? = null,
    plan: ShownPlan? = null,
    onCamera: (MapCamera) -> Unit = {},
    onFloorChoose: (String, String) -> Unit = { _, _ -> },
    onMapHide: () -> Unit = {},
    onCardReturn: (String) -> Unit = onBuildingClick,
    sheetCover: (() -> Int)? = null,
    onRoomClick: (FoundRoom) -> Unit = {},
) {
    val sheet =
        screenSheet(focus = focus, padding = contentPadding, onCardClose = onEmptyMapClick, onCardReturn = onCardReturn)
    val isCovered = sheet.layout.padding.calculateBottomPadding() > 0.dp
    val isList = state.isListSaved ?: (LocalDensity.current.fontScale >= LARGE_FONT_SCALE)
    val states = rememberSaveableStateHolder()
    val areas = rememberMapAreas(state.mapData, sheet.layout.focus)
    val barExit = rememberTopBarExit(isCovered = isCovered, motion = motion)
    var barHeight by remember { mutableIntStateOf(0) }
    var headerHeight by remember { mutableIntStateOf(0) }
    val barTop = with(LocalDensity.current) { barHeight.toDp() }
    val headerTop = with(LocalDensity.current) { headerHeight.toDp() }
    Box(modifier.fillMaxSize()) {
        if (!isList || isCovered) {
            states.SaveableStateProvider(MAP_STATE) {
                Box(Modifier.mapArea(isUnderSheet = sheet.isEntranceFull, focus = LocalFocusManager.current)) {
                    val shown = ShownBarriers(state.barriers.takeUnless { sheet.isCardOpen }.orEmpty())
                    CampusMap(
                        ui = state,
                        barriers = shown,
                        focus = sheet.layout.focus,
                        areas = areas,
                        top = mapTop(isCovered = isCovered, bar = barTop),
                        cameraTop = mapTop(isCovered = isCovered, bar = headerTop),
                        sheet = sheet.copy(cover = sheetCover),
                        mapView = mapView,
                        motion = motion,
                        plan = PlanLink(shown = plan, onView = onCamera, onFloor = onFloorChoose, onHide = onMapHide),
                        onBuildingClick = onBuildingClick,
                        onEmptyMapClick = onEmptyMapClick,
                        entranceActions = entranceActions,
                        modifier = Modifier.fillMaxSize().background(DostepnyKampusTheme.mapColors.lad),
                    )
                    BarrierDescriptions(shown)
                }
            }
        }
        if (barExit.isBarShown(isCovered = isCovered, isList = isList, isUnderFullSheet = sheet.isEntranceFull)) {
            states.SaveableStateProvider(TOP_STATE) {
                CampusSearch(
                    catalog = state.catalog,
                    search = state.search,
                    onSearch = onSearch,
                    isOnline = state.isOnline,
                    syncedAt = state.syncedAt,
                    isList = isList,
                    areas = areas.order,
                    onListChange = onListChange,
                    onBuildingClick = if (isList) onListBuildingClick else onBuildingClick,
                    onRoomClick = onRoomClick,
                    onBarHeight = { headerHeight = it },
                    modifier =
                        (if (isList) Modifier.fillMaxSize() else Modifier)
                            .slideUp(barExit)
                            .onSizeChanged { barHeight = it.height },
                )
            }
        }
        sheet.FullEntranceSheet(ui = state, actions = entranceActions, onBuildingClick = onBuildingClick)
    }
}

private fun Modifier.mapArea(isUnderSheet: Boolean, focus: FocusManager): Modifier =
    fillMaxSize()
        .releasesSearchOnTouch(focus)
        .then(if (isUnderSheet) Modifier.clearAndSetSemantics {} else Modifier)

@Composable
private fun CampusMap(
    ui: CampusMapUiState,
    barriers: ShownBarriers,
    focus: MapFocus?,
    areas: MapAreas,
    top: Dp,
    cameraTop: Dp,
    sheet: ScreenSheet,
    mapView: CampusMapView,
    motion: MapMotion?,
    plan: PlanLink,
    onBuildingClick: (String) -> Unit,
    onEmptyMapClick: () -> Unit,
    entranceActions: EntranceActions,
    modifier: Modifier = Modifier,
) {
    val isKeyboardOpen = rememberKeyboardOpenFlag()
    val topInset = WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
    var size by remember { mutableStateOf(IntSize.Zero) }
    val signs = sheet.signs
    val state =
        rememberCampusScene(
            ui = ui,
            focus = focus,
            areas = areas,
            sheet = sheet,
            size = size,
            top = cameraTop,
            motion = motion,
        )
    Box(modifier.consumeWindowInsets(topInset).onSizeChanged { size = it }) {
        val scene = state.scene
        val move = state.move
        if (scene != null && move != null && state.limits != null) {
            val pinned = signs.rememberPinnedEntrance(catalog = ui.catalog, chosen = scene.selection.entrance)
            mapView.Show(
                data = scene.data,
                selection = scene.selection.copy(entrance = pinned, barrier = signs.barrierId, plan = plan.shown),
                move = move,
                inset = state.inset,
                onCameraIdle = { view ->
                    areas.acknowledge()
                    plan.onView(view.also { state.keep(it) })
                },
                onBuildingsTouch = { state.taps.touch(it, onBuildingClick, onEmptyMapClick).also { signs.close() } },
                onSignTouch = { sign, id -> signs.open(sign, id).also { state.taps.dismiss() } },
                modifier = Modifier.fillMaxSize(),
            )
            pinned?.let { EntranceDescription(it) }
            FollowCamera(camera = move.camera, limits = state.limits, area = areas.current?.name, plan = plan)
            MapStatusBar(topInset, isShown = state.isCovered)
            val underBar = if (state.isCovered) topInset.asPaddingValues() else PaddingValues(top = top)
            MapOverlay(
                camera = move.camera,
                hasControls = state.hasControls && !isKeyboardOpen,
                dataDay = ui.catalog?.dataDay(),
                onCameraChange = state::move,
                areas = areas,
                onAreaShow = state::land,
                modifier = Modifier.overlayArea(underBar = underBar, sheet = sheet),
                hasPanels = !state.isCovered && !isKeyboardOpen,
                hasNotice = !isKeyboardOpen,
                motion = motion,
                start = { FloorColumn(plan = plan.shown, onChoose = plan.onFloor) },
            ) {
                val open = OpenSign(sheet = sheet, barriers = barriers, catalog = ui.catalog, plan = plan.shown)
                MapSheets(open = open, state = state, onBuilding = onBuildingClick, entranceActions = entranceActions)
            }
        }
    }
}

@Composable
private fun rememberCampusScene(
    ui: CampusMapUiState,
    focus: MapFocus?,
    areas: MapAreas,
    sheet: ScreenSheet,
    size: IntSize,
    top: Dp,
    motion: MapMotion?,
): MapSceneState {
    val width = with(LocalDensity.current) { size.width.toDp() }
    return rememberMapSceneState(
        layers = ui.mapData,
        catalog = ui.catalog,
        focus = focus,
        area = areas.chosenName,
        cover = sheet.layout.padding.calculateBottomPadding(),
        size = size,
        top = top,
        motion = motion,
        notice = rememberNoticeHeight(shift = areas.selectionNotice, width = width),
        sheet = sheet.signs.markedCover(focus),
    )
}

private fun Modifier.overlayArea(underBar: PaddingValues, sheet: ScreenSheet): Modifier {
    val area = fillMaxSize().safeDrawingPadding().padding(underBar)
    val cover = sheet.cover ?: return area.padding(sheet.padding)
    return area.layout { measurable, constraints ->
        val bottom = cover().coerceIn(minimumValue = 0, maximumValue = constraints.maxHeight)
        val placeable = measurable.measure(constraints.offset(vertical = -bottom))
        layout(width = placeable.width, height = placeable.height + bottom) { placeable.place(x = 0, y = 0) }
    }
}

/** Arkusz znaku z [sheet] z barierami [barriers], katalogiem [catalog] i planem [plan] na mapie. */
private data class OpenSign(
    val sheet: ScreenSheet,
    val barriers: ShownBarriers,
    val catalog: BuildingCatalog?,
    val plan: ShownPlan?,
)

/**
 * Plan piętra [shown] przy mapie: [onView] dostaje widok kamery, [onFloor] piętro z kolumny, a [onHide] zejście
 * mapy z ekranu.
 */
private data class PlanLink(
    val shown: ShownPlan?,
    val onView: (MapCamera) -> Unit,
    val onFloor: (String, String) -> Unit,
    val onHide: () -> Unit,
)

@Composable
private fun FollowCamera(camera: MapCamera, limits: MapLimits, area: String?, plan: PlanLink) {
    val report by rememberUpdatedState(plan.onView)
    val hide by rememberUpdatedState(plan.onHide)
    DisposableEffect(camera) {
        report(camera)
        onDispose {}
    }
    DisposableEffect(Unit) { onDispose { hide() } }
    AreaEdgeAnnouncement(camera = camera, limits = limits, area = area)
}

@Composable
private fun BoxScope.MapSheets(
    open: OpenSign,
    state: MapSceneState,
    onBuilding: (String) -> Unit,
    entranceActions: EntranceActions,
) {
    val modifier = Modifier.align(Alignment.BottomCenter).padding(Odstepy.s)
    ChosenSignSheet(
        signs = open.sheet.signs,
        barriers = open.barriers,
        catalog = open.catalog,
        plan = open.plan,
        onBuildingTap = { state.taps.choose(it, onBuilding) },
        entranceActions = entranceActions,
        modifier = modifier,
        hidesEntrance = open.sheet.isEntranceFull,
    )
    BuildingChoices(catalog = open.catalog, state = state, onChoose = onBuilding, modifier = modifier)
}

@Composable
private fun BuildingChoices(
    catalog: BuildingCatalog?,
    state: MapSceneState,
    onChoose: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val chosen = catalog?.buildingsOf(state.taps.choice).orEmpty()
    if (chosen.isNotEmpty()) {
        BuildingChoiceSheet(onClose = state.taps::dismiss, modifier = modifier) {
            chosen.forEach { building ->
                ChoiceLink(building) { state.taps.choose(building.symbol, onChoose) }
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
