/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.github.dostepnykampus.core.designsystem.bar.BackHeader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val TITLE = "Jak korzystać z aplikacji"

/**
 * Układ nagłówka „Wstecz” przy zwykłej czcionce i przy 200 % (reguły dużej czcionki).
 *
 * Wymagania: NFR-20, NFR-15
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class BackHeaderTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show() {
        compose.setContent { DostepnyKampusTheme { BackHeader(title = TITLE, onBack = {}) } }
        compose.onNode(isHeading()).assertTextEquals(TITLE)
    }

    /** Zwykła czcionka: tytuł obok „Wstecz”, w tym samym wierszu, makieta hi-fi. */
    @Test
    fun placesTitleNextToBack() {
        show()
        val back = compose.onNodeWithText("Wstecz").getUnclippedBoundsInRoot()
        val title = compose.onNodeWithText(TITLE).getUnclippedBoundsInRoot()
        assertTrue(title.left > back.right && title.top < back.bottom)
    }

    /**
     * Czcionka 200 %: w wierszu tylko „Wstecz”, tytuł pod nim na całą szerokość bez marginesów 16 dp.
     *
     * Wymagania: NFR-20
     */
    @Test
    @Config(fontScale = 2f)
    fun movesTitleBelowBack() {
        show()
        val back = compose.onNodeWithText("Wstecz").getUnclippedBoundsInRoot()
        val title = compose.onNodeWithText(TITLE).getUnclippedBoundsInRoot()
        assertTrue(title.top > back.bottom)
        assertEquals(listOf(16f, 328f), listOf(title.left.value, (title.right - title.left).value))
    }
}
