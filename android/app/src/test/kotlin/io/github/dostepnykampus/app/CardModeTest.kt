/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import io.github.dostepnykampus.feature.map.CardMode
import org.junit.Assert.assertEquals
import org.junit.Test

/** Wybór trybu karty budynku ze skali czcionki, szerokości i orientacji okna (US2.9, NFR-20, NFR-51). */
class CardModeTest {
    // Wiersz: V2, pełny ekran od razu od skali czcionki 1,5; poniżej rozwinięty z małej karty, US2.9, NFR-20.
    @Test
    fun nfr20_large_font_opens_direct() {
        val modes = listOf(1f, 1.49f, 1.5f, 2f).map { cardModeFor(fontScale = it) }
        assertEquals(listOf(CardMode.Expanded, CardMode.Expanded, CardMode.Direct, CardMode.Direct), modes)
    }

    // Wiersz: V2 przy skali czcionki co najmniej 1,5, w oknie co najmniej 600 dp albo poziomo → pełny ekran od razu;
    // pionowo w węższym oknie przy mniejszej czcionce mała karta rozwijana do pełnego ekranu, NFR-20, NFR-51, NFR-22.
    @Test
    fun nfr51_wide_or_landscape_direct() {
        val modes =
            listOf(
                CardWindow(fontScale = 1.49f, widthDp = 599, isLandscape = false),
                CardWindow(fontScale = 1.5f, widthDp = 360, isLandscape = false),
                CardWindow(fontScale = 1f, widthDp = 600, isLandscape = false),
                CardWindow(fontScale = 1f, widthDp = 599, isLandscape = true),
            ).map { it to it.cardMode() }
        val expected = listOf(CardMode.Expanded, CardMode.Direct, CardMode.Direct, CardMode.Direct)
        assertEquals(modes.map { it.first }.zip(expected), modes)
    }
}
