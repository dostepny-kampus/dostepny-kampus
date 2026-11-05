/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dostepnykampus.app.navigation.AppNavigator
import io.github.dostepnykampus.app.navigation.openBuildingFromList
import io.github.dostepnykampus.feature.map.CampusMapScreen
import io.github.dostepnykampus.feature.map.CampusMapUiState
import io.github.dostepnykampus.feature.map.CampusMapView
import io.github.dostepnykampus.feature.map.CampusMapViewModel
import io.github.dostepnykampus.feature.map.MapLibreMapView
import io.github.dostepnykampus.feature.map.navigation.MapFocus

/** V1 ze stanem [viewModel] z grafu Hilt i zaznaczeniem [focus] z trasy (US1, US3.3, NFR-19). */
@Composable
internal fun AppNavigator.HiltMapScreen(focus: MapFocus?, viewModel: CampusMapViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MapScreen(focus = focus, state = state, onListChange = viewModel::chooseList)
}

/**
 * V1 ze stanem [state] i zaznaczeniem [focus] z trasy, np. po „Wstecz” z V2 albo „Pokaż na mapie” (US1.1, US2.1),
 * z czynnościami podpiętymi do nawigatora; [onListChange] zapisuje wybór w przełączniku „Mapa” i „Lista budynków”
 * (NFR-19). Budynek z listy przy TalkBack otwiera V2 od razu na pełnym ekranie (NFR-19, decyzja 115). Przyciski
 * i podpis OSM stoją nad arkuszem V2 (decyzja 87). [mapView] w aplikacji to MapLibre, testy podają podróbkę.
 */
@Composable
internal fun AppNavigator.MapScreen(
    focus: MapFocus?,
    state: CampusMapUiState,
    onListChange: (Boolean) -> Unit,
    mapView: CampusMapView = MapLibreMapView,
) {
    val accessibility = checkNotNull(LocalContext.current.getSystemService(AccessibilityManager::class.java))
    CampusMapScreen(
        state = state,
        onListChange = onListChange,
        onBuildingClick = { openBuilding(it) },
        focus = (focus as? MapFocus.Building)?.symbol,
        show = (focus as? MapFocus.Building)?.takeIf { it.isShown }?.symbol,
        entrance = focus as? MapFocus.Entrance,
        contentPadding = LocalSheetPadding.current,
        onEmptyMapClick = { closeCardOnEmptyMap() },
        onListBuildingClick = { openBuildingFromList(it, accessibility.isTouchExplorationEnabled) },
        mapView = mapView,
    )
}
