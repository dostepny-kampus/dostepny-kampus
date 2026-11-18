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
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dane wpisów warstwy wejść PWr w tabelach entrances i evacuation_exits przy zapisie planu;
 * wartości wymyślone w kształcie danych PWr.
 *
 * <p>Wymagania: BR-24, BR-27, US2.6, US17.1</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
@Transactional
final class PostgisPwrEntrancesIT {

    @Test
    void storesPwrEntrancesAndEvacuationExits(final ApplicationContext context) {
        MatcherAssert.assertThat(
            "Wejścia nie mają danych PWr, obchód nie wygrał albo wpis PWr nie stał się wejściem",
            PostgisPwrEntrancesIT.saved(context).sql(
                """
                SELECT CASE WHEN id LIKE 'pwr/%' THEN 'pwr' ELSE id END || '|'
                    || COALESCE(pwr_name, name) || '|' || CASE WHEN marking_source = 'WALKTHROUGH'
                        THEN marking ELSE COALESCE(pwr_marking, marking) END || '|'
                    || COALESCE(approach, '-') || '|' || COALESCE(door_width_cm::text, '-')
                    || '|' || (pwr_marking IS NOT NULL)
                FROM entrances WHERE building_symbol = 'G-952' ORDER BY 1
                """
            ).query(String.class).list(),
            Matchers.contains(
                "o1|Wejście Cesarskie|ACCESSIBLE|Od ul. Wybrzeże.|120|true",
                "o2|Wejście|INACCESSIBLE|-|-|true",
                "pwr|Wejście|INACCESSIBLE|Do wejścia prowadzą schody.|90|true"
            )
        );
    }

    @Test
    void storesEvacuationExitApartFromEntrances(final ApplicationContext context) {
        MatcherAssert.assertThat(
            "Wyjście ewakuacyjne nie trafiło osobno od wejść",
            PostgisPwrEntrancesIT.saved(context).sql(
                """
                SELECT description || '|' || floor_id || '|' || COALESCE(door_width_cm::text, '-')
                FROM evacuation_exits WHERE building_symbol = 'G-952'
                """
            ).query(String.class).list(),
            Matchers.contains("Wyjście ewakuacyjne z pomieszczenia 41|P0|-")
        );
    }

    private static JdbcClient saved(final ApplicationContext context) {
        final JdbcClient jdbc = context.getBean(JdbcClient.class);
        new SampleBuilding("G-952", "Budynek z wejściami PWr", "DETAILED")
            .wkt(jdbc, "POLYGON((17 51,17.002 51,17.002 51.002,17 51.002,17 51))");
        jdbc.sql(
            """
            INSERT INTO entrances (id, building_symbol, name, main, location, marking,
                marking_date, marking_source)
            SELECT id, 'G-952', name, FALSE, ST_SetSRID(ST_MakePoint(lon, lat), 4326), marking,
                DATE '2026-11-01', source
            FROM (VALUES ('o1', 'W-1', 17.0, 51.0, 'NO_DATA', 'IMPORT'),
                ('o2', NULL, 17.001, 51.0, 'INACCESSIBLE', 'WALKTHROUGH'))
                AS entrance (id, name, lon, lat, marking, source)
            """
        ).update();
        final BuildingPlan plan = new BuildingPlan(
            "G-952", List.of(new PlanFloor("P0", "Parter", 0)), List.of(),
            List.of(
                PostgisPwrEntrancesIT.pwr(
                    "P0", 17.000_05, 51.0, "ACCESSIBLE", "Wejście Cesarskie", "Od ul. Wybrzeże.",
                    120
                ),
                PostgisPwrEntrancesIT.pwr(
                    "P0", 17.001, 51.000_05, "ACCESSIBLE", "Wejście", null, null
                ),
                PostgisPwrEntrancesIT.pwr(
                    "P0", 17.0005, 51.0005, "INACCESSIBLE", "Wejście",
                    "Do wejścia prowadzą schody.", 90
                ),
                new PlanEntrance(
                    "P0", PostgisPwrEntrancesIT.point(17.0002, 51.0003), "ACCESSIBLE",
                    "Wyjście ewakuacyjne", "Wyjście ewakuacyjne z pomieszczenia 41", null, true
                ),
                PostgisPwrEntrancesIT.pwr(
                    "P9", 17.0008, 51.0008, "ACCESSIBLE", "Wejście", null, null
                )
            )
        );
        context.getBean(FloorPlans.class).save(plan);
        context.getBean(FloorPlans.class).save(plan);
        return jdbc;
    }

    private static PlanEntrance pwr(
        final String floor, final double lon, final double lat, final String marking,
        final String name, final @Nullable String approach, final @Nullable Integer width
    ) {
        return new PlanEntrance(
            floor, PostgisPwrEntrancesIT.point(lon, lat), marking, name, approach, width, false
        );
    }

    private static String point(final double lon, final double lat) {
        return String.format("{\"type\":\"Point\",\"coordinates\":[%s,%s]}", lon, lat);
    }
}
