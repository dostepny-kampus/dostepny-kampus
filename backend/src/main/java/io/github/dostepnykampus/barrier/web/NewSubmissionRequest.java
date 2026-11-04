/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.web;

import io.github.dostepnykampus.barrier.domain.NewSubmission;
import io.github.dostepnykampus.barrier.json.SubmissionContentJson;
import java.util.UUID;

/**
 * Ciało POST /submissions, schemat NewSubmission w OpenAPI.
 *
 * @param id Identyfikator zgłoszenia nadany przez klienta
 * @param content Treść zgłoszenia, w JSON płasko obok identyfikatora
 * @since 0.1.0
 */
record NewSubmissionRequest(UUID id, SubmissionContentJson content) {

    /**
     * Przenosi ciało żądania do komendy domeny.
     *
     * @return Komenda utworzenia zgłoszenia
     */
    NewSubmission toCommand() {
        return NewSubmissionRequest.missing();
    }

    private static <T> T missing() {
        throw new UnsupportedOperationException();
    }
}
