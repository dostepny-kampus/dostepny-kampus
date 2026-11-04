/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.persistence;

import io.github.dostepnykampus.PostgisTestConfiguration;
import io.github.dostepnykampus.barrier.domain.InvalidSubmission;
import io.github.dostepnykampus.barrier.domain.NewSubmission;
import io.github.dostepnykampus.barrier.domain.SubmissionContent;
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
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Błędy bazy przy zapisie zgłoszenia: nieznany budynek oraz błędy integralności
 * niezwiązane z budynkiem (BR-14, B-07).
 *
 * @since 0.1.0
 */
@SpringBootTest
@Import(PostgisTestConfiguration.class)
final class JdbcBuildingTest {

    @AfterEach
    void clean(final ApplicationContext context) {
        SubmissionData.of(context).clean();
    }

    @Test
    @Disabled("adapter JdbcSubmissions jest zaślepką")
    void brFourteenUnknownBuildingIsInvalidSubmission(final ApplicationContext context) {
        final SubmissionData data = SubmissionData.of(context);
        final UUID id = data.seed(SubmissionStatus.SENT);
        final SubmissionContent unknown = SubmissionData.content("J-NONE", "p1", "x");
        Assertions.assertEquals(
            List.of("Nieznany budynek zgłoszenia", "Nieznany budynek zgłoszenia"),
            List.of(
                Assertions.assertThrows(
                    InvalidSubmission.class,
                    () -> data.repo().create(new NewSubmission(SubmissionData.id(2), unknown))
                ).getMessage(),
                Assertions.assertThrows(
                    InvalidSubmission.class,
                    () -> data.repo().update(new SubmissionEdit(id, 1, unknown))
                ).getMessage()
            )
        );
    }

    @Test
    @Disabled("adapter JdbcSubmissions jest zaślepką")
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
                        SubmissionData.content(SubmissionData.OTHER, "p1", "x")
                    )
                ),
                "Błąd inny niż brak budynku został zamaskowany"
            );
        } finally {
            data.jdbc().sql("ALTER TABLE submissions DROP CONSTRAINT j_no_other").update();
        }
    }
}
