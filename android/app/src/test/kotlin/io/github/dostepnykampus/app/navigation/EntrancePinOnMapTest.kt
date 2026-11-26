/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.navigation

import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Pinezka i podpis wejścia A-1 są na mapie tylko przy otwartym arkuszu wejścia.
 *
 * Wymagania: US3.3, NFR-18, NFR-53
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp")
class EntrancePinOnMapTest : EntranceSheetFixture() {
    /**
     * Scenariusz (przykład: „Pokaż wejście na mapie” na karcie A-1 | „Zamknij”): przegladanie-mapy.feature:
     *   Pinezka i podpis wejścia są na mapie tylko przy otwartym arkuszu wejścia.
     */
    @Test
    fun hidesPinOnSheetClose() {
        start(withLowCard = true)
        openSheetByRecommended()
        assertPinned(ACCESSIBLE_PIN)

        closeToMap()
    }

    /**
     * Scenariusz (przykład: wiersz wejścia na karcie A-1 | „Wstecz”): przegladanie-mapy.feature:
     *   Pinezka i podpis wejścia są na mapie tylko przy otwartym arkuszu wejścia.
     */
    @Test
    fun hidesPinOnSheetBack() {
        start(withLowCard = true)
        expandCard()
        compose.onNode(mainEntranceRow).performScrollTo().performClick()
        waitFor(sheetBuildingRow)
        assertPinned(MAIN_PIN)

        pressBack()
        waitFor(cardHeader)

        assertUnpinned()
        compose.onNode(cardHeader).assertExists("karta A-1 po zamknięciu arkusza przez „Wstecz”")
    }

    /**
     * Scenariusz (przykład: znak wejścia A-1 na mapie | wiersz budynku A-1): przegladanie-mapy.feature:
     *   Pinezka i podpis wejścia są na mapie tylko przy otwartym arkuszu wejścia.
     */
    @Test
    fun hidesPinOnBuildingRow() {
        start()
        openSheetBySign()
        assertPinned(ACCESSIBLE_PIN)

        compose.onNode(sheetBuildingRow).performClick()
        waitFor(cardHeader)

        assertUnpinned()
        compose.onNode(cardHeader).assertExists("karta A-1 po dotknięciu wiersza budynku")
    }

    /**
     * Scenariusz (przykład: znak wejścia A-1 na mapie | dotyk pustej mapy): przegladanie-mapy.feature:
     *   Pinezka i podpis wejścia są na mapie tylko przy otwartym arkuszu wejścia.
     */
    @Test
    fun hidesPinOnEmptyMap() {
        start()
        openSheetBySign()
        assertPinned(ACCESSIBLE_PIN)

        compose.runOnUiThread { touchBuildings(emptyList()) }
        compose.waitForIdle()

        assertUnpinned()
        assertMapWithoutCard()
    }
}
