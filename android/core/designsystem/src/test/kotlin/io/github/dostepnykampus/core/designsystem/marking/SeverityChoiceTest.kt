/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem.marking

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private val WORDS = listOf("Utrudnienie", "Tylko z pomocą", "Blokada")

/** Wybór „Uciążliwość”: każda wartość ze znakiem i napisem. Wymagania: US7.1, US10.3, NFR-21 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class SeverityChoiceTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /**
     * Wiersz: trzy wartości uciążliwości, każda ze znakiem przed napisem; znak bez własnej nazwy dla TalkBack.
     *
     * Wymagania: US7.1, NFR-21, NFR-18
     */
    @Test
    fun drawsSignPerValue() {
        compose.setContent { DostepnyKampusTheme { SeverityChoice(severity = null, onPick = {}) } }
        val signs = compose.onAllNodesWithTag(SEVERITY_SIGN_TAG, useUnmergedTree = true).fetchSemanticsNodes()
        assertEquals("znak przy każdej wartości", WORDS.size, signs.size)
        WORDS.zip(signs).forEach { (word, sign) ->
            val text = compose.onNodeWithText(word, useUnmergedTree = true).fetchSemanticsNode()
            assertTrue("znak przed napisem „$word”", sign.boundsInRoot.right <= text.boundsInRoot.left)
            val name = sign.config.getOrElseNullable(SemanticsProperties.ContentDescription) { null }
            assertEquals("znak „$word” bez nazwy dla TalkBack", null, name)
        }
    }
}
