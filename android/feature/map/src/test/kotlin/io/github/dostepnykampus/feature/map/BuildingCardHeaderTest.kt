/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Nagłówek karty V2: symbol dużym napisem i nazwa pod nim; budynek bez nazwy ma sam symbol. Wymagania: US2, NFR-18 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class BuildingCardHeaderTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** Wiersz wymagań V2 „Nagłówek”: symbol, pod nim nazwa, a TalkBack czyta „Budynek A-1, Gmach Główny”. */
    @Test
    fun us2_named_symbol_and_name() {
        show(symbol = "A-1", name = "Gmach Główny")
        compose.onNodeWithText("A-1", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("Gmach Główny", useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(hasContentDescription("Budynek A-1, Gmach Główny")).assertIsFocused()
    }

    /** Wiersz wymagań V2 „Nagłówek”: bez nazwy sam symbol, a TalkBack czyta „Budynek A-4”. */
    @Test
    fun us2_nameless_symbol_only() {
        show(symbol = "A-4", name = null)
        compose.onNodeWithText("A-4", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("Budynek A-4", useUnmergedTree = true).assertDoesNotExist()
        compose.onNode(hasContentDescription("Budynek A-4")).assertIsFocused()
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
