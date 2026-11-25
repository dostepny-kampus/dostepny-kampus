/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission.persistence;

import io.github.dostepnykampus.IntegrationTest;
import io.github.dostepnykampus.barrier.submission.CreateResult;
import io.github.dostepnykampus.barrier.submission.EditResult;
import io.github.dostepnykampus.barrier.submission.NewSubmission;
import io.github.dostepnykampus.barrier.submission.Submission;
import io.github.dostepnykampus.barrier.submission.SubmissionEdit;
import io.github.dostepnykampus.barrier.submission.SubmissionStatus;
import io.github.dostepnykampus.barrier.submission.content.SubmissionContent;
import io.github.dostepnykampus.shared.error.ForbiddenChange;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

/**
 * Zapis zgłoszeń na prawdziwej bazie, bez transakcji testowej: idempotentne
 * utworzenie oraz edycja z wersją i statusem.
 *
 * <p>Wymagania: BR-3, BR-9, BR-10, BR-18</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
final class JdbcWriteIT {

    @AfterEach
    void clean(final ApplicationContext context) {
        SubmissionData.of(context).clean();
    }

    @Test
    void createsOnceAndKeepsFirstContent(final ApplicationContext context) {
        final SubmissionData data = SubmissionData.of(context);
        final UUID id = SubmissionData.id(1);
        final CreateResult created = data.repo().create(
            new NewSubmission(
                id, SubmissionData.content("pierwsza"), SubmissionData.AUTHOR, Set.of()
            )
        );
        final CreateResult repeated = data.repo().create(
            new NewSubmission(id, SubmissionData.content("druga"), SubmissionData.AUTHOR, Set.of())
        );
        Assertions.assertThrows(
            ForbiddenChange.class,
            () -> data.repo().create(
                new NewSubmission(id, SubmissionData.content("trzecia"), "inne-konto", Set.of())
            ),
            "Inne konto dostało zapisane zgłoszenie"
        );
        final Submission expected = new Submission(
            id, 1, SubmissionStatus.SENT, data.created(id), SubmissionData.content("pierwsza")
        );
        Assertions.assertEquals(
            List.of(
                new CreateResult.Created(expected), new CreateResult.Existing(expected),
                SubmissionData.AUTHOR
            ),
            List.of(created, repeated, data.author(id)),
            "Ponowne wysłanie zmieniło zgłoszenie albo autora"
        );
    }

    @Test
    void editsOnlyEditableStatuses(final ApplicationContext context) {
        final SubmissionData data = SubmissionData.of(context);
        final int size = SubmissionStatus.values().length;
        final List<EditResult> actual = new ArrayList<>(size);
        final List<EditResult> expected = new ArrayList<>(size);
        final SubmissionContent edited = SubmissionData.content("n");
        for (final SubmissionStatus status : SubmissionStatus.values()) {
            final UUID id = data.seed(status);
            final Instant early = data.created(id);
            final Submission before = data.repo().find(id).orElseThrow();
            actual.add(data.repo().update(new SubmissionEdit(id, 1, edited)));
            if (status.editable()) {
                expected.add(
                    new EditResult.Saved(new Submission(id, 2, status.afterEdit(), early, edited))
                );
            } else {
                expected.add(new EditResult.Conflict(before));
            }
        }
        Assertions.assertEquals(expected, actual, "Wynik edycji nie zgadza się z BR-18");
    }
}
