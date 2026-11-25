/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.debug

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.dostepnykampus.core.model.Account
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
     *   Bez wymaganej roli czynność jest niedostępna w aplikacji i na serwerze.
     */
    @Test
    fun visitorHasNoMeasure() {
        compose.showShell(Account.SignedIn())
        compose.onNodeWithText(ACCOUNT).performClick()
        compose.onNodeWithText(MEASURE).assertDoesNotExist()
        compose.onNodeWithText(PROGRESS).assertDoesNotExist()
    }

    /**
     * Scenariusz (przykład: Odwiedzającego | lista zgłoszeń): uprawnienia.feature:
     *   Bez wymaganej roli czynność jest niedostępna w aplikacji i na serwerze.
     */
    @Test
    fun visitorHasNoMine() {
        compose.showShell(Account.SignedIn())
        compose.onNodeWithText(ACCOUNT).performClick()
        compose.onNodeWithText(MINE).assertDoesNotExist()
    }

    /** Scenariusz: uprawnienia.feature: Samodzielna rejestracja daje tylko uprawnienia Odwiedzającego. */
    @Test
    fun visitorSeesOnlyOwnRole() {
        compose.showShell(Account.SignedIn())
        compose.onNodeWithText(ACCOUNT).performClick()
        compose.onNodeWithText("Twoje role: Odwiedzający").assertIsDisplayed()
        compose.onNodeWithText(MEASURE).assertDoesNotExist()
        compose.onNodeWithText(QUEUE).assertDoesNotExist()
        compose.onNodeWithText(PROGRESS).assertDoesNotExist()
    }

    private companion object {
        const val ACCOUNT = "Konto"
        const val MEASURE = "Zmierz i zgłoś barierę"
        const val MINE = "Moje zgłoszenia"
        const val PROGRESS = "Postęp inwentaryzacji"
        const val QUEUE = "Zgłoszenia do sprawdzenia"
    }
}
