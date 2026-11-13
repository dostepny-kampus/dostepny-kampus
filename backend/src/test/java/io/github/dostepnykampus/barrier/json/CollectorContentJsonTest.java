/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.json;

import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
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

    @Test
    void quickIsForEveryone() {
        Assertions.assertFalse(CollectorContentJsonTest.bound(node -> { }).forCollector());
    }

    @Test
    void kindlessIsForEveryone() {
        Assertions.assertFalse(
            CollectorContentJsonTest.bound(node -> node.remove("kind")).forCollector()
        );
    }

    @Test
    void preciseIsForCollector() {
        Assertions.assertTrue(
            CollectorContentJsonTest.bound(node -> node.put("kind", "PRECISE")).forCollector()
        );
    }

    @Test
    void entranceMarkingIsForCollector() {
        Assertions.assertTrue(
            CollectorContentJsonTest.bound(node -> node.put("kind", "ENTRANCE_MARKING"))
                .forCollector()
        );
    }

    @Test
    void quickReplacingIsForCollector() {
        Assertions.assertTrue(
            CollectorContentJsonTest.bound(CollectorContentJsonTest::replacing).forCollector()
        );
    }

    @Test
    void newSubmissionReplacesNothing() {
        Assertions.assertEquals(
            Optional.empty(), CollectorContentJsonTest.bound(node -> { }).ordinaryChange()
        );
    }

    @Test
    void changeReplacesSubmission() {
        Assertions.assertEquals(
            Optional.of(JsonBodies.REPLACED),
            CollectorContentJsonTest.bound(CollectorContentJsonTest::replacing).ordinaryChange()
        );
    }

    @Test
    void keptBarrierReplacesSubmission() {
        Assertions.assertEquals(
            Optional.of(JsonBodies.REPLACED),
            CollectorContentJsonTest.bound(
                node -> CollectorContentJsonTest.replacing(node).put("barrierRemoved", false)
            ).ordinaryChange()
        );
    }

    @Test
    void removedBarrierIsNotOrdinaryChange() {
        Assertions.assertEquals(
            Optional.empty(),
            CollectorContentJsonTest.bound(
                node -> CollectorContentJsonTest.replacing(node).put("barrierRemoved", true)
            ).ordinaryChange()
        );
    }

    private static SubmissionContentJson bound(final Consumer<ObjectNode> change) {
        return JsonBodies.bind(JsonBodies.with(change));
    }

    private static ObjectNode replacing(final ObjectNode node) {
        return node.put("replacesSubmissionId", JsonBodies.REPLACED.toString());
    }
}
