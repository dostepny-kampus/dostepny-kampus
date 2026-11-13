/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.domain;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Reguły statusu zgłoszenia po wszystkich wartościach enumu: edycja
 * przed decyzją i po odrzuceniu, status po edycji
 * i widoczność publiczna.
 *
 * <p>Wymagania: BR-3, BR-18, BR-6</p>
 *
 * @since 0.1.0
 */
final class SubmissionStatusTest {

    /**
     * Status po edycji autora dla każdego statusu, który wolno edytować.
     */
    private static final Map<SubmissionStatus, SubmissionStatus> AFTER_EDIT = Map.of(
        SubmissionStatus.SENT, SubmissionStatus.SENT,
        SubmissionStatus.IN_REVIEW, SubmissionStatus.IN_REVIEW,
        SubmissionStatus.REJECTED, SubmissionStatus.SENT
    );

    @Test
    void brThreeEditsOnlyBeforeDecisionOrAfterRejection() {
        Assertions.assertAll(
            Stream.of(SubmissionStatus.values()).map(
                status -> () -> Assertions.assertEquals(
                    SubmissionStatusTest.AFTER_EDIT.containsKey(status),
                    status.editable(),
                    String.format("Edytowalność statusu %s niezgodna z BR-3", status)
                )
            )
        );
    }

    @Test
    void brThreeSendsRejectedAgainAfterEdit() {
        Assertions.assertAll(
            SubmissionStatusTest.AFTER_EDIT.entrySet().stream().map(
                entry -> () -> Assertions.assertEquals(
                    entry.getValue(),
                    entry.getKey().afterEdit(),
                    String.format("Zły status po edycji zgłoszenia %s", entry.getKey())
                )
            )
        );
    }

    @Test
    void brEighteenRefusesEditAfterAcceptance() {
        Assertions.assertAll(
            EnumSet.complementOf(
                EnumSet.copyOf(SubmissionStatusTest.AFTER_EDIT.keySet())
            ).stream().map(
                status -> () -> Assertions.assertThrows(
                    IllegalStateException.class,
                    status::afterEdit,
                    String.format("Edycja zgłoszenia %s nie została odrzucona", status)
                )
            )
        );
    }

    @Test
    void brSixShowsOnlyAcceptedPublicly() {
        final Set<SubmissionStatus> visible = EnumSet.of(SubmissionStatus.ACCEPTED);
        Assertions.assertAll(
            Stream.of(SubmissionStatus.values()).map(
                status -> () -> Assertions.assertEquals(
                    visible.contains(status),
                    status.publiclyVisible(),
                    String.format("Widoczność publiczna statusu %s niezgodna z BR-6", status)
                )
            )
        );
    }
}
