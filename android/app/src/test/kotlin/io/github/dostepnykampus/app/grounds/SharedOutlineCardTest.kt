/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.grounds

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import io.github.dostepnykampus.app.sheetHandle
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val DATA_TIMEOUT = 5_000L

/**
 * Kontur wspólny bez planów pięter na mapie V1 z prawdziwych danych PWr.
 * Wymagania: US2, US3.2, US1.1, NFR-18
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class SharedOutlineCardTest : PwrMapFixture() {
    /** Dotknięcie konturu T-18/T-22 otwiera niską kartę bez arkusza „Który budynek?”. Wymagania: US2, US3.2 */
    @Test
    fun opensSharedCardWithoutChoice() {
        showApp()
        compose.runOnUiThread { touch(listOf("T-18", "T-22")) }
        assertSharedCard(searched = "dotyk")
        compose.onNode(sheetHandle(isExpanded = false)).assertIsDisplayed()
        compose.onNodeWithText("Zamknij").assertIsDisplayed()
    }

    /** Dotknięcie w zasięgu dwóch konturów otwiera arkusz „Który budynek?”. Wymagania: US2 */
    @Test
    fun opensChoiceBetweenOutlines() {
        showApp()
        compose.runOnUiThread { touch(listOf("A-1", "A-4")) }
        compose.waitForIdle()
        compose.onNodeWithText("Który budynek?").assertIsDisplayed()
        compose.onNodeWithText("Budynek A-1", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("Budynek A-4", useUnmergedTree = true).assertIsDisplayed()
    }

    /** Wyszukanie „T-18” i „T-22” otwiera tę samą wspólną kartę „T-18 / T-22”. Wymagania: US1.1, US2 */
    @Test
    fun opensSameCardFromBothSymbols() {
        showApp()
        compose.onNode(hasSetTextAction()).performTextInput("T-18")
        compose.onNodeWithContentDescription("T-18, ", substring = true).performClick()
        assertSharedCard("T-18")
        compose.onNodeWithText("Zamknij").performClick()
        compose.onNodeWithContentDescription("Wyczyść pole").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("T-22")
        compose.onNodeWithContentDescription("T-22").performClick()
        assertSharedCard("T-22")
    }

    private fun assertSharedCard(searched: String) {
        compose.waitUntil("karta „T-18 / T-22” dla $searched", timeoutMillis = DATA_TIMEOUT) {
            compose.onAllNodesWithText("T-18 / T-22", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitForIdle()
        compose.onNodeWithText("Który budynek?").assertDoesNotExist()
        compose.onNodeWithText("T-18 / T-22", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithContentDescription("Budynki T-18, T-22").assertIsFocused()
        compose.onNodeWithText("Brak danych o wejściach").assertIsDisplayed()
    }
}
