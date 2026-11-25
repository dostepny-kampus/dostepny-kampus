/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Stany wspólne ekranów ról pracowniczych: teksty z wymagań i czynność przycisku.
 *
 * Wymagania: NFR-50, US14.1
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class StaffProblemTest {
    @get:Rule
    val compose = createComposeRule()

    private var actions = 0

    @Test
    fun showsOffline() {
        show(StaffProblem.OFFLINE)
        compose.onNodeWithText("Bez internetu").assertIsDisplayed()
        compose.onNodeWithText(OFFLINE).assertIsDisplayed()
        compose.onNodeWithText("Spróbuj ponownie").performClick()
        assertEquals("ponowienie po braku sieci", 1, actions)
    }

    @Test
    fun showsServerError() {
        show(StaffProblem.SERVER)
        compose.onNodeWithText("Serwer chwilowo nie odpowiada. Spróbuj ponownie za kilka minut.").assertIsDisplayed()
        compose.onNodeWithText("Spróbuj ponownie").performClick()
        assertEquals("ponowienie po błędzie serwera", 1, actions)
    }

    @Test
    fun showsForbidden() {
        show(StaffProblem.FORBIDDEN)
        compose.onNodeWithText(FORBIDDEN).assertIsDisplayed()
        compose.onNodeWithText("Wróć do mapy").performClick()
        assertEquals("powrót do mapy bez uprawnień", 1, actions)
    }

    private fun show(problem: StaffProblem) {
        compose.setContent { DostepnyKampusTheme { StaffProblemMessage(problem = problem, onAction = { actions++ }) } }
    }

    private companion object {
        const val OFFLINE = "Ta czynność wymaga internetu. Połącz się z internetem i spróbuj ponownie."
        const val FORBIDDEN =
            "Nie masz już uprawnień do tej czynności. Jeśli to pomyłka, poproś Koordynatora o zaproszenie."
    }
}
