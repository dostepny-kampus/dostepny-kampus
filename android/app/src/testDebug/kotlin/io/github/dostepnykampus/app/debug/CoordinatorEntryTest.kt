/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.debug

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.dostepnykampus.core.model.Account
import io.github.dostepnykampus.core.model.StaffRole
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
        compose.showShell(Account.SignedIn(setOf(role)))
        compose.onNodeWithText("praca", substring = true).performClick()
    }

    private companion object {
        const val PROGRESS = "Postęp inwentaryzacji"
    }
}
