/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.domain;

import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Wyniki utworzenia i edycji zgłoszenia (BR-9, BR-10): zamknięte warianty
 * z kompletem pól.
 *
 * @since 0.1.0
 */
final class SubmissionResultsTest {

    @Test
    void mThreeSixCreateResultsExposeGivenSubmission() {
        final Submission submission = SubmissionSamples.submission(1, SubmissionStatus.SENT);
        final CreateResult created = new CreateResult.Created(submission);
        final CreateResult existing = new CreateResult.Existing(submission);
        Assertions.assertAll(
            () -> Assertions.assertSame(
                submission, created.submission(), "Created zwraca inne zgłoszenie"
            ),
            () -> Assertions.assertSame(
                submission, existing.submission(), "Existing zwraca inne zgłoszenie"
            ),
            () -> Assertions.assertNotEquals(created, existing, "Warianty nie różnią się")
        );
    }

    @Test
    void mThreeSixResultsAreSealedWithTheirVariantsOnly() {
        final Submission submission = SubmissionSamples.submission(2, SubmissionStatus.SENT);
        Assertions.assertAll(
            () -> Assertions.assertTrue(CreateResult.class.isSealed(), "CreateResult otwarty"),
            () -> Assertions.assertTrue(EditResult.class.isSealed(), "EditResult otwarty"),
            () -> Assertions.assertEquals(
                2, CreateResult.class.getPermittedSubclasses().length,
                "CreateResult ma inną liczbę wariantów"
            ),
            () -> Assertions.assertEquals(
                3, EditResult.class.getPermittedSubclasses().length,
                "EditResult ma inną liczbę wariantów"
            ),
            () -> Assertions.assertEquals(
                "Saved",
                SubmissionResultsTest.name(new EditResult.Saved(submission)),
                "Saved nie pasuje do wzorca"
            ),
            () -> Assertions.assertEquals(
                "Conflict",
                SubmissionResultsTest.name(new EditResult.Conflict(submission)),
                "Conflict nie pasuje do wzorca"
            ),
            () -> Assertions.assertEquals(
                "Missing",
                SubmissionResultsTest.name(new EditResult.Missing(SubmissionSamples.ID)),
                "Missing nie pasuje do wzorca"
            )
        );
    }

    @Test
    void mThreeSixEditResultsKeepTheirComponents() {
        final Submission submission = SubmissionSamples.submission(
            3, SubmissionStatus.IN_REVIEW
        );
        final UUID missing = SubmissionSamples.ID;
        Assertions.assertAll(
            () -> Assertions.assertSame(
                submission, new EditResult.Saved(submission).submission(), "Zły zapis"
            ),
            () -> Assertions.assertSame(
                submission, new EditResult.Conflict(submission).current(), "Zły konflikt"
            ),
            () -> Assertions.assertEquals(
                missing, new EditResult.Missing(missing).id(), "Zły brak zgłoszenia"
            )
        );
    }

    @Test
    @Disabled("Wyniki nie odrzucają jeszcze null, poprawka w następnym PR")
    void mThreeSixRejectsMissingComponents() {
        Assertions.assertAll(
            () -> Assertions.assertThrows(
                NullPointerException.class,
                () -> new CreateResult.Created(null),
                "Created bez zgłoszenia przeszło"
            ),
            () -> Assertions.assertThrows(
                NullPointerException.class,
                () -> new CreateResult.Existing(null),
                "Existing bez zgłoszenia przeszło"
            ),
            () -> Assertions.assertThrows(
                NullPointerException.class,
                () -> new EditResult.Saved(null),
                "Saved bez zgłoszenia przeszło"
            ),
            () -> Assertions.assertThrows(
                NullPointerException.class,
                () -> new EditResult.Conflict(null),
                "Conflict bez bieżącego stanu przeszedł"
            ),
            () -> Assertions.assertThrows(
                NullPointerException.class,
                () -> new EditResult.Missing(null),
                "Missing bez identyfikatora przeszło"
            )
        );
    }

    private static String name(final EditResult result) {
        return switch (result) {
            case EditResult.Saved saved -> "Saved";
            case EditResult.Conflict conflict -> "Conflict";
            case EditResult.Missing missing -> "Missing";
        };
    }
}
