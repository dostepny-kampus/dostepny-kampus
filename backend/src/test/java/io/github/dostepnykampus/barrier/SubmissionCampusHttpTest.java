/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import io.github.dostepnykampus.IntegrationTest;
import io.github.dostepnykampus.barrier.domain.OutsideCampus;
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
 * Zgłoszenie tylko na terenie PWr: do 500 m od konturu budynku (BR-G5, US4.9).
 *
 * <p>Punkty są te same co w {@code CampusAreaTest}, który sprawdza ich
 * odległość od budynków w bazie.</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
@Transactional
final class SubmissionCampusHttpTest {

    /**
     * Punkt około 400 m od budynku H-4.
     */
    private static final double[] NEAR = {17.048_7, 51.108};

    /**
     * Punkt około 600 m od najbliższego budynku L-2.
     */
    private static final double[] FAR = {17.045_3, 51.108};

    @Test
    void brGFiveAcceptsPointFourHundredMetresFromBuilding(final WebApplicationContext context)
        throws Exception {
        final UUID id = UUID.randomUUID();
        MatcherAssert.assertThat(
            "Zgłoszenie 400 m od budynku nie zostało przyjęte",
            List.of(
                SubmissionCampusHttpTest.post(context, id, SubmissionCampusHttpTest.NEAR)
                    .getStatus(),
                SubmissionCampusHttpTest.saved(context, id)
            ),
            Matchers.equalTo(List.of(201, 1))
        );
    }

    @Test
    void brGFiveRejectsPointSixHundredMetresFromEveryBuilding(
        final WebApplicationContext context
    ) throws Exception {
        final UUID id = UUID.randomUUID();
        final MockHttpServletResponse response =
            SubmissionCampusHttpTest.post(context, id, SubmissionCampusHttpTest.FAR);
        MatcherAssert.assertThat(
            "Zgłoszenie 600 m od budynków nie dostało odmowy z rodzajem błędu",
            List.of(
                response.getStatus(),
                context.getBean(ObjectMapper.class)
                    .readTree(response.getContentAsString(StandardCharsets.UTF_8))
                    .path("type").asString(),
                SubmissionCampusHttpTest.saved(context, id)
            ),
            Matchers.equalTo(List.of(400, OutsideCampus.TYPE.toString(), 0))
        );
    }

    private static MockHttpServletResponse post(
        final WebApplicationContext context, final UUID id, final double... point
    ) throws Exception {
        return new SubmissionRequests(context).post(
            String.format(
                """
                {"id":"%s","kind":"QUICK","barrierGroup":"OTHER",
                "location":{"type":"Point","coordinates":[%s,%s]},
                "photoIds":["5d1c2b3a-4e5f-4a6b-8c7d-9e0f1a2b3c4d"]}
                """, id, point[0], point[1]
            )
        );
    }

    private static int saved(final WebApplicationContext context, final UUID id) {
        return context.getBean(JdbcClient.class)
            .sql("SELECT count(*) FROM submissions WHERE id = :id")
            .param("id", id).query(Integer.class).single();
    }
}
