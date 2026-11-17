/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Kontroler klawiatury, który liczy wywołania [hide]. */
private class HidingKeyboard : SoftwareKeyboardController {
    var hidden = 0

    override fun hide() {
        hidden++
    }

    override fun show() = Unit
}

/**
 * Pole symbolu na V1: wyniki, wybór wyniku, brak wyniku i lista zawężona symbolem; przygotowanie w
 * [CampusMapScreenFixture].
 *
 * Wymagania: US1.1, UC1
 */
@RunWith(RobolectricTestRunner::class)
class CampusSearchScreenTest : CampusMapScreenFixture() {
    /**
     * Zapis „C13” w polu symbolu daje wynik „Budynek C-13”.
     * Scenariusz (przykład: C13): przegladanie-mapy.feature: Wyszukanie budynku po symbolu w różnym zapisie
     *
     * Wymagania: US1.1
     */
    @Test
    fun findsC13WithoutDash() {
        assertFindsC13("C13")
    }

    /**
     * Zapis „c-13” w polu symbolu daje wynik „Budynek C-13”.
     * Scenariusz (przykład: c-13): przegladanie-mapy.feature: Wyszukanie budynku po symbolu w różnym zapisie
     *
     * Wymagania: US1.1
     */
    @Test
    fun findsC13InLowercase() {
        assertFindsC13("c-13")
    }

    /**
     * Zapis „C-13” w polu symbolu daje wynik „Budynek C-13”.
     * Scenariusz (przykład: C-13): przegladanie-mapy.feature: Wyszukanie budynku po symbolu w różnym zapisie
     *
     * Wymagania: US1.1
     */
    @Test
    fun findsC13Exactly() {
        assertFindsC13("C-13")
    }

    private fun assertFindsC13(query: String) {
        compose.setContent { Screen() }
        compose.onNode(hasSetTextAction()).performTextInput(query)
        compose.onNodeWithText("Budynek C-13", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Budynek C-14", useUnmergedTree = true).assertDoesNotExist()
    }

    /** Wiersz: pole symbolu „c14” → wynik „Budynek C-14” → V2 z obrysem. Wymagania: US1.1 */
    @Test
    fun opensCardFromResult() {
        compose.setContent { Screen() }
        compose.onNode(hasSetTextAction()).performTextInput("c14")
        compose.onNodeWithText("Budynek C-14", useUnmergedTree = true).assertExists()
        compose.onNodeWithContentDescription("C-14, Budynek testowy").performClick()
        assertEquals("wynik otwiera kartę C-14", listOf("C-14"), opened)
        compose.onNode(hasSetTextAction()).assertTextEquals("c14", includeEditableText = true)
    }

    /**
     * Wiersz: wpisanie zapytania z wynikami i wybór wyniku nie zmieniają górnego zasłonięcia mapy, bo lista wyników
     * leży nad mapą. Sprawdza stan po wpisaniu; TODO(#2951): pierwsza klatka po pierwszym znaku ma pole niższe o 8 dp.
     *
     * Wymagania: US1.1, NFR-56
     */
    @Test
    fun keepsMapTopWhileTyping() {
        compose.setContent { Screen() }
        compose.waitForIdle()
        val bar = tops.last()
        compose.onNode(hasSetTextAction()).performTextInput("c14")
        compose.onNodeWithText("Budynek C-14", useUnmergedTree = true).assertExists()
        compose.waitForIdle()
        val typed = tops.size
        val typing = tops.last()
        compose.onNodeWithContentDescription("C-14, Budynek testowy").performClick()
        compose.waitForIdle()
        val picked = tops.subList(fromIndex = typed, toIndex = tops.size)
        val message = "góra mapy: z wynikami $typing, po wyborze $picked, pasek $bar"
        assertTrue(message, (picked + typing).all { it == bar })
    }

    /**
     * Wiersz: po wyborze wyniku klawiatura ekranowa się chowa, więc pierwsze „Wstecz” na V2 zamyka V2, a nie tylko
     * klawiaturę. Klawiaturę podaje atrapa kontrolera, bo Robolectric nie ma okna z klawiaturą,
     * a o skutku decyduje fokus pola: po wyborze pole go nie ma. Okno jest w trybie dotyku jak na telefonie; poza nim
     * Robolectric po `clearFocus` oddaje fokus pierwszemu polu, więc fokus nigdy by nie zszedł.
     *
     * Wymagania: US1.1, NFR-15
     */
    @Test
    fun hidesKeyboardOnResult() {
        val keyboard = HidingKeyboard()
        compose.setContent { CompositionLocalProvider(LocalSoftwareKeyboardController provides keyboard) { Screen() } }
        enterTouchMode()
        compose.onNode(hasSetTextAction()).performTextInput("c14")
        compose.onNode(hasSetTextAction()).assertIsFocused()
        compose.onNodeWithContentDescription("C-14, Budynek testowy").performClick()
        assertEquals("klawiatura schowana raz", 1, keyboard.hidden)
        compose.onNode(hasSetTextAction()).assertIsNotFocused()
    }

    /** Wiersz: „A-1 329” → stan „brak wyniku”; „×” „Wyczyść pole” → pole puste. */
    @Test
    fun clearsNoResult() {
        compose.setContent { Screen() }
        compose.onNode(hasSetTextAction()).performTextInput("A-1 329")
        compose.onNodeWithText("Nie znaleźliśmy budynku „A-1 329”.").assertExists()
        compose.onNodeWithContentDescription("Wyczyść pole").performClick()
        compose.onNodeWithText("Nie znaleźliśmy", substring = true).assertDoesNotExist()
        assertEquals("brak wyniku nie otwiera karty", emptyList<String>(), opened)
    }

    /** Wiersz: V1, lista i pole symbolu → lista zawężona do wpisanego budynku. Wymagania: NFR-19, US1.1 */
    @Test
    fun narrowsListBySymbol() {
        compose.setContent { Screen() }
        compose.onNode(showList).performClick()
        compose.onNode(hasSetTextAction()).performTextInput("c14")
        compose.onNodeWithContentDescription("C-14, Budynek testowy").assertExists()
        compose.onNodeWithContentDescription("C-13, Budynek testowy").assertDoesNotExist()
    }

    /** Wiersz: V1, brak wyniku → „Pokaż listę” w karcie czyści pole i pokazuje całą listę (PW-9). Wymagania: UC1 */
    @Test
    fun opensListOnNoResult() {
        compose.setContent { Screen() }
        compose.onNode(hasSetTextAction()).performTextInput("C-31")
        val inCard = hasAnyAncestor(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion))
        compose.onNode(hasText("Pokaż listę") and inCard).performClick()
        compose.onNode(showMap).assertExists()
        compose.onNodeWithText("Nie znaleźliśmy", substring = true).assertDoesNotExist()
        compose.onNodeWithContentDescription("C-13, Budynek testowy").assertExists()
    }
}
