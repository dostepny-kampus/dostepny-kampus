/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.json;

import io.github.dostepnykampus.OpenApiContract;
import io.github.dostepnykampus.barrier.domain.BarrierGroup;
import io.github.dostepnykampus.barrier.domain.BarrierType;
import io.github.dostepnykampus.barrier.domain.InvalidSubmission;
import io.github.dostepnykampus.barrier.domain.SubmissionPoint;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ObjectNode;

/**
 * Pola treści zgłoszenia z własnym mapowaniem (W3-2, W3-3, W3-4): grupa barier
 * i typ bariery mają jawne pary z wartościami spec, a punkt to długość
 * i szerokość.
 *
 * @since 0.1.0
 */
@Disabled("mapowanie pól w JSON jeszcze nie istnieje, kod w następnym PR")
final class ContentFieldsJsonTest {

    /**
     * Grupy barier: stała domeny i wartość ze schematu BarrierGroup
     * w api/openapi.yaml.
     */
    private static final Map<BarrierGroup, String> GROUPS = Map.of(
        BarrierGroup.ENTRANCE, "ENTRANCE_AND_DOORS",
        BarrierGroup.VERTICAL, "VERTICAL_CIRCULATION",
        BarrierGroup.HORIZONTAL, "HORIZONTAL_CIRCULATION",
        BarrierGroup.TOILETS, "TOILETS",
        BarrierGroup.ORIENTATION, "ORIENTATION_AND_INFORMATION",
        BarrierGroup.OTHER, "OTHER"
    );

    @Test
    void wThreeTwoEveryGroupHasPairBothWays() {
        Assertions.assertEquals(
            EnumSet.allOf(BarrierGroup.class), ContentFieldsJsonTest.GROUPS.keySet(),
            "Tabela grup nie ma pary dla każdej stałej domeny"
        );
        Assertions.assertAll(
            ContentFieldsJsonTest.GROUPS.entrySet().stream().map(
                pair -> () -> {
                    final ObjectNode body = JsonBodies.with(
                        node -> node.put("barrierGroup", pair.getValue())
                    );
                    Assertions.assertEquals(
                        pair.getKey(), JsonBodies.bind(body).toContent().barrier().group(),
                        String.format("Zła stała dla %s", pair.getValue())
                    );
                    Assertions.assertEquals(
                        body, JsonBodies.roundTrip(body),
                        String.format("Zły zapis grupy %s", pair.getKey())
                    );
                    Assertions.assertEquals(
                        List.of(),
                        new OpenApiContract().violations("SubmissionContent", body.toString()),
                        String.format("Spec odrzuca wartość %s", pair.getValue())
                    );
                }
            )
        );
    }

    @Test
    void wThreeTwoUnknownGroupFailsBinding() {
        Assertions.assertAll(
            Stream.of("ENTRANCE", "NOPE", "vertical_circulation").map(
                group -> () -> JsonBodies.failsBinding(
                    "Nieznana grupa przeszła", node -> node.put("barrierGroup", group)
                )
            )
        );
    }

    @Test
    void wThreeThreeCodeMapsBothWays() {
        Assertions.assertAll(
            Stream.of(1, 17, 29).map(
                number -> () -> {
                    final ObjectNode body = JsonBodies.with(
                        node -> node.put("barrierType", String.format("TB-%d", number))
                    );
                    Assertions.assertEquals(
                        new BarrierType(number), JsonBodies.bind(body).toContent().barrier().type(),
                        String.format("Zły typ dla TB-%d", number)
                    );
                    Assertions.assertEquals(
                        body, JsonBodies.roundTrip(body),
                        String.format("Zły zapis typu TB-%d", number)
                    );
                }
            )
        );
    }

    @Test
    void wThreeThreeCodeOutsideRangeIsInvalidSubmission() {
        Assertions.assertAll(
            Stream.of("TB-30", "TB-0", "TB-01", "3").map(
                code -> () -> Assertions.assertThrows(
                    InvalidSubmission.class,
                    () -> JsonBodies.bind(
                        JsonBodies.with(node -> node.put("barrierType", code))
                    ).toContent(),
                    String.format("Typ %s przeszedł", code)
                )
            )
        );
    }

    @Test
    void wThreeFourPointIsLongitudeThenLatitude() {
        final ObjectNode body = JsonBodies.with(
            node -> node.set(
                "location", JsonBodies.json("{'type':'Point','coordinates':[21.0,52.2]}")
            )
        );
        Assertions.assertAll(
            () -> Assertions.assertEquals(
                new SubmissionPoint(21.0, 52.2),
                JsonBodies.bind(body).toContent().place().point(),
                "Współrzędne zamienione albo błędny punkt"
            ),
            () -> Assertions.assertEquals(
                body, JsonBodies.roundTrip(body), "Punkt zapisany inaczej niż GeoJSON"
            )
        );
    }

    @Test
    void wThreeFourRejectsOtherTypeAndWrongCoordinateCount() {
        Assertions.assertAll(
            Stream.of(
                "{'type':'LineString','coordinates':[1.0,2.0]}",
                "{'type':'point','coordinates':[1.0,2.0]}",
                "{'type':'Point','coordinates':[1.0]}",
                "{'type':'Point','coordinates':[1.0,2.0,3.0]}",
                "{'type':'Point','coordinates':[]}"
            ).map(
                point -> () -> Assertions.assertTrue(
                    JsonBodies.rejected(
                        JsonBodies.with(node -> node.set("location", JsonBodies.json(point)))
                    ),
                    String.format("Punkt %s przeszedł", point)
                )
            )
        );
    }
}
