/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.compose.ui.unit.dp
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Obraz mapy po zamknięciu arkusza mimo zmiany paska u góry. Wymagania: US18.1, NFR-56 */
class SheetClosingTest {
    /** Wiersz: pole wyszukiwania schowane przy karcie sali wraca po jej zamknięciu i dochodzi cały pasek. */
    @Test
    fun countsBarBackAfterRoomCard() {
        assertEquals(157.dp, barGrowth(wasCovered = false, top = 157.dp, topBefore = 0.dp), "Pasek nie wliczony")
    }

    /** Wiersz: pasek widoczny pod arkuszem i po nim nie zmienia dopełnienia. */
    @Test
    fun keepsSameBar() {
        assertEquals(0.dp, barGrowth(wasCovered = false, top = 157.dp, topBefore = 157.dp), "Pasek wliczony dwa razy")
    }

    /** Wiersz: nad kartą budynku górnego dopełnienia nie było, więc po zamknięciu dochodzi cały pasek. */
    @Test
    fun countsBarAfterBuildingCard() {
        assertEquals(157.dp, barGrowth(wasCovered = true, top = 157.dp, topBefore = 157.dp), "Pasek nie wliczony")
    }
}
