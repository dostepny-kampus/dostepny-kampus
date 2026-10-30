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
    // Wiersz: instrukcja w najwyżej 4 krokach, decyzja 89: 3 kroki, ostatni o koncie, US5.1, US14.2.
    @Test
    fun us5_1_three_steps_last_account() {
        assertTrue(HelpSteps.size <= 4)
        assertEquals(3, HelpSteps.size)
        assertEquals(R.string.pomoc_konto_tytul to R.string.pomoc_konto_tekst, HelpSteps.last().run { title to text })
    }

    // Wiersz: obraz tylko z prawdziwym elementem aplikacji i opisem dla TalkBack, US5.1, NFR-18.
    @Test
    fun nfr18_images_have_description() {
        assertEquals(listOf(HelpExample.SEARCH_FIELD, HelpExample.ENTRANCE, null), HelpSteps.map { it.example })
        assertEquals(R.string.pomoc_budynek_opis, HelpExample.SEARCH_FIELD.description)
        assertEquals(R.string.pomoc_dostepne_opis, HelpExample.ENTRANCE.description)
    }

    // Wiersz: „Wstecz”: poprzedni krok; w kroku 1 przy pierwszym uruchomieniu jak „Pomiń instrukcję”, a po
    // otwarciu z paska powrót do ekranu, z którego otwarto V5, US5.1, NFR-15.
    @Test
    fun us5_1_wstecz_effect() {
        assertEquals(HelpBack.PREVIOUS_STEP, helpBack(step = 1, isFirstLaunch = true))
        assertEquals(HelpBack.PREVIOUS_STEP, helpBack(step = 2, isFirstLaunch = false))
        assertEquals(HelpBack.SKIP, helpBack(step = 0, isFirstLaunch = true))
        assertEquals(HelpBack.LEAVE, helpBack(step = 0, isFirstLaunch = false))
    }
}
