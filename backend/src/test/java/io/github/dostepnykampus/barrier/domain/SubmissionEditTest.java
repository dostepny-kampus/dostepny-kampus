/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.domain;

import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Komenda edycji zgłoszenia (BR-10): wersja bazowa od klienta.
 *
 * @since 0.1.0
 */
final class SubmissionEditTest {

    @Test
    void mThreeThreeRejectsExpectedVersionBelowOne() {
        Assertions.assertAll(
            Stream.of(0, -1, Integer.MIN_VALUE).map(
                version -> () -> Assertions.assertEquals(
                    "Brak wersji bazowej edycji w polu version",
                    Assertions.assertThrows(
                        InvalidSubmission.class,
                        () -> new SubmissionEdit(
                            SubmissionSamples.ID, version, SubmissionSamples.content()
                        ),
                        String.format("Wersja bazowa %d została przyjęta", version)
                    ).getMessage(),
                    "Zły opis błędu wersji bazowej"
                )
            )
        );
    }

    @Test
    void mThreeThreeKeepsComponentsOfVersionOne() {
        final SubmissionEdit edit = new SubmissionEdit(
            SubmissionSamples.ID, 1, SubmissionSamples.content()
        );
        Assertions.assertAll(
            () -> Assertions.assertEquals(
                SubmissionSamples.ID, edit.id(), "Zły identyfikator"
            ),
            () -> Assertions.assertEquals(1, edit.expectedVersion(), "Zła wersja"),
            () -> Assertions.assertEquals(
                SubmissionSamples.content(), edit.content(), "Zła treść"
            )
        );
    }

    @Test
    void mThreeThreeRejectsMissingIdOrContent() {
        Assertions.assertAll(
            () -> Assertions.assertThrows(
                NullPointerException.class,
                () -> new SubmissionEdit(null, 1, SubmissionSamples.content()),
                "Edycja bez identyfikatora przeszła"
            ),
            () -> Assertions.assertThrows(
                NullPointerException.class,
                () -> new SubmissionEdit(SubmissionSamples.ID, 1, null),
                "Edycja bez treści przeszła"
            )
        );
    }
}
