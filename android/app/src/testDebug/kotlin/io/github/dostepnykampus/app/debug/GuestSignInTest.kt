/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
@file:OptIn(TymczasoweRole::class)

package io.github.dostepnykampus.app.debug

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation3.runtime.NavKey
import io.github.dostepnykampus.app.AppScreen
import io.github.dostepnykampus.app.AppShell
import io.github.dostepnykampus.app.navigation.AppNavigator
import io.github.dostepnykampus.core.data.account.TymczasoweRole
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.feature.capture.navigation.ReportRoute
import io.github.dostepnykampus.feature.map.navigation.MapRoute
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Gość na L1 po zmianie konta na zalogowane przechodzi dalej sam: z „Zgłoś barierę” do V4, krok 1, z wejściem
 * wybranym przed L1, a z „Konto” do L2 (US14.2, UC7 1a). Rola pochodzi z przełącznika, jak w aplikacji debug.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class GuestSignInTest {
    /** Ekran testowy Compose. */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val roles = DebugRoles()

    /** „Zgłoś barierę” → L1 → konto Odwiedzającego → V4, krok 1. */
    @Test
    fun guestReportAfterSignIn() {
        show()
        compose.onNodeWithText("Zgłoś barierę").performClick()
        compose.onNodeWithText(NOT_NOW).assertIsDisplayed()
        roles.switchTo(DebugRole.VISITOR.account)
        compose.onNodeWithText("V4 krok 1, wejście: brak").assertIsDisplayed()
    }

    /** „Zgłoś barierę” przy wejściu → L1 → zalogowanie → V4 z tym samym wejściem. */
    @Test
    fun guestEntranceKeptAfterSignIn() {
        show()
        compose.onNodeWithText(AT_ENTRANCE).performClick()
        roles.switchTo(DebugRole.COLLECTOR.account)
        compose.onNodeWithText("V4 krok 1, wejście: A-1-w1").assertIsDisplayed()
    }

    /** „Konto” → L1 → zalogowanie → L2 bez „Nie teraz”; „Nie teraz” na L1 nadal wraca do V1. */
    @Test
    fun guestAccountAfterSignIn() {
        show()
        compose.onNodeWithText(ACCOUNT).performClick()
        compose.onNodeWithText(NOT_NOW).performClick()
        compose.onNodeWithText(MAP).assertIsDisplayed()
        compose.onNodeWithText(ACCOUNT).performClick()
        roles.switchTo(DebugRole.VISITOR.account)
        compose.onNodeWithText(NOT_NOW).assertDoesNotExist()
    }

    private fun show() {
        compose.setContent {
            DostepnyKampusTheme {
                AppShell(firstLaunch = false, roles = roles, onFirstHelpLeft = {}) { route -> Screen(route) }
            }
        }
    }

    @Composable
    private fun AppNavigator.Screen(route: NavKey) {
        when (route) {
            is MapRoute -> {
                Column {
                    Text(MAP)
                    Button(onClick = { report(entranceId = "A-1-w1") }) { Text(AT_ENTRANCE) }
                }
            }

            is ReportRoute -> {
                Text("V4 krok ${route.step.ordinal + 1}, wejście: ${route.entranceId ?: "brak"}")
            }

            else -> {
                AppScreen(route)
            }
        }
    }

    private companion object {
        const val MAP = "V1"
        const val ACCOUNT = "Konto"
        const val NOT_NOW = "Nie teraz"
        const val AT_ENTRANCE = "Zgłoś przy wejściu"
    }
}
