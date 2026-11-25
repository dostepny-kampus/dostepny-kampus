/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.debug

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Włącznik trybu pokazu wersji debug: dotknięcie opisu przełącza tryb. Wymagania: US6.1 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class DemoModeSwitchTest {
    /** Ekran testowy Compose. */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun tapTurnsDemoOn() {
        val changes = mutableListOf<Boolean>()
        compose.setContent {
            DostepnyKampusTheme { DemoModeSwitch(isEnabled = false, onChange = { changes += it }) }
        }
        compose.onNodeWithText(DESCRIPTION, substring = true).assertIsOff()
        compose.onNodeWithText(DESCRIPTION, substring = true).performClick()
        assertEquals("zmiany przełącznika", listOf(true), changes)
    }

    private companion object {
        const val DESCRIPTION = "Pomiar C1 z zapisanej trasy GNSS"
    }
}
