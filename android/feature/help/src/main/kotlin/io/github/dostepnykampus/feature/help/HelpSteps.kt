/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.help

import androidx.annotation.StringRes

/** Obraz w kroku: prawdziwy element aplikacji, podpis [caption] nad nim i opis [description] dla TalkBack. */
internal enum class HelpExample(
    @param:StringRes val caption: Int,
    @param:StringRes val description: Int,
) {
    /** Pole wyszukiwania z V1 (US1.1), bez lupy. */
    SEARCH_FIELD(R.string.pomoc_budynek_podpis, R.string.pomoc_budynek_opis),

    /** Wejście z opisu budynku z oznaczeniem „Dostępne” (US2.1). */
    ENTRANCE(R.string.pomoc_dostepne_podpis, R.string.pomoc_dostepne_opis),
}

/** Krok instrukcji: tytuł [title], najwyżej 3 zdania [text] i opcjonalny obraz [example] (US5.1, NFR-18). */
internal data class HelpStep(
    @param:StringRes val title: Int,
    @param:StringRes val text: Int,
    val example: HelpExample? = null,
)

/** Kroki według makiety hi-fi i wymagań V5; krok 3 ma zdanie o koncie (US14.2, PW-5, decyzja 89). */
internal val HelpSteps =
    listOf(
        HelpStep(
            title = R.string.pomoc_budynek_tytul,
            text = R.string.pomoc_budynek_tekst,
            example = HelpExample.SEARCH_FIELD,
        ),
        HelpStep(
            title = R.string.pomoc_dostepne_tytul,
            text = R.string.pomoc_dostepne_tekst,
            example = HelpExample.ENTRANCE,
        ),
        HelpStep(title = R.string.pomoc_konto_tytul, text = R.string.pomoc_konto_tekst),
    )

/** Skutek „Wstecz” w instrukcji (wymagania V5, NFR-15). */
internal enum class HelpBack {
    /** Poprzedni krok instrukcji. */
    PREVIOUS_STEP,

    /** Krok 1 przy pierwszym uruchomieniu: jak „Pomiń instrukcję”, otwiera V1. */
    SKIP,

    /** Krok 1 po otwarciu z paska: ekran, z którego otwarto V5. */
    LEAVE,
}

/** Skutek „Wstecz” w kroku [step] (od 0); [isFirstLaunch]: instrukcja otworzyła się sama (US5.1). */
internal fun helpBack(step: Int, isFirstLaunch: Boolean): HelpBack =
    when {
        step > 0 -> HelpBack.PREVIOUS_STEP
        isFirstLaunch -> HelpBack.SKIP
        else -> HelpBack.LEAVE
    }
