/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.persistence;

import io.github.dostepnykampus.IntegrationTest;
import io.github.dostepnykampus.barrier.domain.EditResult;
import io.github.dostepnykampus.barrier.domain.Submission;
import io.github.dostepnykampus.barrier.domain.SubmissionEdit;
import io.github.dostepnykampus.barrier.domain.SubmissionStatus;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

/**
 * Wynik edycji jako własny zapis oraz strażnik kluczy obcych (B-07).
 *
 * <p>Wymagania: BR-10</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
final class JdbcErrorsTest {

    @AfterEach
    void clean(final ApplicationContext context) {
        SubmissionData.of(context).clean();
    }

    @Test
    void brTenSavedIsOwnWriteNotLaterRead(final ApplicationContext context) {
        final SubmissionData data = SubmissionData.of(context);
        final UUID id = data.seed(SubmissionStatus.SENT);
        data.jdbc().sql(
            """
            CREATE FUNCTION j_foreign_bump() RETURNS trigger LANGUAGE plpgsql AS $$
            BEGIN
                UPDATE submissions SET version = version + 1 WHERE id = NEW.id;
                RETURN NULL;
            END $$
            """
        ).update();
        data.jdbc().sql(
            """
            CREATE TRIGGER j_foreign_bump AFTER UPDATE ON submissions FOR EACH ROW
            WHEN (pg_trigger_depth() = 0) EXECUTE FUNCTION j_foreign_bump()
            """
        ).update();
        try {
            Assertions.assertEquals(
                List.of(
                    new EditResult.Saved(
                        new Submission(
                            id, 2, SubmissionStatus.SENT, data.created(id),
                            SubmissionData.content("własna")
                        )
                    ),
                    3
                ),
                List.of(
                    data.repo().update(
                        new SubmissionEdit(id, 1, SubmissionData.content("własna"))
                    ),
                    data.row(id).get("version")
                )
            );
        } finally {
            data.jdbc().sql("DROP TRIGGER j_foreign_bump ON submissions").update();
            data.jdbc().sql("DROP FUNCTION j_foreign_bump()").update();
        }
    }

    @Test
    void foreignKeyGuardAllowsOnlyBuildingSymbol(final ApplicationContext context) {
        Assertions.assertEquals(
            Set.of("submissions_building_symbol_fkey"),
            Set.copyOf(
                SubmissionData.of(context).jdbc().sql(
                    """
                    SELECT constraint_name FROM information_schema.table_constraints
                    WHERE table_name = 'submissions' AND constraint_type = 'FOREIGN KEY'
                    """
                ).query(String.class).list()
            )
        );
    }
}
