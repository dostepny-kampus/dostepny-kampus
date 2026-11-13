/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.domain;

import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Zgłoszenie z serwera (US8, V3): wersja co najmniej 1 i komplet pól.
 *
 * @since 0.1.0
 */
final class SubmissionTest {

    @Test
    void brEighteenRecognizesAcceptedSubmission() {
        Assertions.assertTrue(
            SubmissionSamples.submission(1, SubmissionStatus.ACCEPTED).accepted(),
            "Przyjęte zgłoszenie nie zostało rozpoznane"
        );
    }

    @ParameterizedTest
    @EnumSource(value = SubmissionStatus.class, names = "ACCEPTED", mode = EnumSource.Mode.EXCLUDE)
    void brEighteenDoesNotTreatOtherStatusesAsAccepted(final SubmissionStatus status) {
        Assertions.assertFalse(
            SubmissionSamples.submission(1, status).accepted(),
            String.format("Zgłoszenie w statusie %s uznano za przyjęte", status)
        );
    }

    @Test
    void mthreeOneRejectsVersionBelowOneAsServerData() {
        Assertions.assertAll(
            Stream.of(0, -1, Integer.MIN_VALUE).map(
                version -> () -> Assertions.assertEquals(
                    IllegalArgumentException.class,
                    Assertions.assertThrows(
                        RuntimeException.class,
                        () -> SubmissionSamples.submission(version, SubmissionStatus.SENT),
                        String.format("Wersja %d została przyjęta", version)
                    ).getClass(),
                    "Zły typ wyjątku dla wersji poniżej 1"
                )
            )
        );
    }

    @Test
    void mthreeOneKeepsComponentsOfVersionOne() {
        final Submission submission = SubmissionSamples.submission(
            1, SubmissionStatus.ACCEPTED
        );
        Assertions.assertAll(
            () -> Assertions.assertEquals(1, submission.version(), "Zła wersja"),
            () -> Assertions.assertEquals(
                SubmissionStatus.ACCEPTED, submission.status(), "Zły status"
            ),
            () -> Assertions.assertEquals(
                SubmissionSamples.content(), submission.content(), "Zła treść"
            ),
            () -> Assertions.assertEquals(
                Integer.MAX_VALUE,
                SubmissionSamples.submission(
                    Integer.MAX_VALUE, SubmissionStatus.SENT
                ).version(),
                "Największa wersja została odrzucona"
            )
        );
    }

    @Test
    void mthreeOneRejectsMissingReferences() {
        final SubmissionContent content = SubmissionSamples.content();
        Assertions.assertAll(
            () -> Assertions.assertThrows(
                NullPointerException.class,
                () -> new Submission(
                    null, 1, SubmissionStatus.SENT, SubmissionSamples.CREATED, content
                ),
                "Zgłoszenie bez identyfikatora przeszło"
            ),
            () -> Assertions.assertThrows(
                NullPointerException.class,
                () -> new Submission(
                    SubmissionSamples.ID, 1, null, SubmissionSamples.CREATED, content
                ),
                "Zgłoszenie bez statusu przeszło"
            ),
            () -> Assertions.assertThrows(
                NullPointerException.class,
                () -> new Submission(
                    SubmissionSamples.ID, 1, SubmissionStatus.SENT, null, content
                ),
                "Zgłoszenie bez chwili utworzenia przeszło"
            ),
            () -> Assertions.assertThrows(
                NullPointerException.class,
                () -> new Submission(
                    SubmissionSamples.ID, 1, SubmissionStatus.SENT,
                    SubmissionSamples.CREATED, null
                ),
                "Zgłoszenie bez treści przeszło"
            )
        );
    }
}
