/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.json;

import io.github.dostepnykampus.IntegrationTest;
import io.github.dostepnykampus.OpenApiContract;
import io.github.dostepnykampus.barrier.domain.BarrierGroup;
import io.github.dostepnykampus.barrier.domain.NewEntranceMarking;
import io.github.dostepnykampus.barrier.domain.SubmissionKind;
import io.github.dostepnykampus.barrier.domain.SubmissionStatus;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import org.springframework.context.ApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Wartości enumów serializowanych przez serwer odpowiadają schematom OpenAPI.
 *
 * @since 0.1.0
 */
@IntegrationTest
final class ApiEnumsTest {

    @Test
    void usEightStatusesMatchSpec(final ApplicationContext context) {
        ApiEnumsTest.assertMatches(
            new OpenApiContract(), context, "SubmissionStatus",
            Stream.of(SubmissionStatus.values())
        );
    }

    @Test
    void brSevenKindsMatchSpec(final ApplicationContext context) {
        ApiEnumsTest.assertMatches(
            new OpenApiContract(), context, "SubmissionContent/properties/kind",
            Stream.of(SubmissionKind.values())
        );
    }

    @Test
    void usFourGroupsMatchSpec(final ApplicationContext context) {
        ApiEnumsTest.assertMatches(
            new OpenApiContract(), context, "SubmissionContent/properties/barrierGroup",
            Stream.of(BarrierGroup.values()).map(GroupJson::of)
        );
    }

    @Test
    void brTwentyFourMarkingsMatchSpec(final ApplicationContext context) {
        ApiEnumsTest.assertMatches(
            new OpenApiContract(), context, "SubmissionContent/properties/entranceMarking",
            Stream.of(NewEntranceMarking.values())
        );
    }

    @Test
    void rejectsExtraStatusInContract(final ApplicationContext context) {
        final ObjectNode changed = context.getBean(ObjectMapper.class).createObjectNode();
        changed.put("openapi", "3.1.0");
        changed.putObject("components").putObject("schemas").putObject("SubmissionStatus")
            .putArray("enum").addAll(new OpenApiContract().enumValues("SubmissionStatus"))
            .add("TEST_ONLY");
        Assertions.assertThrows(
            AssertionFailedError.class,
            () -> ApiEnumsTest.assertMatches(
                new OpenApiContract(changed::toString), context, "SubmissionStatus",
                Stream.of(SubmissionStatus.values())
            ),
            "Dodatkowa wartość w kontrakcie musi złamać zgodność enumu"
        );
    }

    private static void assertMatches(
        final OpenApiContract contract, final ApplicationContext context,
        final String pointer, final Stream<? extends Enum<?>> values
    ) {
        final ObjectMapper mapper = context.getBean(ObjectMapper.class);
        Assertions.assertEquals(
            contract.enumValues(pointer),
            values.map(value -> mapper.<JsonNode>valueToTree(value)).toList(),
            "Wartości JSON różnią się od OpenAPI dla ".concat(pointer)
        );
    }
}
