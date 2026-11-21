/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.designsystem.LARGE_FONT_SCALE
import io.github.dostepnykampus.core.model.building.BuildingCatalog
import io.github.dostepnykampus.core.model.room.FoundRoom
import io.github.dostepnykampus.feature.map.area.MapAreas
import io.github.dostepnykampus.feature.map.area.rememberMapAreas
import io.github.dostepnykampus.feature.map.area.rememberNoticeHeight
import io.github.dostepnykampus.feature.map.barrier.BarrierDescriptions
import io.github.dostepnykampus.feature.map.barrier.ShownBarriers
import io.github.dostepnykampus.feature.map.camera.CampusMapView
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.camera.MapMotion
import io.github.dostepnykampus.feature.map.camera.TouchSign
import io.github.dostepnykampus.feature.map.campus.MapSelection
import io.github.dostepnykampus.feature.map.campus.SelectedEntrance
import io.github.dostepnykampus.feature.map.campus.ShownPlan
import io.github.dostepnykampus.feature.map.campus.selection
import io.github.dostepnykampus.feature.map.entrance.EntranceActions
import io.github.dostepnykampus.feature.map.navigation.MapFocus
import io.github.dostepnykampus.feature.map.plan.mapPanel
import io.github.dostepnykampus.feature.map.plan.roomAt
import io.github.dostepnykampus.feature.map.plan.showsSign
import io.github.dostepnykampus.feature.map.room.OpenRoom
import io.github.dostepnykampus.feature.map.room.RoomCardLayer
import io.github.dostepnykampus.feature.map.room.RoomScene
import io.github.dostepnykampus.feature.map.room.RoomSheetSources
import io.github.dostepnykampus.feature.map.room.RoomView
import io.github.dostepnykampus.feature.map.room.markersOn
import io.github.dostepnykampus.feature.map.room.rememberRoomView

private const val TOP_STATE = "top"
private const val MAP_STATE = "map"

