/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.persistence;

import io.github.dostepnykampus.IntegrationTest;
import io.github.dostepnykampus.barrier.domain.CreateResult;
import io.github.dostepnykampus.barrier.domain.EditResult;
import io.github.dostepnykampus.barrier.domain.NewSubmission;
import io.github.dostepnykampus.barrier.domain.Submission;
import io.github.dostepnykampus.barrier.domain.SubmissionContent;
import io.github.dostepnykampus.barrier.domain.SubmissionEdit;
import io.github.dostepnykampus.barrier.domain.SubmissionStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

/**
 * Zapis zgłoszeń na prawdziwej bazie, bez transakcji testowej: idempotentne
 * utworzenie oraz edycja z wersją i statusem (BR-3, BR-9, BR-10, BR-18).
 *
 * @since 0.1.0
 */
@IntegrationTest
final class JdbcWriteTest {

    @AfterEach
    void clean(final ApplicationContext context) {
        SubmissionData.of(context).clean();
    }

    @Test
    void brNineCreatesOnceAndKeepsFirstContent(final ApplicationContext context) {
        final SubmissionData data = SubmissionData.of(context);
        final UUID id = SubmissionData.id(1);
        final CreateResult created = data.repo().create(
            new NewSubmission(id, SubmissionData.content("pierwsza"), SubmissionData.AUTHOR)
        );
        final CreateResult repeated = data.repo().create(
            new NewSubmission(id, SubmissionData.content("druga"), "inne-konto")
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
    void brEighteenEditsOnlyEditableStatuses(final ApplicationContext context) {
        final SubmissionData data = SubmissionData.of(context);
        final int size = SubmissionStatus.values().length;
        final List<EditResult> actual = new ArrayList<>(size);
        final List<EditResult> expected = new ArrayList<>(size);
        final SubmissionContent edited = SubmissionData.content("n");
        final SubmissionContent old = SubmissionData.content("opis");
        for (final SubmissionStatus status : SubmissionStatus.values()) {
            final UUID id = data.seed(status);
            final Instant early = data.created(id);
            actual.add(data.repo().update(new SubmissionEdit(id, 1, edited)));
            if (status.editable()) {
                expected.add(
                    new EditResult.Saved(new Submission(id, 2, status.afterEdit(), early, edited))
                );
            } else {
                expected.add(new EditResult.Conflict(new Submission(id, 1, status, early, old)));
            }
        }
        Assertions.assertEquals(expected, actual, "Wynik edycji nie zgadza się z BR-18");
    }
}
