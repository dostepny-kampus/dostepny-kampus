/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.navigation

import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dostepnykampus.app.LocalSheetPadding
import io.github.dostepnykampus.app.SheetLayout
import io.github.dostepnykampus.app.SheetSceneStrategy
import io.github.dostepnykampus.app.closeCardOnEmptyMap
import io.github.dostepnykampus.app.isFullOnlySheet
import io.github.dostepnykampus.app.shownFocus
import io.github.dostepnykampus.core.model.room.FoundRoom
import io.github.dostepnykampus.feature.capture.navigation.ReportPlace
import io.github.dostepnykampus.feature.map.CampusMapScreen
import io.github.dostepnykampus.feature.map.CampusMapUiState
import io.github.dostepnykampus.feature.map.CampusMapViewModel
import io.github.dostepnykampus.feature.map.MapLibreMapView
import io.github.dostepnykampus.feature.map.camera.CampusMapView
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.entrance.BuildingEntrancesViewModel
import io.github.dostepnykampus.feature.map.entrance.EntranceActions
import io.github.dostepnykampus.feature.map.entrance.EntranceLiftViewModel
import io.github.dostepnykampus.feature.map.entrance.focusedBuilding
import io.github.dostepnykampus.feature.map.entrance.withBuildingEntrances
import io.github.dostepnykampus.feature.map.navigation.BuildingRoute
import io.github.dostepnykampus.feature.map.navigation.MapFocus
import io.github.dostepnykampus.feature.map.navigation.MapRoute
import io.github.dostepnykampus.feature.map.plan.MapPlanViewModel
import io.github.dostepnykampus.feature.map.plan.PlanReport
import io.github.dostepnykampus.feature.map.plan.ShownPlan
import io.github.dostepnykampus.feature.map.room.OpenRoom
import io.github.dostepnykampus.feature.map.room.RoomCardViewModel

/**
 * V1 ze stanem [viewModel], planem piętra [planViewModel] i wejściami z serwera [entrancesViewModel] z grafu Hilt oraz
 * zaznaczeniem [focus] z trasy; piętro z zaznaczenia budynku staje się wybranym piętrem jego planu, a wejścia
 * zaznaczonego budynku z karty z serwera zastępują w arkuszu wejścia i pinezce wejścia tego budynku z katalogu mapy.
 *
 * Wymagania: US1, US3.3, NFR-19, US16.1, US2.1
 */
@Composable
internal fun AppNavigator.HiltMapScreen(
    focus: MapFocus?,
    viewModel: CampusMapViewModel = hiltViewModel(),
    planViewModel: MapPlanViewModel = hiltViewModel(),
    entrancesViewModel: BuildingEntrancesViewModel = hiltViewModel(),
    liftViewModel: EntranceLiftViewModel = hiltViewModel(),
    mapView: CampusMapView = MapLibreMapView,
    roomViewModel: RoomCardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val plan by planViewModel.shown.collectAsStateWithLifecycle()
    val entrances by entrancesViewModel.shown.collectAsStateWithLifecycle()
    val lift by liftViewModel.shown.collectAsStateWithLifecycle()
    val card by roomViewModel.shown.collectAsStateWithLifecycle()
    val room = card?.takeIf { current is MapRoute }
    val found = room?.found
    val roomFocus = remember(found) { found?.focus() }
    val selected = roomFocus ?: focus
    val shown = remember(state, entrances) { state.copy(catalog = state.catalog?.withBuildingEntrances(entrances)) }
    SideEffect {
        planViewModel.show(selected)
        entrancesViewModel.show(focusedBuilding(selected))
    }
    MapScreen(
        focus = selected,
        state = shown,
        onListChange = viewModel::chooseList,
        onSearch = viewModel::search,
        mapView = mapView,
        plan = plan,
        onCamera = planViewModel::camera,
        onFloorChoose = planViewModel::chooseFloor,
        onMapHide = planViewModel::hide,
        room = room?.let { OpenRoom(card = it, onClose = roomViewModel::close) },
        onOpenRoom = roomViewModel::show,
        entrance =
            EntranceActions(
                onShow = liftViewModel::show,
                lift = lift,
                onBuildingSelect = planViewModel::selectEntrance,
            ),
    )
}

