/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.persistence;

import io.github.dostepnykampus.IntegrationTest;
import io.github.dostepnykampus.barrier.domain.EditResult;
import io.github.dostepnykampus.barrier.domain.SubmissionEdit;
import io.github.dostepnykampus.barrier.domain.SubmissionStatus;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

/**
 * Edycja ze starą wersją, edycja brakującego zgłoszenia i uszkodzony wiersz (BR-10, B-02).
 *
 * @since 0.1.0
 */
@IntegrationTest
final class JdbcRowTest {

    @AfterEach
    void clean(final ApplicationContext context) {
        SubmissionData.of(context).clean();
    }

    @Test
    void brTenStaleVersionConflictsAndMissingIsReported(final ApplicationContext context) {
        final SubmissionData data = SubmissionData.of(context);
        final UUID id = data.seed(SubmissionStatus.SENT);
        Assertions.assertEquals(
            List.of(
                new EditResult.Conflict(
                    ((EditResult.Saved) data.repo().update(
                        new SubmissionEdit(id, 1, SubmissionData.content("a"))
                    )).submission()
                ),
                new EditResult.Missing(SubmissionData.id(7))
            ),
            List.of(
                data.repo().update(new SubmissionEdit(id, 1, SubmissionData.content("b"))),
                data.repo().update(
                    new SubmissionEdit(SubmissionData.id(7), 1, SubmissionData.content("c"))
                )
            )
        );
    }

    @Test
    void unreadableRowFailsWithItsId(final ApplicationContext context) {
        final SubmissionData data = SubmissionData.of(context);
        final UUID id = SubmissionData.id(1);
        data.jdbc().sql(
            """
            INSERT INTO submissions (id, content)
            VALUES (:id, CAST(
                '{"kind": "QUICK", "location": {"type": "Point", "coordinates": [17, 51]},
                    "photoIds": "zepsute"}' AS JSONB
            ))
            """
        ).param("id", id).update();
        Assertions.assertTrue(
            Assertions.assertThrows(IllegalStateException.class, () -> data.repo().find(id))
                .getMessage().contains(id.toString()),
            "Błąd odczytu wiersza nie wskazuje identyfikatora"
        );
    }
}
