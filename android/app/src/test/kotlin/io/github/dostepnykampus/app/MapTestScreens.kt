/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import io.github.dostepnykampus.app.navigation.AppNavigator
import io.github.dostepnykampus.app.navigation.BuildingCard
import io.github.dostepnykampus.app.navigation.MapScreen
import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.feature.map.building.BuildingCardViewModel
import io.github.dostepnykampus.feature.map.camera.CampusMapView
import io.github.dostepnykampus.feature.map.navigation.BuildingRoute
import io.github.dostepnykampus.feature.map.navigation.MapRoute
import kotlinx.coroutines.flow.flowOf

/**
 * Mapa i karta budynku z katalogu [repository], zawsze w sieci, z podstawioną mapą [mapView];
 * pozostałe trasy rysuje [other].
 */
@Composable
internal fun AppNavigator.MapOrBuildingScreen(
    route: NavKey,
    repository: BuildingCatalogRepository,
    choice: TestMapListChoice,
    mapView: CampusMapView,
    other: @Composable AppNavigator.(NavKey) -> Unit,
) {
    when (route) {
        is MapRoute -> {
            val model = remember { inlineMapViewModel(repository, { flowOf(true) }, choice) }
            val state by model.uiState.collectAsStateWithLifecycle()
            MapScreen(
                focus = route.focus,
                state = state,
                onListChange = model::chooseList,
                onSearch = model::search,
                mapView = mapView,
            )
        }

        is BuildingRoute -> {
            val card = remember { BuildingCardViewModel(route.symbol, repository, { flowOf(true) }) }
            val state by card.uiState.collectAsStateWithLifecycle()
            BuildingCard(symbol = route.symbol, state = state, isOnline = true, onRetry = card::retry)
        }

        else -> {
            other(route)
        }
    }
}
