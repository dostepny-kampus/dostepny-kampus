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
import io.github.dostepnykampus.shared.error.InvalidInput;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.node.ObjectNode;

/**
 * Pełna treść zgłoszenia z OpenAPI w JSON (W3-1): wiąże się z domeną i wraca
 * jako to samo drzewo. Nieznany rodzaj, zdjęcie bez UUID, liczba zamiast symbolu budynku
 * i ciało, które nie jest obiektem, kończą wiązanie błędem.
 *
 * <p>Wymagania: US4, US7, US8.4, US17.2, BR-18, BR-19, BR-24</p>
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

    @Test
    void allowsQuickForEveryone() {
        Assertions.assertFalse(
            SubmissionContentJsonTest.bound(node -> { }).forCollector(),
            "Szybkie zgłoszenie jest dla każdego"
        );
    }

    @Test
    void kindlessIsForEveryone() {
        Assertions.assertFalse(
            SubmissionContentJsonTest.bound(node -> node.remove("kind")).forCollector(),
            "Treść bez rodzaju jest dla każdego"
        );
    }

    @Test
    void reservesPreciseForCollector() {
        Assertions.assertTrue(
            SubmissionContentJsonTest.bound(node -> node.put("kind", "PRECISE")).forCollector(),
            "Zgłoszenie precyzyjne jest dla zbierającego"
        );
    }

    @Test
    void reservesEntranceMarkingForCollector() {
        Assertions.assertTrue(
            SubmissionContentJsonTest.bound(node -> node.put("kind", "ENTRANCE_MARKING"))
                .forCollector(),
            "Oznaczenie wejścia jest dla zbierającego"
        );
    }

    @Test
    void reservesQuickReplacingForCollector() {
        Assertions.assertTrue(
            SubmissionContentJsonTest.bound(SubmissionContentJsonTest::replacing).forCollector(),
            "Szybkie zgłoszenie zastępujące inne jest dla zbierającego"
        );
    }

    @Test
    void replacesNothingWithNewSubmission() {
        Assertions.assertEquals(
            Optional.empty(), SubmissionContentJsonTest.bound(node -> { }).ordinaryChange(),
            "Nowe zgłoszenie niczego nie zastępuje"
        );
    }

    @Test
    void replacesSubmissionOnChange() {
        Assertions.assertEquals(
            Optional.of(JsonBodies.REPLACED),
            SubmissionContentJsonTest.bound(SubmissionContentJsonTest::replacing).ordinaryChange(),
            "Zmiana zastępuje wskazane zgłoszenie"
        );
    }

    @Test
    void replacesSubmissionWithKeptBarrier() {
        Assertions.assertEquals(
            Optional.of(JsonBodies.REPLACED),
            SubmissionContentJsonTest.bound(
                node -> SubmissionContentJsonTest.replacing(node).put("barrierRemoved", false)
            ).ordinaryChange(),
            "Zachowana bariera zastępuje zgłoszenie"
        );
    }

    @Test
    void distinguishesRemovedBarrierFromOrdinaryChange() {
        Assertions.assertEquals(
            Optional.empty(),
            SubmissionContentJsonTest.bound(
                node -> SubmissionContentJsonTest.replacing(node).put("barrierRemoved", true)
            ).ordinaryChange(),
            "Usunięta bariera nie jest zwykłą zmianą"
        );
    }

    @Test
    void omitsFalseFlagsAndMissingFields() {
        final ObjectNode input = JsonBodies.with(
            node -> {
                node.put("manualCorrection", false);
                node.put("barrierRemoved", false);
                node.set("floor", JsonBodies.json("{'number':1,'mezzanine':false}"));
            }
        );
        Assertions.assertAll(
            () -> Assertions.assertEquals(
                JsonBodies.json(JsonBodies.QUICK),
                JsonBodies.roundTrip(JsonBodies.with(node -> { })),
                "Pola puste zapisane jako null"
            ),
            () -> Assertions.assertEquals(
                JsonBodies.with(node -> node.set("floor", JsonBodies.json("{'number':1}"))),
                JsonBodies.roundTrip(input),
                "Fałszywa flaga wróciła jako pole"
            )
        );
    }

    @Test
    void rejectsExplicitNull() {
        Assertions.assertAll(
            Stream.of(
                "kind", "location", "accuracyMeters", "manualCorrection", "floor",
                "barrierGroup", "barrierType", "description", "replacesSubmissionId",
                "barrierRemoved", "entranceMarking", "photoIds"
            ).map(
                field -> () -> JsonBodies.failsBinding(
                    String.format("Null w polu %s przeszedł", field), node -> node.putNull(field)
                )
            )
        );
        JsonBodies.failsBinding(
            "Null w piętrze przeszedł",
            node -> node.set("floor", JsonBodies.json("{'mezzanine':null}"))
        );
        JsonBodies.failsBinding(
            "Null we współrzędnych przeszedł",
            node -> node.set(
                "location", JsonBodies.json("{'type':'Point','coordinates':[17.06,null]}")
            )
        );
    }

    @Test
    void rejectsEmptyTextInEnumFields() {
        Assertions.assertAll(
            Stream.of("kind", "barrierGroup", "entranceMarking").map(
                field -> () -> JsonBodies.failsBinding(
                    String.format("Pusty napis w polu %s przeszedł", field),
                    node -> node.put(field, "")
                )
            )
        );
    }

    @Test
    void rejectsNoDataMarking() {
        final Consumer<ObjectNode> marking = node -> {
            node.put("kind", "ENTRANCE_MARKING");
            node.put("entranceId", "node/7");
        };
        Assertions.assertAll(
            () -> Assertions.assertEquals(
                NewEntranceMarking.INACCESSIBLE,
                JsonBodies.bind(
                    JsonBodies.with(
                        marking.andThen(node -> node.put("entranceMarking", "INACCESSIBLE"))
                    )
                ).toContent().entranceMarking(),
                "Oznaczenie niedostępne nie przeszło"
            ),
            () -> JsonBodies.failsBinding(
                "Zmiana oznaczenia na brak danych przeszła",
                marking.andThen(node -> node.put("entranceMarking", "NO_DATA"))
            )
        );
    }

    @Test
    void givesInvalidInputForMissingRequiredFields() {
        Assertions.assertAll(
            Stream.of("kind", "location", "photoIds").map(
                field -> () -> Assertions.assertThrows(
                    InvalidInput.class,
                    () -> JsonBodies.bind(JsonBodies.with(node -> node.remove(field))).toContent(),
                    String.format("Brak pola %s nie dał błędu 400", field)
                )
            )
        );
        Assertions.assertThrows(
            InvalidInput.class,
            () -> JsonBodies.bind(JsonBodies.json("{}")).toContent(),
            "Pusta treść nie dała błędu 400"
        );
    }

    private static SubmissionContentJson bound(final Consumer<ObjectNode> change) {
        return JsonBodies.bind(JsonBodies.with(change));
    }

    private static ObjectNode replacing(final ObjectNode node) {
        return node.put("replacesSubmissionId", JsonBodies.REPLACED.toString());
    }
}
