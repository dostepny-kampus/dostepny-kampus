/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
@file:OptIn(TymczasoweRole::class)

package io.github.dostepnykampus.app.debug

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation3.runtime.NavKey
import io.github.dostepnykampus.app.AppShell
import io.github.dostepnykampus.app.navigation.AppNavigator
import io.github.dostepnykampus.app.navigation.AppScreen
import io.github.dostepnykampus.core.data.account.TymczasoweRole
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.Account
import io.github.dostepnykampus.core.model.StaffRole
import io.github.dostepnykampus.feature.map.navigation.MapRoute
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Wejście „Postęp inwentaryzacji” w L2 „Konto i praca”: jest tylko dla Koordynatora. Pasek i L2 są prawdziwe,
 * mapa ma lekki zastępnik.
 *
 * Wymagania: US11.1, US14.1
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class CoordinatorEntryTest {
    /** Ekran testowy Compose. */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /**
     * Koordynator widzi wejście do K1.
     *
     * Wymagania: US11.1
     */
    @Test
    fun coordinatorSeesProgress() {
        show(StaffRole.COORDINATOR)
        compose.onNodeWithText(PROGRESS).assertIsDisplayed()
    }

    /**
     * Zbierający nie widzi wejścia do K1.
     *
     * Wymagania: US14.1
     */
    @Test
    fun collectorHasNoProgress() {
        show(StaffRole.COLLECTOR)
        compose.onNodeWithText(PROGRESS).assertDoesNotExist()
    }

    /**
     * Weryfikator nie widzi wejścia do K1.
     *
     * Wymagania: US14.1
     */
    @Test
    fun verifierHasNoProgress() {
        show(StaffRole.VERIFIER)
        compose.onNodeWithText(PROGRESS).assertDoesNotExist()
    }

    private fun show(role: StaffRole) {
        val roles = DebugRoles().apply { switchTo(Account.SignedIn(setOf(role))) }
        compose.setContent {
            DostepnyKampusTheme {
                AppShell(firstLaunch = false, roles = roles, onFirstHelpLeft = {}) { route -> Screen(route) }
            }
        }
        /* Napis pozycji paska łamie się twardą spacją zależnie od szerokości, więc szukamy po jego końcówce. */
        compose.onNodeWithText("praca", substring = true).performClick()
    }

    @Composable
    private fun AppNavigator.Screen(route: NavKey) {
        if (route is MapRoute) Text("V1") else AppScreen(route)
    }

    private companion object {
        const val PROGRESS = "Postęp inwentaryzacji"
    }
}
