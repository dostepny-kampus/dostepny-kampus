/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

/**
 * Testy US4 i US7: serwer sprawdza pola wymagane przez schemat NewSubmission
 * i odrzuca zgłoszenie bez zdjęcia, miejsca albo identyfikatora (BR-7, BR-8).
 *
 * @since 0.1.0
 */
final class SubmissionDraftTest {

    /**
     * Poprawne zgłoszenie szybkie z polem spoza schematu.
     */
    private static final String VALID = String.join(
        "",
        "{\"id\":\"0b6f6c1e-8a43-4d5e-9f43-2f1d3c4b5a69\",\"kind\":\"QUICK\",\"extra\":1,",
        "\"location\":{\"type\":\"Point\",\"coordinates\":[17.0,51.1]},",
        "\"buildingSymbol\":\"C-13\",\"barrierGroup\":\"OTHER\",",
        "\"photoIds\":[\"5d1c2b3a-4e5f-4a6b-8c7d-9e0f1a2b3c4d\"]}"
    );

    @Test
    void usFourKeepsOnlyContentFields() {
        final SubmissionDraft draft = SubmissionDraftTest.draft("", "");
        MatcherAssert.assertThat(
            "Treść zgłoszenia ma identyfikator albo pole spoza schematu",
            draft.content().has("id") || draft.content().has("extra"),
            Matchers.is(false)
        );
    }

    @Test
    void usFourReadsPlaceFromGeoJson() {
        MatcherAssert.assertThat(
            "Szerokość geograficzna nie jest drugą współrzędną GeoJSON",
            SubmissionDraftTest.draft("", "").lat(),
            Matchers.closeTo(51.1, 1e-9)
        );
    }

    @Test
    void usFourRejectsSubmissionWithoutPhoto() {
        Assertions.assertThrows(
            ResponseStatusException.class,
            SubmissionDraftTest.parse("[\"5d1c2b3a-4e5f-4a6b-8c7d-9e0f1a2b3c4d\"]", "[]"),
            "Zgłoszenie bez zdjęcia przeszło"
        );
    }

    @Test
    void usFourRejectsBadPhotoId() {
        Assertions.assertThrows(
            ResponseStatusException.class,
            SubmissionDraftTest.parse("\"5d1c2b3a-4e5f-4a6b-8c7d-9e0f1a2b3c4d\"", "\"foto\""),
            "Zdjęcie bez UUID przeszło"
        );
    }

    @Test
    void usFourRejectsBadSubmissionId() {
        Assertions.assertThrows(
            ResponseStatusException.class,
            SubmissionDraftTest.parse("0b6f6c1e-8a43-4d5e-9f43-2f1d3c4b5a69", "abc"),
            "Zgłoszenie bez UUID przeszło"
        );
    }

    @Test
    void usFourRejectsUnknownKind() {
        Assertions.assertThrows(
            ResponseStatusException.class,
            SubmissionDraftTest.parse("QUICK", "FAST"),
            "Nieznany rodzaj zgłoszenia przeszedł"
        );
    }

    @Test
    void usFourRejectsLineInsteadOfPoint() {
        Assertions.assertThrows(
            ResponseStatusException.class,
            SubmissionDraftTest.parse("\"Point\"", "\"LineString\""),
            "Linia zamiast punktu przeszła"
        );
    }

    @Test
    void usSevenRejectsNumericBuilding() {
        Assertions.assertThrows(
            ResponseStatusException.class,
            SubmissionDraftTest.parse("\"C-13\"", "13"),
            "Liczba zamiast symbolu budynku przeszła"
        );
    }

    @Test
    void usFourRejectsBodyThatIsNoObject() {
        Assertions.assertThrows(
            ResponseStatusException.class,
            () -> SubmissionDraft.of(new ObjectMapper().readTree("[]")),
            "Tablica JSON przeszła jako zgłoszenie"
        );
    }

    private static Executable parse(final String valid, final String wrong) {
        return () -> SubmissionDraftTest.draft(valid, wrong);
    }

    private static SubmissionDraft draft(final String valid, final String wrong) {
        String body = SubmissionDraftTest.VALID;
        if (!valid.isEmpty()) {
            body = body.replace(valid, wrong);
        }
        return SubmissionDraft.of(new ObjectMapper().readTree(body));
    }
}
