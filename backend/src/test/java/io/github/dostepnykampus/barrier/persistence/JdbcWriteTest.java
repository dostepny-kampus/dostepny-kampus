/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.persistence;

import io.github.dostepnykampus.PostgisTestConfiguration;
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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

/**
 * Zapis zgłoszeń na prawdziwej bazie, bez transakcji testowej: idempotentne
 * utworzenie oraz edycja z wersją i statusem (BR-3, BR-9, BR-10, BR-18).
 *
 * @since 0.1.0
 */
@SpringBootTest
@Import(PostgisTestConfiguration.class)
final class JdbcWriteTest {

    @AfterEach
    void clean(final ApplicationContext context) {
        new SubmissionData(context).clean();
    }

    @Test
    @Disabled("adapter JdbcSubmissions jest zaślepką")
    void brNineCreatesOnceAndKeepsFirstContent(final ApplicationContext context) {
        final SubmissionData data = SubmissionData.of(context);
        final UUID id = data.id(1);
        final CreateResult created = data.repo().create(
            new NewSubmission(id, data.content("pierwsza"))
        );
        final Submission expected = new Submission(
            id, 1, SubmissionStatus.SENT, data.created(id), data.content("pierwsza")
        );
        Assertions.assertEquals(
            List.of(new CreateResult.Created(expected), new CreateResult.Existing(expected)),
            List.of(created, data.repo().create(new NewSubmission(id, data.content("druga"))))
        );
    }

    @Test
    @Disabled("adapter JdbcSubmissions jest zaślepką")
    void brEighteenEditsOnlyEditableStatuses(final ApplicationContext context) {
        final SubmissionData data = SubmissionData.of(context);
        final List<EditResult> actual = new ArrayList<>();
        final List<EditResult> expected = new ArrayList<>();
        final Instant early = SubmissionData.EARLY;
        final SubmissionContent edited = data.content("n");
        final SubmissionContent old = data.content("opis");
        for (final SubmissionStatus status : SubmissionStatus.values()) {
            final UUID id = data.seed(
                status.ordinal() + 1, SubmissionData.BUILDING, "p1", status, SubmissionData.EARLY
            );
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
