/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.area

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.feature.map.rememberNoticeReserve
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * Miejsce na napis o zmianie terenu, które kamera zostawia nad małą kartą V2.
 *
 * Wymagania: US1.8, US2.8, NFR-53
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AreaNoticeReserveTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val shift = AreaShift(name = "Wittigowo", kilometers = 2, direction = CompassDirection.SouthEast)

    /**
     * Wiersz: V2, napis w węższej mapie ma więcej wierszy, więc kamera zostawia na niego więcej miejsca; bez
     * napisu miejsca nie zostawia.
     *
     * Wymagania: US2.8, NFR-53
     */
    @Test
    fun measuresNoticeByMapWidth() {
        var wide = 0.dp
        var narrow = 0.dp
        var none = 1.dp
        compose.setContent {
            DostepnyKampusTheme {
                wide = rememberNoticeHeight(shift = shift, width = WIDE)
                narrow = rememberNoticeHeight(shift = shift, width = NARROW)
                none = rememberNoticeHeight(shift = null, width = WIDE)
            }
        }
        compose.waitForIdle()
        assertTrue("napis $wide", wide > 0.dp)
        assertTrue("wąska mapa $narrow, szeroka $wide", narrow > wide)
        assertEquals(0.dp, none)
    }

    /**
     * Wiersz: V2, zgaszenie napisu nie zmienia miejsca zostawionego przez kamerę dla tego budynku; inny budynek
     * bierze wysokość napisu ze swojego wyboru.
     *
     * Wymagania: US1.8, US2.8
     */
    @Test
    fun keepsReserveUntilOtherBuilding() {
        var building by mutableStateOf("T-15")
        var notice by mutableStateOf(RESERVE)
        var reserve = 0.dp
        compose.setContent { reserve = rememberNoticeReserve(building = building, notice = notice) }
        notice = 0.dp
        compose.waitForIdle()
        assertEquals(RESERVE, reserve)
        building = "A-1"
        compose.waitForIdle()
        assertEquals(0.dp, reserve)
    }

    private companion object {
        val WIDE: Dp = 360.dp
        val NARROW: Dp = 200.dp
        val RESERVE: Dp = 72.dp
    }
}
