/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.review.application;

import io.github.dostepnykampus.barrier.review.ReviewDecision;
import io.github.dostepnykampus.barrier.review.ReviewRepository;
import io.github.dostepnykampus.barrier.review.SimilarSubmission;
import io.github.dostepnykampus.barrier.submission.CreateResult;
import io.github.dostepnykampus.barrier.submission.EditResult;
import io.github.dostepnykampus.barrier.submission.NewSubmission;
import io.github.dostepnykampus.barrier.submission.Submission;
import io.github.dostepnykampus.barrier.submission.SubmissionEdit;
import io.github.dostepnykampus.barrier.submission.SubmissionRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Wzięcie zgłoszenia do weryfikacji: wynik portu bez zmian i konto Weryfikatora bez zmian.
 *
 * <p>Wymagania: BR-11, US10.4</p>
 *
 * @since 0.1.0
 */
final class SubmissionReviewTest {

    /**
     * Identyfikator wziętego zgłoszenia.
     */
    private static final UUID ID = UUID.fromString("0b1c2d3e-4f50-4a6b-8c7d-9e0f1a2b3c4d");

    @Test
    void returnsClaimResultAndPassesReviewer() {
        final EditResult result = new EditResult.Missing(SubmissionReviewTest.ID);
        final SubmissionReviewTest.RecordingReviews reviews =
            new SubmissionReviewTest.RecordingReviews(result);
        final SubmissionReview review = new SubmissionReview(
            reviews, new SubmissionReviewTest.UnusedSubmissions(), change -> true, symbol -> true
        );
        Assertions.assertAll(
            () -> Assertions.assertSame(
                result, review.claim(SubmissionReviewTest.ID, "weryfikator"),
                "Wynik wzięcia nie wrócił tym samym obiektem"
            ),
            () -> Assertions.assertEquals(
                List.of(SubmissionReviewTest.ID, "weryfikator"), reviews.seen(),
                "Zgłoszenie albo konto Weryfikatora zmienione po drodze"
            )
        );
    }

    /**
     * Atrapa portu weryfikacji: zapamiętuje wzięcie i zwraca podany wynik.
     *
     * @since 0.1.0
     */
    private static final class RecordingReviews implements ReviewRepository {

        /** Wynik zwracany z wzięcia. */
        private final EditResult claimed;

        /** Argumenty wzięcia w kolejności. */
        private final List<Object> seen;

        RecordingReviews(final EditResult claimed) {
            this.claimed = claimed;
            this.seen = new ArrayList<>(2);
        }

        @Override
        public EditResult claim(final UUID id, final String reviewer) {
            this.seen.add(id);
            this.seen.add(reviewer);
            return this.claimed;
        }

        @Override
        public EditResult decide(final ReviewDecision decision) {
            throw new UnsupportedOperationException("Wzięcie nie zapisuje decyzji");
        }

        @Override
        public boolean claimedBy(final UUID id, final String account) {
            throw new UnsupportedOperationException("Wzięcie nie pyta, kto wziął");
        }

        @Override
        public List<SimilarSubmission> similar(final UUID id) {
            throw new UnsupportedOperationException("Wzięcie nie szuka podobnych");
        }

        @Override
        public List<Submission> reviewQueue(final String reviewer) {
            throw new UnsupportedOperationException("Wzięcie nie czyta kolejki");
        }

        List<Object> seen() {
            return List.copyOf(this.seen);
        }
    }

    /**
     * Atrapa portu zgłoszeń, której wzięcie nie używa.
     *
     * @since 0.1.0
     */
    private static final class UnusedSubmissions implements SubmissionRepository {

        @Override
        public void lock(final UUID id) {
            throw new UnsupportedOperationException("Wzięcie nie blokuje zgłoszenia");
        }

        @Override
        public CreateResult create(final NewSubmission submission) {
            throw new UnsupportedOperationException("Wzięcie nie tworzy zgłoszenia");
        }

        @Override
        public EditResult update(final SubmissionEdit edit) {
            throw new UnsupportedOperationException("Wzięcie nie edytuje zgłoszenia");
        }

        @Override
        public Optional<Submission> archive(final UUID id) {
            throw new UnsupportedOperationException("Wzięcie nie archiwizuje zgłoszenia");
        }
    }
}
