/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.TextUnit
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w390dp-h400dp-xhdpi")
class BottomBarTest {
    @get:Rule
    val compose = createComposeRule()

    private val labels = listOf("Mapa", "Zgłoś barierę", "Pomoc", "Konto")
    private val chosen = mutableListOf<Int>()

    @Test
    fun nfr18_tabs_names_and_selection() {
        compose.setContent { DostepnyKampusTheme { Bar(selected = 2, account = "Konto") } }
        labels.forEach { label ->
            val item = compose.onNodeWithText(label)
            item.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
            item.assertWidthIsAtLeast(CeleDotyku.minimum)
            item.assertHeightIsAtLeast(CeleDotyku.minimum)
        }
        compose.onNodeWithText("Pomoc").assertIsSelected()
        compose.onNodeWithText("Mapa").assertIsNotSelected().performClick()
        assertEquals(listOf(0), chosen)
    }

    @Test
    fun nfr16_pasek_dolny_jasny() {
        capture()
    }

    @Test
    @Config(qualifiers = "+night")
    fun nfr16_pasek_dolny_ciemny() {
        capture()
    }

    @Test
    @Config(fontScale = 2f)
    fun nfr20_pasek_dolny_czcionka_200() {
        capture()
    }

    @Test
    @Config(qualifiers = "w360dp-h400dp-xhdpi", fontScale = 2f)
    fun nfr20_pasek_dolny_360_200() {
        capture()
        val sizes = listOf("Mapa", "Zgłoś barierę", "Pomoc", "Konto", "Konto i\u00A0praca").map(::fontSizeOf)
        assertEquals(1, sizes.distinct().size)
    }

    private fun capture() {
        compose.setContent {
            DostepnyKampusTheme {
                Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                    Bar(selected = 2, account = "Konto")
                    Bar(selected = 3, account = "Konto i praca")
                }
            }
        }
        compose.onRoot().captureRoboImage()
    }

    private fun fontSizeOf(label: String): TextUnit {
        val layouts = mutableListOf<TextLayoutResult>()
        val node = compose.onAllNodesWithText(label, useUnmergedTree = true)[0].fetchSemanticsNode()
        node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        return layouts[0].layoutInput.style.fontSize
    }

    @Composable
    private fun Bar(selected: Int, account: String) {
        val icons = with(NavigationIcons) { listOf(Map, Camera, Help, Account) }
        val items = (labels.dropLast(1) + account).zip(icons) { text, icon -> BottomBarItem(label = text, icon = icon) }
        BottomBar(items = BottomBarItems(items), selectedIndex = selected, onSelect = chosen::add)
    }
}
