/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.github.dostepnykampus.barrier.json.SubmissionContentJson;
import io.github.dostepnykampus.barrier.submission.Submission;
import io.github.dostepnykampus.barrier.submission.SubmissionStatus;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
record SubmissionResponse(
    UUID id, int version, SubmissionStatus status, Instant createdAt,
    @JsonUnwrapped SubmissionContentJson content
) {

    /**
     * Zapisuje zgłoszenie z domeny w odpowiedzi.
     *
     * @param submission Zgłoszenie
     */
    SubmissionResponse(final Submission submission) {
        this(
            submission.id(), submission.version(), submission.status(), submission.createdAt(),
            new SubmissionContentJson(submission.content())
        );
    }
}
