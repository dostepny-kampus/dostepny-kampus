/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.domain;

/**
 * Nowe oznaczenie wejścia w zmianie oznaczenia wejścia, wartości jak pole
 * entranceMarking schematu SubmissionContent w OpenAPI (US17.2); „brak
 * danych” nie jest zmianą oznaczenia (BR-24).
 *
 * @since 0.1.0
 */
public enum NewEntranceMarking {

    /** Wejście dostępne. */
    ACCESSIBLE,

    /** Wejście niedostępne. */
    INACCESSIBLE
}
