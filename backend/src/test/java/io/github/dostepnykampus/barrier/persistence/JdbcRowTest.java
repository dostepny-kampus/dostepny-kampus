/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.persistence;

import io.github.dostepnykampus.PostgisTestConfiguration;
import io.github.dostepnykampus.barrier.domain.EditResult;
import io.github.dostepnykampus.barrier.domain.SubmissionEdit;
import io.github.dostepnykampus.barrier.domain.SubmissionStatus;
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
 * Edycja ze starą wersją, edycja brakującego zgłoszenia i uszkodzony wiersz (BR-10, B-02).
 *
 * @since 0.1.0
 */
@SpringBootTest
@Import(PostgisTestConfiguration.class)
final class JdbcRowTest {

    @AfterEach
    void clean(final ApplicationContext context) {
        new SubmissionData(context).clean();
    }

    @Test
    @Disabled("adapter JdbcSubmissions jest zaślepką")
    void brTenStaleVersionConflictsAndMissingIsReported(final ApplicationContext context) {
        final SubmissionData data = SubmissionData.of(context);
        final UUID id = data.seed(
            1, SubmissionData.BUILDING, "p1", SubmissionStatus.SENT, SubmissionData.EARLY
        );
        final EditResult saved = data.repo().update(new SubmissionEdit(id, 1, data.content("a")));
        Assertions.assertEquals(
            List.of(
                new EditResult.Conflict(((EditResult.Saved) saved).submission()),
                new EditResult.Missing(data.id(7))
            ),
            List.of(
                data.repo().update(new SubmissionEdit(id, 1, data.content("b"))),
                data.repo().update(new SubmissionEdit(data.id(7), 1, data.content("c")))
            )
        );
    }

    @Test
    @Disabled("adapter JdbcSubmissions jest zaślepką")
    void unreadableRowFailsWithItsId(final ApplicationContext context) {
        final SubmissionData data = SubmissionData.of(context);
        final UUID id = data.id(1);
        data.jdbc().sql(
            """
            INSERT INTO submissions (id, kind, location, content)
            VALUES (:id, 'QUICK', ST_SetSRID(ST_MakePoint(17, 51), 4326),
                CAST('{"stare":1}' AS JSONB))
            """
        ).param("id", id).update();
        Assertions.assertTrue(
            Assertions.assertThrows(IllegalStateException.class, () -> data.repo().find(id))
                .getMessage().contains(id.toString()),
            "Błąd odczytu wiersza nie wskazuje identyfikatora"
        );
    }
}
