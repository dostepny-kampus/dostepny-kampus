/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import io.github.dostepnykampus.app.navigation.CardWindow
import io.github.dostepnykampus.app.navigation.cardMode
import io.github.dostepnykampus.app.navigation.cardModeFor
import io.github.dostepnykampus.feature.map.building.CardMode
import org.junit.Assert.assertEquals
import org.junit.Test

/** Wybór trybu karty budynku ze skali czcionki, szerokości i orientacji okna. Wymagania: US2.9, NFR-20, NFR-51 */
class CardModeTest {
    /**
     * Wiersz: V2 od skali czcionki 1,5 na cały ekran nad mapą z „Zamknij”; poniżej rozwinięty z małej karty.
     *
     * Wymagania: US2.9, NFR-20
     */
    @Test
    fun opensLargeForLargeFont() {
        val modes = listOf(1f, 1.49f, 1.5f, 2f).map { cardModeFor(fontScale = it) }
        assertEquals(listOf(CardMode.Expanded, CardMode.Expanded, CardMode.Large, CardMode.Large), modes)
    }

    /**
     * Wiersz: V2 w oknie co najmniej 600 dp albo poziomo → pełny ekran od razu; pionowo w węższym oknie przy skali
     * czcionki co najmniej 1,5 karta na cały ekran nad mapą, przy mniejszej mała karta rozwijana do pełnego ekranu.
     *
     * Wymagania: NFR-20, NFR-51, NFR-22
     */
    @Test
    fun opensDirectWhenWide() {
        val modes =
            listOf(
                CardWindow(fontScale = 1.49f, widthDp = 599, isLandscape = false),
                CardWindow(fontScale = 1.5f, widthDp = 360, isLandscape = false),
                CardWindow(fontScale = 1f, widthDp = 600, isLandscape = false),
                CardWindow(fontScale = 1f, widthDp = 599, isLandscape = true),
            ).map { it to it.cardMode() }
        val expected = listOf(CardMode.Expanded, CardMode.Large, CardMode.Direct, CardMode.Direct)
        assertEquals(modes.map { it.first }.zip(expected), modes)
    }
}
