/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val HEADER = "Wejścia"
private const val NAME = "Gmach Testowy"

/**
 * V2 Karta budynku: kreska między górą karty a pełnym opisem na każdej wysokości karty i w pełnym ekranie od razu,
 * pod nagłówkiem karty i nad początkiem pełnego opisu.
 *
 * Wymagania: US2.8, NFR-53
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class CardDescriptionLineTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /**
     * Wiersz: V2, niska wysokość: pod kreską wystaje początek pełnego opisu.
     * Scenariusz (częściowo): przegladanie-mapy.feature: Niska karta nad mapą mówi słowami o wejściach
     */
    @Test
    fun drawsLineOnLowCard() {
        open(CardMode.Compact)
        val start = compose.onNode(hasContentDescription(HEADER) and hasClickAction()).getUnclippedBoundsInRoot().top
        assertLineAbove(start)
    }

    /**
     * Wiersz: V2, połowa ekranu: na karcie pod kreską początek pełnego opisu.
     * Scenariusz (częściowo): przegladanie-mapy.feature: Uchwyt zmienia wysokość karty bez przeciągania
     */
    @Test
    fun drawsLineOnHalfCard() {
        open(CardMode.Expanded)
        assertLineAbove(compose.onNodeWithText(HEADER).getUnclippedBoundsInRoot().top)
    }

    /** Wiersz: V2, karta przy dużej czcionce na cały ekran nad mapą: pod kreską zaczyna się pełny opis. */
    @Test
    fun drawsLineOnLargeCard() {
        open(CardMode.Large)
        assertLineAbove(compose.onNodeWithText(HEADER).getUnclippedBoundsInRoot().top)
    }

    /** Wiersz: V2, pełny ekran od razu: pod kreską zaczyna się pełny opis. */
    @Test
    fun drawsLineOnDirectCard() {
        open(CardMode.Direct)
        assertLineAbove(compose.onNodeWithText(HEADER).getUnclippedBoundsInRoot().top)
    }

    private fun assertLineAbove(start: Dp) {
        val line = compose.onNodeWithTag(DESCRIPTION_LINE_TAG).getUnclippedBoundsInRoot()
        val name = compose.onNodeWithText(NAME).getUnclippedBoundsInRoot()
        assertTrue("kreska pod nagłówkiem: od ${line.top}, nazwa do ${name.bottom}", line.top >= name.bottom)
        assertTrue("kreska nad pełnym opisem: do ${line.bottom}, opis od $start", line.bottom <= start)
    }

    private fun open(mode: CardMode) {
        compose.setContent {
            DostepnyKampusTheme {
                BuildingCardContent(
                    symbol = "A-2",
                    mode = mode,
                    card = BuildingCardUiState.Ready(symbol = "A-2", name = NAME, entrances = emptyList()),
                    isOnline = true,
                    onShowOnMap = {},
                    onBack = {},
                    onClose = {},
                    onReport = {},
                    onRetry = {},
                )
            }
        }
        compose.waitForIdle()
    }
}
