/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission.content;

import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Typ bariery z klasyfikacji TB-1…TB-29: kod ze wzorca schematu
 * BarrierType w OpenAPI, numer i typ „inna bariera”.
 *
 * <p>Wymagania: US7.3</p>
 *
 * @since 0.1.0
 */
final class BarrierTypeTest {

    /**
     * Opis błędu typu spoza klasyfikacji, jak dotąd w regułach zgłoszenia.
     */
    private static final String OUTSIDE = "Typ bariery spoza TB-1…TB-29";

    @Test
    void usSevenThreeParsesNumberOfEveryType() {
        Assertions.assertAll(
            IntStream.rangeClosed(1, 29).mapToObj(
                number -> () -> Assertions.assertEquals(
                    number,
                    new BarrierType(String.format("TB-%d", number)).number(),
                    String.format("Kod TB-%d dał inny numer typu", number)
                )
            )
        );
    }

    @Test
    void usSevenThreeKeepsCodeOfEveryType() {
        Assertions.assertAll(
            IntStream.rangeClosed(1, 29).mapToObj(number -> String.format("TB-%d", number)).map(
                code -> () -> Assertions.assertEquals(
                    code,
                    new BarrierType(code).code(),
                    String.format("Typ z kodu %s ma inny kod", code)
                )
            )
        );
    }

    @Test
    void usSevenThreeMarksOnlyTwentyNineAsOther() {
        Assertions.assertAll(
            IntStream.rangeClosed(1, 29).mapToObj(
                number -> () -> Assertions.assertEquals(
                    number == 29,
                    new BarrierType(String.format("TB-%d", number)).other(),
                    String.format("Typ TB-%d źle oznaczony jako „inna bariera”", number)
                )
            )
        );
    }

    @Test
    void usSevenThreeRejectsCodeOutsidePattern() {
        Assertions.assertAll(
            Stream.of(
                "TB-0", "TB-30", "TB-01", "tb-1", " TB-1", "TB-1 ", "TB1", "",
                "TB-+1", "TB-1".concat(Character.toString(0x0A)),
                "TB-".concat(Character.toString(0x0663)),
                "TB-".concat(Character.toString(0xFF13))
            ).map(
                code -> () -> Assertions.assertEquals(
                    BarrierTypeTest.OUTSIDE,
                    Assertions.assertThrows(
                        InvalidSubmission.class,
                        () -> new BarrierType(code),
                        String.format("Kod „%s” spoza wzorca został przyjęty", code)
                    ).getMessage(),
                    String.format("Zły opis błędu dla kodu „%s”", code)
                )
            )
        );
    }

    @Test
    void usSevenThreeRejectsNumberOutsideClassification() {
        Assertions.assertAll(
            Stream.of(0, 30, -1, Integer.MIN_VALUE, Integer.MAX_VALUE).map(
                number -> () -> Assertions.assertEquals(
                    BarrierTypeTest.OUTSIDE,
                    Assertions.assertThrows(
                        InvalidSubmission.class,
                        () -> new BarrierType(number),
                        String.format("Numer typu %d został przyjęty", number)
                    ).getMessage(),
                    String.format("Zły opis błędu dla numeru %d", number)
                )
            )
        );
    }
}
