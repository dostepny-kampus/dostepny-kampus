/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.domain;

import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Kontrakt punktu WGS84 zgłoszenia (US4, US7): zakres domknięty,
 * skończone współrzędne i długość przed szerokością.
 *
 * @since 0.1.0
 */
final class SubmissionPointTest {

    @Test
    void usFourAcceptsClosedCoordinateBounds() {
        Assertions.assertAll(
            Stream.of(-180.0, 0.0, 180.0).flatMap(
                longitude -> Stream.of(-90.0, 0.0, 90.0).map(
                    latitude -> () -> Assertions.assertDoesNotThrow(
                        () -> new SubmissionPoint(longitude, latitude),
                        "Punkt na domkniętej granicy WGS84 został odrzucony"
                    )
                )
            )
        );
    }

    @Test
    void usFourKeepsLongitudeBeforeLatitude() {
        final SubmissionPoint point = new SubmissionPoint(17.062_09, 51.107_85);
        Assertions.assertAll(
            () -> Assertions.assertEquals(
                17.062_09, point.longitude(), "Długość punktu została zamieniona"
            ),
            () -> Assertions.assertEquals(
                51.107_85, point.latitude(), "Szerokość punktu została zamieniona"
            )
        );
    }

    @Disabled("Zakres WGS84 sprawdza konstruktor dopiero w następnej zmianie")
    @Test
    void usFourRejectsInvalidLongitudeThroughConstructor() {
        Assertions.assertAll(
            Stream.of(
                -180.000_001, 180.000_001, Double.NaN,
                Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY
            ).map(
                longitude -> () -> Assertions.assertThrows(
                    InvalidSubmission.class,
                    () -> new SubmissionPoint(longitude, 0.0),
                    "Konstruktor przyjął niepoprawną długość WGS84"
                )
            )
        );
    }

    @Disabled("Zakres WGS84 sprawdza konstruktor dopiero w następnej zmianie")
    @Test
    void usSevenRejectsInvalidLatitudeThroughConstructor() {
        Assertions.assertAll(
            Stream.of(
                -90.000_001, 90.000_001, Double.NaN,
                Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY
            ).map(
                latitude -> () -> Assertions.assertThrows(
                    InvalidSubmission.class,
                    () -> new SubmissionPoint(0.0, latitude),
                    "Konstruktor przyjął niepoprawną szerokość WGS84"
                )
            )
        );
    }
}
