/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.room;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Funkcja PWr trafia do swojej grupy pomieszczeń.
 *
 * <p>Wymagania: US16.1, US18.1</p>
 *
 * @since 0.1.0
 */
final class PwrFunctionTest {

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = {
            "Sala dydaktyczna|CLASSROOM",
            "Laboratorium|CLASSROOM",
            "Pracownia komputerowa|CLASSROOM",
            "Sala wykładowa|LECTURE_HALL",
            "Aula|LECTURE_HALL",
            "Korytarz|CORRIDOR",
            "Klatka schodowa|STAIRCASE",
            "Schody poza klatka schodowa|STAIRCASE",
            "Winda|ELEVATOR",
            "Toaleta dla OzN|ACCESSIBLE_TOILET",
            "Toaleta dla osób z niepełnosprawnością|ACCESSIBLE_TOILET",
            "Toaleta damska|TOILET",
            "WC męskie|TOILET",
            "Gabinet|OFFICE",
            "Pomieszczenie administracyjne|OFFICE",
            "Dziekanat Wydziału Chemicznego|OFFICE",
            "Biuro Szkoły Doktorskiej|OFFICE",
            "Biblioteka Górnictwa|OTHER",
            "Portiernia|OTHER",
            "Inne|OTHER"
        }
    )
    void mapsFunctionOfPwrToCategory(final String function, final RoomCategory group) {
        MatcherAssert.assertThat(
            String.format("Funkcja „%s” trafiła do złej grupy", function),
            new PwrFunction(function).category(),
            Matchers.is(group)
        );
    }

    @ParameterizedTest
    @CsvSource({"'  KORYTARZ ',CORRIDOR", "toaleta męska,TOILET"})
    void ignoresCaseAndSpaces(final String function, final RoomCategory group) {
        MatcherAssert.assertThat(
            "Wielkość liter albo spacje zmieniły grupę funkcji",
            new PwrFunction(function).category(),
            Matchers.is(group)
        );
    }
}
