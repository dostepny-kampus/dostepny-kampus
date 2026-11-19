/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.feature.map.camera.TouchSign
import io.github.dostepnykampus.feature.map.campus.campusMapData
import io.github.dostepnykampus.feature.map.entrance.EntranceActions
import io.github.dostepnykampus.feature.map.navigation.MapFocus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

private const val CARD_HEIGHT = 200

private val SHEET_EDGE = 32.dp

private const val MAIN_ACCESSIBLE = """"entrance":"main","wheelchair":"yes","ref":"Główne""""

private const val SIDE_ACCESSIBLE = """"entrance":"yes","wheelchair":"yes","ref":"Boczne""""

/**
 * Dotknięcie znaku wejścia na mapie V1 otwiera arkusz wejścia, na widoku zastępczym mapy.
 *
 * Wymagania: US2.1, US3.3, US4.6, NFR-17, NFR-53
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MapEntranceTouchTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val opened = mutableListOf<String>()
    private val returned = mutableListOf<String>()
    private var closedCards = 0
    private val reports = mutableListOf<String>()
    private val map = TouchableMap()
    private val repository =
        unitSquareCatalog(
            symbol = "C-13",
            entrances =
                listOf(
                    entranceFeature(symbol = "C-13", id = "node/1", tags = MAIN_ACCESSIBLE),
                    entranceFeature(symbol = "C-13", id = "node/2", tags = SIDE_ACCESSIBLE),
                ),
        )

    /** Wiersz: V1, dotknięcie znaku wejścia otwiera arkusz, nie zaznacza budynku i nie otwiera V2. */
    @Test
    fun opensSheetWithoutSelection() {
        start()
        compose.runOnIdle { map.touchSign(TouchSign.ENTRANCE, "node/1") }

        compose.onNodeWithText("Wejście").assertExists()
        compose.onNodeWithText("Dostępne, wejście główne").assertExists()
        compose.runOnIdle { assertEquals("bez V2", emptyList<String>(), opened) }
        compose.runOnIdle { assertEquals("bez zaznaczenia budynku", null, map.selected) }
    }

    /** Wiersz: V1, „Zamknij” zamyka arkusz wejścia. Wymagania: NFR-53 */
    @Test
    fun closesSheetOnClose() {
        start()
        compose.runOnIdle { map.touchSign(TouchSign.ENTRANCE, "node/1") }
        compose.onNodeWithText("Zamknij").performClick()

        compose.onNodeWithText("Wejście").assertDoesNotExist()
    }

    /** Wiersz: V1, dotknięcie pustej mapy zamyka arkusz wejścia. Wymagania: NFR-53 */
    @Test
    fun closesSheetOnEmptyMap() {
        start()
        compose.runOnIdle { map.touchSign(TouchSign.ENTRANCE, "node/1") }
        compose.onNodeWithText("Wejście").assertExists()
        compose.runOnIdle { map.touchBuildings(emptyList()) }

        compose.onNodeWithText("Wejście").assertDoesNotExist()
    }

    /** Wiersz: V1, „Wstecz” przy otwartym arkuszu wejścia zamyka arkusz i zostawia mapę. Wymagania: NFR-15 */
    @Test
    fun closesSheetOnBack() {
        start()
        compose.runOnIdle { map.touchSign(TouchSign.ENTRANCE, "node/1") }
        compose.onNodeWithText("Wejście").assertExists()
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }

        compose.onNodeWithText("Wejście").assertDoesNotExist()
        compose.runOnIdle { assertFalse("mapa zostaje", compose.activity.isFinishing) }
    }

    /**
     * Wiersz: wejście zaznaczone z trasy stoi na środku części mapy nad arkuszem: dół kamery zasłania wysokość
     *   arkusza.
     * Scenariusz (częściowo): przegladanie-mapy.feature: Pinezka wejścia jest widoczna nad arkuszem wejścia
     *
     * Wymagania: US3.3, NFR-53
     */
    @Test
    fun centersEntranceAboveSheet() {
        start(focus = MapFocus.Entrance(id = "node/1", buildingSymbol = "C-13"))
        val header = compose.onNodeWithText("Wejście").getUnclippedBoundsInRoot()
        val root = compose.onRoot().getUnclippedBoundsInRoot()

        val inset = checkNotNull(map.camera).second
        val density = compose.activity.resources.displayMetrics.density
        val covered = (inset.bottom / density).dp
        assertTrue("arkusz pod nagłówkiem zasłania dół: $covered", covered >= root.bottom - header.top)
        assertTrue("najwyżej o odstępy arkusza więcej: $covered", covered <= root.bottom - header.top + SHEET_EDGE)
    }

    /** Wiersz: arkusz wejścia, „Zgłoś barierę przy wejściu” przekazuje to wejście. Wymagania: US4.6 */
    @Test
    fun reportsAtEntranceFromSheet() {
        start()
        compose.runOnIdle { map.touchSign(TouchSign.ENTRANCE, "node/2") }
        compose.onNode(hasContentDescription("Zgłoś barierę przy wejściu: »Boczne«")).performScrollTo().performClick()

        compose.runOnIdle { assertEquals("wejście boczne", listOf("node/2"), reports) }
    }

    /** Wiersz: V1, znak wejścia przy otwartej karcie budynku otwiera arkusz w miejsce karty. Wymagania: US2.1 */
    @Test
    fun replacesBuildingCard() {
        start(isCardOpen = true)
        compose.runOnIdle { map.touchSign(TouchSign.ENTRANCE, "node/1") }

        compose.onNodeWithText("Wejście").assertExists()
        compose.runOnIdle { assertEquals("karta zamknięta", 1, closedCards) }
    }

    /** Wiersz: arkusz otwarty przy karcie budynku, „Zamknij” zamyka arkusz razem z kartą. Wymagania: NFR-53 */
    @Test
    fun closeLeavesMapWithoutCard() {
        start(isCardOpen = true)
        compose.runOnIdle { map.touchSign(TouchSign.ENTRANCE, "node/1") }
        compose.onNodeWithText("Zamknij").performClick()

        compose.onNodeWithText("Wejście").assertDoesNotExist()
        compose.runOnIdle { assertEquals("bez karty", emptyList<String>(), opened) }
    }

    /**
     * Wiersz: arkusz otwarty znakiem bez karty, wiersz „Budynek C-13” zamyka arkusz i otwiera kartę.
     * Scenariusz (częściowo, przykład: dotknie znaku wejścia A-1 na mapie): przegladanie-mapy.feature:
     *   Wiersz budynku w arkuszu otwiera kartę budynku bez przelotu mapy
     *
     * Wymagania: US2.8, US3.3
     */
    @Test
    fun rowOpensBuildingCard() {
        start()
        compose.runOnIdle { map.touchSign(TouchSign.ENTRANCE, "node/1") }
        compose.onNodeWithContentDescription("Budynek C-13").performClick()

        compose.onNodeWithText("Wejście").assertDoesNotExist()
        compose.runOnIdle { assertEquals("karta budynku", listOf("C-13"), opened) }
    }

    /**
     * Wiersz: arkusz otwarty przy karcie budynku, wiersz budynku wraca do tej karty.
     * Scenariusz (częściowo): przegladanie-mapy.feature: Wiersz budynku w arkuszu otwartym z karty wraca do tej karty
     *
     * Wymagania: US2.8
     */
    @Test
    fun rowReturnsToCard() {
        start(isCardOpen = true)
        compose.runOnIdle { map.touchSign(TouchSign.ENTRANCE, "node/1") }
        compose.onNodeWithContentDescription("Budynek C-13").performClick()

        compose.runOnIdle {
            assertEquals("powrót do karty", listOf("C-13"), returned)
            assertEquals("bez nowej karty", emptyList<String>(), opened)
        }
    }

    /**
     * Wiersz: arkusz wejścia zaznaczonego z karty, „Wstecz” wraca do karty.
     * Scenariusz (częściowo): przegladanie-mapy.feature: „Wstecz” w arkuszu wejścia wraca tam, skąd arkusz
     *   otwarto, a „Zamknij” zostawia mapę
     *
     * Wymagania: US3.3, NFR-15, NFR-53
     */
    @Test
    fun backFromCardEntrance() {
        start(focus = MapFocus.Entrance(id = "node/1", buildingSymbol = "C-13"))
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.runOnIdle {
            assertEquals("powrót do karty", listOf("C-13"), returned)
            assertEquals("bez nowej karty", emptyList<String>(), opened)
        }
    }

    /** Wiersz: arkusz wejścia zaznaczonego z karty, „Zamknij” zostawia mapę bez karty. Wymagania: NFR-53 */
    @Test
    fun closeFromCardEntrance() {
        start(focus = MapFocus.Entrance(id = "node/1", buildingSymbol = "C-13"))
        compose.onNodeWithText("Zamknij").performClick()

        compose.onNodeWithText("Wejście").assertDoesNotExist()
        compose.runOnIdle { assertEquals("bez karty", emptyList<String>(), opened) }
    }

    /** Wiersz: arkusz otwarty przy karcie budynku, „Wstecz” wraca do karty. Wymagania: NFR-15 */
    @Test
    fun backReturnsToCard() {
        start(isCardOpen = true)
        compose.runOnIdle { map.touchSign(TouchSign.ENTRANCE, "node/1") }
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }

        compose.onNodeWithText("Wejście").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals("powrót do karty", listOf("C-13"), returned)
            assertEquals("bez nowej karty", emptyList<String>(), opened)
        }
    }

    /** Wiersz: arkusz otwarty przy karcie budynku, pusta mapa zamyka arkusz bez karty. Wymagania: NFR-53 */
    @Test
    fun emptyMapClosesWithoutCard() {
        start(isCardOpen = true)
        compose.runOnIdle { map.touchSign(TouchSign.ENTRANCE, "node/1") }
        compose.runOnIdle { map.touchBuildings(emptyList()) }

        compose.onNodeWithText("Wejście").assertDoesNotExist()
        compose.runOnIdle { assertEquals("bez karty", emptyList<String>(), opened) }
    }

    private fun start(isCardOpen: Boolean = false, focus: MapFocus? = null) {
        val viewModel =
            CampusMapViewModel(
                repository = repository,
                networkStatus = { flowOf(true) },
                mapListChoice = TestMapListChoice(),
                computeContext = UnconfinedTestDispatcher(),
                geometry = ::campusMapData,
            )
        compose.setContent {
            DostepnyKampusTheme {
                CampusMapScreen(
                    onBuildingClick = { opened += it },
                    onCardReturn = { returned += it },
                    focus = if (isCardOpen) MapFocus.Building("C-13") else focus,
                    contentPadding = PaddingValues(bottom = if (isCardOpen) CARD_HEIGHT.dp else 0.dp),
                    onEmptyMapClick = { closedCards++ },
                    entranceActions = EntranceActions(onReport = { reports += it }),
                    mapView = map.view,
                    viewModel = viewModel,
                )
            }
        }
        val state = viewModel.uiState
        compose.waitUntil { state.value.catalog != null && state.value.mapData != null }
        compose.waitForIdle()
    }
}
