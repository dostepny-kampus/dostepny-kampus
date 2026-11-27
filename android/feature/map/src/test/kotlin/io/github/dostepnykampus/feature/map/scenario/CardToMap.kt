/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.scenario

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dostepnykampus.core.data.barrier.AcceptedBarrier
import io.github.dostepnykampus.core.data.barrier.AcceptedBarriers
import io.github.dostepnykampus.core.data.building.ServerEntrances
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.designsystem.MapColors
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.feature.map.CampusMapScreen
import io.github.dostepnykampus.feature.map.building.BuildingCardScreen
import io.github.dostepnykampus.feature.map.building.BuildingCardViewModel
import io.github.dostepnykampus.feature.map.building.CardMode
import io.github.dostepnykampus.feature.map.camera.CameraTransition
import io.github.dostepnykampus.feature.map.camera.CampusMapView
import io.github.dostepnykampus.feature.map.camera.MapInset
import io.github.dostepnykampus.feature.map.campus.CampusMapData
import io.github.dostepnykampus.feature.map.campus.SelectedEntrance
import io.github.dostepnykampus.feature.map.entrance.BuildingEntrances
import io.github.dostepnykampus.feature.map.entrance.EntranceActions
import io.github.dostepnykampus.feature.map.entrance.EntranceLift
import io.github.dostepnykampus.feature.map.entrance.withBuildingEntrances
import io.github.dostepnykampus.feature.map.navigation.MapFocus
import io.github.dostepnykampus.feature.map.testMapViewModel
import io.github.dostepnykampus.feature.map.unitSquareCatalog
import kotlinx.coroutines.flow.flowOf

/** Wejście [id] z serwera z nazwą [name] budynku [symbol] w punkcie (0,3; 0,4). Wymagania: US2.1, US3.3 */
internal fun serverEntrance(id: String, symbol: String, name: String, marking: EntranceMarking): Entrance =
    Entrance(
        id = id,
        buildingSymbol = symbol,
        name = name,
        isMain = false,
        type = null,
        location = GeoPoint(longitude = 0.3, latitude = 0.4),
        marking = marking,
    )

/**
 * Karta budynku [symbol], którą „Pokaż wejście na mapie” i wiersz wejścia zastępują mapą V1 z arkuszem wejścia, na
 * widoku zastępczym mapy. Wejścia z serwera zastępują wejścia katalogu jak w aplikacji.
 *
 * Wymagania: US2.1, US3.3
 */
internal class CardToMap(private val compose: ComposeContentTestRule, private val symbol: String) {
    private var focus by mutableStateOf<MapFocus?>(null)

    var pinned: SelectedEntrance? = null
        private set

    var camera: Pair<CameraTransition, MapInset>? = null
        private set

    lateinit var mapColors: MapColors
        private set

    private val view =
        CampusMapView { _: CampusMapData, selection, move, inset, _, _, _, _, _: Modifier ->
            pinned = selection.entrance
            camera = move to inset
        }

    fun show(entrances: List<Entrance>, barriers: List<AcceptedBarrier> = emptyList(), lift: EntranceLift? = null) {
        val repository = unitSquareCatalog(symbol)
        val card =
            BuildingCardViewModel(
                symbol = symbol,
                repository = repository,
                networkStatus = { flowOf(true) },
                serverEntrances = ServerEntrances { entrances },
                acceptedBarriers = AcceptedBarriers { barriers },
            )
        val map = testMapViewModel(repository)
        compose.setContent {
            DostepnyKampusTheme {
                mapColors = DostepnyKampusTheme.mapColors
                val state by map.uiState.collectAsStateWithLifecycle()
                val fetched = BuildingEntrances(symbol = symbol, entrances = entrances)
                val shown = state.copy(catalog = state.catalog?.withBuildingEntrances(fetched), barriers = barriers)
                val chosen = focus
                if (chosen == null) {
                    BuildingCardScreen(
                        symbol = symbol,
                        mode = CardMode.Expanded,
                        onShowOnMap = {},
                        onBack = {},
                        onClose = {},
                        onShowEntrance = { id -> focus = MapFocus.Entrance(id = id, buildingSymbol = symbol) },
                        viewModel = card,
                    )
                } else {
                    CampusMapScreen(
                        state = shown,
                        onListChange = {},
                        onSearch = {},
                        onBuildingClick = {},
                        focus = chosen,
                        entranceActions = EntranceActions(lift = lift),
                        mapView = view,
                    )
                }
            }
        }
        compose.waitUntil { map.uiState.value.mapData != null }
    }

    fun tap(target: SemanticsMatcher) {
        val clickable = hasClickAction() and target
        compose.waitUntil { compose.onAllNodes(clickable).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(clickable).performScrollTo().performClick()
        compose.waitForIdle()
    }

    fun backToCard() {
        compose.runOnIdle { focus = null }
        compose.waitForIdle()
    }
}
