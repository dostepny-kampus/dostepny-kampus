/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission.web;

import io.github.dostepnykampus.barrier.submission.ReviewDecision;
import io.github.dostepnykampus.barrier.submission.content.BarrierType;
import io.github.dostepnykampus.barrier.submission.content.InvalidSubmission;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Ciało decyzji Weryfikatora: przyjęcie niesie typ bariery i uciążliwość razem albo wcale,
 * a powód odrzucenia ma najwyżej 500 znaków.
 *
 * <p>Wymagania: US10.3, BR-14, BR-16</p>
 *
 * @since 0.1.0
 */
final class ReviewRequestTest {

    /**
     * Identyfikator zgłoszenia z adresu.
     */
    private static final UUID ID = UUID.fromString("0b1c2d3e-4f50-4a6b-8c7d-9e0f1a2b3c4d");

    @Test
    void mapsAcceptWithTypeAndSeverityToClassifiedDecision() {
        Assertions.assertEquals(
            new ReviewDecision.Classified(ReviewRequestTest.ID, 1, "konto", new BarrierType(3), 2),
            new ReviewRequest("ACCEPT", 1, null, null, "TB-3", 2)
                .toDecision(ReviewRequestTest.ID, "konto"),
            "Przyjęcie z typem i uciążliwością nie dało decyzji z klasyfikacją"
        );
    }

    @Test
    void mapsAcceptWithoutClassificationToPlainAcceptance() {
        Assertions.assertEquals(
            new ReviewDecision.Accepted(ReviewRequestTest.ID, 1, "konto"),
            new ReviewRequest("ACCEPT", 1, null, null, null, null)
                .toDecision(ReviewRequestTest.ID, "konto"),
            "Przyjęcie bez typu i uciążliwości nie dało zwykłej decyzji o przyjęciu"
        );
    }

    @Test
    void refusesAcceptWithOnlyOneOfTypeAndSeverity() {
        Assertions.assertAll(
            Stream.of(
                new ReviewRequest("ACCEPT", 1, null, null, "TB-3", null),
                new ReviewRequest("ACCEPT", 1, null, null, null, 2)
            ).map(
                request -> () -> Assertions.assertThrows(
                    InvalidSubmission.class,
                    () -> request.toDecision(ReviewRequestTest.ID, "konto"),
                    "Przyjęcie z jednym z pól typu i uciążliwości zostało przyjęte"
                )
            )
        );
    }

    @Test
    void acceptsRejectionReasonOfFiveHundredCharacters() {
        Assertions.assertEquals(
            new ReviewDecision.Rejected(ReviewRequestTest.ID, 1, "konto", "a".repeat(500)),
            new ReviewRequest("REJECT", 1, "a".repeat(500), null, null, null)
                .toDecision(ReviewRequestTest.ID, "konto"),
            "Powód odrzucenia o 500 znakach nie przeszedł"
        );
    }

    @Test
    void refusesRejectionReasonLongerThanFiveHundredCharacters() {
        Assertions.assertThrows(
            InvalidSubmission.class,
            () -> new ReviewRequest("REJECT", 1, "a".repeat(501), null, null, null)
                .toDecision(ReviewRequestTest.ID, "konto"),
            "Powód odrzucenia o 501 znakach został przyjęty"
        );
    }
}
