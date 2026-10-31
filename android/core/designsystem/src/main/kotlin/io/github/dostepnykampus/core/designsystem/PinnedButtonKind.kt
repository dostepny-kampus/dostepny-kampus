/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem

/** Rodzaj przycisku przypiętego: jedyna czynność główna ekranu, czynność tonalna albo obrysowana. */
enum class PinnedButtonKind {
    /**
     * Wypełniony: jedyna czynność główna na ekranie. W jasnym motywie tło `primary`, w ciemnym `primaryContainer`
     * z ramką [Obramowania.wypelniony] w kolorze `primary`.
     */
    Filled,

    /** Tonalny `secondaryContainer`, np. „Pokaż na mapie”. */
    Tonal,

    /** Obrysowany: napis `primary`, ramka [Obramowania.ramka] w kolorze `outline`, np. „Anuluj”. */
    Outlined,
}
