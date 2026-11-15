/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.room.json;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Etykieta piętra z każdego zapisu nazwy w danych PWr.
 *
 * <p>Wymagania: US16.1, NFR-18</p>
 *
 * @since 0.1.0
 */
final class FloorNameTest {

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = {
            "Bud. A1 p 1|1 piętro",
            "Bud. C5 p 10|10 piętro",
            "Bud. C13 p1|1 piętro",
            "Bud L1 p3|3 piętro",
            "Bud. A1 parter|Parter",
            "Budynek H14 parter|Parter",
            "Bud. A1 piwnica|Piwnica",
            "Bud. B4 przyziemie|Przyziemie",
            "Bud. B5 antresola|Antresola",
            "antresola|Antresola",
            "Bud. A1 dach|dach",
            "Bud. D20 p 9|9 piętro"
        }
    )
    void labelsFloorFromPwrName(final String name, final String expected) {
        MatcherAssert.assertThat(
            "Nazwa piętra z danych PWr nie dała ujednoliconej etykiety",
            new FloorName(name).label(),
            Matchers.equalTo(expected)
        );
    }
}
