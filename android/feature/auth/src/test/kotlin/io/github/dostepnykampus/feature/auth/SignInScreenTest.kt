/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.auth

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.dostepnykampus.core.data.account.SignInEntry
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * L1 „Prośba o logowanie”: prośba o konto, brak sieci i błąd logowania.
 *
 * Wymagania: US14.2, US14.3, UC6, UC7, NFR-50
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class SignInScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val events = mutableListOf<String>()

    /** Wejście ze „Zgłoś barierę”: zdanie o trzech krokach; „Zaloguj się” i „Załóż konto” otwierają logowanie. */
    @Test
    fun opensSignInAndRegistration() {
        show(SignInUiState(), isForReport = true)
        compose.onNodeWithText("Aby zgłosić barierę, potrzebne jest konto.").assertIsDisplayed()
        compose.onNodeWithText(THREE_STEPS).assertIsDisplayed()
        compose.onNodeWithText("Zaloguj się").performClick()
        compose.onNodeWithText("Załóż konto").performClick()
        assertEquals("otwarte logowanie", listOf("SIGN_IN", "REGISTER"), events)
    }

    /** Wejście z „Konto”: bez zdania o trzech krokach; „Nie teraz” wraca do mapy. */
    @Test
    fun leavesWithNotNow() {
        show(SignInUiState(), isForReport = false)
        compose.onNodeWithText(THREE_STEPS).assertDoesNotExist()
        compose.onNodeWithText("Nie teraz").performClick()
        assertEquals("powrót do mapy", listOf("leave"), events)
    }

    /** Brak sieci: komunikat i tylko „Wróć do mapy”. */
    @Test
    fun offersMapWhenOffline() {
        show(SignInUiState(isOnline = false), isForReport = true)
        compose.onNodeWithText("Logowanie wymaga internetu.").assertIsDisplayed()
        compose.onNodeWithText("Zaloguj się").assertDoesNotExist()
        compose.onNodeWithText("Wróć do mapy").performClick()
        assertEquals("powrót do mapy", listOf("leave"), events)
    }

    /** Błąd logowania: komunikat, „Spróbuj ponownie” i „Nie teraz”. */
    @Test
    fun retriesAfterFailure() {
        show(SignInUiState(stage = SignInStage.FAILED), isForReport = true)
        compose.onNodeWithText("Logowanie się nie udało albo zostało przerwane.").assertIsDisplayed()
        compose.onNodeWithText("Spróbuj ponownie").performClick()
        compose.onNodeWithText("Nie teraz").performClick()
        assertEquals("ponowienie i powrót", listOf("retry", "leave"), events)
    }

    private fun show(state: SignInUiState, isForReport: Boolean) {
        compose.setContent {
            DostepnyKampusTheme {
                SignInContent(
                    state = state,
                    isForReport = isForReport,
                    actions = SignInActions(onBack = { events += "back" }, onLeave = { events += "leave" }),
                    onOpen = { entry: SignInEntry -> events += entry.name },
                    onRetry = { events += "retry" },
                )
            }
        }
    }

    private companion object {
        const val THREE_STEPS = "Po zalogowaniu od razu zrobisz zdjęcie. Całość to 3 krótkie kroki."
    }
}
