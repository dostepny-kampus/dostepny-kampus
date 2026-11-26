/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem.card

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val TITLE = "Budynek A-1"
private const val OPEN = "Otwórz"
private const val INFO = "Twoje role: Odwiedzający"
private const val REFRESH = "Odśwież"

/**
 * Karta pozycji listy, czynność karty i panel informacyjny: wcięcie treści, szerokość i wysokość czynności, jej
 * rola i nazwa oraz wzrost z czcionką.
 *
 * Wymagania: NFR-17, NFR-18, NFR-20
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class CardTest {
    @get:Rule
    val compose = createComposeRule()

    private var opened = 0

    /** Treść karty stoi 16 dp od krawędzi karty na całą szerokość okna. */
    @Test
    fun cardIndentsContent() {
        show { ListItemCard { Text(TITLE) } }
        val title = compose.onNodeWithText(TITLE).getUnclippedBoundsInRoot()
        assertEquals("Treść karty zaczyna się 16 dp od krawędzi", 16f, title.left.value)
        assertEquals("Treść karty zaczyna się 16 dp od góry", 16f, title.top.value)
    }

    /** Treść panelu stoi 16 dp od krawędzi panelu. */
    @Test
    fun panelIndentsContent() {
        show { InfoPanel { Text(INFO) } }
        val info = compose.onNodeWithText(INFO).getUnclippedBoundsInRoot()
        assertEquals(
            "Treść panelu zaczyna się 16 dp od krawędzi",
            listOf(16f, 16f),
            listOf(info.left.value, info.top.value),
        )
    }

    /** Czynność jest przyciskiem szerokim jak napis i co najmniej 48 dp wysokim. */
    @Test
    fun actionIsCompactButton() {
        show { ListItemCard { CardAction(text = OPEN, onClick = { opened++ }, opensScreen = true) } }
        val node = compose.onNodeWithText(OPEN)
        node.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        val bounds = node.getUnclippedBoundsInRoot()
        assertTrue("Czynność ma być węższa niż treść karty: $bounds", (bounds.right - bounds.left).value < 328f)
        assertTrue("Czynność ma mieć co najmniej 48 dp: $bounds", (bounds.bottom - bounds.top).value >= 48f)
        node.performClick()
        assertEquals("Dotknięcie czynności wywołuje ją raz", 1, opened)
    }

    /** Przycisk obrysowany w treści ma co najmniej 48 dp, a wyłączony nie wywołuje czynności. */
    @Test
    fun outlinedActionIsButton() {
        show {
            Column {
                OutlinedAction(onClick = { opened++ }) { Text(REFRESH) }
                OutlinedAction(onClick = { opened++ }, enabled = false) { Text(OPEN) }
            }
        }
        val refresh = compose.onNodeWithText(REFRESH)
        val bounds = refresh.getUnclippedBoundsInRoot()
        assertTrue("Przycisk ma mieć co najmniej 48 dp: $bounds", (bounds.bottom - bounds.top).value >= 48f)
        refresh.performClick()
        compose.onNodeWithText(OPEN).assertIsNotEnabled().performClick()
        assertEquals("Czynność wywołuje tylko włączony przycisk", 1, opened)
    }

    /** Przy czcionce 200 % czynność rośnie z napisem ponad 48 dp. */
    @Test
    fun actionGrowsWithFont() {
        show(fontScale = 2f) { CardAction(text = OPEN, onClick = {}) }
        val bounds = compose.onNodeWithText(OPEN).getUnclippedBoundsInRoot()
        assertTrue("Czynność przy 200 % ma być wyższa niż 48 dp: $bounds", (bounds.bottom - bounds.top).value > 48f)
    }

    private fun show(fontScale: Float = 1f, content: @Composable () -> Unit) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                DostepnyKampusTheme(content = content)
            }
        }
    }
}
