/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
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

/** Osoba z jedną rolą pracowniczą w L2 „Konto i praca”: widzi czynności swojej roli, nie cudze. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class StaffScreensTest {
    /** Ekran testowy Compose. */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /**
     * Scenariusz (przykład: Zbierającego | kolejka weryfikacji): uprawnienia.feature:
     *   Bez wymaganej roli czynność jest niedostępna w aplikacji i na serwerze
     * Scenariusz (przykład: Zbierającego | postęp inwentaryzacji): uprawnienia.feature:
     *   Bez wymaganej roli czynność jest niedostępna w aplikacji i na serwerze.
     */
    @Test
    fun collectorHasNoQueueNorProgress() {
        openWork(StaffRole.COLLECTOR)
        compose.onNodeWithText(MEASURE).assertIsDisplayed()
        compose.onNodeWithText(MINE).assertIsDisplayed()
        compose.onNodeWithText(QUEUE).assertDoesNotExist()
        compose.onNodeWithText(PROGRESS).assertDoesNotExist()
    }

    /**
     * Scenariusz (przykład: Weryfikatora | eksport): uprawnienia.feature:
     *   Bez wymaganej roli czynność jest niedostępna w aplikacji i na serwerze
     * Scenariusz (przykład: Weryfikatora | zaproszenie): uprawnienia.feature:
     *   Bez wymaganej roli czynność jest niedostępna w aplikacji i na serwerze.
     */
    @Test
    fun verifierHasNoExportNorInvite() {
        openWork(StaffRole.VERIFIER)
        compose.onNodeWithText("Konto i praca").assertIsDisplayed()
        compose.onNodeWithText(MINE).assertDoesNotExist()
        compose.onNodeWithText(MEASURE).assertDoesNotExist()
        compose.onNodeWithText(EXPORT).assertDoesNotExist()
        compose.onNodeWithText(INVITE).assertDoesNotExist()
    }

    /**
     * Scenariusz (przykład: Koordynatora | pomiar precyzyjny): uprawnienia.feature:
     *   Bez wymaganej roli czynność jest niedostępna w aplikacji i na serwerze
     * Scenariusz (przykład: Koordynatora | kolejka weryfikacji): uprawnienia.feature:
     *   Bez wymaganej roli czynność jest niedostępna w aplikacji i na serwerze.
     */
    @Test
    fun coordinatorHasNoMeasureQueue() {
        openWork(StaffRole.COORDINATOR)
        compose.onNodeWithText(PROGRESS).assertIsDisplayed()
        compose.onNodeWithText(MEASURE).assertDoesNotExist()
        compose.onNodeWithText(MINE).assertDoesNotExist()
        compose.onNodeWithText(QUEUE).assertDoesNotExist()
    }

    private fun openWork(role: StaffRole) {
        compose.showShell(Account.SignedIn(setOf(role)))
        compose.onNodeWithText("praca", substring = true).performClick()
    }

    private companion object {
        const val MEASURE = "Zmierz i zgłoś barierę"
        const val MINE = "Moje zgłoszenia"
        const val PROGRESS = "Postęp inwentaryzacji"
        const val QUEUE = "Zgłoszenia do sprawdzenia"
        const val EXPORT = "Pobierz eksport"
        const val INVITE = "Zaproś osobę"
    }
}
