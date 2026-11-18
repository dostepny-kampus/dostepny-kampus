/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Grupa „Wejścia” na V2, gdy budynek nie ma wejść w danych. Wymagania: US3.1, US17.1 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class BuildingCardEntrancesTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /**
     * Wiersz: bez wejść w danych → w grupie „Wejścia” zdanie o braku danych, bez pustej ramki.
     * Scenariusz: przegladanie-mapy.feature: Karta budynku bez wejść w danych mówi o braku danych zamiast pustej
     *   grupy
     *
     * Wymagania: US3.1, US17.1
     */
    @Test
    fun saysNoDataWithoutEntrances() {
        compose.setContent {
            DostepnyKampusTheme {
                BuildingCardContent(
                    symbol = "A-2",
                    mode = CardMode.Expanded,
                    card = BuildingCardUiState.Ready(symbol = "A-2", name = null, entrances = emptyList()),
                    isOnline = true,
                    onShowOnMap = {},
                    onBack = {},
                    onClose = {},
                    onReport = {},
                    onRetry = {},
                )
            }
        }

        compose.onNodeWithText("Wejścia").performScrollTo().assertIsDisplayed()
        val sentence = "Nie mamy jeszcze danych o wejściach do tego budynku."
        compose.onNodeWithText(sentence).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Zgłoś barierę przy wejściu", substring = true).assertDoesNotExist()
    }
}
