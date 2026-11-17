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

/**
 * Zapis planu liczy grupę funkcji, podpis numeru i obrys piętra.
 *
 * <p>Wymagania: US16.1, US18.1</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
final class PostgisPlansDerivedIT {

    @Test
    void labelsLargestRoomOfNumberAndSkipsSmallOnes(final ApplicationContext context) {
        MatcherAssert.assertThat(
            "Podpis numeru nie trafił tylko do największego pomieszczenia od 20 m²",
            PostgisPlansDerivedIT.saved(context, "G-941").sql(
                """
                SELECT id || ' ' || category || ' ' || labelled FROM rooms
                WHERE building_symbol = 'G-941' ORDER BY id
                """
            ).query(String.class).list(),
            Matchers.contains(
                "1 CLASSROOM true", "2 CLASSROOM false", "3 ACCESSIBLE_TOILET false",
                "4 CORRIDOR false", "5 OTHER false", "6 OTHER false"
            )
        );
    }

    @Test
    void joinsTouchingRoomsIntoOneFloorOutline(final ApplicationContext context) {
        MatcherAssert.assertThat(
            "Obrys piętra nie łączy stykających się pomieszczeń albo łączy odległe",
            PostgisPlansDerivedIT.saved(context, "G-942").sql(
                """
                SELECT id || ' ' || COALESCE(ST_NumGeometries(outline)::text, 'brak') FROM floors
                WHERE building_symbol = 'G-942' ORDER BY sort_order
                """
            ).query(String.class).list(),
            Matchers.contains("P0 4", "P1 1", "P2 brak")
        );
    }

    @Test
    void closesNarrowWallsAndKeepsSquareCorners(final ApplicationContext context) {
        MatcherAssert.assertThat(
            "Obrys piętra nie domyka ściany 1,1 m, domyka przejście 1,5 m albo zaokrągla narożniki",
            PostgisPlansDerivedIT.saved(
                context, "G-943",
                List.of(
                    PostgisPlansDerivedIT.room("1", "P0", "", "Inne", 0, 10),
                    PostgisPlansDerivedIT.room("2", "P0", "", "Inne", 11.1, 10),
                    PostgisPlansDerivedIT.room("3", "P1", "", "Inne", 0, 10),
                    PostgisPlansDerivedIT.room("4", "P1", "", "Inne", 11.5, 10)
                )
            ).sql(
                """
                SELECT id || ' ' || ST_NumGeometries(outline) || ' ' || ST_NPoints(outline)
                FROM floors WHERE building_symbol = 'G-943' AND outline IS NOT NULL
                ORDER BY sort_order
                """
            ).query(String.class).list(),
            Matchers.contains("P0 1 5", "P1 2 10")
        );
    }

    private static JdbcClient saved(final ApplicationContext context, final String symbol) {
        return PostgisPlansDerivedIT.saved(
            context, symbol,
            List.of(
                PostgisPlansDerivedIT.room("1", "P0", "101", "Sala dydaktyczna", 0, 10),
                PostgisPlansDerivedIT.room("2", "P0", "101", "Laboratorium", 20, 6),
                PostgisPlansDerivedIT.room("3", "P0", "102", "Toaleta dla OzN", 40, 4),
                PostgisPlansDerivedIT.room("4", "P0", "", "Korytarz", 60, 10),
                PostgisPlansDerivedIT.room("5", "P1", "", "Inne", 0, 10),
                PostgisPlansDerivedIT.room("6", "P1", "", "Inne", 10, 10)
            )
        );
    }

    private static JdbcClient saved(
        final ApplicationContext context, final String symbol, final List<PlanRoom> rooms
    ) {
        final JdbcClient jdbc = context.getBean(JdbcClient.class);
        new SampleBuilding(symbol, "Budynek z planem", "DETAILED")
            .wkt(jdbc, "POLYGON((17 51,17.01 51,17.01 51.01,17 51.01,17 51))");
        context.getBean(FloorPlans.class).save(
            new BuildingPlan(
                symbol,
                List.of(
                    new PlanFloor("P0", "Parter", 0), new PlanFloor("P1", "1 piętro", 1),
                    new PlanFloor("P2", "2 piętro", 2)
                ),
                rooms,
                List.of()
            )
        );
        return jdbc;
    }

    private static PlanRoom room(
        final String id, final String floor, final String number, final String function,
        final double east, final double side
    ) {
        return new PlanRoom(
            id, floor, number, function, PostgisPlansDerivedIT.box(east, side)
        );
    }

    private static String box(final double east, final double side) {
        final double lon = 1 / 70_197.0;
        final double west = 17 + east * lon;
        final double south = 51;
        return String.format(
            Locale.ROOT,
            String.join(
                "",
                "{\"type\":\"MultiPolygon\",\"coordinates\":[[[[%1$.9f,%2$.9f],[%3$.9f,%2$.9f],",
                "[%3$.9f,%4$.9f],[%1$.9f,%4$.9f],[%1$.9f,%2$.9f]]]]}"
            ),
            west, south, west + side * lon, south + side / 111_229.0
        );
    }
}
