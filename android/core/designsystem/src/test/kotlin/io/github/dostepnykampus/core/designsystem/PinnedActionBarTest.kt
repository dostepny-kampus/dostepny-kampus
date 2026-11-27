/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
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
import org.robolectric.annotation.GraphicsMode

private const val REPORT = "Zgłoś barierę"
private const val SHOW = "Pokaż na mapie"
private const val SAVE = "Zapisz"
private const val SEND = "Wyślij do sprawdzenia"
private const val BAR_WIDTH_DP = 328
private const val WIDE_BAR_WIDTH_DP = 568

/**
 * Układ jedynego przycisku oraz przycisków z wagą w przypiętym pasku przy zwykłej czcionce i przy 200 %.
 *
 * Wymagania: NFR-20, NFR-17, NFR-52
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class PinnedActionBarTest {
    @get:Rule
    val compose = createComposeRule()

    /** Zwykła czcionka: jedyny przycisk z krótkim napisem zajmuje całą szerokość pasa. */
    @Test
    fun stretchesSoleButton() {
        show(soleLabel = SAVE)
        val button = compose.onNodeWithText(SAVE).getUnclippedBoundsInRoot()
        assertEquals("Jedyny przycisk ma $BAR_WIDTH_DP dp szerokości", BAR_WIDTH_DP.dp, button.right - button.left)
    }

    /** Czcionka 200 %: jedyny przycisk zajmuje całą szerokość pasa bez łamania słów w środku. */
    @Test
    fun stretchesSoleAtFont200() {
        show(fontScale = 2f, soleLabel = SEND)
        assertEquals("Napis „$SEND” nie łamie słowa w środku", emptyList<Int>(), brokenWords(SEND))
        val button = compose.onNodeWithText(SEND).getUnclippedBoundsInRoot()
        assertEquals(
            "Jedyny przycisk przy czcionce 200 % ma $BAR_WIDTH_DP dp szerokości",
            BAR_WIDTH_DP.dp,
            button.right - button.left,
        )
    }

    /** Okno 600 dp: jedyny przycisk wypełnia całą szerokość pasa bez zawężania do napisu. */
    @Test
    @Config(qualifiers = "w600dp-h800dp-xhdpi")
    fun stretchesSoleInWideBar() {
        show(soleLabel = SAVE)
        val button = compose.onNodeWithText(SAVE).getUnclippedBoundsInRoot()
        assertEquals(
            "Jedyny przycisk w oknie 600 dp ma $WIDE_BAR_WIDTH_DP dp szerokości",
            WIDE_BAR_WIDTH_DP.dp,
            button.right - button.left,
        )
    }

    /** Zwykła czcionka: oba przyciski z wagą stoją w jednym wierszu. */
    @Test
    fun keepsWeightedButtonsInOneRow() {
        show()
        val report = compose.onNodeWithText(REPORT).getUnclippedBoundsInRoot()
        val shown = compose.onNodeWithText(SHOW).getUnclippedBoundsInRoot()
        assertTrue("„$SHOW” ma stać na prawo od „$REPORT”", shown.left > report.right)
        assertEquals("Przyciski mają stać w tym samym wierszu", report.top, shown.top)
    }

    /** Zawinięty napis wyznacza wysokość przycisków w swoim wierszu. Wymagania: NFR-17 */
    @Test
    fun matchesHeightOfWrappedButton() {
        compose.setContent {
            DostepnyKampusTheme {
                PinnedActionBar {
                    PinnedButton(text = "Dalej", onClick = {}, modifier = Modifier.weight(1f))
                    PinnedButton(
                        text = "Pomiń instrukcję",
                        onClick = {},
                        modifier = Modifier.weight(1f),
                        kind = PinnedButtonKind.Outlined,
                    )
                }
            }
        }

        val next = compose.onNodeWithText("Dalej").getUnclippedBoundsInRoot()
        val skip = compose.onNodeWithText("Pomiń instrukcję").getUnclippedBoundsInRoot()
        assertTrue("„Pomiń instrukcję” stoi obok „Dalej”", skip.left > next.right)
        assertTrue("Zawinięty napis podnosi wysokość przycisku", skip.bottom - skip.top > CeleDotyku.przypiety)
        assertEquals("Przyciski mają wspólną górną krawędź", skip.top, next.top)
        assertEquals("Przyciski mają wspólną dolną krawędź", skip.bottom, next.bottom)
    }

    /** Czcionka 200 %: słowo nie mieści się w udziale przycisku, więc przyciski stoją jeden pod drugim. */
    @Test
    fun stacksWhenWordDoesNotFit() {
        show(fontScale = 2f)
        listOf(REPORT, SHOW).forEach { label ->
            assertEquals("Napis „$label” nie łamie słowa w środku", emptyList<Int>(), brokenWords(label))
        }
        val report = compose.onNodeWithText(REPORT).getUnclippedBoundsInRoot()
        val shown = compose.onNodeWithText(SHOW).getUnclippedBoundsInRoot()
        assertTrue("„$SHOW” $shown ma stać pod „$REPORT” $report", shown.top >= report.bottom)
        assertEquals(
            "Oba przyciski mają $BAR_WIDTH_DP dp szerokości",
            listOf(BAR_WIDTH_DP.toFloat(), BAR_WIDTH_DP.toFloat()),
            listOf((report.right - report.left).value, (shown.right - shown.left).value),
        )
    }

    private fun show(fontScale: Float = 1f, soleLabel: String? = null) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                Bar(soleLabel = soleLabel)
            }
        }
    }

    @Composable
    private fun Bar(soleLabel: String? = null) {
        DostepnyKampusTheme {
            PinnedActionBar {
                if (soleLabel != null) {
                    PinnedButton(text = soleLabel, onClick = {})
                } else {
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
