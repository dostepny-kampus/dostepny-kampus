/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.navigation

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.app.EXPAND_HANDLE
import io.github.dostepnykampus.app.grounds.PwrMapFixture
import io.github.dostepnykampus.app.sheetHandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private val MAP_BUTTONS =
    listOf(
        "Powiększ",
        "Pomniejsz",
        "Przesuń mapę w górę",
        "Przesuń mapę w dół",
        "Przesuń mapę w lewo",
        "Przesuń mapę w prawo",
    )
private val FREE_MAP_FOR_BUTTONS = 300.dp

/**
 * Karta budynku A-1 z danych PWr nad mapą V1: wysokość zmieniana uchwytem i przyciski mapy nad niską mapą.
 *
 * Wymagania: US2.8, NFR-53
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class CardOverPwrMapTest : PwrMapFixture() {
    private fun openCard() {
        showApp()
        compose.runOnUiThread { touch(listOf("A-1")) }
        compose.waitForIdle()
    }

    private fun top(node: SemanticsNodeInteraction): Dp = node.getUnclippedBoundsInRoot().top

    private fun handleTop(): Dp = top(compose.onNodeWithContentDescription(EXPAND_HANDLE))

    private fun tapHandle() {
        compose.onNodeWithContentDescription(EXPAND_HANDLE).performClick()
        compose.waitForIdle()
    }

    /**
     * Wiersz: dotknięcie A-1 → niska karta; uchwyt → cały ekran z pełnym opisem pod kreską (nagłówek,
     * wejścia, piętra); znowu uchwyt → niska karta na tej samej wysokości.
     * Scenariusz: przegladanie-mapy.feature: Uchwyt zmienia wysokość karty bez przeciągania
     *
     * Wymagania: US2.8, NFR-53
     */
    @Test
    fun changesHeightByHandleTaps() {
        openCard()
        val low = handleTop()
        assertTrue("niska karta zostawia mapę nad sobą: $low", low > 0.dp)
        compose.onNodeWithText("Piętra").assertDoesNotExist()
        tapHandle()
        assertEquals("góra uchwytu: opis na całym ekranie", 0f, handleTop().value, 1f)
        compose.onNode(sheetHandle(isExpanded = true)).assertExists("uchwyt rozwiniętej karty")
        val line = compose.onNodeWithContentDescription(EXPAND_HANDLE).getUnclippedBoundsInRoot().bottom
        val header = compose.onNode(hasContentDescription("Budynek A-1, ", substring = true))
        val parts = listOf(header) + listOf("Wejścia", "Piętra").map(compose::onNodeWithText)
        parts.forEach { assertTrue("pełny opis zaczyna się pod kreską $line", top(it) >= line) }
        tapHandle()
        assertEquals("góra uchwytu: znowu niska", low.value, handleTop().value, 1f)
        compose.onNode(sheetHandle(isExpanded = false)).assertExists("uchwyt niskiej karty")
        compose.onNodeWithText("Piętra").assertDoesNotExist()
    }

    /**
     * Wiersz: A-1 na mapie ekranu niskiego, nad kartą mniej niż 300 dp mapy → nad kartą nie ma „Powiększ”,
     * „Pomniejsz” ani czterech strzałek przesuwania; po „Zamknij” wszystkie wracają.
     * Scenariusz: przegladanie-mapy.feature: Przy niskiej mapie nad kartą nie ma przycisków mapy
     *
     * Wymagania: US2.8, NFR-53
     */
    @Test
    @Config(qualifiers = "w360dp-h600dp")
    fun hidesMapButtonsOverLowMap() {
        showApp()
        MAP_BUTTONS.forEach { compose.onNodeWithContentDescription(it).assertExists("$it bez karty") }
        compose.runOnUiThread { touch(listOf("A-1")) }
        compose.waitForIdle()
        assertTrue("nad kartą ${handleTop()} mapy", handleTop() < FREE_MAP_FOR_BUTTONS)
        MAP_BUTTONS.forEach { compose.onNodeWithContentDescription(it).assertDoesNotExist() }
        compose.onNodeWithText("Zamknij").performClick()
        compose.waitForIdle()
        MAP_BUTTONS.forEach { compose.onNodeWithContentDescription(it).assertExists("$it po zamknięciu karty") }
    }
}
