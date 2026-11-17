/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val NO_DATA = "Brak danych o wejściach"
private const val SYMBOL_WEIGHT = 800
private const val SYMBOL_SP = 36
private const val NAME_SP = 22

/** Nagłówek karty V2: symbol i nazwa pod nim; budynek bez nazwy ma sam symbol. Wymagania: US2, NFR-16, NFR-18 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class BuildingCardHeaderTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** Wiersz wymagań V2 „Nagłówek”: symbol, pod nim nazwa, a TalkBack czyta „Budynek A-1, Gmach Główny”. */
    @Test
    fun showsSymbolAndName() {
        show(symbol = "A-1", name = "Gmach Główny")
        compose.onNodeWithText("A-1", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("Gmach Główny", useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(hasContentDescription("Budynek A-1, Gmach Główny")).assertIsFocused()
    }

    /** Wiersz wymagań V2 „Nagłówek”: bez nazwy sam symbol, a TalkBack czyta „Budynek A-4”. */
    @Test
    fun showsSymbolOnly() {
        show(symbol = "A-4", name = null)
        compose.onNodeWithText("A-4", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("Budynek A-4", useUnmergedTree = true).assertDoesNotExist()
        compose.onNode(hasContentDescription("Budynek A-4")).assertIsFocused()
    }

    /**
     * Wiersz wymagań V2 „Nagłówek”: bez danych o wejściach pod nazwą cichy napis „Brak danych o wejściach”, nad
     *   „Sprawdź, którędy wejść”.
     */
    @Test
    fun showsQuietLineWithoutEntrances() {
        show(symbol = "A-1", name = "Gmach Główny")
        val name = compose.onNodeWithText("Gmach Główny", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val line = compose.onNodeWithText(NO_DATA).getUnclippedBoundsInRoot()
        val check = compose.onNodeWithText("Sprawdź, którędy wejść").getUnclippedBoundsInRoot()
        assertTrue("„$NO_DATA” pod nazwą: $line, nazwa $name", line.top >= name.bottom)
        assertTrue("„$NO_DATA” nad „Sprawdź…”: $line, $check", line.bottom <= check.top)
        compose.onAllNodes(hasText(NO_DATA)).assertCountEquals(1)
    }

    /** Wiersz wymagań V2 „Nagłówek”: symbol 36 sp z wagą 800, nazwa pod nim 22 sp, jak nagłówek grupy. */
    @Test
    fun usesHeaderTextSizes() {
        show(symbol = "A-1", name = "Gmach Główny")
        val symbol = styleOf("A-1")
        assertEquals("rozmiar symbolu", SYMBOL_SP.sp, symbol.fontSize)
        assertEquals("waga symbolu", FontWeight(SYMBOL_WEIGHT), symbol.fontWeight)
        assertEquals("rozmiar nazwy", NAME_SP.sp, styleOf("Gmach Główny").fontSize)
    }

    private fun styleOf(text: String): TextStyle {
        val layouts = mutableListOf<TextLayoutResult>()
        compose
            .onNodeWithText(text, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        return layouts.single().layoutInput.style
    }

    private fun show(symbol: String, name: String?) {
        compose.setContent {
            DostepnyKampusTheme {
                BuildingCardContent(
                    symbol = symbol,
                    mode = CardMode.Compact,
                    card = BuildingCardUiState.Ready(symbol = symbol, name = name, entrances = emptyList()),
                    isOnline = true,
                    onCheckEntrances = {},
                    onShowOnMap = {},
                    onBack = {},
                    onClose = {},
                    onReportEntrance = {},
                    onRetry = {},
                )
            }
        }
    }
}
