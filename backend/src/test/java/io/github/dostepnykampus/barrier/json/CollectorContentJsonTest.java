/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.json;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.node.ObjectNode;

/**
 * Treść zgłoszenia w JSON rozstrzyga, czy zgłoszenie składa tylko Zbierający
 * i które zgłoszenie zastępuje zwykła zmiana.
 *
 * <p>Wymagania: US7, US17.2, US8.4, BR-18, BR-19</p>
 *
 * @since 0.1.0
 */
final class CollectorContentJsonTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("roles")
    void tellsWhoSubmits(final String name, final Consumer<ObjectNode> change, final boolean collector) {
        Assertions.assertEquals(
            collector, JsonBodies.bind(JsonBodies.with(change)).forCollector(),
            String.format("Zła rola dla: %s", name)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("changes")
    void findsReplacedSubmission(
        final String name, final Consumer<ObjectNode> change, final Optional<UUID> replaced
    ) {
        Assertions.assertEquals(
            replaced, JsonBodies.bind(JsonBodies.with(change)).ordinaryChange(),
            String.format("Złe zastępowane zgłoszenie dla: %s", name)
        );
    }

    private static Stream<Arguments> roles() {
        return Stream.of(
            Arguments.of("szybkie", (Consumer<ObjectNode>) CollectorContentJsonTest::same, false),
            Arguments.of("bez rodzaju", (Consumer<ObjectNode>) node -> node.remove("kind"), false),
            Arguments.of("precyzyjne", CollectorContentJsonTest.kind("PRECISE"), true),
            Arguments.of("oznaczenie wejścia", CollectorContentJsonTest.kind("ENTRANCE_MARKING"), true),
            Arguments.of("szybkie zastępujące", (Consumer<ObjectNode>) CollectorContentJsonTest::replacing, true)
        );
    }

    private static Stream<Arguments> changes() {
        return Stream.of(
            Arguments.of("bez pola replaces", (Consumer<ObjectNode>) CollectorContentJsonTest::same, Optional.empty()),
            Arguments.of(
                "zwykła zmiana", (Consumer<ObjectNode>) CollectorContentJsonTest::replacing, Optional.of(JsonBodies.REPLACED)
            ),
            Arguments.of(
                "zmiana bez usunięcia",
                (Consumer<ObjectNode>) node -> CollectorContentJsonTest.replacing(node).put("barrierRemoved", false),
                Optional.of(JsonBodies.REPLACED)
            ),
            Arguments.of(
                "bariera usunięta",
                (Consumer<ObjectNode>) node -> CollectorContentJsonTest.replacing(node).put("barrierRemoved", true),
                Optional.empty()
            )
        );
    }

    private static void same(final ObjectNode node) {
        node.put("kind", "QUICK");
    }

    private static Consumer<ObjectNode> kind(final String kind) {
        return node -> node.put("kind", kind);
    }

    private static ObjectNode replacing(final ObjectNode node) {
        return node.put("replacesSubmissionId", JsonBodies.REPLACED.toString());
    }
}
