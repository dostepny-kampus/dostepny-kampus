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
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.feature.map.camera.TouchSign
import io.github.dostepnykampus.feature.map.campus.campusMapData
import io.github.dostepnykampus.feature.map.navigation.MapFocus
import io.github.dostepnykampus.feature.map.plan.SIGN_PLAN
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Dotknięcie znaku toalety dostosowanej na planie piętra otwiera arkusz, na widoku zastępczym mapy.
 *
 * Wymagania: US16.1, NFR-17, NFR-53
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MapPlanSignTouchTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val opened = mutableListOf<String>()
    private val map = TouchableMap()

    /** Wiersz: V1, dotknięcie toalety na planie otwiera arkusz bez zaznaczenia budynku; pusta mapa go zamyka. */
    @Test
    fun opensSheetAndClosesOnEmptyMap() {
        start()
        compose.runOnIdle { map.touchSign(TouchSign.PLAN, "wc") }

        compose.onNodeWithText("Toaleta dostosowana").assertExists()
        val untouched = null to emptyList<String>()
        compose.runOnIdle { assertEquals("bez zaznaczenia budynku i V2", untouched, map.selected to opened.toList()) }
        compose.runOnIdle { map.touchBuildings(emptyList()) }
        compose.onNodeWithText("Toaleta dostosowana").assertDoesNotExist()
    }

    /** Wiersz: V1, „Wstecz” przy otwartym arkuszu toalety zamyka arkusz. Wymagania: NFR-15 */
    @Test
    fun closesSheetOnBack() {
        start()
        compose.runOnIdle { map.touchSign(TouchSign.PLAN, "wc") }
        compose.onNodeWithText("Toaleta dostosowana").assertExists()
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }

        compose.onNodeWithText("Toaleta dostosowana").assertDoesNotExist()
    }

    /** Wiersz: V1, toaleta przy otwartej karcie budynku otwiera arkusz w miejsce karty; „Wstecz” wraca do karty. */
    @Test
    fun replacesBuildingCard() {
        start(isCardOpen = true)
        compose.runOnIdle { map.touchSign(TouchSign.PLAN, "wc") }
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.runOnIdle { assertEquals("karta zamknięta i otwarta znów", listOf("bez karty", "C-13"), opened) }
    }

    /** Wiersz: V1, toaleta przy otwartej karcie budynku, „Zamknij” zamyka arkusz razem z kartą. Wymagania: NFR-53 */
    @Test
    fun closeLeavesMapWithoutCard() {
        start(isCardOpen = true)
        compose.runOnIdle { map.touchSign(TouchSign.PLAN, "wc") }
        compose.onNodeWithText("Zamknij").performClick()
        compose.runOnIdle { assertEquals("karta zamknięta", listOf("bez karty"), opened) }
    }

    private fun start(isCardOpen: Boolean = false) {
        val viewModel =
            CampusMapViewModel(
                repository = unitSquareCatalog("C-13"),
                networkStatus = { flowOf(true) },
                mapListChoice = TestMapListChoice(),
                computeContext = UnconfinedTestDispatcher(),
                geometry = ::campusMapData,
            )
        compose.setContent {
            val state by viewModel.uiState.collectAsState()
            DostepnyKampusTheme {
                CampusMapScreen(
                    state = state,
                    onListChange = {},
                    onSearch = {},
                    onBuildingClick = { opened += it },
                    mapView = map.view,
                    plan = SIGN_PLAN,
                    focus = MapFocus.Building("C-13").takeIf { isCardOpen },
                    contentPadding = PaddingValues(bottom = if (isCardOpen) 200.dp else 0.dp),
                    onEmptyMapClick = { opened += "bez karty" },
                )
            }
        }
        compose.waitUntil { viewModel.uiState.value.mapData != null }
        compose.waitForIdle()
    }
}