/**
 * V1 ze stanem [state] i zaznaczeniem [focus] z trasy; [onListChange] zapisuje widok, mapę albo listę, [onSearch]
 * liczy wyszukiwanie. Budynek z listy przy TalkBack otwiera V2 od razu na pełnym ekranie. Przyciski i podpis OSM stoją
 * nad arkuszem V2; bez arkusza i pod arkuszem na cały ekran przy dużej czcionce V1 liczy widok bez arkusza.
 * [mapView] to MapLibre albo podróbka w testach, [plan] to plan piętra przy środku mapy, [onCamera] dostaje widok po
 * ruchu kamery, [onFloorChoose] piętro wybrane w kolumnie pięter, a [onMapHide] zejście mapy z ekranu. Zgłoszenie
 * przy wejściu z arkusza wejścia prowadzi do V4 albo L1, a [entrance] podaje arkuszowi windę za drzwiami.
 *
 * Wymagania: US1.1, US2.1, NFR-19, US3.3, US4.6, US14.2, US18.1
 */
@Composable
internal fun AppNavigator.MapScreen(
    focus: MapFocus?,
    state: CampusMapUiState,
    onListChange: (Boolean) -> Unit,
    onSearch: (String) -> Unit,
    mapView: CampusMapView = MapLibreMapView,
    plan: ShownPlan? = null,
    onCamera: (MapCamera) -> Unit = {},
    onFloorChoose: (String, String) -> Unit = { _, _ -> },
    onMapHide: () -> Unit = {},
    entrance: EntranceActions = EntranceActions(),
    room: OpenRoom? = null,
    onOpenRoom: ((FoundRoom) -> Unit)? = null,
) {
    val accessibility = checkNotNull(LocalContext.current.getSystemService(AccessibilityManager::class.java))
    val isFullOnly = isFullOnlySheet(LocalDensity.current.fontScale)
    val isSheetAbove = SheetSceneStrategy.isSheet(current) && !isFullOnly
    CampusMapScreen(
        state = state,
        onListChange = onListChange,
        onSearch = onSearch,
        onBuildingClick = { symbol ->
            room?.onClose?.invoke()
            openBuilding(symbol)
        },
        focus = shownFocus(focus, isSheetAbove = isSheetAbove),
        contentPadding = if (isSheetAbove) LocalSheetPadding.current else SheetLayout(),
        onEmptyMapClick = { closeCardOnEmptyMap() },
        onListBuildingClick = { openBuildingFromList(it, accessibility.isTouchExplorationEnabled) },
        entranceActions = entrance.copy(onReport = { report(it) }, onPlanReport = { report(place = it.toPlace()) }),
        mapView = mapView,
        motion = mapMotion,
        plan = plan,
        onCamera = onCamera,
        onFloorChoose = onFloorChoose,
        onMapHide = onMapHide,
        onCardReturn = { returnToCard(it) },
        sheetCover = LocalSheetPadding.current.shownCover.takeUnless { isFullOnly },
        onRoomClick = onOpenRoom ?: { found -> showOnMap(found.focus()) },
        room = room,
    )
}

/**
 * Powrót z arkusza znaku do V2 budynku [symbol], którą arkusz zastąpił: z wysokością i przewinięciem
 * zapamiętanymi przez [keepCard], gdy to karta tego budynku, inaczej jak [AppNavigator.openBuilding].
 *
 * Wymagania: US2.8, US3.3
 */
internal fun AppNavigator.returnToCard(symbol: String) {
    openCard(keptCard?.takeIf { it.symbol == symbol } ?: BuildingRoute(symbol))
}

/** Zapamiętuje V2 [card] z wysokością i przewinięciem z chwili, gdy zeszła z ekranu. Wymagania: US2.8 */
internal fun AppNavigator.keepCard(card: BuildingRoute) {
    keptCard = card
}

private fun PlanReport.toPlace(): ReportPlace =
    ReportPlace(
        category = category,
        buildingSymbol = building,
        floorId = floorId,
        floorLabel = floorLabel,
        latitude = point.latitude,
        longitude = point.longitude,
    )

private fun FoundRoom.focus(): MapFocus =
    MapFocus.Building(symbol = building, command = System.nanoTime(), floor = floor.id, room = room.id)
