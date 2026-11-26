/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.navigation

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.app.EXPAND_HANDLE
import io.github.dostepnykampus.app.sheetHandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val SCROLLED_PX = 60f

/**
 * „Wstecz” w arkuszu wejścia wraca do karty A-1 na jej wysokości i przewinięciu, a „Zamknij” zostawia mapę
 * bez karty budynku.
 *
 * Wymagania: US2.8, US3.3, NFR-15, NFR-53
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp")
class EntranceSheetReturnTest : EntranceSheetFixture() {
    /**
     * Wiersz: karta A-1 rozwinięta i przewinięta, wiersz wejścia → „Wstecz” zwraca kartę rozwiniętą i tak samo
     * przewiniętą.
     * Scenariusz: przegladanie-mapy.feature:
     *   „Wstecz” w arkuszu wejścia wraca tam, skąd arkusz otwarto, a „Zamknij” zostawia mapę.
     */
    @Test
    @Config(qualifiers = "w360dp-h420dp")
    fun returnsToScrolledCard() {
        start(withLowCard = true)
        compose.onNodeWithContentDescription(EXPAND_HANDLE).performClick()
        compose.waitForIdle()
        compose.onNode(sheetHandle(isExpanded = true)).assertExists("karta A-1 rozwinięta")
        compose.onNode(cardScroll).performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, SCROLLED_PX) }
        compose.waitForIdle()
        assertEquals("karta A-1 przewinięta przed arkuszem", SCROLLED_PX, scrolled(), 0f)
        compose.onNode(recommendedRow).performClick()
        waitForSheet()
        assertPinned(ACCESSIBLE_PIN)
        compose.onNode(cardHeader).assertDoesNotExist()

        pressBack()
        compose.waitUntil { compose.onAllNodes(cardScroll).fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()

        assertUnpinned()
        compose.onNode(cardHeader).assertExists("karta A-1 po powrocie przez „Wstecz”")
        compose.onNode(sheetHandle(isExpanded = true)).assertExists("karta A-1 rozwinięta po powrocie")
        assertEquals("karta A-1 tak samo przewinięta po powrocie", SCROLLED_PX, scrolled(), 0f)
    }

    /**
     * Wiersz: niska karta A-1, „Pokaż wejście na mapie” → „Wstecz” zwraca kartę A-1 na niskiej wysokości.
     * Scenariusz: przegladanie-mapy.feature:
     *   „Wstecz” w arkuszu wejścia wraca tam, skąd arkusz otwarto, a „Zamknij” zostawia mapę.
     */
    @Test
    fun returnsToLowCardOnBack() {
        start(withLowCard = true)
        compose.onNode(sheetHandle(isExpanded = false)).assertExists("karta A-1 na niskiej wysokości")
        openSheetByRecommended()
        assertPinned(ACCESSIBLE_PIN)
        compose.onNode(cardHeader).assertDoesNotExist()

        pressBack()
        waitForCard()

        assertUnpinned()
        compose.onNode(cardHeader).assertExists("karta A-1 po powrocie przez „Wstecz”")
        compose.onNode(sheetHandle(isExpanded = false)).assertExists("karta A-1 znowu na niskiej wysokości")
        val handleTop = compose.onNodeWithContentDescription(EXPAND_HANDLE).getUnclippedBoundsInRoot().top
        assertTrue("niska karta A-1 zostawia mapę nad sobą", handleTop > 0.dp)
        compose.onNodeWithText(MAIN_PIN.label).assertDoesNotExist()
    }

    /**
     * Wiersz: niska karta A-1, „Pokaż wejście na mapie” → „Zamknij” zostawia mapę bez karty budynku.
     * Scenariusz: przegladanie-mapy.feature:
     *   „Wstecz” w arkuszu wejścia wraca tam, skąd arkusz otwarto, a „Zamknij” zostawia mapę.
     */
    @Test
    fun leavesMapOnCloseFromCard() {
        start(withLowCard = true)
        compose.onNode(sheetHandle(isExpanded = false)).assertExists("karta A-1 na niskiej wysokości")
        openSheetByRecommended()
        assertPinned(ACCESSIBLE_PIN)

        closeSheet()

        assertUnpinned()
        assertMapWithoutCard()
    }

    /**
     * Wiersz: znak wejścia A-1 na mapie bez karty → „Zamknij” zostawia mapę bez karty budynku.
     * Scenariusz: przegladanie-mapy.feature:
     *   „Wstecz” w arkuszu wejścia wraca tam, skąd arkusz otwarto, a „Zamknij” zostawia mapę.
     */
    @Test
    fun leavesMapOnCloseFromSign() {
        start()
        assertMapWithoutCard()
        openSheetBySign()
        assertPinned(ACCESSIBLE_PIN)

        closeSheet()

        assertUnpinned()
        assertMapWithoutCard()
    }

    private fun scrolled(): Float =
        compose.onNode(cardScroll).fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
}
