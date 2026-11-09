/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.BarrierGroup
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Krok 2 V4: 6 grup barier dużymi przyciskami i pytanie o piętro z „Nie wiem” (US4.3, US4.5, BR-14). */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class QuickGroupStepTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    // Wiersz: V4 krok 2, wybór grupy → krok 3 albo pytanie o piętro, US4.3.
    @Test
    fun us4_3_six_groups() {
        val chosen = mutableListOf<BarrierGroup>()
        compose.setContent { DostepnyKampusTheme { QuickGroupQuestion(onGroup = { chosen.add(it) }) } }
        compose.onNodeWithText("Czego dotyczy bariera?").assertIsDisplayed()
        listOf("Wejście i drzwi", "Schody i windy", "Przejścia i chodniki", "Toalety", "Napisy i oznaczenia")
            .forEach { compose.onNodeWithText(it).assertIsDisplayed() }
        compose.onNodeWithText("Coś innego").performClick()
        assertEquals(listOf(BarrierGroup.OTHER), chosen)
    }

    // Wiersz: V4 krok 2, pytanie o piętro → „Nie wiem” zostawia piętro puste, US4.5.
    @Test
    fun us4_5_floor_or_unknown() {
        val floors = mutableListOf<Int?>()
        compose.setContent {
            DostepnyKampusTheme { QuickFloorQuestion(group = BarrierGroup.TOILETS, onFloor = { floors.add(it) }) }
        }
        compose.onNodeWithText("Na którym piętrze?").assertIsDisplayed()
        compose.onNodeWithText("Nie wiem").performClick()
        compose.onNodeWithText("Parter").performClick()
        compose.onNodeWithText("2").performClick()
        assertEquals(listOf(null, 0, 2), floors)
    }
}
