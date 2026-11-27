/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.building.Floor
import io.github.dostepnykampus.feature.map.camera.CampusMapView
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.campus.CampusMapData
import io.github.dostepnykampus.feature.map.campus.ShownPlan
import io.github.dostepnykampus.feature.map.campus.campusMapData
import io.github.dostepnykampus.feature.map.navigation.MapFocus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * Ekran V1 z widokiem zastępczym, który nigdy nie zgłasza końca gestu: plan piętra dostaje widok kamery także po ruchu
 * wykonanym przez aplikację, np. po „Pokaż na planie” albo po wyborze budynku, i traci go, gdy mapa schodzi z ekranu.
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
    private var idle: (MapCamera) -> Unit = {}
    private val fakeMap =
        CampusMapView { _: CampusMapData, _, move, _, _, onCameraIdle, _, _, _: Modifier ->
            moves += move.camera
            idle = onCameraIdle
        }
    private val repository = unitSquareCatalog("C-13")
    private val viewModel by lazy {
        CampusMapViewModel(
            repository = repository,
            networkStatus = { flowOf(true) },
            mapListChoice = TestMapListChoice(),
            computeContext = UnconfinedTestDispatcher(),
            geometry = ::campusMapData,
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

    /**
     * Wiersz: R2, plan budynku A-1 był na mapie, osoba przechodzi na „Lista budynków” i wybiera budynek bez planu →
     *   karta wjeżdża nad mapą bez kolumny pięter, także w pierwszej klatce.
     *
     * Wymagania: US16.1
     */
    @Test
    fun hidesOldFloorsAfterList() {
        var plan by mutableStateOf<ShownPlan?>(OLD_PLAN)
        var isList by mutableStateOf(false)
        var cover by mutableStateOf(0.dp)
        compose.setContent {
            val state by viewModel.uiState.collectAsState()
            DostepnyKampusTheme {
                CampusMapScreen(
                    state = state.copy(isListSaved = isList),
                    onListChange = {},
                    onSearch = {},
                    onBuildingClick = {},
                    focus = MapFocus.Building("C-13").takeIf { cover > 0.dp },
                    contentPadding = PaddingValues(bottom = cover),
                    mapView = fakeMap,
                    plan = plan,
                    onMapHide = { plan = null },
                )
            }
        }
        compose.waitUntil { moves.isNotEmpty() }
        compose.onNodeWithContentDescription(BASEMENT).assertExists()
        isList = true
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        cover = CARD
        repeat(FRAMES) {
            compose.mainClock.advanceTimeByFrame()
            compose.onNodeWithContentDescription(BASEMENT).assertDoesNotExist()
        }
    }

    /** Bez planu legenda na ekranie nie ma części „Plan piętra”, a z planem ją pokazuje. Wymagania: US1.4, US16.1 */
    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun showsPlanLegendOnlyWithPlan() {
        var plan by mutableStateOf<ShownPlan?>(null)
        compose.setContent {
            val state by viewModel.uiState.collectAsState()
            DostepnyKampusTheme {
                CampusMapScreen(
                    state = state,
                    onListChange = {},
                    onSearch = {},
                    onBuildingClick = {},
                    mapView = fakeMap,
                    plan = plan,
                )
            }
        }
        compose.waitUntil { moves.isNotEmpty() }
        compose.onNodeWithContentDescription(INFO).performClick()
        compose.onNodeWithText("Legenda").assertExists()
        compose.onNodeWithText(PLAN_HEADING).assertDoesNotExist()
        compose.onNodeWithText("Zamknij").performClick()
        plan = OLD_PLAN
        compose.waitForIdle()
        compose.onNodeWithContentDescription(INFO).performClick()
        compose
            .onNodeWithText(PLAN_HEADING)
            .performScrollTo()
            .assertIsDisplayed()
    }

    /** Napis nad budynkiem podstawowym od powiększenia 17, bez karty i kolumny pięter. Wymagania: US16.2, BR-G1 */
    @Test
    fun showsMissingPlanFromZoom17() {
        var cover by mutableStateOf(0.dp)
        var hasServerList by mutableStateOf(false)
        compose.setContent {
            val state by viewModel.uiState.collectAsState()
            DostepnyKampusTheme {
                CampusMapScreen(
                    state = state.copy(catalog = state.catalog?.copy(hasServerList = hasServerList)),
                    onListChange = {},
                    onSearch = {},
                    onBuildingClick = {},
                    focus = MapFocus.Building("C-13").takeIf { cover > 0.dp },
                    contentPadding = PaddingValues(bottom = cover),
                    mapView = fakeMap,
                    plan = OLD_PLAN,
                )
            }
        }
        compose.waitUntil { moves.isNotEmpty() }
        val near = MapCamera(center = GeoPoint(longitude = 0.75, latitude = 0.25), zoom = 16.9)
        compose.runOnIdle { idle(near.copy(zoom = 17.0)) }
        compose.onNodeWithText(MISSING_PLAN).assertDoesNotExist()
        compose.runOnIdle { hasServerList = true }
        compose.onNodeWithText(MISSING_PLAN).assertIsDisplayed()
        compose.runOnIdle { idle(near) }
        compose.onNodeWithText(MISSING_PLAN).assertDoesNotExist()
        compose.runOnIdle { idle(near.copy(zoom = 17.0)) }
        compose.onNodeWithText(MISSING_PLAN).assertIsDisplayed()
        compose.onNodeWithContentDescription(BASEMENT).assertDoesNotExist()
        val street = MapCamera(center = GeoPoint(longitude = 0.25, latitude = 0.75), zoom = 17.0)
        compose.runOnIdle { idle(street) }
        compose.onNodeWithText(MISSING_PLAN).assertDoesNotExist()
        compose.runOnIdle { idle(near.copy(zoom = 17.0)) }
        compose.runOnIdle { cover = CARD }
        compose.onNodeWithText(MISSING_PLAN).assertDoesNotExist()
    }

    private companion object {
        const val BASEMENT = "Piwnica"
        const val INFO = "Legenda i źródła mapy"
        const val PLAN_HEADING = "Plan piętra"
        const val MISSING_PLAN = "Nie mamy planu pięter tego budynku."
        const val FRAMES = 10
        val CARD = 300.dp
        val OLD_PLAN =
            ShownPlan(
                building = "A-1",
                floors =
                    listOf(
                        Floor(id = "P-1", label = BASEMENT, order = -1),
                        Floor(id = "P0", label = "Parter", order = 0),
                        Floor(id = "P3", label = "3 piętro", order = 3),
                    ),
                floor = "P0",
                rooms = emptyMap(),
            )
    }
}
