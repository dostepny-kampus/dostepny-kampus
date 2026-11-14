/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.systemtest

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
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
import io.github.dostepnykampus.app.MainActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val WAIT_MS = 30_000L
private const val CHECK = "Sprawdź, którędy wejść"
private const val EXPAND = "Rozwiń opis budynku"
private const val SERVER_ASSESSMENT = "Z map i opisów, stan z "

private val BuildingA1InCatalog = hasContentDescription("A-1, ", substring = true)

/**
 * Aplikacja z własnym grafem Hilt i siecią na emulatorze, serwer z bazą na komputerze gospodarza: katalog budynków
 * i karta budynku idą z serwera pod adresem z `API_BASE_URL`. Dzień i źródło oznaczenia wejść ma tylko odpowiedź
 * serwera, nie plik konturów w aplikacji.
 *
 * Wymagania: US17.3, NFR-41
 */
@RunWith(AndroidJUnit4::class)
class BuildingCardFromServerTest {
    /** Prawdziwa aktywność aplikacji, uruchamiana od nowa dla każdego testu. */
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun waitFor(matcher: SemanticsMatcher) {
        compose.waitUntil(timeoutMillis = WAIT_MS) { compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }
    }

    /** Zamyka instrukcję pierwszego uruchomienia, która jest na czystych danych aplikacji. Wymagania: US5.1 */
    @Before
    fun leaveHelp() {
        waitFor(hasText("Pomoc"))
        Espresso.pressBack()
        waitFor(hasSetTextAction())
    }

    /**
     * Scenariusz (częściowo): oznaczenie-wejscia.feature: Wspólna data i źródło stoją raz nad listą wejść.
     *
     * Wymagania: US17.3, US1.1, US2.8
     */
    @Test
    fun cardA1FromServer() {
        compose
            .onNode(hasSetTextAction())
            .performSemanticsAction(SemanticsActions.SetText) { it(AnnotatedString("A-1")) }
        waitFor(BuildingA1InCatalog)
        compose.onAllNodes(BuildingA1InCatalog).onFirst().performClick()
        waitFor(hasText(CHECK))
        compose.onNode(hasContentDescription(EXPAND)).performClick()
        val assessment = hasText(SERVER_ASSESSMENT, substring = true)
        waitFor(assessment)
        compose.onAllNodes(assessment).assertCountEquals(1)
    }
}
