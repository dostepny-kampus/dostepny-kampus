/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.persistence;

import io.github.dostepnykampus.IntegrationTest;
import io.github.dostepnykampus.barrier.submission.NewSubmission;
import io.github.dostepnykampus.barrier.submission.SubmissionEdit;
import io.github.dostepnykampus.barrier.submission.SubmissionStatus;
import io.github.dostepnykampus.barrier.submission.content.InvalidSubmission;
import io.github.dostepnykampus.barrier.submission.content.SubmissionContent;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Błędy bazy przy zapisie zgłoszenia: nieznany budynek oraz błędy integralności
 * niezwiązane z budynkiem (B-07).
 *
 * <p>Wymagania: BR-14</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
final class JdbcBuildingTest {

    @AfterEach
    void clean(final ApplicationContext context) {
        SubmissionData.of(context).clean();
    }

    @Test
    void brFourteenUnknownBuildingIsInvalidSubmission(final ApplicationContext context) {
        final SubmissionData data = SubmissionData.of(context);
        final UUID id = data.seed(SubmissionStatus.SENT);
        final SubmissionContent unknown = SubmissionData.content("J-NONE", "p1", "x");
        Assertions.assertEquals(
            List.of("Nieznany budynek zgłoszenia", "Nieznany budynek zgłoszenia"),
            List.of(
                Assertions.assertThrows(
                    InvalidSubmission.class,
                    () -> data.repo().create(
                        new NewSubmission(SubmissionData.id(2), unknown, SubmissionData.AUTHOR)
                    )
                ).getMessage(),
                Assertions.assertThrows(
                    InvalidSubmission.class,
                    () -> data.repo().update(new SubmissionEdit(id, 1, unknown))
                ).getMessage()
            )
        );
    }

    @Test
    void otherIntegrityErrorsAreNotMasked(final ApplicationContext context) {
        final SubmissionData data = SubmissionData.of(context);
        data.jdbc().sql(
            """
            ALTER TABLE submissions ADD CONSTRAINT j_no_other
            CHECK (building_symbol IS DISTINCT FROM 'J-901')
            """
        ).update();
        try {
            Assertions.assertThrows(
                DataIntegrityViolationException.class,
                () -> data.repo().create(
                    new NewSubmission(
                        SubmissionData.id(1),
                        SubmissionData.content(SubmissionData.OTHER, "p1", "x"),
                        SubmissionData.AUTHOR
                    )
                ),
                "Błąd inny niż brak budynku został zamaskowany"
            );
        } finally {
            data.jdbc().sql("ALTER TABLE submissions DROP CONSTRAINT j_no_other").update();
        }
    }
}
