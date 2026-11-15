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
import io.github.dostepnykampus.building.room.PlanRoom;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

/**
 * Zapis planu naprawia kontur pomieszczenia z samoprzecięciem.
 *
 * <p>Wymagania: US16</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
@Transactional
final class PostgisValidOutlineIT {

    @Test
    void repairsSelfIntersectingOutline(final ApplicationContext context) {
        final JdbcClient jdbc = context.getBean(JdbcClient.class);
        new SampleBuilding("G-981", "Budynek z planem", "BASIC")
            .wkt(jdbc, "POLYGON((17 51,17.001 51,17.001 51.001,17 51.001,17 51))");
        context.getBean(FloorPlans.class).save(
            new BuildingPlan(
                "G-981",
                List.of(new PlanFloor("P0", "Parter", 0)),
                List.of(
                    new PlanRoom(
                        "39", "P0", "39", "Laboratorium",
                        String.join(
                            "",
                            "{\"type\":\"MultiPolygon\",\"coordinates\":[[[[17.0001,51.0001],",
                            "[17.0005,51.0005],[17.0005,51.0001],[17.0001,51.0005],[17.0001,51.0001]]]]}"
                        )
                    )
                ),
                List.of()
            )
        );
        MatcherAssert.assertThat(
            "Kontur z samoprzecięciem nie został naprawiony do poprawnego MultiPolygon",
            jdbc.sql(
                """
                SELECT ST_IsValid(outline) || ' ' || GeometryType(outline) FROM rooms
                WHERE building_symbol = 'G-981'
                """
            ).query(String.class).single(),
            Matchers.equalTo("true MULTIPOLYGON")
        );
    }
}
