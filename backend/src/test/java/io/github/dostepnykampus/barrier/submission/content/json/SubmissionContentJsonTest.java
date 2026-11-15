/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission.content.json;

import io.github.dostepnykampus.OpenApiContract;
import io.github.dostepnykampus.barrier.submission.content.BarrierDetails;
import io.github.dostepnykampus.barrier.submission.content.BarrierGroup;
import io.github.dostepnykampus.barrier.submission.content.BarrierType;
import io.github.dostepnykampus.barrier.submission.content.NewEntranceMarking;
import io.github.dostepnykampus.barrier.submission.content.SubmissionContent;
import io.github.dostepnykampus.barrier.submission.content.SubmissionFloor;
import io.github.dostepnykampus.barrier.submission.content.SubmissionKind;
import io.github.dostepnykampus.barrier.submission.content.SubmissionPlace;
import io.github.dostepnykampus.barrier.submission.content.SubmissionPoint;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.exc.MismatchedInputException;

/**
 * Pełna treść zgłoszenia z OpenAPI w JSON (W3-1): wiąże się z domeną i wraca
 * jako to samo drzewo. Nieznany rodzaj, zdjęcie bez UUID, liczba zamiast symbolu budynku
 * i ciało, które nie jest obiektem, kończą wiązanie błędem.
 *
 * <p>Wymagania: US4, US7</p>
 *
 * @since 0.1.0
 */
final class SubmissionContentJsonTest {

    @Test
    void bindsFullContentToDomain() {
        Assertions.assertEquals(
            new SubmissionContent(
                SubmissionKind.PRECISE,
                new SubmissionPlace(
                    new SubmissionPoint(17.06, 51.1), 4.5, true, "C-13",
                    new SubmissionFloor("p1", 1, true)
                ),
                new BarrierDetails(BarrierGroup.VERTICAL, new BarrierType(29), 3, "opis", true),
                "node/7", NewEntranceMarking.ACCESSIBLE, JsonBodies.REPLACED,
                List.of(JsonBodies.PHOTO)
            ),
            JsonBodies.bind(JsonBodies.json(JsonBodies.FULL)).toContent(),
            "Pełna treść nie przeszła do domeny"
        );
    }

    @Test
    void writesBackSameJsonForContent() {
        final JsonNode full = JsonBodies.json(JsonBodies.FULL);
        Assertions.assertAll(
            () -> Assertions.assertEquals(
                List.of(), new OpenApiContract().violations("SubmissionContent", full.toString()),
                "Treść testowa nie jest zgodna z OpenAPI"
            ),
            () -> Assertions.assertEquals(
                full, JsonBodies.roundTrip(full), "Zapis treści różni się od wejścia"
            )
        );
    }

    @Test
    void usFourRejectsUnknownKind() {
        Assertions.assertThrows(
            MismatchedInputException.class,
            () -> JsonBodies.bind(JsonBodies.with(node -> node.put("kind", "FAST"))),
            "Nieznany rodzaj zgłoszenia przeszedł"
        );
    }

    @Test
    void usFourRejectsPhotoIdThatIsNoUuid() {
        Assertions.assertThrows(
            MismatchedInputException.class,
            () -> JsonBodies.bind(
                JsonBodies.with(node -> node.set("photoIds", JsonBodies.json("['foto']")))
            ),
            "Zdjęcie bez UUID przeszło"
        );
    }

    @Test
    void usSevenRejectsNumericBuildingSymbol() {
        Assertions.assertThrows(
            MismatchedInputException.class,
            () -> JsonBodies.bind(JsonBodies.with(node -> node.put("buildingSymbol", 13))),
            "Liczba zamiast symbolu budynku przeszła"
        );
    }

    @Test
    void usFourRejectsBodyThatIsNoObject() {
        Assertions.assertThrows(
            MismatchedInputException.class,
            () -> JsonBodies.bind(JsonBodies.json("[]")),
            "Tablica JSON przeszła jako zgłoszenie"
        );
    }
}
