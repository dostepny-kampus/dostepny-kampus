/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building;

import io.github.dostepnykampus.IntegrationTest;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Teren PWr to wszystko do 500 m od konturu budynku: punkt 400 m od
 * budynku leży na terenie, a punkt 600 m od każdego budynku poza nim.
 *
 * <p>Oba punkty leżą na zachód od kampusu głównego. Odległość od najbliższego
 * konturu liczy w teście ta sama baza, z dokładnością do 50 m, więc test
 * pokazuje, że punkty są w odległości z nazwy.</p>
 *
 * <p>Wymagania: BR-G5</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
final class CampusAreaTest {

    @Test
    void brGFiveCoversPointFourHundredMetresFromBuilding(final ApplicationContext context) {
        MatcherAssert.assertThat(
            "Punkt 400 m od H-4 leży poza terenem PWr",
            List.of(
                CampusAreaTest.metres(context, 51.108, 17.048_7),
                context.getBean(CampusArea.class).covers(51.108, 17.048_7)
            ),
            Matchers.equalTo(List.of(400L, true))
        );
    }

    @Test
    void brGFiveLeavesPointSixHundredMetresFromEveryBuilding(final ApplicationContext context) {
        MatcherAssert.assertThat(
            "Punkt 600 m od każdego budynku leży na terenie PWr",
            List.of(
                CampusAreaTest.metres(context, 51.108, 17.045_3),
                context.getBean(CampusArea.class).covers(51.108, 17.045_3)
            ),
            Matchers.equalTo(List.of(600L, false))
        );
    }

    private static long metres(
        final ApplicationContext context, final double lat, final double lon
    ) {
        return context.getBean(JdbcClient.class).sql(
            """
            SELECT CAST(round(min(ST_Distance(outline::geography,
                ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography)) / 50) * 50 AS BIGINT)
            FROM buildings
            """
        ).param("lat", lat).param("lon", lon).query(Long.class).single();
    }
}
