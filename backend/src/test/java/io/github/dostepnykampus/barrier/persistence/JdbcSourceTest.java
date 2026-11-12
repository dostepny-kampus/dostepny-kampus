/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.persistence;

import io.github.dostepnykampus.IntegrationTest;
import io.github.dostepnykampus.barrier.domain.SubmissionStatus;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataAccessException;

/**
 * Rodzaj, budynek, punkt i piętro zgłoszenia mają jedno źródło: treść JSON
 * (#837). Kolumn nie da się zapisać osobno, a zmiana treści zmienia je.
 *
 * @since 0.1.0
 */
@IntegrationTest
final class JdbcSourceTest {

    @AfterEach
    void clean(final ApplicationContext context) {
        SubmissionData.of(context).clean();
    }

    @Test
    void columnPointDifferentFromContentCannotBeSaved(final ApplicationContext context) {
        final SubmissionData data = SubmissionData.of(context);
        final UUID id = data.seed(SubmissionStatus.SENT);
        Assertions.assertThrows(
            DataAccessException.class,
            () -> data.jdbc().sql(
                """
                UPDATE submissions SET location = ST_SetSRID(ST_MakePoint(17.062, 51.108), 4326)
                WHERE id = :id
                """
            ).param("id", id).update(),
            "Punkt w kolumnie da się zapisać inny niż w treści"
        );
    }

    @Test
    void columnsFollowContent(final ApplicationContext context) {
        final SubmissionData data = SubmissionData.of(context);
        final UUID id = data.seed(SubmissionStatus.SENT);
        data.jdbc().sql(
            """
            UPDATE submissions SET content = jsonb_set(
                content || '{"floor": {"floorId": "p9"}, "buildingSymbol": null}',
                '{location,coordinates}', '[17.062, 51.108]'
            ) WHERE id = :id
            """
        ).param("id", id).update();
        Assertions.assertEquals(
            "QUICK - POINT(17.062 51.108) p9",
            data.jdbc().sql(
                """
                SELECT concat_ws(' ', kind, coalesce(building_symbol, '-'),
                    ST_AsText(location), floor_id)
                FROM submissions WHERE id = :id
                """
            ).param("id", id).query(String.class).single(),
            "Kolumny zgłoszenia nie wynikają z treści"
        );
    }
}
