/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.domain;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Komenda utworzenia zgłoszenia (BR-9): komplet pól.
 *
 * @since 0.1.0
 */
final class NewSubmissionTest {

    @Test
    void mthreeTwoKeepsIdAndContent() {
        final NewSubmission command = new NewSubmission(
            SubmissionSamples.ID, SubmissionSamples.content()
        );
        Assertions.assertAll(
            () -> Assertions.assertEquals(
                SubmissionSamples.ID, command.id(), "Zły identyfikator"
            ),
            () -> Assertions.assertEquals(
                SubmissionSamples.content(), command.content(), "Zła treść"
            )
        );
    }

    @Test
    void mthreeTwoRejectsMissingIdOrContent() {
        Assertions.assertAll(
            () -> Assertions.assertThrows(
                NullPointerException.class,
                () -> new NewSubmission(null, SubmissionSamples.content()),
                "Komenda bez identyfikatora przeszła"
            ),
            () -> Assertions.assertThrows(
                NullPointerException.class,
                () -> new NewSubmission(SubmissionSamples.ID, null),
                "Komenda bez treści przeszła"
            )
        );
    }
}
