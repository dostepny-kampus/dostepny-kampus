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
 * Szczegóły bariery (US7): zakres uciążliwości i długość opisu.
 *
 * @since 0.1.0
 */
final class BarrierDetailsTest {

    @Test
    @Disabled("Reguły treści zgłoszenia dochodzą w następnym PR")
    void usSevenRejectsSeverityOutsideOneToThree() {
        Assertions.assertAll(
            Stream.of(0, 4, -1, Integer.MAX_VALUE).map(
                severity -> () -> Assertions.assertEquals(
                    "Uciążliwość spoza zakresu 1–3",
                    Assertions.assertThrows(
                        InvalidSubmission.class,
                        () -> BarrierDetailsTest.details(severity, null),
                        String.format("Uciążliwość %d została przyjęta", severity)
                    ).getMessage(),
                    "Zły opis błędu uciążliwości"
                )
            )
        );
    }

    @Test
    void usSevenAcceptsSeverityInRangeAndMissing() {
        Assertions.assertAll(
            Stream.of(1, 2, 3, null).map(
                severity -> () -> Assertions.assertDoesNotThrow(
                    () -> BarrierDetailsTest.details(severity, null),
                    "Poprawna uciążliwość została odrzucona"
                )
            )
        );
    }

    @Test
    @Disabled("Reguły treści zgłoszenia dochodzą w następnym PR")
    void usSevenRejectsDescriptionLongerThanTwoThousand() {
        Assertions.assertEquals(
            "Opis dłuższy niż 2000 znaków",
            Assertions.assertThrows(
                InvalidSubmission.class,
                () -> BarrierDetailsTest.details(null, "x".repeat(2001)),
                "Opis o 2001 znakach został przyjęty"
            ).getMessage(),
            "Zły opis błędu długiego opisu"
        );
    }

    @Test
    void usSevenAcceptsDescriptionOfTwoThousand() {
        Assertions.assertDoesNotThrow(
            () -> BarrierDetailsTest.details(null, "x".repeat(2000)),
            "Opis o 2000 znaków został odrzucony"
        );
    }

    @Test
    void usSevenAcceptsAllFieldsMissing() {
        Assertions.assertDoesNotThrow(
            () -> new BarrierDetails(null, null, null, null, false),
            "Szczegóły bez żadnego pola zostały odrzucone"
        );
    }

    private static BarrierDetails details(final Integer severity, final String text) {
        return new BarrierDetails(BarrierGroup.OTHER, null, severity, text, false);
    }
}
