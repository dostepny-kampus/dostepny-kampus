/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem

/** Rodzaj przycisku przypiętego: jedyna czynność główna ekranu, czynność tonalna albo obrysowana. */
enum class PinnedButtonKind {
    /** Wypełniony `primary`: jedyna czynność główna na ekranie. */
    Filled,

    /** Tonalny `secondaryContainer`, np. „Pokaż na mapie”. */
    Tonal,

    /** Obrysowany: napis `primary`, ramka [Obramowania.ramka] w kolorze `outline`, np. „Anuluj”. */
    Outlined,
}
