/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.web;

import io.github.dostepnykampus.barrier.domain.SubmissionQuery;
import io.github.dostepnykampus.barrier.domain.SubmissionStatus;
import io.github.dostepnykampus.shared.error.InvalidInput;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Filtr listy zgłoszeń (W4-5): bez statusu tylko publiczne, status niepubliczny
 * wymaga mine=true, budynek i piętro trafiają do zapytania.
 *
 * <p>Wymagania: BR-6</p>
 *
 * @since 0.1.0
 */
final class SubmissionFilterTest {

    @Test
    void wfiveWithoutStatusAsksForPublicOnlyWithBuildingAndFloor() {
        Assertions.assertEquals(
            new SubmissionQuery(Set.of(SubmissionStatus.ACCEPTED), "B1", "p2"),
            new SubmissionFilter("B1", "p2", null, false).toQuery(),
            "Brak statusu nie dał publicznych zgłoszeń z budynkiem i piętrem"
        );
    }

    @Test
    void wfiveNonPublicStatusWithoutMineIsRejected() {
        Assertions.assertAll(
            List.of(
                List.of(SubmissionStatus.IN_REVIEW),
                List.of(SubmissionStatus.ACCEPTED, SubmissionStatus.SENT)
            ).stream().map(
                status -> () -> Assertions.assertEquals(
                    "Bez mine=true dozwolony jest tylko status ACCEPTED",
                    Assertions.assertThrows(
                        InvalidInput.class,
                        new SubmissionFilter(null, null, status, false)::toQuery,
                        String.format("Status niepubliczny bez mine przeszedł: %s", status)
                    ).getMessage(),
                    "Zły komunikat błędu"
                )
            )
        );
    }

    @Test
    void wfiveMineAllowsAnyStatusAndAcceptedIsAlwaysAllowed() {
        Assertions.assertAll(
            () -> Assertions.assertEquals(
                new SubmissionQuery(Set.of(SubmissionStatus.IN_REVIEW), null, null),
                new SubmissionFilter(
                    null, null, List.of(SubmissionStatus.IN_REVIEW), true
                ).toQuery(),
                "Własne zgłoszenia w przeglądzie nie przeszły"
            ),
            () -> Assertions.assertEquals(
                new SubmissionQuery(Set.of(SubmissionStatus.ACCEPTED), null, null),
                new SubmissionFilter(
                    null, null, List.of(SubmissionStatus.ACCEPTED), false
                ).toQuery(),
                "Przyjęte bez mine nie przeszły"
            )
        );
    }
}
