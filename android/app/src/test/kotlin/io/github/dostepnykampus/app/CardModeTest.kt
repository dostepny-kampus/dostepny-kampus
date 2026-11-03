/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import io.github.dostepnykampus.feature.map.CardMode
import org.junit.Assert.assertEquals
import org.junit.Test

/** Wybór trybu karty budynku z skali czcionki (US2.9, NFR-20). */
class CardModeTest {
    // Wiersz: V2, pełny ekran od razu od skali czcionki 1,5; poniżej rozwinięty z małej karty, US2.9, NFR-20.
    @Test
    fun nfr20_large_font_opens_direct() {
        val modes = listOf(1f, 1.49f, 1.5f, 2f).map { cardModeFor(fontScale = it) }
        assertEquals(listOf(CardMode.Expanded, CardMode.Expanded, CardMode.Direct, CardMode.Direct), modes)
    }
}
