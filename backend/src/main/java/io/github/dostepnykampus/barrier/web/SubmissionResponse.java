/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.web;

import io.github.dostepnykampus.barrier.domain.Submission;
import io.github.dostepnykampus.barrier.domain.SubmissionStatus;
import io.github.dostepnykampus.barrier.json.SubmissionContentJson;
import java.time.Instant;
import java.util.UUID;

/**
 * Odpowiedź z zgłoszeniem, schemat Submission w OpenAPI.
 *
 * @param id Identyfikator zgłoszenia
 * @param version Wersja zgłoszenia
 * @param status Status zgłoszenia
 * @param createdAt Chwila utworzenia zgłoszenia
 * @param content Treść zgłoszenia, w JSON płasko obok pozostałych pól
 * @since 0.1.0
 */
record SubmissionResponse(
    UUID id, int version, SubmissionStatus status, Instant createdAt,
    SubmissionContentJson content
) {

    /**
     * Zapisuje zgłoszenie z domeny w odpowiedzi.
     *
     * @param submission Zgłoszenie
     */
    SubmissionResponse(final Submission submission) {
        this(
            SubmissionResponse.missing(), 1, SubmissionStatus.SENT, SubmissionResponse.missing(),
            SubmissionResponse.missing()
        );
    }

    private static <T> T missing() {
        throw new UnsupportedOperationException();
    }
}
