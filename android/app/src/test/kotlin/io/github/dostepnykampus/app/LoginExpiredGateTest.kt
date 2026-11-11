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
import io.github.dostepnykampus.core.model.Account
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Po 401 aplikacja prosi o ponowne zalogowanie, a zalogowanie wznawia kolejkę bez dotykania ekranu (BR-26). */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class LoginExpiredGateTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val roles = TestRoles()
    private var isExpired by mutableStateOf(false)
    private var resumes = 0

    /** Scenariusz (częściowo): synchronizacja.feature: Wygasłe zalogowanie czeka na ponowne zalogowanie. */
    @Test
    fun br26_expired_then_resumed() {
        show()
        compose.onNodeWithText(EXPIRED).assertDoesNotExist()

        isExpired = true
        compose.onNodeWithText(EXPIRED).assertIsDisplayed()
        compose.onNodeWithText("Aplikacja").assertIsDisplayed()

        roles.account.value = Account.SignedIn()
        compose.waitForIdle()
        assertEquals(1, resumes)

        isExpired = false
        compose.onNodeWithText(EXPIRED).assertDoesNotExist()
    }

    @Test
    fun `bez zmiany konta czeka`() {
        roles.account.value = Account.SignedIn()
        isExpired = true
        show()
        compose.onNodeWithText(EXPIRED).assertIsDisplayed()
        assertEquals(0, resumes)
    }

    private fun show() {
        compose.setContent {
            DostepnyKampusTheme {
                LoginExpiredGate(isExpired = isExpired, roles = roles, onLoggedIn = { resumes++ }) {
                    Text("Aplikacja")
                }
            }
        }
    }

    private companion object {
        const val EXPIRED = "Logowanie wygasło. Zaloguj się, a wyślemy zgłoszenia same."
    }
}
