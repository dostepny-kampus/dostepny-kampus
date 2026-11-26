/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.voice

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Normalizacja tekstu z rozpoznawania mowy: wielkość liter, znaki polskie, interpunkcja i liczebniki.
 */
class SpeechTextNormalizerTest {
    /** Zamienia wielkie litery na małe i porządkuje białe znaki. */
    @Test
    fun foldsCaseAndWhitespace() {
        assertNormalized(
            mapOf(
                "Budynek GŁÓWNY" to "budynek glowny",
                "  Sala   A1\t\nWejście  " to "sala a1 wejscie",
            ),
        )
    }

    /** Składa polskie znaki diakrytyczne do liter ASCII. */
    @Test
    fun foldsPolishDiacritics() {
        assertNormalized(
            mapOf(
                "ąćęłńóśźż ĄĆĘŁŃÓŚŹŻ" to "acelnoszz acelnoszz",
                "Zażółć gęślą jaźń" to "zazolc gesla jazn",
            ),
        )
    }

    /** Usuwa interpunkcję oraz myślniki między literą i liczbą albo między wyrazami. */
    @Test
    fun removesPunctuationAndDashes() {
        assertNormalized(
            mapOf(
                "Budynek A-1, wejście główne!" to "budynek a 1 wejscie glowne",
                "Czy to C-13? Tak: „parter”." to "czy to c 13 tak parter",
                "Piętro — pierwsze; sala (329)…" to "pietro 1 sala 329",
            ),
        )
    }

    /** Zachowuje znak minus bezpośrednio przed cyfrą jako ujemny numer piętra. */
    @Test
    fun keepsNegativeFloorNumber() {
        assertNormalized(
            mapOf(
                "-1" to "-1",
                "Piętro -1, piwnica." to "pietro -1 piwnica",
                "Poziom -2!" to "poziom -2",
            ),
        )
    }

    /** Zamienia liczebniki główne i porządkowe od 0 do 9 na cyfry. */
    @Test
    fun replacesUnitsAndOrdinals() {
        assertNormalized(
            mapOf(
                "zero zerowy na zerowym" to "0 0 na 0",
                "jeden jedna jednego pierwszy pierwsza pierwsze pierwszego" to "1 1 1 1 1 1 1",
                "dwa dwie dwóch drugi druga drugie drugiego" to "2 2 2 2 2 2 2",
                "trzy trzech trzeci trzecia trzecie trzeciego" to "3 3 3 3 3 3",
                "cztery czterech czwarty czwarta czwarte" to "4 4 4 4 4",
                "pięć pięciu piąty piąta piątym" to "5 5 5 5 5",
                "sześć sześciu szósty szósta" to "6 6 6 6",
                "siedem siedmiu siódmy siódma" to "7 7 7 7",
                "osiem ośmiu ósmy ósma" to "8 8 8 8",
                "dziewięć dziewięciu dziewiąty dziewiątej" to "9 9 9 9",
            ),
        )
    }

    /** Zamienia liczebniki z zakresu 10–90 na cyfry, także w odmianie i bez polskich znaków. */
    @Test
    fun replacesTeensAndTens() {
        assertNormalized(
            mapOf(
                "dziesięć dziesiąty jedenasty dwunasta" to "10 10 11 12",
                "trzynaście czternaście piętnaście szesnaście" to "13 14 15 16",
                "siedemnaście osiemnaście dziewiętnaście" to "17 18 19",
                "dwadzieścia dwudziesty trzydzieści czterdzieści" to "20 20 30 40",
                "pięćdziesiąt sześćdziesiąt siedemdziesiąt osiemdziesiąt dziewięćdziesiąt" to "50 60 70 80 90",
                "pietnascie piecdziesiat dziewiecdziesiat" to "15 50 90",
            ),
        )
    }

    /** Zamienia liczebniki setne i tysiąc na cyfry. */
    @Test
    fun replacesHundreds() {
        assertNormalized(
            mapOf(
                "sto setny dwieście trzysta czterysta" to "100 100 200 300 400",
                "pięćset sześćset siedemset osiemset dziewięćset" to "500 600 700 800 900",
                "tysiąc tysięczny" to "1000 1000",
            ),
        )
    }

    /** Składa sąsiednie liczebniki w jedną liczbę wielocyfrową. */
    @Test
    fun combinesCompoundNumbers() {
        assertNormalized(
            mapOf(
                "dwadzieścia jeden" to "21",
                "dwudziesty czwarty" to "24",
                "sto pięć" to "105",
                "sto trzynaście" to "113",
                "sto dwudziesty piąty" to "125",
                "trzysta dwadzieścia dziewięć" to "329",
                "tysiąc dwieście trzydzieści cztery" to "1234",
            ),
        )
    }

    /** Rozdziela kolejne liczebniki, gdy nie tworzą poprawnej liczby złożonej. */
    @Test
    fun keepsSeparateNumbersApart() {
        assertNormalized(
            mapOf(
                "trzy dwa dziewięć" to "3 2 9",
                "pięćdziesiąt dwanaście" to "50 12",
                "trzysta trzynaście pięć" to "313 5",
                "dwadzieścia trzydzieści" to "20 30",
                "sto dwieście" to "100 200",
                "zero pięć" to "0 5",
                "sto zero" to "100 0",
            ),
        )
    }

    /** Normalizuje pełne wypowiedzi z dialogu o budynku, piętrze i sali bez zmiany słów niebędących liczbą. */
    @Test
    fun normalizesDialogSentences() {
        assertNormalized(
            mapOf(
                "Pokaż budynek C-13, sala trzysta dwadzieścia dziewięć." to "pokaz budynek c 13 sala 329",
                "Powtórz jeszcze raz!" to "powtorz jeszcze raz",
                "Wejdź na drugie piętro w budynku A jeden." to "wejdz na 2 pietro w budynku a 1",
            ),
        )
    }

    /** Zwraca pusty napis dla wejścia pustego albo złożonego z samych spacji i interpunkcji. */
    @Test
    fun returnsEmptyForBlankInput() {
        assertNormalized(
            mapOf(
                "" to "",
                "   \t\n  " to "",
                "..., !? — ()" to "",
            ),
        )
    }

    private fun assertNormalized(cases: Map<String, String>) {
        cases.forEach { (input, expected) ->
            assertEquals(expected, normalizeSpeechText(input), "Normalizacja „$input”")
        }
    }
}
