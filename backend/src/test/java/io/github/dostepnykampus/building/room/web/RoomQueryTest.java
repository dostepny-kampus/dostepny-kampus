/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.room.web;

import io.github.dostepnykampus.shared.error.InvalidInput;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Odczyt zapytania sali w różnych zapisach symbolu budynku.
 *
 * <p>Wymagania: US18.1</p>
 *
 * @since 0.1.0
 */
final class RoomQueryTest {

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = {
            "A-1 329|A-1 329",
            "a1 329|A-1 329",
            " A 1  329 |A-1 329",
            "C–13 2.21|C-13 2.21",
            "d-20B 015|D-20b 015",
            "329|- 329"
        }
    )
    void readsBuildingAndNumber(final String text, final String expected) {
        final RoomQuery query = new RoomQuery(text);
        MatcherAssert.assertThat(
            "Zapytanie nie dało symbolu budynku i numeru sali",
            String.join(" ", query.building().orElse("-"), query.number()),
            Matchers.equalTo(expected)
        );
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "", "   ", "A-1 329 330", "budynek A-1 sala 329",
            "A-1 12345678901234567890123456789012345678901234567"
        }
    )
    void rejectsTextWithoutRoomNumber(final String text) {
        MatcherAssert.assertThat(
            "Tekst bez numeru sali, z nadmiarem części albo za długi nie dał błędu 400",
            Assertions.assertThrows(
                InvalidInput.class, () -> new RoomQuery(text).number(),
                "Zły tekst zapytania został przyjęty"
            ).getMessage(),
            Matchers.containsString("numer sali")
        );
    }
}
