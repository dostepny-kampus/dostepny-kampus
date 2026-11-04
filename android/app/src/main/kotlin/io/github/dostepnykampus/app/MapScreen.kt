/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dostepnykampus.app.navigation.AppNavigator
import io.github.dostepnykampus.feature.map.CampusMapScreen
import io.github.dostepnykampus.feature.map.CampusMapUiState
import io.github.dostepnykampus.feature.map.CampusMapView
import io.github.dostepnykampus.feature.map.CampusMapViewModel
import io.github.dostepnykampus.feature.map.MapLibreMapView

/** V1 ze stanem [viewModel] z grafu Hilt i obrysem budynku [focus] z trasy (US1, NFR-19). */
@Composable
internal fun AppNavigator.HiltMapScreen(focus: String?, viewModel: CampusMapViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MapScreen(focus = focus, state = state, onListChange = viewModel::chooseList)
}

/**
 * V1 ze stanem [state] i obrysem budynku [focus] z trasy, np. po „Wstecz” z V2 albo „Pokaż na mapie” (US1.1, US2.1),
 * z czynnościami podpiętymi do nawigatora; [onListChange] zapisuje wybór w przełączniku „Mapa” i „Lista budynków”
 * (NFR-19). Przyciski i podpis OSM stoją nad arkuszem V2 (decyzja 87). [mapView] w aplikacji to MapLibre, testy
 * podają podróbkę.
 */
@Composable
internal fun AppNavigator.MapScreen(
    focus: String?,
    state: CampusMapUiState,
    onListChange: (Boolean) -> Unit,
    mapView: CampusMapView = MapLibreMapView,
) {
    CampusMapScreen(
        state = state,
        onListChange = onListChange,
        onBuildingClick = { openBuilding(it) },
        focus = focus,
        contentPadding = LocalSheetPadding.current,
        onEmptyMapClick = { closeCardOnEmptyMap() },
        mapView = mapView,
    )
}
