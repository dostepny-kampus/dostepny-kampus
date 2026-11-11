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
import io.github.dostepnykampus.core.model.Floor
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

    /** Wiersz: V4 krok 2, wybór grupy → krok 3 albo pytanie o piętro, US4.3. */
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

    /** Wiersz: V4 krok 2, pytanie o piętro → „Nie wiem” zostawia piętro puste, US4.5. */
    @Test
    fun us4_5_floor_or_unknown() {
        val answers = mutableListOf<QuickFloor>()
        show(floors = emptyList(), answers = answers)
        compose.onNodeWithText("Na którym piętrze?").assertIsDisplayed()
        compose.onNodeWithText("Nie wiem").performClick()
        compose.onNodeWithText("Parter").performClick()
        compose.onNodeWithText("2").performClick()
        assertEquals(listOf(QuickFloor(), QuickFloor(number = 0), QuickFloor(number = 2)), answers)
    }

    /** Wiersz: V4 krok 2 w budynku podstawowym, piętra pod parterem i wyższe niż 4, US4.5. */
    @Test
    fun us4_5_basic_range() {
        show(floors = emptyList(), answers = mutableListOf())
        listOf("-1", "Parter", "5", "10").forEach { compose.onNodeWithText(it).assertExists() }
        compose.onNodeWithText("-2").assertDoesNotExist()
        compose.onNodeWithText("11").assertDoesNotExist()
    }

    /** Wiersz: V4 krok 2 w budynku szczegółowym, etykiety pięter z danych PWr od najniższego, US4.5. */
    @Test
    fun us4_5_detailed_labels() {
        val answers = mutableListOf<QuickFloor>()
        val basement = Floor(id = "A-1/p-1", label = "Piwnica", order = -1)
        show(floors = listOf(basement, Floor(id = "A-1/p0", label = "Parter", order = 0)), answers = answers)
        compose.onNodeWithText("Nie wiem").assertIsDisplayed()
        compose.onNodeWithText("2").assertDoesNotExist()
        compose.onNodeWithText("Piwnica").performClick()
        assertEquals(listOf(QuickFloor(floor = basement)), answers)
    }

    private fun show(floors: List<Floor>, answers: MutableList<QuickFloor>) {
        compose.setContent {
            DostepnyKampusTheme {
                QuickFloorQuestion(group = BarrierGroup.TOILETS, floors = floors, onFloor = { answers.add(it) })
            }
        }
    }
}
