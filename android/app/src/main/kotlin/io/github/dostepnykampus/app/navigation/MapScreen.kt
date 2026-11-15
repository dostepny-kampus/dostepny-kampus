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
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dostepnykampus.app.LocalSheetPadding
import io.github.dostepnykampus.app.SheetSceneStrategy
import io.github.dostepnykampus.app.closeCardOnEmptyMap
import io.github.dostepnykampus.app.shownFocus
import io.github.dostepnykampus.feature.map.CampusMapScreen
import io.github.dostepnykampus.feature.map.CampusMapUiState
import io.github.dostepnykampus.feature.map.CampusMapViewModel
import io.github.dostepnykampus.feature.map.MapLibreMapView
import io.github.dostepnykampus.feature.map.camera.CampusMapView
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.navigation.MapFocus
import io.github.dostepnykampus.feature.map.plan.MapPlanViewModel
import io.github.dostepnykampus.feature.map.plan.ShownPlan

/**
 * V1 ze stanem [viewModel] i planem piętra [planViewModel] z grafu Hilt oraz zaznaczeniem [focus] z trasy; piętro
 * z zaznaczenia budynku staje się wybranym piętrem jego planu.
 *
 * Wymagania: US1, US3.3, NFR-19, US16.1
 */
@Composable
internal fun AppNavigator.HiltMapScreen(
    focus: MapFocus?,
    viewModel: CampusMapViewModel = hiltViewModel(),
    planViewModel: MapPlanViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val plan by planViewModel.shown.collectAsStateWithLifecycle()
    SideEffect { planViewModel.show(focus) }
    MapScreen(
        focus = focus,
        state = state,
        onListChange = viewModel::chooseList,
        onSearch = viewModel::search,
        plan = plan,
        onCamera = planViewModel::camera,
        onFloorChoose = planViewModel::chooseFloor,
    )
}

/**
 * V1 ze stanem [state] i zaznaczeniem [focus] z trasy, np. po „Wstecz” z V2 albo „Pokaż na mapie”,
 * z czynnościami podpiętymi do nawigatora; [onListChange] zapisuje widok, mapę albo listę,
 * a [onSearch] liczy wyszukiwanie w modelu widoku. Budynek z listy przy TalkBack otwiera V2
 * od razu na pełnym ekranie. Przyciski i podpis OSM stoją nad arkuszem V2.
 * [mapView] w aplikacji to MapLibre, testy podają podróbkę. [plan] to plan piętra przy środku mapy, a [onCamera]
 * dostaje widok mapy po ruchu kamery, a [onFloorChoose] piętro wybrane w kolumnie pięter.
 *
 * Wymagania: US1.1, US2.1, NFR-19
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
) {
    val accessibility = checkNotNull(LocalContext.current.getSystemService(AccessibilityManager::class.java))
    CampusMapScreen(
        state = state,
        onListChange = onListChange,
        onSearch = onSearch,
        onBuildingClick = { openBuilding(it) },
        focus = shownFocus(focus, isSheetAbove = SheetSceneStrategy.isSheet(current)),
        contentPadding = LocalSheetPadding.current,
        onEmptyMapClick = { closeCardOnEmptyMap() },
        onListBuildingClick = { openBuildingFromList(it, accessibility.isTouchExplorationEnabled) },
        mapView = mapView,
        motion = mapMotion,
        plan = plan,
        onCamera = onCamera,
        onFloorChoose = onFloorChoose,
    )
}
