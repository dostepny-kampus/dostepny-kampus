/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Wspólny wybór piętra: rola wyboru jednej opcji, stan „wybrane” i cel dotyku. Wymagania: NFR-17, NFR-18, NFR-20 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class FloorPickerTest {
    @get:Rule
    val compose = createComposeRule()

    private val options =
        FloorOptions(listOf(FloorOption(key = "p0", label = "Parter"), FloorOption(key = "p1", label = "1 piętro")))
    private val chosen = mutableListOf<FloorOption>()

    private fun show(layout: FloorPickerLayout) {
        compose.setContent {
            DostepnyKampusTheme {
                FloorPicker(options = options, selectedKey = "p1", onSelect = { chosen += it }, layout = layout)
            }
        }
    }

    /** Chipy: każda opcja to przycisk opcji, wybrana ma stan „wybrane”, cel co najmniej 48 dp. Wymagania: NFR-18 */
    @Test
    fun nfr18_chips_radio_selected() {
        show(FloorPickerLayout.CHIPS)
        val radio = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)
        compose.onNodeWithText("1 piętro").assert(radio).assertIsSelected()
        val parter = compose.onNodeWithText("Parter")
        parter.assert(radio)
        parter.assertIsNotSelected()
        parter.performClick()
        assertEquals(listOf(options.items.first()), chosen)
        val bounds = compose.onNodeWithText("Parter").getUnclippedBoundsInRoot()
        assertTrue(bounds.bottom - bounds.top >= 48.dp)
    }

    /** Lista: opcje na całą szerokość, każda co najmniej 56 dp. Wymagania: NFR-17 */
    @Test
    fun nfr17_list_full_width() {
        show(FloorPickerLayout.LIST)
        val bounds = compose.onNodeWithText("Parter").getUnclippedBoundsInRoot()
        assertEquals(360.dp, bounds.right - bounds.left)
        assertTrue(bounds.bottom - bounds.top >= 56.dp)
    }

    /** Czcionka 200 %: napisy rosną, a cel dotyku zostaje co najmniej 48 dp. Wymagania: NFR-20 */
    @Test
    @Config(fontScale = 2f)
    fun nfr20_chips_large_font() {
        show(FloorPickerLayout.CHIPS)
        val bounds = compose.onNodeWithText("1 piętro").getUnclippedBoundsInRoot()
        assertTrue(bounds.bottom - bounds.top >= 48.dp)
    }
}
