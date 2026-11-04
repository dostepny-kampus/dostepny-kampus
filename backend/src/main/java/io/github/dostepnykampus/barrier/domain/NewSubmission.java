/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Komenda utworzenia zgłoszenia, schemat NewSubmission w OpenAPI (BR-9).
 *
 * @param id Identyfikator zgłoszenia nadany przez klienta
 * @param content Treść zgłoszenia
 * @since 0.1.0
 */
public record NewSubmission(UUID id, SubmissionContent content) {

    /**
     * Komenda z identyfikatorem i treścią; brak któregokolwiek kończy się
     * niekontrolowanym wyjątkiem NullPointerException.
     */
    public NewSubmission {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(content, "content");
    }
}
