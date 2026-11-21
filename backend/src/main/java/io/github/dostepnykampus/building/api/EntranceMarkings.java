/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.api;

import java.time.Instant;

/**
 * Aktualizacja oznaczeń wejść po zatwierdzeniu obchodu.
 *
 * <p>Wymagania: US17.2, US17.3, BR-23, BR-24</p>
 *
 * @since 0.1.0
 */
@FunctionalInterface
public interface EntranceMarkings {

    /**
     * Stosuje zmianę, jeśli wejście istnieje i od zgłoszenia nie przyjęto nowszej zmiany.
     *
     * @param change Przyjęta zmiana oznaczenia
     * @return Prawda, gdy zapisano oznaczenie
     */
    boolean apply(Change change);

    /**
     * Przyjęta zmiana oznaczenia konkretnego wejścia.
     *
     * @param building Symbol budynku
     * @param entrance Identyfikator wejścia w budynku
     * @param marking Oznaczenie ustalone w trakcie obchodu
     * @param submitted Chwila wysłania zgłoszenia
     * @param accepted Chwila przyjęcia zgłoszenia
     * @since 0.1.0
     */
    record Change(
        String building, String entrance, Marking marking, Instant submitted, Instant accepted
    ) {
    }

    /**
     * Nowe oznaczenie dostępności wejścia.
     *
     * @since 0.1.0
     */
    enum Marking {

        /** Wejście dostępne. */
        ACCESSIBLE,
        /** Wejście niedostępne. */
        INACCESSIBLE
    }
}
