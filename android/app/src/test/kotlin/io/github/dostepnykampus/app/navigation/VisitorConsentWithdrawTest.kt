/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.navigation

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.github.dostepnykampus.app.AppShell
import io.github.dostepnykampus.app.MapTab
import io.github.dostepnykampus.app.TestRoles
import io.github.dostepnykampus.app.TestSignIn
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.Account
import io.github.dostepnykampus.feature.capture.navigation.ReportRoute
import io.github.dostepnykampus.feature.map.navigation.MapRoute
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Zalogowany Odwiedzający nie jest proszony o logowanie ponownie, dopóki nie wyloguje się albo nie wycofa zgody.
 *
 * Wymagania: US14.4, NFR-29, NFR-30
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class VisitorConsentWithdrawTest {
    /** Ekran testowy Compose. */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /**
     * Zalogowany po wznowieniu otwiera od razu V4, a po wycofaniu zgody wraca do V1 i znów musi się zalogować.
     * Scenariusz: uprawnienia.feature: Zalogowany Odwiedzający nie jest proszony o logowanie ponownie
     */
    @Test
    fun asksSignInAfterWithdrawal() {
        val roles = TestRoles(Account.SignedIn())
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            DostepnyKampusTheme {
                AppShell(firstLaunch = false, roles = roles, onFirstHelpLeft = {}) { route ->
                    when (route) {
                        is MapRoute -> Text("V1")
                        is SignInRoute -> TestSignIn(route)
                        is ReportRoute -> Text("V4 krok 1")
                        else -> AppScreen(route)
                    }
                }
            }
        }
        compose.onNodeWithText("Zgłoś barierę").performClick()
        compose.onNodeWithText("V4 krok 1").assertIsDisplayed()
        compose.onNodeWithText("Zaloguj się").assertDoesNotExist()
        pressBack()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Zgłoś barierę").performClick()
        compose.onNodeWithText("V4 krok 1").assertIsDisplayed()
        pressBack()
        compose.onNodeWithText("Konto").performClick()
        compose.onNodeWithText("Wycofaj zgodę").performScrollTo().performClick()
        compose.onNodeWithText("Zostaw zgodę").performClick()
        compose.onNodeWithText("Twoje role: Odwiedzający").assertIsDisplayed()
        compose.onNodeWithText("Wycofaj zgodę").performScrollTo().performClick()
        compose.onNodeWithText("Wycofaj zgodę").performClick()
        assertEquals("wycofanie zgody zmienia konto na Gościa", Account.Guest, roles.account.value)
        compose.onNodeWithText("V1").assertIsDisplayed()
        compose.onNode(MapTab).assertIsSelected()
        compose.onNodeWithText("Zgłoś barierę").performClick()
        compose.onNodeWithText("Zaloguj się").assertIsDisplayed()
        compose.onNodeWithText("V4 krok 1").assertDoesNotExist()
    }

    private fun pressBack() {
        compose.waitForIdle()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }
}
