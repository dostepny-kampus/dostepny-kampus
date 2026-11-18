/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.room.json;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Nazwa, opis dojścia i znak wyjścia ewakuacyjnego z pola uwag wpisu warstwy wejść PWr;
 * wartości wymyślone w kształcie danych PWr.
 *
 * <p>Wymagania: BR-27</p>
 *
 * @since 0.1.0
 */
final class EntranceNoteTest {

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        nullValues = "-",
        value = {
            "Wejście Cesarskie. Od ul. Wybrzeże.|Wejście Cesarskie|Od ul. Wybrzeże.",
            "Wejście \"C\"|Wejście \"C\"|-",
            "Wejście dostosowane.|Wejście dostosowane|-",
            "Do wejścia prowadzą schody.|Wejście|Do wejścia prowadzą schody.",
            "Wejście znajduje się w obniżeniu.|Wejście|Wejście znajduje się w obniżeniu.",
            "Wejście od ul. Wybrzeże.|Wejście|Wejście od ul. Wybrzeże.",
            "''|Wejście|-"
        }
    )
    void splitsNoteIntoNameAndApproach(
        final String note, final String name, final @Nullable String approach
    ) {
        final EntranceNote parsed = new EntranceNote(note);
        MatcherAssert.assertThat(
            "Uwagi nie dały nazwy według pierwszego zdania i opisu dojścia z reszty",
            new String[] {parsed.name(), parsed.approach()},
            Matchers.arrayContaining(name, approach)
        );
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = {
            "Wyjście ewakuacyjne z pomieszczenia 41|true",
            "wyjście ewakuacyjne na dziedziniec|true",
            "Wejście Cesarskie. Wyjście ewakuacyjne obok.|false",
            "Do wejścia prowadzą schody.|false"
        }
    )
    void recognisesEvacuationExitByNote(final String note, final boolean exit) {
        MatcherAssert.assertThat(
            "Wyjście ewakuacyjne nie zostało rozpoznane po początku uwag",
            new EntranceNote(note).exit(),
            Matchers.is(exit)
        );
    }
}
