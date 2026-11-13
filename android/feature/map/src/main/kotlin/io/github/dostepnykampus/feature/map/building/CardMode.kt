/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

/** Tryb treści V2 Karta budynku. Wymagania: US2.8, US2.9 */
enum class CardMode {
    /** Mała karta nad mapą: nagłówek, wiersz o wejściach, „Sprawdź, którędy wejść” i „Zamknij”. */
    Compact,

    /** Pełny ekran rozwinięty z małej karty: górny pasek „Wstecz” i cały opis budynku. */
    Expanded,

    /**
     * Pełny ekran otwarty od razu, bez małej karty: duża czcionka, szerokie okno, poziomo albo wybór z listy przy
     * TalkBack. Tylko tu jest „Pokaż budynek na mapie”.
     *
     * Wymagania: NFR-19, NFR-20
     */
    Direct,
}
