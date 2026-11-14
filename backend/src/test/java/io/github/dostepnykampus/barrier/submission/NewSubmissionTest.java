/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Komenda utworzenia zgłoszenia: komplet pól.
 *
 * <p>Wymagania: BR-9</p>
 *
 * @since 0.1.0
 */
final class NewSubmissionTest {

    @Test
    void mthreeTwoKeepsIdAndContent() {
        final NewSubmission command = new NewSubmission(
            SubmissionSamples.ID, SubmissionSamples.content(), "konto"
        );
        Assertions.assertAll(
            () -> Assertions.assertEquals(
                SubmissionSamples.ID, command.id(), "Zły identyfikator"
            ),
            () -> Assertions.assertEquals(
                SubmissionSamples.content(), command.content(), "Zła treść"
            ),
            () -> Assertions.assertEquals("konto", command.author(), "Zły autor")
        );
    }

    @Test
    void mthreeTwoRejectsMissingIdOrContent() {
        Assertions.assertAll(
            () -> Assertions.assertThrows(
                NullPointerException.class,
                () -> new NewSubmission(null, SubmissionSamples.content(), "konto"),
                "Komenda bez identyfikatora przeszła"
            ),
            () -> Assertions.assertThrows(
                NullPointerException.class,
                () -> new NewSubmission(SubmissionSamples.ID, null, "konto"),
                "Komenda bez treści przeszła"
            ),
            () -> Assertions.assertThrows(
                NullPointerException.class,
                () -> new NewSubmission(SubmissionSamples.ID, SubmissionSamples.content(), null),
                "Komenda bez autora przeszła"
            )
        );
    }
}
