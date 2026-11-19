/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.dostepnykampus.feature.map.camera.TouchSign
import io.github.dostepnykampus.feature.map.navigation.MapFocus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

private const val MAIN = """"entrance":"main","wheelchair":"yes","ref":"Główne""""

private val ROUTE = MapFocus.Entrance(id = "node/1", buildingSymbol = "C-13")

/**
 * Pinezka i podpis wejścia na mapie V1 są tylko przy otwartym arkuszu tego wejścia, na widoku zastępczym mapy.
 *
 * Wymagania: US3.3, NFR-18, NFR-53
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EntrancePinTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val map = TouchableMap()
    private val repository =
        unitSquareCatalog(
            symbol = "C-13",
            entrances = listOf(entranceFeature(symbol = "C-13", id = "node/1", tags = MAIN)),
        )

    /**
     * Wiersz: wejście zaznaczone z trasy ma pinezkę, a „Zamknij” usuwa ją razem z arkuszem.
     * Scenariusz (częściowo, przykład: „Pokaż wejście na mapie” na karcie A-1): przegladanie-mapy.feature:
     *   Pinezka i podpis wejścia są na mapie tylko przy otwartym arkuszu wejścia
     */
    @Test
    fun removesPinOnClose() {
        start(focus = ROUTE)
        compose.runOnIdle { assertEquals("pinezka przy arkuszu", "node/1", map.pinned) }
        compose.onNodeWithText("Zamknij").performClick()

        compose.runOnIdle { assertEquals("bez pinezki po „Zamknij”", null, map.pinned) }
    }

    /**
     * Wiersz: „Wstecz” zamyka arkusz wejścia z trasy razem z pinezką.
     * Scenariusz (częściowo, przykład: wiersz wejścia na karcie A-1): przegladanie-mapy.feature:
     *   Pinezka i podpis wejścia są na mapie tylko przy otwartym arkuszu wejścia
     */
    @Test
    fun removesPinOnBack() {
        start(focus = ROUTE)
        compose.runOnIdle { assertEquals("pinezka przy arkuszu", "node/1", map.pinned) }
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }

        compose.runOnIdle { assertEquals("bez pinezki po „Wstecz”", null, map.pinned) }
    }

    /**
     * Wiersz: znak dotknięty na mapie daje pinezkę, a wiersz budynku usuwa ją razem z arkuszem.
     * Scenariusz (częściowo, przykład: znak wejścia A-1 na mapie | wiersz budynku A-1): przegladanie-mapy.feature:
     *   Pinezka i podpis wejścia są na mapie tylko przy otwartym arkuszu wejścia
     */
    @Test
    fun pinsTouchedSignUntilBuildingRow() {
        start()
        compose.runOnIdle { map.touchSign(TouchSign.ENTRANCE, "node/1") }
        compose.runOnIdle { assertEquals("pinezka po dotknięciu znaku", "node/1", map.pinned) }
        compose.onNodeWithContentDescription("Budynek C-13").performClick()

        compose.runOnIdle { assertEquals("bez pinezki po wierszu budynku", null, map.pinned) }
    }

    /**
     * Wiersz: dotknięcie pustej mapy zamyka arkusz otwarty znakiem razem z pinezką.
     * Scenariusz (częściowo, przykład: znak wejścia A-1 na mapie | dotyk pustej mapy): przegladanie-mapy.feature:
     *   Pinezka i podpis wejścia są na mapie tylko przy otwartym arkuszu wejścia
     */
    @Test
    fun removesPinOnEmptyMap() {
        start()
        compose.runOnIdle { map.touchSign(TouchSign.ENTRANCE, "node/1") }
        compose.runOnIdle { assertEquals("pinezka po dotknięciu znaku", "node/1", map.pinned) }
        compose.runOnIdle { map.touchBuildings(emptyList()) }

        compose.runOnIdle { assertEquals("bez pinezki po pustej mapie", null, map.pinned) }
    }

    private fun start(focus: MapFocus? = null) {
        compose.showMap(viewModel = testMapViewModel(repository = repository), map = map.view, focus = focus)
    }
}
