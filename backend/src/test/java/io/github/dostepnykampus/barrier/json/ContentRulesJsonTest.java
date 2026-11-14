/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.json;

import io.github.dostepnykampus.barrier.submission.content.NewEntranceMarking;
import io.github.dostepnykampus.shared.error.InvalidInput;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ObjectNode;

/**
 * Pomijanie pól i ścisłość treści zgłoszenia w JSON (W3-5, W3-6, W3-7):
 * flagi fałszywe i brak pól nie trafiają do wyjścia, jawny null, pusty napis
 * w polu enum i NO_DATA kończą wiązanie błędem, a brak pól wymaganych daje
 * błąd 400, nie NPE.
 *
 * @since 0.1.0
 */
final class ContentRulesJsonTest {

    @Test
    void wthreeFiveOmitsFalseFlagsAndMissingFields() {
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
    void wthreeSixRejectsExplicitNull() {
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
    void wthreeSixRejectsEmptyTextInEnumFields() {
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
    void brTwentyFourRejectsNoDataMarking() {
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
    void wthreeSevenMissingRequiredFieldsGiveInvalidInput() {
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
}
