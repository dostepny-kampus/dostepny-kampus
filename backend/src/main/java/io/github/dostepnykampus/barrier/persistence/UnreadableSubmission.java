/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.persistence;

import java.util.UUID;

/**
 * Nieczytelna treść zapisanego zgłoszenia.
 *
 * @since 0.1.0
 */
final class UnreadableSubmission extends IllegalStateException {

    /** Identyfikator nieczytelnego zgłoszenia. */
    private final UUID id;

    UnreadableSubmission(final UUID id, final String message, final RuntimeException cause) {
        super(message, cause);
        this.id = id;
    }

    UUID id() {
        return this.id;
    }
}
