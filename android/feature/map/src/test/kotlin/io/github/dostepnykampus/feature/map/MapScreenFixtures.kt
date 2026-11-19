/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.feature.map.camera.CampusMapView
import io.github.dostepnykampus.feature.map.campus.campusMapData
import io.github.dostepnykampus.feature.map.navigation.MapFocus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher

/** Model mapy V1 nad katalogiem [repository], z siecią, bez barier i sal; wybór „Mapa” albo „Lista” z [choice]. */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun testMapViewModel(
    repository: BuildingCatalogRepository,
    choice: TestMapListChoice = TestMapListChoice(),
): CampusMapViewModel =
    CampusMapViewModel(
        repository = repository,
        networkStatus = { flowOf(true) },
        mapListChoice = choice,
        computeContext = UnconfinedTestDispatcher(),
        geometry = ::campusMapData,
    )

/** Ekran mapy V1 z modelem [viewModel] i zaznaczeniem [focus] na widoku [map]; czeka na katalog i dane mapy. */
internal fun ComposeContentTestRule.showMap(
    viewModel: CampusMapViewModel,
    map: CampusMapView,
    focus: MapFocus? = null,
) {
    setContent {
        DostepnyKampusTheme {
            CampusMapScreen(onBuildingClick = {}, focus = focus, mapView = map, viewModel = viewModel)
        }
    }
    val state = viewModel.uiState
    waitUntil { state.value.catalog != null && state.value.mapData != null }
    waitForIdle()
}
