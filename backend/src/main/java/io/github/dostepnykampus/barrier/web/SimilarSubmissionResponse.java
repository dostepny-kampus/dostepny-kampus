/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.web;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.github.dostepnykampus.barrier.domain.SimilarSubmission;

/**
 * Odpowiedź z podobnym zgłoszeniem, schemat SimilarSubmission w OpenAPI.
 *
 * @param submission Zgłoszenie, w JSON płasko obok odległości
 * @param distanceMeters Odległość od sprawdzanego zgłoszenia w metrach
 * @since 0.1.0
 */
record SimilarSubmissionResponse(
    @JsonUnwrapped SubmissionResponse submission, double distanceMeters
) {

    /**
     * Zapisuje podobne zgłoszenie z domeny w odpowiedzi.
     *
     * @param similar Podobne zgłoszenie
     */
    SimilarSubmissionResponse(final SimilarSubmission similar) {
        this(new SubmissionResponse(similar.submission()), similar.distanceMeters());
    }
}