/**
 * Ekran V1 „Mapa kampusu”: nagłówek z przejściem między mapą a listą i polem symbolu, bez sieci linia „Bez
 * internetu”, a pod nimi lista albo mapa z konturami z pliku, przyciskami mapy i podpisem OSM. Widok kamery jest stanem
 * zapisywanym; pod arkuszem V2 budynek z [focus] stoi nad arkuszem w punkcie podpisu. Jeden trafiony budynek otwiera
 * V2, kilka czeka na wybór w arkuszu „Który budynek?”. Przyciski mapy są bez arkusza V2 i klawiatury, każdy z polem 48
 * dp i nazwą dla TalkBack. Podpis OpenStreetMap jest odnośnikiem. Na ciemnym tle elementy pływające mają obwódkę.
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
 * zamyka klawiaturę i wyniki pola „Szukaj” bez wyboru obiektu pod palcem, a tekst w polu zostaje.
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
 * @param onPlanSelect budynek karty albo arkusza nad mapą, który trzyma plan; `null`, gdy plan wybiera środek mapy
 * @param onCardReturn powrót z arkusza znaku do karty budynku, którą arkusz zastąpił
 * @param sheetCover zasłonięty dół okna w px w bieżącej klatce, za którym przyciski mapy i podpis OSM idą razem
 *   z krawędzią arkusza V2 w ruchu; czytany tylko w fazie układu; `null` stawia je nad [contentPadding]
 * @param onRoomClick sala wybrana w wynikach pola szukaj
 * @param room karta sali i jej zamknięcie; `null` bez karty
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
    onPlanSelect: (String?) -> Unit = {},
    onCardReturn: (String) -> Unit = onBuildingClick,
    sheetCover: (() -> Int)? = null,
    onRoomClick: (FoundRoom) -> Unit = {},
    room: OpenRoom? = null,
) {
    val sheet =
        screenSheet(focus = focus, padding = contentPadding, onCardClose = onEmptyMapClick, onCardReturn = onCardReturn)
            .withPanel(focus = focus, catalog = state.catalog, opened = room to plan, onSelect = onPlanSelect)
    val isCovered = isMapCovered(sheet = sheet.layout.padding, isRoomOpen = room != null)
    val states = rememberSaveableStateHolder()
    val areas = rememberMapAreas(state.mapData, sheet.layout.focus)
    val barExit = rememberTopBarExit(isCovered = isCovered, motion = motion)
    var barHeight by remember { mutableIntStateOf(0) }
    var headerHeight by remember { mutableIntStateOf(0) }
    val search = rememberSearchFocus()
    sheet.RoomLayout(room, state.roomSheets(onBuildingClick, entranceActions), modifier) { isLarge ->
        val isList = if (room == null) state.isListSaved ?: isLarge else false
        if (!isList || isCovered) {
            states.SaveableStateProvider(MAP_STATE) {
                MapArea(state, sheet, search) { shown ->
                    CampusMap(
                        ui = state,
                        barriers = shown,
                        focus = sheet.layout.focus,
                        areas = areas,
                        top = mapTop(isCovered = isCovered, bar = barHeight),
                        cameraTop = mapTop(isCovered = isCovered, bar = headerHeight) ?: 0.dp,
                        sheet = sheet.copy(cover = sheetCover),
                        mapView = remember(mapView, search) { mapView.closingSearchFirst(search) },
                        motion = motion,
                        plan = PlanLink(shown = plan, onView = onCamera, onFloor = onFloorChoose, onHide = onMapHide),
                        onBuildingClick = onBuildingClick,
                        onEmptyMapClick = onEmptyMapClick,
                        onRoomClick = onRoomClick,
                        entrance = entranceActions,
                        room = room,
                        isRoomFull = isLarge,
                        modifier = Modifier.fillMaxSize().background(DostepnyKampusTheme.mapColors.lad),
                    )
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
                    searchFocus = search,
                    modifier = Modifier.searchBarArea(isList = isList, barExit = barExit) { barHeight = it },
                )
            }
        }
        sheet.FullEntranceSheet(ui = state, actions = entranceActions, onBuildingClick = onBuildingClick)
    }
}

@Composable
private fun ScreenSheet.withPanel(
    focus: MapFocus?,
    catalog: BuildingCatalog?,
    opened: Pair<OpenRoom?, ShownPlan?>,
    onSelect: (String?) -> Unit,
): ScreenSheet {
    val (room, plan) = opened
    val isBelowMap = LocalDensity.current.fontScale < LARGE_FONT_SCALE
    val entrance = signs.entranceId
    val marked = (focus as? MapFocus.Entrance)?.takeIf { it.id == entrance }
    val entered = catalog?.run { entrances.firstOrNull { it.id == entrance } }
    val sign = signs.planSignId?.let { plan?.building }
    val building = marked?.buildingSymbol ?: entered?.buildingSymbol ?: sign
    val roomCover = signs.roomHeight.takeIf { room != null && isBelowMap } ?: 0
    val roomWay = signs.roomWay?.takeIf { (id) -> isBelowMap && id == room?.run { card.found.room.id } }?.second ?: 0
    val cover = signs.markedCover(layout.focus).takeIf { it > 0 } ?: signs.planCover().takeIf { isBelowMap } ?: 0
    val panel =
        mapPanel(
            focus = focus,
            sheetBuilding = building,
            sheetCover = cover,
            roomCover = roomCover,
            roomWayCover = roomWay,
        )
    SideEffect { onSelect(panel.building) }
    val gone = signs.planSignId?.takeUnless { plan.showsSign(it) }
    SideEffect { if (gone != null) signs.close() }
    return copy(panel = panel)
}

private fun CampusMapUiState.roomSheets(onBuilding: (String) -> Unit, actions: EntranceActions): RoomSheetSources =
    RoomSheetSources(onBuilding, ShownBarriers(barriers), actions)

@Composable
private fun ScreenSheet.RoomLayout(
    room: OpenRoom?,
    roomSheets: RoomSheetSources,
    modifier: Modifier = Modifier,
    content: @Composable (isLarge: Boolean) -> Unit,
) {
    val isLarge = LocalDensity.current.fontScale >= LARGE_FONT_SCALE
    val found = room?.run { card.found }
    var isPlanShown by rememberSaveable(found?.building, found?.run { this.room.id }) { mutableStateOf(false) }
    BackHandler(enabled = room != null) {
        if (isLarge && isPlanShown) isPlanShown = false else room?.run { onClose() }
    }
    DisposableEffect(found, signs) {
        if (found != null) signs.close()
        onDispose {}
    }
    if (room != null && isLarge && !isPlanShown) {
        RoomCardLayer(room, roomSheets, modifier) { isPlanShown = true }
    } else {
        Box(modifier.fillMaxSize()) { content(isLarge) }
    }
}

@Composable
private fun MapArea(
    state: CampusMapUiState,
    sheet: ScreenSheet,
    search: SearchFocus,
    content: @Composable (ShownBarriers) -> Unit,
) {
    val shown = ShownBarriers(state.barriers.takeUnless { sheet.isCardOpen }.orEmpty())
    val modifier =
        Modifier
            .fillMaxSize()
            .releasesSearchOnTouch(search)
            .then(if (sheet.isEntranceFull) Modifier.clearAndSetSemantics {} else Modifier)
    Box(modifier) {
        content(shown)
        BarrierDescriptions(shown)
    }
}

@Composable
private fun CampusMap(
    ui: CampusMapUiState,
    barriers: ShownBarriers,
    focus: MapFocus?,
    areas: MapAreas,
    top: Dp?,
    cameraTop: Dp,
    sheet: ScreenSheet,
    mapView: CampusMapView,
    motion: MapMotion?,
    plan: PlanLink,
    onBuildingClick: (String) -> Unit,
    onEmptyMapClick: () -> Unit,
    onRoomClick: (FoundRoom) -> Unit,
    entrance: EntranceActions,
    room: OpenRoom?,
    isRoomFull: Boolean,
    modifier: Modifier = Modifier,
) {
    val isKeyboardOpen = rememberKeyboardOpenFlag()
    val topInset = WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
    var size by remember { mutableStateOf(IntSize.Zero) }
    val signs = sheet.signs
    val isRoomOnMap = room != null && !isRoomFull
    val (state, rooms) =
        rememberCampusScene(
            ui = ui,
            focus = focus,
            areas = areas,
            sheet = sheet,
            size = size,
            top = cameraTop,
            motion = motion,
            room = RoomScene(room = room, isOnMap = isRoomOnMap, plan = plan.shown),
        )
    Box(modifier.consumeWindowInsets(topInset).onSizeChanged { size = it }) {
        val move = state.move
        if (state.scene != null && move != null && state.limits != null) {
            val pinned = signs.rememberPinnedEntrance(ui.catalog, state.scene.selection.entrance)
            mapView.Show(
                data = state.scene.data,
                selection = signs.overlay(base = state.scene.selection, plan = plan.shown, room = room, pin = pinned),
                move = move,
                inset = state.inset,
                onCameraIdle = { view ->
                    areas.acknowledge()
                    plan.onView(view.also { state.keep(it) }.also(rooms::onIdle))
                },
                onBuildingsTouch = { state.taps.touch(it, onBuildingClick, onEmptyMapClick).also { signs.close() } },
                onSignTouch =
                    signs.signTouch(room, state.taps::dismiss) {
                        plan.shown?.roomAt(it)?.let(rooms.tapped(onRoomClick))
                    },
                modifier = Modifier.fillMaxSize(),
            )
            pinned?.let { EntranceDescription(it) }
            CampusMapPanels(
                state = state,
                catalog = ui.catalog,
                areas = areas,
                top = top,
                topInset = topInset,
                sheet = sheet,
                isKeyboardOpen = isKeyboardOpen,
                motion = motion,
                roomCover = rooms.mapCover,
                plan = plan,
            ) {
                val shown = room.takeIf { isRoomOnMap && !isKeyboardOpen }
                val open =
                    OpenSign(sheet = sheet, barriers = barriers, catalog = ui.catalog, plan = plan.shown, room = shown)
                MapSheets(open = open, state = state, onBuilding = onBuildingClick, entranceActions = entrance) {
                        height,
                        card,
                    ->
                    signs.measureRoom(height = height, card = card)
                }
            }
        }
    }
}

private fun SignSheetState.overlay(
    base: MapSelection,
    plan: ShownPlan?,
    room: OpenRoom?,
    pin: SelectedEntrance?,
): MapSelection {
    val card = room?.card
    val building = card?.run { found.building }
    val floor = plan?.takeIf { it.building == building }?.floor
    val markers = floor?.let { card?.markersOn(it) }.orEmpty()
    return base.copy(entrance = pin, barrier = barrierId, plan = plan, roomSteps = markers)
}

private fun SignSheetState.signTouch(
    room: OpenRoom?,
    dismiss: () -> Unit,
    onRoom: (String) -> Unit,
): (TouchSign, String) -> Unit =
    { sign, id ->
        when (sign) {
            TouchSign.ROOM -> {
                onRoom(id)
            }

            TouchSign.ROOM_STEP -> {
                val step = id.toIntOrNull()
                if (room != null && step != null) room.onStep(step)
            }

            TouchSign.BARRIER, TouchSign.ENTRANCE, TouchSign.PLAN -> {
                room?.run { onClose() }
                open(sign, id)
                dismiss()
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
    room: RoomScene,
): Pair<MapSceneState, RoomView> {
    val width = with(LocalDensity.current) { size.width.toDp() }
    val view =
        rememberRoomView(scene = room, focus = focus, panel = sheet.panel, top = top, height = size.height)
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
        sheet = view.cover,
        roomCamera = view.camera,
    ) to view
}

/**
 * Plan piętra [shown] przy mapie: [onView] dostaje widok kamery, [onFloor] piętro z kolumny, a [onHide] zejście
 * mapy z ekranu.
 */
internal data class PlanLink(
    val shown: ShownPlan?,
    val onView: (MapCamera) -> Unit,
    val onFloor: (String, String) -> Unit,
    val onHide: () -> Unit,
)

@Composable
private fun rememberKeyboardOpenFlag(): Boolean {
    val ime = WindowInsets.ime
    val density = LocalDensity.current
    val isOpen by remember(ime, density) { derivedStateOf { ime.getBottom(density) > 0 } }
    return isOpen
}
