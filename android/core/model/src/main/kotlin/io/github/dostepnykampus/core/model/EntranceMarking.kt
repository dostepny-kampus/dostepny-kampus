/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.model

/** Oznaczenie wejścia (`EntranceMarking` z kontraktu API, BR-24). */
enum class EntranceMarking {
    /** Dostępne: osoba na wózku wejdzie bez pokonywania stopni. */
    ACCESSIBLE,

    /** Niedostępne. */
    INACCESSIBLE,

    /** Brak danych: import nie dał informacji o dostępności wejścia. */
    NO_DATA,
}
