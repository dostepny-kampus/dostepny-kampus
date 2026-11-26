/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission;

/**
 * Rola autora zgłoszenia w chwili wysłania, schemat Role w OpenAPI, w kolejności schematu.
 *
 * <p>Wymagania: BR-1, BR-5</p>
 *
 * @since 0.1.0
 */
public enum AuthorRole {

    /** Odwiedzający; ma ją każde konto. */
    VISITOR,

    /** Zbierający. */
    COLLECTOR,

    /** Weryfikator. */
    VERIFIER,

    /** Koordynator. */
    COORDINATOR
}
