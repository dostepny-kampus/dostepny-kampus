/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.help

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Kroki instrukcji V5 i skutek „Wstecz” bez interfejsu. */
class HelpStepsTest {
    /** Wiersz: instrukcja w najwyżej 4 krokach: 3 kroki, ostatni o koncie. Wymagania: US5.1, US14.2 */
    @Test
    fun endsThirdStepOnAccount() {
        assertTrue(HelpSteps.size <= 4, "najwyżej 4 kroki")
        assertEquals(3, HelpSteps.size, "liczba kroków")
        assertEquals(
            R.string.pomoc_konto_tytul to R.string.pomoc_konto_tekst,
            HelpSteps.last().run { title to text },
            "ostatni krok o koncie",
        )
    }

    /** Wiersz: obraz tylko z prawdziwym elementem aplikacji i opisem dla TalkBack. Wymagania: US5.1, NFR-18 */
    @Test
    fun describesImages() {
        assertEquals(
            listOf(HelpExample.SEARCH_FIELD, HelpExample.ENTRANCE, null),
            HelpSteps.map { it.example },
            "obrazy kroków",
        )
        assertEquals(R.string.pomoc_budynek_opis, HelpExample.SEARCH_FIELD.description, "opis pola wyszukiwania")
        assertEquals(R.string.pomoc_dostepne_opis, HelpExample.ENTRANCE.description, "opis wejścia")
    }

    /**
     * Wiersz: „Wstecz”: poprzedni krok; w kroku 1 przy pierwszym uruchomieniu jak „Pomiń instrukcję”, a po
     * otwarciu z paska powrót do ekranu, z którego otwarto V5.
     *
     * Wymagania: US5.1, NFR-15
     */
    @Test
    fun goesBackOnBackButton() {
        assertEquals(
            HelpBack.PREVIOUS_STEP,
            helpBack(step = 1, isFirstLaunch = true),
            "wstecz w kroku 1 przy pierwszym uruchomieniu",
        )
        assertEquals(HelpBack.PREVIOUS_STEP, helpBack(step = 2, isFirstLaunch = false), "wstecz w kroku 2")
        assertEquals(
            HelpBack.SKIP,
            helpBack(step = 0, isFirstLaunch = true),
            "pominięcie w kroku 0 przy pierwszym uruchomieniu",
        )
        assertEquals(HelpBack.LEAVE, helpBack(step = 0, isFirstLaunch = false), "wyjście z kroku 0")
    }
}
