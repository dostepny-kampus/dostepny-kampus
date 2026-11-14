/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission.content.json;

import io.github.dostepnykampus.IntegrationTest;
import io.github.dostepnykampus.OpenApiContract;
import io.github.dostepnykampus.barrier.submission.SubmissionStatus;
import io.github.dostepnykampus.barrier.submission.content.BarrierGroup;
import io.github.dostepnykampus.barrier.submission.content.NewEntranceMarking;
import io.github.dostepnykampus.barrier.submission.content.SubmissionKind;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Wartości enumów serializowanych przez serwer odpowiadają schematom OpenAPI.
 *
 * <p>Wymagania: BR-7, BR-24</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
final class ApiEnumsTest {

    @Test
    void usEightStatusesMatchSpec(final ApplicationContext context) {
        Assertions.assertEquals(
            new OpenApiContract().enumValues("SubmissionStatus"),
            ApiEnumsTest.json(context, Stream.of(SubmissionStatus.values())),
            "Wartości JSON różnią się od OpenAPI dla ".concat("SubmissionStatus")
        );
    }

    @Test
    void kindsMatchSpec(final ApplicationContext context) {
        Assertions.assertEquals(
            new OpenApiContract().enumValues("SubmissionContent/properties/kind"),
            ApiEnumsTest.json(context, Stream.of(SubmissionKind.values())),
            "Wartości JSON różnią się od OpenAPI dla ".concat("SubmissionContent/properties/kind")
        );
    }

    @Test
    void usFourGroupsMatchSpec(final ApplicationContext context) {
        Assertions.assertEquals(
            new OpenApiContract().enumValues("SubmissionContent/properties/barrierGroup"),
            ApiEnumsTest.json(context, Stream.of(BarrierGroup.values()).map(GroupJson::of)),
            "Wartości JSON różnią się od OpenAPI dla ".concat("SubmissionContent/properties/barrierGroup")
        );
    }

    @Test
    void markingsMatchSpec(final ApplicationContext context) {
        Assertions.assertEquals(
            new OpenApiContract().enumValues("SubmissionContent/properties/entranceMarking"),
            ApiEnumsTest.json(context, Stream.of(NewEntranceMarking.values())),
            "Wartości JSON różnią się od OpenAPI dla ".concat("SubmissionContent/properties/entranceMarking")
        );
    }

    @Test
    void rejectsExtraStatusInContract(final ApplicationContext context) {
        final ObjectNode changed = context.getBean(ObjectMapper.class).createObjectNode();
        changed.put("openapi", "3.1.0");
        changed.putObject("components").putObject("schemas").putObject("SubmissionStatus")
            .putArray("enum").addAll(new OpenApiContract().enumValues("SubmissionStatus"))
            .add("TEST_ONLY");
        Assertions.assertNotEquals(
            new OpenApiContract(changed::toString).enumValues("SubmissionStatus"),
            ApiEnumsTest.json(context, Stream.of(SubmissionStatus.values())),
            "Dodatkowa wartość w kontrakcie musi złamać zgodność enumu"
        );
    }

    private static List<JsonNode> json(
        final ApplicationContext context, final Stream<? extends Enum<?>> values
    ) {
        final ObjectMapper mapper = context.getBean(ObjectMapper.class);
        return values.map(value -> mapper.<JsonNode>valueToTree(value)).toList();
    }
}
