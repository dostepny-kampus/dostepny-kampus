/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Zapytanie o listę zgłoszeń: statusy i opcjonalne filtry.
 *
 * <p>Wymagania: US8</p>
 *
 * @since 0.1.0
 */
final class SubmissionQueryTest {

    @Test
    void mthreeFourRejectsEmptyStatuses() {
        Assertions.assertEquals(
            IllegalArgumentException.class,
            Assertions.assertThrows(
                RuntimeException.class,
                () -> new SubmissionQuery(Set.of(), "B1", "p2"),
                "Zapytanie bez statusów zostało przyjęte"
            ).getClass(),
            "Zły typ wyjątku dla pustych statusów"
        );
    }

    @Test
    void mthreeFourRejectsMissingStatuses() {
        Assertions.assertThrows(
            NullPointerException.class,
            () -> new SubmissionQuery(null, "B1", "p2"),
            "Zapytanie bez zbioru statusów zostało przyjęte"
        );
    }

    @Test
    void mthreeFourCopiesStatusesAsUnmodifiableSet() {
        final Set<SubmissionStatus> source = new HashSet<>(Set.of(SubmissionStatus.SENT));
        final SubmissionQuery query = new SubmissionQuery(source, null, null);
        source.add(SubmissionStatus.ACCEPTED);
        Assertions.assertAll(
            () -> Assertions.assertEquals(
                Set.of(SubmissionStatus.SENT),
                query.statuses(),
                "Zmiana źródła zmieniła statusy zapytania"
            ),
            () -> Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> query.statuses().add(SubmissionStatus.REJECTED),
                "Statusy zapytania dają się zmienić"
            )
        );
    }

    @Test
    void mthreeFourAcceptsMissingBuildingAndFloor() {
        final SubmissionQuery query = new SubmissionQuery(
            Set.of(SubmissionStatus.ACCEPTED), null, null
        );
        Assertions.assertAll(
            () -> Assertions.assertNull(query.buildingSymbol(), "Symbol nie jest pusty"),
            () -> Assertions.assertNull(query.floorId(), "Piętro nie jest puste")
        );
    }
}
