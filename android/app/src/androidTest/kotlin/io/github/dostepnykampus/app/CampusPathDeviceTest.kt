/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.AnnotatedString
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val WAIT_MS = 20_000L
private const val CHECK = "Sprawdź, którędy wejść"

private val BuildingA1InCatalog = hasContentDescription("A-1, ", substring = true)

/**
 * Ścieżka od wyszukania budynku do wejścia na emulatorze, na prawdziwej aktywności i grafie Hilt aplikacji:
 * wyszukanie budynku, karta V2 i „Którędy wejść” V3. Sieć w grafie zastępuje [NoServerNetworkModule]: serwera
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

    private fun waitFor(matcher: SemanticsMatcher) {
        compose.waitUntil(timeoutMillis = WAIT_MS) { compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }
    }

    /**
     * Każdy test zaczyna od czystych danych aplikacji (job emulatora robi `pm clear` przed każdym testem), więc
     * instrukcja „Pomoc” z pierwszego uruchomienia jest zawsze i zamyka się gestem wstecz. Lokalnie przed
     * testem: `adb shell pm clear io.github.dostepnykampus`.
     *
     * Wymagania: US5.1
     */
    @Before
    fun leaveHelp() {
        waitFor(hasText("Pomoc"))
        Espresso.pressBack()
        waitFor(hasSetTextAction())
    }

    /**
     * Wiersze: V1, wpisanie symbolu i wybór wyniku → V2; „Sprawdź, którędy wejść” → V3; „Wstecz” → V2.
     *
     * Symbol jest wpisany akcją semantyki, bez fokusu pola, więc klawiatura ekranowa się nie otwiera. Otwarta
     * klawiatura pochłania „Wstecz” (chowa się zamiast cofnąć V3), a chwila jej otwarcia i zamknięcia zależy od
     * emulatora, więc wynik testu zależałby od czasu.
     *
     * Wymagania: US1.1, US2, US3
     */
    @Test
    fun us3_search_card_entrances() {
        compose
            .onNode(hasSetTextAction())
            .performSemanticsAction(SemanticsActions.SetText) { it(AnnotatedString("A-1")) }
        waitFor(BuildingA1InCatalog)
        compose.onAllNodes(BuildingA1InCatalog).onFirst().performClick()
        waitFor(hasText(CHECK))
        compose.onNode(hasText(CHECK)).performClick()
        waitFor(hasText("Którędy wejść do A-1"))
        Espresso.pressBack()
        waitFor(hasText(CHECK))
    }

    /**
     * Wiersz: V1, „Lista budynków” → budynek → mała karta V2 nad mapą, z pliku bez serwera.
     *
     * Wymagania: US2.8, NFR-11, NFR-19
     */
    @Test
    fun us2_list_card() {
        compose.onAllNodes(hasText("Lista budynków") or hasText("Lista")).onFirst().performClick()
        waitFor(BuildingA1InCatalog)
        compose.onAllNodes(BuildingA1InCatalog).onFirst().performClick()
        waitFor(hasText(CHECK))
    }
}
