/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.activity.ComponentActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.feature.map.camera.CampusMapView
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.campus.CampusMapData
import io.github.dostepnykampus.feature.map.navigation.MapFocus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Ekran V1 z widokiem zastępczym, który nigdy nie zgłasza końca gestu: plan piętra dostaje widok kamery także po ruchu
 * wykonanym przez aplikację, np. po „Pokaż na planie” albo po wyborze budynku.
 *
 * Wymagania: US16.1
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MapPlanCameraTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val moves = mutableListOf<MapCamera>()
    private val reported = mutableListOf<MapCamera>()
    private val fakeMap =
        CampusMapView { _: CampusMapData, _, move, _, _, _, _, _: Modifier ->
            moves += move.camera
        }
    private val repository = unitSquareCatalog("C-13")
    private val viewModel by lazy {
        CampusMapViewModel(
            repository = repository,
            networkStatus = { flowOf(true) },
            mapListChoice = TestMapListChoice(),
            computeContext = UnconfinedTestDispatcher(),
        )
    }

    /**
     * Wiersz: R2, budynek C-13 zaznaczony, kamerę przesuwa aplikacja, a osoba nie robi gestu → plan dostaje widok,
     *   do którego kamera zmierza.
     *
     * Wymagania: US16.1
     */
    @Test
    fun reportsCameraMovedByApp() {
        compose.setContent {
            val state by viewModel.uiState.collectAsState()
            DostepnyKampusTheme {
                CampusMapScreen(
                    state = state,
                    onListChange = {},
                    onSearch = {},
                    onBuildingClick = {},
                    focus = MapFocus.Building("C-13"),
                    mapView = fakeMap,
                    onCamera = { reported += it },
                )
            }
        }
        compose.waitUntil { moves.isNotEmpty() && reported.isNotEmpty() }
        compose.waitForIdle()
        assertEquals("plan nie dostał widoku po ruchu aplikacji", moves.last(), reported.last())
    }
}
