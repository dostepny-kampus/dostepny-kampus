/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.feature.map.camera.TouchSign
import io.github.dostepnykampus.feature.map.campus.campusMapData
import io.github.dostepnykampus.feature.map.navigation.MapFocus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val HEADER = "Wejście, budynek C-13"

private val ENTRANCE = entranceFeature(symbol = "C-13", id = "node/1", tags = """"entrance":"main","wheelchair":"yes"""")

private val ZoomIn = hasContentDescription("Powiększ")

private val Attribution = hasText("© OpenStreetMap", substring = true)

/**
 * Arkusz wejścia na cały ekran, bez mapy: przy dużej czcionce, w szerokim oknie i po wejściu z karty otwartej od razu
 * na cały ekran.
 *
 * Wymagania: NFR-18, NFR-19, NFR-20, NFR-51, US3.3
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class EntranceSheetFullScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val map = TouchableMap()

    /** Wiersz: zwykła czcionka, wąskie okno → arkusz na dole mapy, mapa z przyciskami i podpisem OSM zostaje. */
    @Test
    fun keepsMapAtNormalFont() {
        start()
        compose.runOnIdle { map.touchSign(TouchSign.ENTRANCE, "node/1") }

        compose.onNodeWithText(HEADER).assertIsFocused()
        compose.onNode(ZoomIn).assertExists()
        compose.onNode(Attribution).assertExists()
    }

    /**
     * Wiersz: skala czcionki 1,5 → arkusz na cały ekran z fokusem na nagłówku; TalkBack nie czyta mapy, jej przycisków,
     *   podpisu OSM ani pola wyszukiwania. Po „Zamknij” mapa wraca.
     */
    @Test
    @Config(fontScale = 1.5f)
    fun coversMapAtLargeFont() {
        start()
        compose.runOnIdle { map.touchSign(TouchSign.ENTRANCE, "node/1") }

        assertFullSheet()
        compose.onNodeWithText("Zamknij").performClick()
        compose.onNodeWithText(HEADER).assertDoesNotExist()
        compose.onNode(ZoomIn).assertExists()
    }

    /** Wiersz: okno szerokie na co najmniej 600 dp → arkusz na cały ekran. Wymagania: NFR-51 */
    @Test
    @Config(qualifiers = "w600dp-h960dp-xhdpi")
    fun coversMapInWideWindow() {
        start()
        compose.runOnIdle { map.touchSign(TouchSign.ENTRANCE, "node/1") }

        assertFullSheet()
    }

    /** Wiersz: „Pokaż wejście na mapie” z karty otwartej od razu na cały ekran → arkusz tego wejścia na cały ekran. */
    @Test
    fun coversMapAfterDirectCard() {
        start(focus = MapFocus.Entrance(id = "node/1", buildingSymbol = "C-13", isFullSheet = true))

        assertFullSheet()
    }

    /** Zrzut arkusza na cały ekran przy czcionce 200%. Wymagania: NFR-20 */
    @Test
    @Config(fontScale = 2f)
    fun matchesLargeFontMockup() {
        start()
        compose.runOnIdle { map.touchSign(TouchSign.ENTRANCE, "node/1") }

        compose.onNodeWithText(HEADER).assertExists()
        compose.onRoot().captureRoboImage()
    }

    private fun assertFullSheet() {
        compose.onNodeWithText(HEADER).assertIsFocused()
        compose.onNode(ZoomIn).assertDoesNotExist()
        compose.onNode(Attribution).assertDoesNotExist()
        compose.onNode(hasSetTextAction()).assertDoesNotExist()
    }

    private fun start(focus: MapFocus? = null) {
        val choice = TestMapListChoice().apply { savedChoice.value = false }
        val viewModel =
            CampusMapViewModel(
                repository = unitSquareCatalog(symbol = "C-13", entrances = listOf(ENTRANCE)),
                networkStatus = { flowOf(true) },
                mapListChoice = choice,
                computeContext = UnconfinedTestDispatcher(),
                geometry = ::campusMapData,
                barriers = { emptyList() },
            )
        compose.setContent {
            DostepnyKampusTheme {
                CampusMapScreen(onBuildingClick = {}, focus = focus, mapView = map.view, viewModel = viewModel)
            }
        }
        val state = viewModel.uiState
        compose.waitUntil { state.value.catalog != null && state.value.mapData != null }
        compose.waitForIdle()
    }
}
