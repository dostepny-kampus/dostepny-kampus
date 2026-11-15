/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import io.github.dostepnykampus.CampusPoint;
import io.github.dostepnykampus.IntegrationTest;
import io.github.dostepnykampus.barrier.submission.OutsideCampus;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

/**
 * Zgłoszenie tylko na terenie PWr: wewnątrz granicy terenu.
 *
 * <p>Punkt wewnątrz to przerwa między budynkami jednego terenu, a punkt spoza terenu
 * leży daleko na zachód od kampusu głównego; {@code CampusAreaIT} sprawdza
 * punkty przy samej granicy.</p>
 *
 * <p>Wymagania: BR-G5, US4.9</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
@Transactional
final class SubmissionCampusHttpIT {

    /**
     * Punkt poza granicą każdego terenu, około 600 m od najbliższego budynku.
     */
    private static final CampusPoint FAR = new CampusPoint(51.108, 17.045_3);

    @Test
    void acceptsPointInGapBetweenBuildings(final WebApplicationContext context)
        throws Exception {
        final UUID id = UUID.randomUUID();
        MatcherAssert.assertThat(
            "Zgłoszenie z przerwy między budynkami terenu nie zostało przyjęte",
            List.of(
                SubmissionCampusHttpIT.post(context, id, CampusPoint.GAP)
                    .getStatus(),
                SubmissionCampusHttpIT.saved(context, id)
            ),
            Matchers.equalTo(List.of(201, 1))
        );
    }

    @Test
    void rejectsPointOutsideEveryArea(
        final WebApplicationContext context
    ) throws Exception {
        final UUID id = UUID.randomUUID();
        final MockHttpServletResponse response =
            SubmissionCampusHttpIT.post(context, id, SubmissionCampusHttpIT.FAR);
        MatcherAssert.assertThat(
            "Zgłoszenie spoza terenu nie dostało odmowy z rodzajem błędu",
            List.of(
                response.getStatus(),
                context.getBean(ObjectMapper.class)
                    .readTree(response.getContentAsString(StandardCharsets.UTF_8))
                    .path("type").asString(),
                SubmissionCampusHttpIT.saved(context, id)
            ),
            Matchers.equalTo(List.of(400, OutsideCampus.TYPE.toString(), 0))
        );
    }

    private static MockHttpServletResponse post(
        final WebApplicationContext context, final UUID id, final CampusPoint point
    ) throws Exception {
        return new SubmissionRequests(context).post(
            String.format(
                """
                {"id":"%s","kind":"QUICK","barrierGroup":"OTHER",
                "location":{"type":"Point","coordinates":%s},
                "photoIds":["5d1c2b3a-4e5f-4a6b-8c7d-9e0f1a2b3c4d"]}
                """, id, point.json()
            )
        );
    }

    private static int saved(final WebApplicationContext context, final UUID id) {
        return context.getBean(JdbcClient.class)
            .sql("SELECT count(*) FROM submissions WHERE id = :id")
            .param("id", id).query(Integer.class).single();
    }
}
