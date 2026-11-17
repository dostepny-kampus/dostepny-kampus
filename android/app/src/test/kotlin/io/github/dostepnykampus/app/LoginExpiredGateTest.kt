/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Aplikacja pokazuje prośbę o ponowne zalogowanie, gdy kolejka czeka po wygaśnięciu sesji.
 *
 * Wymagania: BR-26
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class LoginExpiredGateTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var isExpired by mutableStateOf(false)

    /** Scenariusz (częściowo): synchronizacja.feature: Wygasłe zalogowanie czeka na ponowne zalogowanie. */
    @Test
    fun showsBannerWhileExpired() {
        show()
        compose.onNodeWithText(EXPIRED).assertDoesNotExist()

        isExpired = true
        compose.onNodeWithText(EXPIRED).assertIsDisplayed()
        compose.onNodeWithText("Aplikacja").assertIsDisplayed()

        isExpired = false
        compose.onNodeWithText(EXPIRED).assertDoesNotExist()
    }

    private fun show() {
        compose.setContent {
            DostepnyKampusTheme {
                LoginExpiredGate(isExpired = isExpired) {
                    Text("Aplikacja")
                }
            }
        }
    }

    private companion object {
        const val EXPIRED = "Logowanie wygasło. Zaloguj się, a wyślemy zgłoszenia same."
    }
}
