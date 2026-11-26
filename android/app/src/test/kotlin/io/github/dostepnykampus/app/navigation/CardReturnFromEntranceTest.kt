/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.navigation

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import io.github.dostepnykampus.app.EXPAND_HANDLE
import io.github.dostepnykampus.app.grounds.PwrMapFixture
import io.github.dostepnykampus.app.sheetHandle
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val BUILDING_ROW = "Budynek A-1"
private const val SCROLLED_PX = 60f

/**
 * Karta budynku A-1 z danych PWr nad mapą V1, rozwinięta i przewinięta: wiersz wejścia otwiera arkusz wejścia,
 * a wiersz budynku w arkuszu przywraca kartę rozwiniętą i przewiniętą tak samo. Dane A-1 są krótkie, więc okno
 * ma 420 dp.
 *
 * Wymagania: US2.8, US3.3, NFR-53
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h420dp")
class CardReturnFromEntranceTest : PwrMapFixture() {
    private val entranceRow = hasClickAction() and hasText("Wejście")
    private val cardHeader = hasContentDescription("$BUILDING_ROW, ", substring = true)
    private val cardScroll = hasScrollAction() and hasAnyDescendant(hasText("Wejścia"))

    /**
     * Scenariusz: przegladanie-mapy.feature: Wiersz budynku w arkuszu otwartym z karty wraca do tej karty.
     */
    @Test
    fun returnsToScrolledCard() {
        showApp()
        compose.runOnUiThread { touch(listOf("A-1")) }
        compose.waitForIdle()
        compose.onNodeWithContentDescription(EXPAND_HANDLE).performClick()
        compose.waitForIdle()
        compose.onNode(cardScroll).performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, SCROLLED_PX) }
        compose.waitForIdle()
        assertEquals("karta przewinięta przed arkuszem", SCROLLED_PX, scrolled(), 0f)

        compose.onNode(entranceRow).performClick()
        compose.waitForIdle()
        compose.onNode(cardHeader).assertDoesNotExist()
        compose.onNodeWithContentDescription(BUILDING_ROW).performClick()
        compose.waitUntil { compose.onAllNodes(cardScroll).fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()

        compose.onNode(cardHeader).assertExists("karta A-1 zamiast arkusza")
        compose.onNode(sheetHandle(isExpanded = true)).assertExists("karta rozwinięta po powrocie")
        assertEquals("karta tak samo przewinięta", SCROLLED_PX, scrolled(), 0f)
    }

    private fun scrolled(): Float =
        compose
            .onNode(cardScroll)
            .fetchSemanticsNode()
            .config[SemanticsProperties.VerticalScrollAxisRange]
            .value()
}
