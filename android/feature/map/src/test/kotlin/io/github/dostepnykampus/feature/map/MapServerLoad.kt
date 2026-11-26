/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.feature.map.campus.MapServer
import io.github.dostepnykampus.feature.map.campus.campusMapData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlin.coroutines.EmptyCoroutineContext

private val CATALOG_WITH_ONE_BUILDING =
    BuildingCatalogRepository(EmptyCoroutineContext) {
        """{"type":"FeatureCollection","attribution":"© OpenStreetMap contributors","features":[""" +
            """{"type":"Feature","properties":{"kind":"building","symbols":["C-13"],"osm_id":"way/1"},""" +
            """"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}}]}"""
    }

/** Model widoku mapy z jednym budynkiem, stanem sieci [online] i danymi [server], obserwowany w tle testu. */
internal fun TestScope.mapWithServer(online: Flow<Boolean>, server: MapServer): CampusMapViewModel {
    val viewModel =
        CampusMapViewModel(
            repository = CATALOG_WITH_ONE_BUILDING,
            networkStatus = { online },
            mapListChoice = TestMapListChoice(),
            computeContext = EmptyCoroutineContext,
            geometry = ::campusMapData,
            server = server,
        )
    backgroundScope.launch { viewModel.uiState.collect {} }
    return viewModel
}
