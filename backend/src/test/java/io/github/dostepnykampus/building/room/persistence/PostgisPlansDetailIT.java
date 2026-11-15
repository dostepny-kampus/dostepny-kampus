/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.room.persistence;

import io.github.dostepnykampus.IntegrationTest;
import io.github.dostepnykampus.SampleBuilding;
import io.github.dostepnykampus.building.room.BuildingPlan;
import io.github.dostepnykampus.building.room.FloorPlans;
import io.github.dostepnykampus.building.room.PlanFloor;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

/**
 * Zapis planu czyni budynek szczegółowym.
 *
 * <p>Wymagania: BR-G1, US16</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
@Transactional
final class PostgisPlansDetailIT {

    @Test
    void marksOnlyBuildingWithPlanDetailed(final ApplicationContext context) {
        final JdbcClient jdbc = context.getBean(JdbcClient.class);
        for (final String symbol : List.of("G-971", "G-972")) {
            new SampleBuilding(symbol, "Budynek podstawowy", "BASIC")
                .wkt(jdbc, "POLYGON((17 51,17.001 51,17.001 51.001,17 51.001,17 51))");
        }
        context.getBean(FloorPlans.class).save(
            new BuildingPlan(
                "G-971", List.of(new PlanFloor("P0", "Parter", 0)), List.of(), List.of()
            )
        );
        MatcherAssert.assertThat(
            "Budynek z zapisanym planem nie jest szczegółowy albo zmienił się inny budynek",
            jdbc.sql(
                """
                SELECT symbol || ' ' || detail_level FROM buildings
                WHERE symbol IN ('G-971', 'G-972') ORDER BY symbol
                """
            ).query(String.class).list(),
            Matchers.contains("G-971 DETAILED", "G-972 BASIC")
        );
    }
}
