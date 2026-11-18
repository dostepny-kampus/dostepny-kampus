/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.dostepnykampus.core.testing.DeviceSteps
import io.github.dostepnykampus.core.testing.inCatalog
import io.github.dostepnykampus.core.testing.openedCard
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val WAIT_MS = 20_000L

private val BuildingA1InCatalog = inCatalog("A-1")

/**
 * Ścieżka od wyszukania budynku do wejścia na emulatorze, na prawdziwej aktywności i grafie Hilt aplikacji:
 * wyszukanie budynku i karta V2 z wierszem o wejściach. Sieć w grafie zastępuje [NoServerNetworkModule]: serwera
 * nie ma, więc katalog i wejścia zawsze pochodzą z pliku w aplikacji.
 *
 * Wymagania: US1, US2, US3, NFR-41
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class CampusPathDeviceTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    private val steps = DeviceSteps(compose, WAIT_MS)

    /**
     * Każdy test zaczyna od czystych danych aplikacji (job emulatora robi `pm clear` przed każdym testem), więc
     * instrukcja „Pomoc” z pierwszego uruchomienia jest zawsze i zamyka się gestem wstecz. Lokalnie przed
     * testem: `adb shell pm clear io.github.dostepnykampus`.
     *
     * Wymagania: US5.1
     */
    @Before
    fun leaveHelp() {
        steps.leaveHelp()
    }

    /**
     * Wiersz: V1, wpisanie symbolu i wybór wyniku → V2 z wierszem o wejściach. Wejście A-1 w pliku nie ma danych
     * o dostępności, więc karta mówi „Brak danych o wejściach” i nie ma „Pokaż wejście na mapie”.
     *
     * Wymagania: US1.1, US2, US3.1, BR-25
     */
    @Test
    fun goesFromSearchToEntrances() {
        steps.openCard("A-1")
        steps.waitFor(hasText("Brak danych o wejściach"))
        compose.onAllNodes(hasText("Pokaż wejście na mapie")).assertCountEquals(0)
    }

    /**
     * Wiersz: V1, „Pokaż listę” → budynek → mała karta V2 nad mapą, z pliku bez serwera.
     *
     * Wymagania: US2.8, NFR-11, NFR-19
     */
    @Test
    fun opensCardFromList() {
        compose.onNode(hasText("Pokaż listę")).performClick()
        steps.waitFor(BuildingA1InCatalog)
        compose.onAllNodes(BuildingA1InCatalog).onFirst().performClick()
        steps.waitFor(openedCard())
    }

    /**
     * Wiersz: prawdziwa klawiatura → wybór A-1 → V2 bez fokusu i klawiatury → jedno Wstecz zamyka V2.
     *
     * Wymagania: US1.1
     */
    @Test
    fun goesBackAfterKeyboard() {
        val previous = DeviceEvidence.shell("settings get secure show_ime_with_hard_keyboard").trim()
        check(previous in setOf("0", "1", "null")) { "Nieznane ustawienie klawiatury: $previous" }
        try {
            DeviceEvidence.shell("settings put secure show_ime_with_hard_keyboard 1")
            compose.onNode(hasSetTextAction()).performClick()
            compose.waitUntil(timeoutMillis = WAIT_MS) { DeviceEvidence.isKeyboardVisible() }
            InstrumentationRegistry.getInstrumentation().sendStringSync("A-1")
            steps.waitFor(BuildingA1InCatalog)
            compose.onNode(hasSetTextAction()).assertIsFocused()
            DeviceEvidence.record("v1-keyboard")
            compose.onAllNodes(BuildingA1InCatalog).onFirst().performClick()
            steps.waitFor(openedCard())
            compose.waitUntil(timeoutMillis = WAIT_MS) { !DeviceEvidence.isKeyboardVisible() }
            compose.onAllNodes(hasSetTextAction() and isFocused()).assertCountEquals(0)
            DeviceEvidence.record("v2-no-keyboard")
            Espresso.pressBack()
            compose.waitUntil(timeoutMillis = WAIT_MS) {
                compose.onAllNodes(openedCard()).fetchSemanticsNodes().isEmpty()
            }
            steps.waitFor(hasSetTextAction())
            compose.waitUntil(timeoutMillis = WAIT_MS) {
                compose.onAllNodes(hasSetTextAction() and isFocused()).fetchSemanticsNodes().isEmpty()
            }
            compose.onNode(hasSetTextAction()).assertIsNotFocused()
            compose.waitUntil(timeoutMillis = WAIT_MS) { !DeviceEvidence.isKeyboardVisible() }
            DeviceEvidence.record("v1-after-back")
        } finally {
            val command =
                if (previous == "null") {
                    "settings delete secure show_ime_with_hard_keyboard"
                } else {
                    "settings put secure show_ime_with_hard_keyboard $previous"
                }
            DeviceEvidence.shell(command)
        }
    }
}
