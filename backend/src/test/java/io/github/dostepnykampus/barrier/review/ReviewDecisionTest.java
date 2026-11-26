/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.review;

import io.github.dostepnykampus.barrier.submission.SubmissionStatus;
import io.github.dostepnykampus.barrier.submission.content.BarrierType;
import io.github.dostepnykampus.barrier.submission.content.InvalidSubmission;
import io.github.dostepnykampus.barrier.submission.content.SubmissionKind;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Decyzja Weryfikatora: przyjęcie albo odrzucenie z niepustym powodem.
 *
 * <p>Wymagania: US10.3, BR-14, BR-16</p>
 *
 * @since 0.1.0
 */
final class ReviewDecisionTest {

    /**
     * Identyfikator ocenianego zgłoszenia.
     */
    private static final UUID ID = UUID.fromString("0b1c2d3e-4f50-4a6b-8c7d-9e0f1a2b3c4d");

    @Test
    void rejectsBlankReason() {
        Assertions.assertAll(
            Stream.of("", " ", "\t").map(
                reason -> () -> Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> new ReviewDecision.Rejected(ReviewDecisionTest.ID, 1, "konto", reason),
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
                new ReviewDecision.Accepted(ReviewDecisionTest.ID, 1, "konto").outcome(),
                new ReviewDecision.Rejected(ReviewDecisionTest.ID, 1, "konto", "powód").outcome()
            ),
            "Status po decyzji nie zgadza się z rodzajem decyzji"
        );
    }

    @Test
    void requiresClassificationOfQuickSubmission() {
        Assertions.assertThrows(
            InvalidSubmission.class,
            () -> new ReviewDecision.Accepted(ReviewDecisionTest.ID, 1, "konto")
                .admit(SubmissionKind.QUICK),
            "Przyjęcie zgłoszenia szybkiego bez typu i uciążliwości zostało dopuszczone"
        );
    }

    @Test
    void refusesClassificationOfOtherKinds() {
        Assertions.assertAll(
            Stream.of(SubmissionKind.PRECISE, SubmissionKind.ENTRANCE_MARKING).map(
                kind -> () -> Assertions.assertThrows(
                    InvalidSubmission.class,
                    () -> new ReviewDecision.Classified(
                        ReviewDecisionTest.ID, 1, "konto", new BarrierType(3), 2
                    ).admit(kind),
                    String.format("Typ i uciążliwość przy rodzaju %s zostały dopuszczone", kind)
                )
            )
        );
    }

    @Test
    void refusesSeverityOutsideOneToThree() {
        Assertions.assertAll(
            Stream.of(0, 4).map(
                severity -> () -> Assertions.assertThrows(
                    InvalidSubmission.class,
                    () -> new ReviewDecision.Classified(
                        ReviewDecisionTest.ID, 1, "konto", new BarrierType(3), severity
                    ),
                    String.format("Uciążliwość %d spoza zakresu 1–3 została przyjęta", severity)
                )
            )
        );
    }

    @Test
    void acceptsSeverityAtBoundsOfRange() {
        Assertions.assertAll(
            Stream.of(1, 3).map(
                severity -> () -> Assertions.assertEquals(
                    SubmissionStatus.ACCEPTED,
                    new ReviewDecision.Classified(
                        ReviewDecisionTest.ID, 1, "konto", new BarrierType(3), severity
                    ).outcome(),
                    String.format("Uciążliwość %d z zakresu 1–3 nie została przyjęta", severity)
                )
            )
        );
    }
}
