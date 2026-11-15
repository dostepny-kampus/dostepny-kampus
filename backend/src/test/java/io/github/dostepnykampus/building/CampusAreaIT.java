/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building;

import io.github.dostepnykampus.CampusPoint;
import io.github.dostepnykampus.IntegrationTest;
import io.github.dostepnykampus.building.api.CampusArea;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Teren PWr to wnętrze granicy któregoś z pięciu terenów z pliku danych, razem z linią
 * granicy: przerwa między budynkami i zapas za obszarem uczelni należą do terenu,
 * a punkt 20 m za granicą nie.
 *
 * <p>Wymagania: BR-G5, US4.9</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
final class CampusAreaIT {

    @Test
    void coversGapBetweenBuildingsOfOneArea(final ApplicationContext context) {
        MatcherAssert.assertThat(
            "Punkt w przerwie 60 m między budynkami terenu leży poza terenem PWr",
            List.of(
                CampusPoint.GAP.awayFromBuildings(context.getBean(JdbcClient.class), 25),
                CampusAreaIT.covers(context, CampusPoint.GAP)
            ),
            Matchers.equalTo(List.of(true, true))
        );
    }

    @Test
    void coversMarginBehindUniversityArea(final ApplicationContext context) {
        MatcherAssert.assertThat(
            "Punkt 20 m za obszarem uczelni, w zapasie, leży poza terenem PWr",
            List.of(
                CampusPoint.MARGIN.awayFromBuildings(context.getBean(JdbcClient.class), 30),
                CampusAreaIT.covers(context, CampusPoint.MARGIN)
            ),
            Matchers.equalTo(List.of(true, true))
        );
    }

    @Test
    void coversPointOnBoundaryLine(final ApplicationContext context) {
        MatcherAssert.assertThat(
            "Punkt na linii granicy leży poza terenem PWr",
            CampusAreaIT.covers(
                context, CampusPoint.onBoundary(context.getBean(JdbcClient.class))
            ),
            Matchers.is(true)
        );
    }

    @Test
    void leavesPointTwentyMetresBeyondBoundary(final ApplicationContext context) {
        MatcherAssert.assertThat(
            "Punkt 20 m za granicą leży na terenie PWr",
            CampusAreaIT.covers(
                context, CampusPoint.beyondBoundary(context.getBean(JdbcClient.class), 20)
            ),
            Matchers.is(false)
        );
    }

    private static boolean covers(final ApplicationContext context, final CampusPoint point) {
        return context.getBean(CampusArea.class).covers(point.lat(), point.lon());
    }
}
