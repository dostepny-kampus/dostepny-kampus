/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

/**
 * Testy BR-7, BR-8, BR-19 i BR-24: pola wymagane zależą od rodzaju
 * zgłoszenia, a pola z kontraktu mają swoje zakresy.
 *
 * @since 0.1.0
 */
final class SubmissionRulesTest {

    /**
     * Poprawny pomiar precyzyjny w budynku.
     */
    private static final String PRECISE = String.join(
        "",
        "{\"kind\":\"PRECISE\",\"barrierType\":\"TB-3\",\"severity\":2,",
        "\"buildingSymbol\":\"C-13\",\"floor\":{\"number\":0}}"
    );

    @Test
    void brSevenAcceptsCompletePreciseSubmission() {
        Assertions.assertDoesNotThrow(
            SubmissionRulesTest.check(SubmissionRulesTest.PRECISE),
            "Pełny pomiar precyzyjny nie przeszedł"
        );
    }

    @Test
    void brSevenNeedsBarrierType() {
        Assertions.assertThrows(
            ResponseStatusException.class,
            SubmissionRulesTest.precise("\"barrierType\":\"TB-3\",", ""),
            "Pomiar bez typu przeszedł"
        );
    }

    @Test
    void brSevenNeedsSeverity() {
        Assertions.assertThrows(
            ResponseStatusException.class,
            SubmissionRulesTest.precise("\"severity\":2,", ""),
            "Pomiar bez uciążliwości przeszedł"
        );
    }

    @Test
    void brSevenNeedsFloorInBuilding() {
        Assertions.assertThrows(
            ResponseStatusException.class,
            SubmissionRulesTest.precise("{\"number\":0}", "{}"),
            "Pomiar w budynku bez piętra przeszedł"
        );
    }

    @Test
    void brSevenNeedsDescriptionOfOtherBarrier() {
        Assertions.assertThrows(
            ResponseStatusException.class,
            SubmissionRulesTest.precise("TB-3", "TB-29"),
            "TB-29 bez opisu przeszedł"
        );
    }

    @Test
    void brSevenRejectsUnknownBarrierType() {
        Assertions.assertThrows(
            ResponseStatusException.class,
            SubmissionRulesTest.precise("TB-3", "TB-30"),
            "Typ TB-30 przeszedł"
        );
    }

    @Test
    void brSevenRejectsSeverityAboveThree() {
        Assertions.assertThrows(
            ResponseStatusException.class,
            SubmissionRulesTest.precise("\"severity\":2", "\"severity\":4"),
            "Uciążliwość 4 przeszła"
        );
    }

    @Test
    void brNineteenNeedsReplacedSubmission() {
        Assertions.assertThrows(
            ResponseStatusException.class,
            SubmissionRulesTest.precise("\"severity\":2,", "\"barrierRemoved\":true,"),
            "Usunięcie bez zgłoszenia przeszło"
        );
    }

    @Test
    void brEightNeedsBarrierGroup() {
        Assertions.assertThrows(
            ResponseStatusException.class,
            SubmissionRulesTest.check("{\"kind\":\"QUICK\"}"),
            "Zgłoszenie szybkie bez grupy barier przeszło"
        );
    }

    @Test
    void brTwentyFourNeedsNewMarking() {
        Assertions.assertThrows(
            ResponseStatusException.class,
            SubmissionRulesTest.check("{\"kind\":\"ENTRANCE_MARKING\",\"entranceId\":\"node/1\"}"),
            "Zmiana oznaczenia wejścia bez oznaczenia przeszła"
        );
    }

    @Test
    void brTwentyFourRejectsNoDataMarking() {
        Assertions.assertThrows(
            ResponseStatusException.class,
            SubmissionRulesTest.check(
                "{\"kind\":\"ENTRANCE_MARKING\",\"entranceId\":\"e\",\"entranceMarking\":\"NO_DATA\"}"
            ),
            "Zmiana oznaczenia wejścia na „brak danych” przeszła"
        );
    }

    private static Executable precise(final String valid, final String wrong) {
        return SubmissionRulesTest.check(SubmissionRulesTest.PRECISE.replace(valid, wrong));
    }

    private static Executable check(final String body) {
        return () -> SubmissionRules.check(new ObjectMapper().readTree(body));
    }
}
