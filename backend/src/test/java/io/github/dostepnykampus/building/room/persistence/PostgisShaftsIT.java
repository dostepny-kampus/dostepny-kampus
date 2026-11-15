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
import java.util.Locale;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

/**
 * Zapis planu łączy pomieszczenia windy z kolejnych pięter w szyb.
 *
 * <p>Wymagania: US18.2</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
@Transactional
final class PostgisShaftsIT {

    @Test
    void joinsOverlappingLiftsIntoShaftNamedByLowestFloor(final ApplicationContext context) {
        final JdbcClient jdbc = context.getBean(JdbcClient.class);
        new SampleBuilding("G-961", "Budynek z windami", "DETAILED")
            .wkt(jdbc, "POLYGON((17 51,17.01 51,17.01 51.01,17 51.01,17 51))");
        final List<PlanFloor> floors = List.of(
            new PlanFloor("P0", "Parter", 0), new PlanFloor("P1", "1 piętro", 1),
            new PlanFloor("P2", "2 piętro", 2)
        );
        final List<PlanRoom> rooms = List.of(
            PostgisShaftsIT.room("a2", "P2", "Winda", 1),
            PostgisShaftsIT.room("a1", "P1", "Winda", 0.5),
            PostgisShaftsIT.room("a0", "P0", "Winda", 0),
            PostgisShaftsIT.room("b1", "P1", "Winda", 3.2),
            PostgisShaftsIT.room("b2", "P2", "Winda", 4.8),
            PostgisShaftsIT.room("c2", "P2", "Winda", 10),
            PostgisShaftsIT.room("k0", "P0", "Korytarz", 0)
        );
        final FloorPlans plans = context.getBean(FloorPlans.class);
        final BuildingPlan plan = new BuildingPlan("G-961", floors, rooms, List.of());
        plans.save(plan);
        plans.save(plan);
        MatcherAssert.assertThat(
            "Szyb windy nie objął nakładających się pomieszczeń albo nazwa nie pochodzi z dołu",
            jdbc.sql(
                """
                SELECT id || ' ' || COALESCE(lift_id, '-') FROM rooms
                WHERE building_symbol = 'G-961' ORDER BY id
                """
            ).query(String.class).list(),
            Matchers.contains(
                "a0 P0/a0", "a1 P0/a0", "a2 P0/a0", "b1 P1/b1", "b2 P2/b2", "c2 P2/c2", "k0 -"
            )
        );
    }

    private static PlanRoom room(
        final String id, final String floor, final String function, final double east
    ) {
        final double lon = 1 / 70_197.0;
        final double west = 17 + east * lon;
        return new PlanRoom(
            id, floor, "", function,
            String.format(
                Locale.ROOT,
                String.join(
                    "",
                    "{\"type\":\"MultiPolygon\",\"coordinates\":[[[[%1$.9f,51],[%2$.9f,51],",
                    "[%2$.9f,%3$.9f],[%1$.9f,%3$.9f],[%1$.9f,51]]]]}"
                ),
                west, west + 3 * lon, 51 + 3 / 111_229.0
            )
        );
    }
}
