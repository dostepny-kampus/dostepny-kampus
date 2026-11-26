/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.inspection.invitation

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.designsystem.IMPORTANT_SIGN_TAG
import io.github.dostepnykampus.core.model.StaffRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val BAD_EMAIL = "Adres e-mail jest niepełny, np. brakuje znaku @. Popraw go i wyślij ponownie."

/**
 * K2 „Zaproś osobę”: adres, rola, wysłanie, wyniki i błędy.
 *
 * Wymagania: US12.1, US12.2, NFR-50
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class InvitationScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val events = mutableListOf<String>()

    private val state = mutableStateOf(InvitationUiState())

    /** Ekran K2 ze stanem, który test zmienia. */
    @Before
    fun setUp() {
        compose.setContent {
            DostepnyKampusTheme {
                InvitationContent(
                    state = state.value,
                    onBack = { events += "back" },
                    onLeave = { events += "leave" },
                    onEdit = { email -> events += email },
                    onChoose = { role -> events += role.name },
                    onSend = { events += "send" },
                )
            }
        }
    }

    /** Zwykły stan: adres, wybór roli i „Wyślij zaproszenie”. */
    @Test
    fun entersAndSends() {
        compose.onNode(hasSetTextAction()).performTextInput("a@pwr.edu.pl")
        compose.onNodeWithText("Weryfikator").performClick()
        compose.onNodeWithText("Wyślij zaproszenie").performClick()
        assertEquals("adres, rola i wysłanie", listOf("a@pwr.edu.pl", "VERIFIER", "send"), events)
    }

    /** Błędy danych stoją nad polem adresu i nad wyborem roli. */
    @Test
    fun showsProblems() {
        show(InvitationUiState(problems = setOf(InvitationProblem.EMAIL, InvitationProblem.ROLE)))
        compose.onNodeWithText(BAD_EMAIL).assertIsDisplayed()
        compose.onNodeWithText("Wybierz rolę.").assertIsDisplayed()
    }

    /** Przed każdym błędem pola stoi znak „!” w kole. Wymagania: NFR-21 */
    @Test
    fun signsBeforeProblems() {
        show(InvitationUiState(problems = setOf(InvitationProblem.EMAIL, InvitationProblem.ROLE)))
        val signs = compose.onAllNodesWithTag(IMPORTANT_SIGN_TAG, useUnmergedTree = true).fetchSemanticsNodes()
        val texts = listOf(BAD_EMAIL, "Wybierz rolę.").map { text -> compose.onNodeWithText(text).fetchSemanticsNode() }
        assertEquals("znak przy każdym błędzie", 2, signs.size)
        signs.zip(texts).forEach { (sign, text) ->
            assertTrue("znak przed tekstem", sign.boundsInRoot.right <= text.boundsInRoot.left)
        }
    }

    /** Wyniki: zaproszenie wysłane i rola dodana z nazwą roli w dopełniaczu. */
    @Test
    fun showsResults() {
        show(InvitationUiState(stage = InvitationStage.SENT))
        compose.onNodeWithText("Zaproszenie wysłane na podany adres.").assertIsDisplayed()
        show(InvitationUiState(stage = InvitationStage.ROLE_ADDED, addedRole = StaffRole.COLLECTOR))
        compose.onNodeWithText("To konto już istnieje.").assertIsDisplayed()
        compose.onNodeWithText("Dodaliśmy mu rolę Zbierającego, dotychczasowe role zostają.").assertIsDisplayed()
    }

    /** Brak sieci ponawia wysłanie, a brak uprawnień wraca do mapy. */
    @Test
    fun handlesFailures() {
        show(InvitationUiState(stage = InvitationStage.OFFLINE))
        compose.onNodeWithText("Spróbuj ponownie").performClick()
        show(InvitationUiState(stage = InvitationStage.FORBIDDEN))
        compose.onNodeWithText("Wróć do mapy").performClick()
        assertEquals("ponowienie i mapa", listOf("send", "leave"), events)
    }

    private fun show(next: InvitationUiState) {
        compose.runOnIdle { state.value = next }
    }
}
