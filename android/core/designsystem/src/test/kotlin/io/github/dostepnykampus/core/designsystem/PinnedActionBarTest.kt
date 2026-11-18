/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import io.github.dostepnykampus.core.designsystem.bar.PinnedActionBar
import io.github.dostepnykampus.core.designsystem.bar.PinnedButton
import io.github.dostepnykampus.core.designsystem.bar.PinnedButtonKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val REPORT = "Zgłoś barierę"
private const val SHOW = "Pokaż na mapie"

/**
 * Układ przycisków z wagą w przypiętym pasku przy zwykłej czcionce i przy 200 %.
 *
 * Wymagania: NFR-20, NFR-17
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class PinnedActionBarTest {
    @get:Rule
    val compose = createComposeRule()

    /** Zwykła czcionka: oba przyciski z wagą stoją w jednym wierszu. */
    @Test
    fun keepsWeightedButtonsInOneRow() {
        show()
        val report = compose.onNodeWithText(REPORT).getUnclippedBoundsInRoot()
        val shown = compose.onNodeWithText(SHOW).getUnclippedBoundsInRoot()
        assertTrue("„$SHOW” ma stać na prawo od „$REPORT”", shown.left > report.right)
        assertEquals("Przyciski mają stać w tym samym wierszu", report.top, shown.top)
    }

    /** Czcionka 200 %: słowo nie mieści się w udziale przycisku, więc przyciski stoją jeden pod drugim. */
    @Test
    @Config(fontScale = 2f)
    fun stacksWeightedButtonsWhenWordDoesNotFit() {
        show()
        listOf(REPORT, SHOW).forEach { label ->
            assertEquals("Napis „$label” nie łamie słowa w środku", emptyList<Int>(), brokenWords(label))
        }
        val report = compose.onNodeWithText(REPORT).getUnclippedBoundsInRoot()
        val shown = compose.onNodeWithText(SHOW).getUnclippedBoundsInRoot()
        assertTrue("„$SHOW” ma stać pod „$REPORT”", shown.top >= report.bottom)
        assertEquals(
            "Oba przyciski mają 328 dp szerokości",
            listOf(328f, 328f),
            listOf((report.right - report.left).value, (shown.right - shown.left).value),
        )
    }

    private fun show() {
        compose.setContent {
            DostepnyKampusTheme {
                PinnedActionBar {
                    PinnedButton(text = REPORT, onClick = {}, modifier = Modifier.weight(3f))
                    PinnedButton(
                        text = SHOW,
                        onClick = {},
                        modifier = Modifier.weight(2f),
                        kind = PinnedButtonKind.Tonal,
                    )
                }
            }
        }
    }

    private fun brokenWords(label: String): List<Int> {
        val layouts = mutableListOf<TextLayoutResult>()
        compose
            .onNodeWithText(label, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val layout = layouts.single()
        return (0 until layout.lineCount - 1)
            .map { line -> layout.getLineEnd(line) }
            .filter { end -> !label[end - 1].isWhitespace() && !label[end].isWhitespace() }
    }
}
