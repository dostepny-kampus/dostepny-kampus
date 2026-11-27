/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.auth

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.Account
import io.github.dostepnykampus.core.model.StaffRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * L2 „Konto”: role konta, czynności ról, klauzula informacyjna, wycofanie zgody i wylogowanie.
 *
 * Wymagania: US14.1, US14.3, US14.4, NFR-29, NFR-30
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class AccountScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val events = mutableListOf<String>()

    /** Zbierający i Koordynator: wszystkie role na karcie i czynności obu ról. */
    @Test
    fun staffSeesRolesAndWork() {
        show(Account.SignedIn(setOf(StaffRole.COORDINATOR, StaffRole.COLLECTOR), displayName = "Jan Kowalski"))
        compose.onNodeWithText("Konto i praca").assertIsDisplayed()
        val name = compose.onNodeWithText("Jan Kowalski").assertIsDisplayed()
        val roles = compose.onNodeWithText("Twoje role: Odwiedzający, Zbierający, Koordynator").assertIsDisplayed()
        assertTrue(
            "nazwa nad rolami",
            name.getUnclippedBoundsInRoot().bottom <= roles.getUnclippedBoundsInRoot().top,
        )
        compose.onNodeWithText("Zmierz i zgłoś barierę").performScrollTo().performClick()
        compose.onNodeWithText("Moje zgłoszenia").performScrollTo().performClick()
        compose.onNodeWithText("Postęp inwentaryzacji").performScrollTo().performClick()
        compose.onNodeWithText("Zaproś osobę").performScrollTo().performClick()
        compose.onNodeWithText("Pobierz eksport").performScrollTo().performClick()
        assertEquals("czynności ról", listOf("MEASURE", "MINE", "PROGRESS", "INVITE", "EXPORT"), events)
    }

    /** „Wyloguj się” i „Wstecz” prowadzą poza ekran. */
    @Test
    fun signsOutAndGoesBack() {
        show(Account.SignedIn())
        compose.onNodeWithText("Twoje role: Odwiedzający").assertIsDisplayed()
        compose.onNodeWithText("Wyloguj się").performScrollTo().performClick()
        compose.onNodeWithText("Wstecz").performClick()
        assertEquals("wylogowanie i powrót", listOf("signOut", "back"), events)
    }

    /** Sekcja „Konto”: klauzula, „Wycofaj zgodę”, „Wyloguj się”; klauzula otwiera treść z odnośnikiem i wraca. */
    @Test
    fun showsPrivacyNoticeAndReturns() {
        show(Account.SignedIn())
        val privacy = compose.onNodeWithText("Informacja o danych osobowych").performScrollTo().assertIsDisplayed()
        val withdraw = compose.onNodeWithText("Wycofaj zgodę").performScrollTo().assertIsDisplayed()
        val signOut = compose.onNodeWithText("Wyloguj się").performScrollTo().assertIsDisplayed()
        val privacyBottom = privacy.getUnclippedBoundsInRoot().bottom
        val withdrawBounds = withdraw.getUnclippedBoundsInRoot()
        assertTrue("klauzula nad wycofaniem", privacyBottom <= withdrawBounds.top)
        assertTrue("wycofanie nad wylogowaniem", withdrawBounds.bottom <= signOut.getUnclippedBoundsInRoot().top)
        privacy.performClick()
        assertClauseDisplayed("Informacja o danych osobowych")
        assertClauseDisplayed("Politechnika Wrocławska nie jest administratorem Twoich danych.")
        val link = compose.onNodeWithText("repozytorium projektu na GitHub", substring = true).performScrollTo()
        val linkText = link.fetchSemanticsNode().config[SemanticsProperties.Text].single()
        val urls = linkText.getLinkAnnotations(start = 0, end = linkText.length).map { (it.item as LinkAnnotation.Url).url }
        assertEquals("odnośnik kontaktu w klauzuli", listOf(REPOSITORY_URL), urls)
        assertClauseDisplayed("Twoja zgoda (art. 6 ust. 1 lit. a RODO), wyrażona przy zakładaniu konta.")
        assertClauseDisplayed("Ta sama informacja jest na stronie zakładania konta.")
        compose.onNodeWithText("Wstecz").performClick()
        compose.onNodeWithText("Twoje role: Odwiedzający").assertIsDisplayed()
        compose.onNodeWithText("Informacja o danych osobowych").performScrollTo().performClick()
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText("Twoje role: Odwiedzający").assertIsDisplayed()
        assertFalse("systemowe wstecz w klauzuli nie zamyka aktywności", compose.activity.isFinishing)
        assertEquals("powrót z klauzuli zostawia L2", emptyList<String>(), events)
    }

    /** „Wycofaj zgodę” otwiera potwierdzenie; „Zostaw zgodę” i „Wstecz” wracają, a potwierdzenie wylogowuje. */
    @Test
    fun withdrawsConsentWhenConfirmed() {
        show(Account.SignedIn())
        compose.onNodeWithText("Wycofaj zgodę").performScrollTo().performClick()
        assertClauseDisplayed("Wylogujemy Cię i w ciągu miesiąca usuniemy Twoje dane osobowe.")
        assertClauseDisplayed("Wysłane zdjęcia barier zostaną, ale bez Twoich danych.")
        val withdraw = compose.onNodeWithText("Wycofaj zgodę").assertIsDisplayed().getUnclippedBoundsInRoot()
        val keep = compose.onNodeWithText("Zostaw zgodę").assertIsDisplayed().getUnclippedBoundsInRoot()
        assertEquals("równa szerokość przycisków", withdraw.width, keep.width)
        assertEquals("równa wysokość przycisków", withdraw.height, keep.height)
        compose.onNodeWithText("Zostaw zgodę").performClick()
        compose.onNodeWithText("Twoje role: Odwiedzający").assertIsDisplayed()
        compose.onNodeWithText("Wycofaj zgodę").performScrollTo().performClick()
        compose.onNodeWithText("Wstecz").performClick()
        compose.onNodeWithText("Twoje role: Odwiedzający").assertIsDisplayed()
        assertEquals("anulowanie wycofania nie wylogowuje", emptyList<String>(), events)
        compose.onNodeWithText("Wycofaj zgodę").performScrollTo().performClick()
        compose.onNodeWithText("Wycofaj zgodę").performClick()
        assertEquals("potwierdzenie wycofania zgody wylogowuje", listOf("signOut"), events)
    }

    private fun assertClauseDisplayed(text: String) {
        compose.onNodeWithText(text, substring = true).performScrollTo().assertIsDisplayed()
    }

    private fun show(account: Account.SignedIn) {
        val actions =
            AccountActions(
                onBack = { events += "back" },
                onWork = { work -> events += work.name },
                onSignOut = { events += "signOut" },
            )
        compose.setContent {
            DostepnyKampusTheme {
                AccountScreen(account = account, actions = actions)
            }
        }
    }
}
