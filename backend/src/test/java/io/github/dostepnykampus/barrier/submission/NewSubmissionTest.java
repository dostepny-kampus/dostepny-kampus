/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission;

import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Komenda utworzenia zgłoszenia: komplet pól i role autora.
 *
 * <p>Wymagania: BR-9, BR-5</p>
 *
 * @since 0.1.0
 */
final class NewSubmissionTest {

    @Test
    void keepsIdContentAndAuthor() {
        final NewSubmission command = new NewSubmission(
            SubmissionSamples.ID, SubmissionSamples.content(), "konto",
            Set.of(AuthorRole.VISITOR, AuthorRole.COLLECTOR)
        );
        Assertions.assertAll(
            () -> Assertions.assertEquals(
                SubmissionSamples.ID, command.id(), "Zły identyfikator"
            ),
            () -> Assertions.assertEquals(
                SubmissionSamples.content(), command.content(), "Zła treść"
            ),
            () -> Assertions.assertEquals("konto", command.author(), "Zły autor"),
            () -> Assertions.assertEquals(
                Set.of(AuthorRole.VISITOR, AuthorRole.COLLECTOR), command.roles(),
                "Złe role autora"
            )
        );
    }

    @Test
    void copiesRoles() {
        final Set<AuthorRole> roles = EnumSet.of(AuthorRole.VISITOR);
        final NewSubmission command = new NewSubmission(
            SubmissionSamples.ID, SubmissionSamples.content(), "konto", roles
        );
        roles.add(AuthorRole.COORDINATOR);
        Assertions.assertAll(
            () -> Assertions.assertEquals(
                Set.of(AuthorRole.VISITOR), command.roles(),
                "Komenda zmieniła role po zmianie zbioru wejściowego"
            ),
            () -> Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> command.roles().add(AuthorRole.VERIFIER),
                "Role komendy dają się zmienić"
            )
        );
    }

    @Test
    void rejectsMissingField() {
        Assertions.assertAll(
            () -> Assertions.assertThrows(
                NullPointerException.class,
                () -> new NewSubmission(null, SubmissionSamples.content(), "konto", Set.of()),
                "Komenda bez identyfikatora przeszła"
            ),
            () -> Assertions.assertThrows(
                NullPointerException.class,
                () -> new NewSubmission(SubmissionSamples.ID, null, "konto", Set.of()),
                "Komenda bez treści przeszła"
            ),
            () -> Assertions.assertThrows(
                NullPointerException.class,
                () -> new NewSubmission(
                    SubmissionSamples.ID, SubmissionSamples.content(), null, Set.of()
                ),
                "Komenda bez autora przeszła"
            ),
            () -> Assertions.assertThrows(
                NullPointerException.class,
                () -> new NewSubmission(
                    SubmissionSamples.ID, SubmissionSamples.content(), "konto", null
                ),
                "Komenda bez ról autora przeszła"
            )
        );
    }
}
