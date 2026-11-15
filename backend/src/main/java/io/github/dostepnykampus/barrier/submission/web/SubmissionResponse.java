/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.github.dostepnykampus.barrier.submission.Submission;
import io.github.dostepnykampus.barrier.submission.SubmissionStatus;
import io.github.dostepnykampus.barrier.submission.content.json.SubmissionContentJson;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Odpowiedź z zgłoszeniem, schemat Submission w OpenAPI.
 *
 * <p>Wymagania: BR-17</p>
 *
 * @param id Identyfikator zgłoszenia
 * @param version Wersja zgłoszenia
 * @param status Status zgłoszenia
 * @param createdAt Chwila utworzenia zgłoszenia
 * @param verifiedAt Chwila przyjęcia albo {@code null}, gdy zgłoszenia nie przyjęto
 * @param content Treść zgłoszenia, w JSON płasko obok pozostałych pól
 * @since 0.1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record SubmissionResponse(
    UUID id, int version, SubmissionStatus status, Instant createdAt,
    @Nullable Instant verifiedAt, @JsonUnwrapped SubmissionContentJson content
) {

    /**
     * Zapisuje zgłoszenie z domeny w odpowiedzi.
     *
     * @param submission Zgłoszenie
     */
    SubmissionResponse(final Submission submission) {
        this(
            submission.id(), submission.version(), submission.status(), submission.createdAt(),
            SubmissionResponse.known(submission.verification()),
            new SubmissionContentJson(submission.content())
        );
    }

    private static @Nullable Instant known(final Instant verification) {
        final Instant result;
        if (Instant.MIN.equals(verification)) {
            result = null;
        } else {
            result = verification;
        }
        return result;
    }
}
