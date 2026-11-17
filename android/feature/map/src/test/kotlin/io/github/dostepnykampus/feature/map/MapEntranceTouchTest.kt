/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.activity.ComponentActivity
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.feature.map.camera.CampusMapView
import io.github.dostepnykampus.feature.map.camera.TouchSign
import io.github.dostepnykampus.feature.map.campus.CampusMapData
import io.github.dostepnykampus.feature.map.campus.campusMapData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Dotknięcie znaku wejścia na mapie V1 otwiera arkusz wejścia, na widoku zastępczym mapy.
 *
 * Wymagania: US2.1, US3.3, NFR-17, NFR-53
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MapEntranceTouchTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val opened = mutableListOf<String>()
    private val entrances = mutableListOf<String>()
    private var touchSign: (TouchSign, String) -> Unit = { _, _ -> }
    private var touchBuildings: (List<String>) -> Unit = {}
    private var selected: String? = null
    private val fakeMap =
        CampusMapView { _: CampusMapData, selection, _, _, _, onBuildingsTouch, onSignTouch, _: Modifier ->
            selected = selection.building
            touchBuildings = onBuildingsTouch
            touchSign = onSignTouch
        }
    private val repository =
        BuildingCatalogRepository(ioContext = EmptyCoroutineContext) {
            """{"type":"FeatureCollection","attribution":"© OpenStreetMap contributors","features":[""" +
                """{"type":"Feature","properties":{"kind":"building","symbols":["C-13"],"osm_id":"way/1"},""" +
                """"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}},""" +
                """{"type":"Feature","properties":{"kind":"entrance","symbols":["C-13"],"osm_id":"node/1",""" +
                """"entrance":"main","wheelchair":"yes","ref":"Główne"},""" +
                """"geometry":{"type":"Point","coordinates":[0,0]}}]}"""
        }

    /** Wiersz: V1, dotknięcie znaku wejścia otwiera arkusz, nie zaznacza budynku i nie otwiera V2. */
    @Test
    fun opensSheetWithoutSelectingBuilding() {
        start()
        compose.runOnIdle { touchSign(TouchSign.ENTRANCE, "node/1") }

        compose.onNodeWithText("Wejście, budynek C-13").assertExists()
        compose.onNodeWithText("Dostępne, wejście główne").assertExists()
        compose.runOnIdle { assertEquals("bez V2", emptyList<String>(), opened) }
        compose.runOnIdle { assertEquals("bez zaznaczenia budynku", null, selected) }
    }

    /** Wiersz: V1, „Zamknij” zamyka arkusz wejścia. Wymagania: NFR-53 */
    @Test
    fun closesSheetOnClose() {
        start()
        compose.runOnIdle { touchSign(TouchSign.ENTRANCE, "node/1") }
        compose.onNodeWithText("Zamknij").performClick()

        compose.onNodeWithText("Wejście, budynek C-13").assertDoesNotExist()
    }

    /** Wiersz: V1, dotknięcie pustej mapy zamyka arkusz wejścia. Wymagania: NFR-53 */
    @Test
    fun closesSheetOnEmptyMap() {
        start()
        compose.runOnIdle { touchSign(TouchSign.ENTRANCE, "node/1") }
        compose.onNodeWithText("Wejście, budynek C-13").assertExists()
        compose.runOnIdle { touchBuildings(emptyList()) }

        compose.onNodeWithText("Wejście, budynek C-13").assertDoesNotExist()
    }

    /** Wiersz: V1, „Wstecz” przy otwartym arkuszu wejścia zamyka arkusz i zostawia mapę. Wymagania: NFR-15 */
    @Test
    fun closesSheetOnBack() {
        start()
        compose.runOnIdle { touchSign(TouchSign.ENTRANCE, "node/1") }
        compose.onNodeWithText("Wejście, budynek C-13").assertExists()
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }

        compose.onNodeWithText("Wejście, budynek C-13").assertDoesNotExist()
        compose.runOnIdle { assertFalse("mapa zostaje", compose.activity.isFinishing) }
    }

    /** Wiersz: arkusz wejścia, „Sprawdź, którędy wejść” otwiera V3 i zamyka arkusz. Wymagania: US3.3 */
    @Test
    fun opensEntrancesFromSheet() {
        start()
        compose.runOnIdle { touchSign(TouchSign.ENTRANCE, "node/1") }
        compose.onNodeWithText("Sprawdź, którędy wejść").performClick()

        assertEquals("V3 budynku wejścia", listOf("C-13"), entrances)
        compose.onNodeWithText("Wejście, budynek C-13").assertDoesNotExist()
    }

    private fun start() {
        val viewModel =
            CampusMapViewModel(
                repository = repository,
                networkStatus = { flowOf(true) },
                mapListChoice = TestMapListChoice(),
                computeContext = UnconfinedTestDispatcher(),
                geometry = ::campusMapData,
                barriers = { emptyList() },
            )
        compose.setContent {
            DostepnyKampusTheme {
                CampusMapScreen(
                    onBuildingClick = { opened += it },
                    onEntrancesClick = { entrances += it },
                    mapView = fakeMap,
                    viewModel = viewModel,
                )
            }
        }
        val state = viewModel.uiState
        compose.waitUntil { state.value.catalog != null && state.value.mapData != null }
        compose.waitForIdle()
    }
}
