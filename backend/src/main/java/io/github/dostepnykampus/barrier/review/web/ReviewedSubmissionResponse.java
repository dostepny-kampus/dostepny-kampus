/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.review.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.github.dostepnykampus.barrier.review.application.ReviewedSubmission;
import io.github.dostepnykampus.barrier.submission.AuthorRole;
import io.github.dostepnykampus.barrier.submission.web.SubmissionResponse;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Odpowiedź z zgłoszeniem dla Weryfikatora, schemat Submission w OpenAPI z polem author.
 *
 * <p>Wymagania: BR-5</p>
 *
 * @param submission Zgłoszenie, w JSON płasko
 * @param author Autor albo {@code null}, gdy serwer nie zna konta autora
 * @since 0.1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record ReviewedSubmissionResponse(
    @JsonUnwrapped SubmissionResponse submission, @Nullable Author author
) {

    /**
     * Zapisuje zgłoszenie w weryfikacji w odpowiedzi.
     *
     * @param reviewed Zgłoszenie z autorem
     */
    ReviewedSubmissionResponse(final ReviewedSubmission reviewed) {
        this(
            new SubmissionResponse(reviewed.submission()), ReviewedSubmissionResponse.of(reviewed)
        );
    }

    private static @Nullable Author of(final ReviewedSubmission reviewed) {
        final String pseudonym = reviewed.pseudonym();
        final Author result;
        if (pseudonym == null) {
            result = null;
        } else {
            result = new Author(pseudonym, reviewed.roles());
        }
        return result;
    }

    /**
     * Autor widziany przez Weryfikatora: tylko pseudonim i role.
     *
     * @param pseudonym Pseudonim autora
     * @param roles Role autora z chwili wysłania
     * @since 0.1.0
     */
    record Author(String pseudonym, List<AuthorRole> roles) {
    }
}
