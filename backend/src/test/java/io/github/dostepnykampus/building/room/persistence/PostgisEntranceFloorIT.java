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
import io.github.dostepnykampus.building.room.PlanEntrance;
import io.github.dostepnykampus.building.room.PlanFloor;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

/**
 * Piętro wejścia budynku z najbliższego wejścia PWr przy zapisie planu.
 *
 * <p>Wymagania: US18.2</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
@Transactional
final class PostgisEntranceFloorIT {

    @Test
    void takesFloorOfNearestPwrEntranceInThreshold(final ApplicationContext context) {
        final JdbcClient jdbc = context.getBean(JdbcClient.class);
        new SampleBuilding("G-951", "Budynek z wejściami", "DETAILED")
            .wkt(jdbc, "POLYGON((17 51,17.002 51,17.002 51.002,17 51.002,17 51))");
        jdbc.sql(
            """
            INSERT INTO entrances (id, building_symbol, main, location, floor_id, marking,
                marking_date, marking_source)
            SELECT id, 'G-951', FALSE, ST_SetSRID(ST_MakePoint(lon, lat), 4326), floor,
                'NO_DATA', DATE '2026-11-01', 'IMPORT'
            FROM (VALUES ('e1', 17.0, 51.0, NULL), ('e2', 17.001, 51.0, NULL),
                ('e3', 17.0, 51.0005, 'P0')) AS entrance (id, lon, lat, floor)
            """
        ).update();
        context.getBean(FloorPlans.class).save(
            new BuildingPlan(
                "G-951",
                List.of(new PlanFloor("P0", "Parter", 0), new PlanFloor("P1", "1 piętro", 1)),
                List.of(),
                List.of(
                    PostgisEntranceFloorIT.pwr("P9", 17.0, 51.0),
                    PostgisEntranceFloorIT.pwr("P1", 17.0001, 51.0),
                    PostgisEntranceFloorIT.pwr("P0", 17.001, 51.0001)
                )
            )
        );
        MatcherAssert.assertThat(
            "Wejście nie wzięło piętra najbliższego wejścia PWr z piętrem planu do 15 m",
            jdbc.sql(
                """
                SELECT id || ' ' || COALESCE(floor_id, '-') FROM entrances
                WHERE building_symbol = 'G-951' ORDER BY id
                """
            ).query(String.class).list(),
            Matchers.contains("e1 P1", "e2 P0", "e3 -")
        );
    }

    private static PlanEntrance pwr(final String floor, final double lon, final double lat) {
        return new PlanEntrance(
            floor, String.format("{\"type\":\"Point\",\"coordinates\":[%s,%s]}", lon, lat),
            "NO_DATA", "Wejście", null, null, false
        );
    }
}
