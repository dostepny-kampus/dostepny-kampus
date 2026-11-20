/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.testing

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.AnnotatedString
import androidx.test.espresso.Espresso

/** Opis uchwytu wysokości karty budynku; uchwyt jest na karcie bez względu na dane o wejściach. */
public const val CARD_HANDLE: String = "Wysokość karty"

/** Otwarta karta budynku, rozpoznana po uchwycie wysokości. */
public fun openedCard(): SemanticsMatcher = hasContentDescription(CARD_HANDLE)

/**
 * Wiersz budynku [symbol] w wynikach wyszukiwania i na liście budynków.
 *
 * @param symbol symbol budynku, np. `A-1`
 */
public fun inCatalog(symbol: String): SemanticsMatcher = hasContentDescription("$symbol, ", substring = true)

/**
 * Kroki testów na urządzeniu na ekranach aplikacji, wspólne dla testów aplikacji i testów systemowych.
 *
 * @param compose reguła testu z uruchomioną aktywnością aplikacji
 * @param waitMs najdłuższe czekanie na węzeł w milisekundach
 */
public class DeviceSteps(private val compose: ComposeTestRule, private val waitMs: Long = WAIT_MS) {
    /**
     * Czeka, aż na ekranie będzie co najmniej jeden węzeł zgodny z [matcher].
     *
     * @throws androidx.compose.ui.test.ComposeTimeoutException gdy węzła nie ma po [waitMs]
     */
    public fun waitFor(matcher: SemanticsMatcher) {
        compose.waitUntil(timeoutMillis = waitMs) { compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }
    }

    /**
     * Zamyka gestem wstecz instrukcję „Pomoc” z pierwszego uruchomienia i czeka na pole symbolu budynku. Wymaga
     * czystych danych aplikacji, bez nich instrukcji nie ma.
     *
     * Wymagania: US5.1
     */
    public fun leaveHelp() {
        waitFor(hasText("Pomoc"))
        Espresso.pressBack()
        waitFor(hasSetTextAction())
    }

    /**
     * Wpisuje [query] w pole szukaj akcją semantyki, bez klawiatury ekranowej, wybiera wynik o [symbol] i czeka
     * na kartę budynku.
     *
     * Wymagania: US1.1, US2
     *
     * @param symbol symbol budynku, np. `A-1`
     * @param query wpisywany tekst, domyślnie symbol budynku
     */
    public fun openCard(symbol: String, query: String = symbol) {
        compose
            .onNode(hasSetTextAction())
            .performSemanticsAction(SemanticsActions.SetText) { it(AnnotatedString(query)) }
        waitFor(inCatalog(symbol))
        compose.onAllNodes(inCatalog(symbol)).onFirst().performClick()
        waitFor(openedCard())
    }

    private companion object {
        const val WAIT_MS = 20_000L
    }
}
