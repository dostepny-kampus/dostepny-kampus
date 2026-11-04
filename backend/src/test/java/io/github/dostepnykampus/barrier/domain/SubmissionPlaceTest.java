/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.domain;

import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Miejsce zgłoszenia (US6, BR-G2): dokładność pozycji i kopie z innym
 * budynkiem.
 *
 * @since 0.1.0
 */
final class SubmissionPlaceTest {

    /**
     * Opis błędu ujemnej dokładności.
     */
    private static final String NEGATIVE = "Dokładność pozycji jest ujemna";

    @Test
    void usSixRejectsNegativeAccuracy() {
        Assertions.assertAll(
            Stream.of(-0.001, -1.0, Double.NEGATIVE_INFINITY).map(
                accuracy -> () -> Assertions.assertEquals(
                    SubmissionPlaceTest.NEGATIVE,
                    Assertions.assertThrows(
                        InvalidSubmission.class,
                        () -> SubmissionPlaceTest.place(accuracy, "B1"),
                        String.format("Dokładność %s została przyjęta", accuracy)
                    ).getMessage(),
                    "Zły opis błędu ujemnej dokładności"
                )
            )
        );
    }

    @Test
    void usSixRejectsNanAccuracy() {
        Assertions.assertEquals(
            SubmissionPlaceTest.NEGATIVE,
            Assertions.assertThrows(
                InvalidSubmission.class,
                () -> SubmissionPlaceTest.place(Double.NaN, "B1"),
                "Dokładność NaN została przyjęta"
            ).getMessage(),
            "Zły opis błędu dokładności NaN"
        );
    }

    @Test
    void usSixAcceptsZeroAndMissingAccuracy() {
        Assertions.assertAll(
            () -> Assertions.assertDoesNotThrow(
                () -> SubmissionPlaceTest.place(0.0, "B1"),
                "Dokładność 0 została odrzucona"
            ),
            () -> Assertions.assertDoesNotThrow(
                () -> SubmissionPlaceTest.place(null, "B1"),
                "Brak dokładności został odrzucony"
            )
        );
    }

    @Test
    void usSixRejectsMissingPoint() {
        Assertions.assertThrows(
            NullPointerException.class,
            () -> new SubmissionPlace(null, 1.0, false, "B1", null),
            "Miejsce bez punktu zostało przyjęte"
        );
    }

    @Test
    void brGTwoWithBuildingChangesOnlyBuilding() {
        final SubmissionPlace origin = SubmissionPlaceTest.place(4.5, "B1");
        final SubmissionPlace copy = origin.withBuilding("C3");
        Assertions.assertAll(
            () -> Assertions.assertEquals(
                new SubmissionPlace(origin.point(), 4.5, true, "C3", origin.floor()),
                copy,
                "Kopia z nowym budynkiem zmieniła inne pola"
            ),
            () -> Assertions.assertEquals(
                "B1", origin.buildingSymbol(), "Oryginał zmienił budynek"
            )
        );
    }

    @Test
    void brGTwoOutsideBuildingsClearsOnlyBuilding() {
        final SubmissionPlace origin = SubmissionPlaceTest.place(4.5, "B1");
        final SubmissionPlace copy = origin.outsideBuildings();
        Assertions.assertAll(
            () -> Assertions.assertEquals(
                new SubmissionPlace(origin.point(), 4.5, true, null, origin.floor()),
                copy,
                "Kopia poza budynkami zmieniła piętro albo inne pola"
            ),
            () -> Assertions.assertEquals(
                "B1", origin.buildingSymbol(), "Oryginał utracił budynek"
            )
        );
    }

    private static SubmissionPlace place(final Double accuracy, final String building) {
        return new SubmissionPlace(
            new SubmissionPoint(17.06, 51.1),
            accuracy,
            true,
            building,
            new SubmissionFloor("p2", 2, false)
        );
    }
}
