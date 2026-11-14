/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.quick

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.BarrierGroup
import io.github.dostepnykampus.core.model.Floor
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Krok 2 V4: 6 grup barier dużymi przyciskami i pytanie o piętro z „Nie wiem”. Wymagania: US4.3, US4.5, BR-14 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class QuickGroupStepTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /**
     * Wiersz: V4 krok 2, 6 grup dużymi przyciskami co najmniej 48 × 48 dp, bez pytań o typ i uciążliwość; wybór
     * grupy → krok 3 albo pytanie o piętro. Napisy to prostsze nazwy grup z V4 (PW-7).
     * Scenariusz: zgloszenie-szybkie.feature: Wybór jednej z 6 grup barier dużymi przyciskami
     *
     * Wymagania: US4.3, NFR-17
     */
    @Test
    fun us4_3_six_groups() {
        val chosen = mutableListOf<BarrierGroup>()
        compose.setContent { DostepnyKampusTheme { QuickGroupQuestion(onGroup = { chosen.add(it) }) } }
        compose.onNodeWithText("Czego dotyczy bariera?").assertIsDisplayed()
        val groups =
            listOf(
                "Wejście i drzwi",
                "Schody i windy",
                "Przejścia i chodniki",
                "Toalety",
                "Napisy i oznaczenia",
                "Coś innego",
            )
        groups.forEach { label ->
            val button = compose.onNodeWithText(label).assertIsDisplayed()
            button.assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
        }
        val notAsked = listOf("Typ bariery", "Uciążliwość")
        notAsked.forEach { compose.onNodeWithText(it, substring = true).assertDoesNotExist() }
        compose.onNodeWithText("Coś innego").performClick()
        assertEquals(listOf(BarrierGroup.OTHER), chosen)
    }

    /**
     * Wiersz: V4 krok 2 po powrocie, grupa wybrana wcześniej jest zaznaczona dla oka i dla TalkBack.
     * Scenariusz (częściowo): zgloszenie-szybkie-powrot.feature: Powrót między krokami nie usuwa zdjęcia ani grupy
     *   barier
     *
     * Wymagania: US4.8, NFR-21
     */
    @Test
    fun us4_8_group_stays_selected() {
        compose.setContent { DostepnyKampusTheme { QuickGroupQuestion(selected = BarrierGroup.TOILETS, onGroup = {}) } }
        compose.onNodeWithText("✓ Toalety").assertIsSelected()
        compose.onNodeWithText("Coś innego").assertIsNotSelected()
    }

    /** Wiersz: V4 krok 2, pytanie o piętro → „Nie wiem” zostawia piętro puste. Wymagania: US4.5 */
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

    /** Wiersz: V4 krok 2 w budynku podstawowym, piętra pod parterem i wyższe niż 4. Wymagania: US4.5 */
    @Test
    fun us4_5_basic_range() {
        show(floors = emptyList(), answers = mutableListOf())
        listOf("-1", "Parter", "5", "10").forEach { compose.onNodeWithText(it).assertExists() }
        compose.onNodeWithText("-2").assertDoesNotExist()
        compose.onNodeWithText("11").assertDoesNotExist()
    }

    /** Wiersz: V4 krok 2 w budynku szczegółowym, etykiety pięter z danych PWr od najniższego. Wymagania: US4.5 */
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

    /** Wiersz: V4 krok 2 to wspólny wybór piętra: grupa jednej opcji, a każda opcja jest przyciskiem radiowym. */
    @Test
    fun us4_5_shared_picker() {
        show(floors = emptyList(), answers = mutableListOf())
        val radios = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)
        compose.onAllNodes(radios).assertCountEquals(BASIC_FLOOR_COUNT + 1)
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup)).assertExists()
    }

    /**
     * Wiersz: V4 krok 2 przy czcionce 200%: każde piętro ma cel co najmniej 48 dp i pełny napis.
     *
     * Wymagania: NFR-17, NFR-20
     */
    @Test
    @Config(qualifiers = "w360dp-h800dp", fontScale = 2f)
    fun nfr20_floor_targets() {
        show(floors = emptyList(), answers = mutableListOf())
        listOf("Nie wiem", "-1", "Parter", "10").forEach { label ->
            compose
                .onNodeWithText(label)
                .performScrollTo()
                .assertHeightIsAtLeast(48.dp)
                .assertIsDisplayed()
            compose.onNodeWithText(label).assertTextEquals(label)
        }
    }

    private fun show(floors: List<Floor>, answers: MutableList<QuickFloor>) {
        compose.setContent {
            DostepnyKampusTheme {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    QuickFloorQuestion(
                        group = BarrierGroup.TOILETS,
                        floors = QuickFloors(floors),
                        onFloor = { answers.add(it) },
                    )
                }
            }
        }
    }

    private companion object {
        const val BASIC_FLOOR_COUNT = 12
    }
}
