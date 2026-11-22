/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.debug

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation3.runtime.NavKey
import io.github.dostepnykampus.app.AppShell
import io.github.dostepnykampus.app.TestRoles
import io.github.dostepnykampus.app.navigation.AppNavigator
import io.github.dostepnykampus.app.navigation.AppScreen
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.Account
import io.github.dostepnykampus.feature.map.navigation.MapRoute
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Zalogowany Odwiedzający w L2 „Konto”: bez czynności zbierających i koordynatora. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class VisitorScreensTest {
    /** Ekran testowy Compose. */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /**
     * Scenariusz (przykład: Odwiedzającego | pomiar precyzyjny): uprawnienia.feature:
     *   Bez wymaganej roli czynność jest niedostępna w aplikacji i na serwerze
     * Scenariusz (częściowo): uprawnienia.feature: Samodzielna rejestracja daje tylko uprawnienia Odwiedzającego.
     */
    @Test
    fun visitorHasNoMeasure() {
        show()
        compose.onNodeWithText("Konto").performClick()
        compose.onNodeWithText(MEASURE).assertDoesNotExist()
        compose.onNodeWithText(PROGRESS).assertDoesNotExist()
    }

    /**
     * Scenariusz (przykład: Odwiedzającego | lista zgłoszeń): uprawnienia.feature:
     *   Bez wymaganej roli czynność jest niedostępna w aplikacji i na serwerze.
     */
    @Test
    fun visitorHasNoMine() {
        show()
        compose.onNodeWithText("Konto").performClick()
        compose.onNodeWithText(MINE).assertDoesNotExist()
    }

    private fun show() {
        val roles = TestRoles(Account.SignedIn())
        compose.setContent {
            DostepnyKampusTheme {
                AppShell(firstLaunch = false, roles = roles, onFirstHelpLeft = {}) { route -> Screen(route) }
            }
        }
    }

    @Composable
    private fun AppNavigator.Screen(route: NavKey) {
        if (route is MapRoute) Text("V1") else AppScreen(route)
    }

    private companion object {
        const val MEASURE = "Zmierz i zgłoś barierę"
        const val MINE = "Moje zgłoszenia"
        const val PROGRESS = "Postęp inwentaryzacji"
    }
}
