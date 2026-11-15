/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Decyzja Weryfikatora: przyjęcie albo odrzucenie z niepustym powodem.
 *
 * <p>Wymagania: BR-16</p>
 *
 * @since 0.1.0
 */
final class ReviewDecisionTest {

    @Test
    void rejectsBlankReason() {
        Assertions.assertAll(
            Stream.of("", " ", "\t").map(
                reason -> () -> Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> new ReviewDecision.Rejected(SubmissionSamples.ID, 1, "konto", reason),
                    String.format("Pusty powód %s został przyjęty", reason)
                )
            )
        );
    }

    @Test
    void reportsStatusOfOutcome() {
        Assertions.assertEquals(
            List.of(SubmissionStatus.ACCEPTED, SubmissionStatus.REJECTED),
            List.of(
                new ReviewDecision.Accepted(SubmissionSamples.ID, 1, "konto").outcome(),
                new ReviewDecision.Rejected(SubmissionSamples.ID, 1, "konto", "powód").outcome()
            ),
            "Status po decyzji nie zgadza się z rodzajem decyzji"
        );
    }
}
