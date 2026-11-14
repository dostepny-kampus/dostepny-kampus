/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

package io.github.dostepnykampus.building.room.persistence;

import io.github.dostepnykampus.IntegrationTest;
import io.github.dostepnykampus.SampleBuilding;
import io.github.dostepnykampus.building.room.FloorPlans;
import io.github.dostepnykampus.building.room.PlanFloor;
import io.github.dostepnykampus.building.room.PlanRoom;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

/**
 * Testy zapisu pięter i pomieszczeń bez duplikatów.
 *
 * @since 0.1.0
 */
@IntegrationTest
@Transactional
final class JdbcFloorPlansTest {

    /**
     * Sprawdza idempotentny zapis, aktualizację, usuwanie i wartości NULL.
     *
     * @param context Kontekst aplikacji
     */
    @Test
    void savesUpdatesAndDeletesWithoutDuplicates(final ApplicationContext context) {
        final FloorPlans plans = context.getBean(FloorPlans.class);
        final JdbcClient jdbc = context.getBean(JdbcClient.class);
        final String target = "T-1";
        final String other = "T-2";
        final String zero = "0";
        final String hall = "Korytarz";
        final String func = "Sala";
        final String small = JdbcFloorPlansTest.poly("17.001");
        final String large = JdbcFloorPlansTest.poly("17.002");
        final String wkt = "MULTIPOLYGON(((17 51, 17 51.001, 17.001 51.001, 17.001 51, 17 51)))";
        new SampleBuilding(target, target, "A").wkt(jdbc, wkt);
        new SampleBuilding(other, other, "A").wkt(jdbc, wkt);
        plans.save(
            other,
            List.of(new PlanFloor(zero, "P", 0)),
            List.of(new PlanRoom(zero, "10", func, small))
        );
        final List<PlanFloor> floors = List.of(
            new PlanFloor(zero, "Parter", 0), new PlanFloor("1", "P1", 1)
        );
        final List<PlanRoom> rooms = List.of(
            new PlanRoom(zero, " 101 ", func, small), new PlanRoom(zero, "", hall, small),
            new PlanRoom(zero, null, hall, large), new PlanRoom("1", "20", func, small)
        );
        plans.save(target, floors, rooms);
        plans.save(target, floors, rooms);
        final String geomsql =
            "SELECT ST_AsText(outline) FROM rooms WHERE building_symbol = ? AND number = ?";
        final String before = jdbc.sql(geomsql).params(target, "101").query(String.class).single();
        final String updated = "Parter nowy";
        plans.save(
            target,
            List.of(new PlanFloor(zero, updated, 1)),
            List.of(
                new PlanRoom(zero, "101", func, large),
                new PlanRoom(zero, "   ", hall, small),
                new PlanRoom(zero, null, hall, large)
            )
        );
        final List<String> numbers = jdbc.sql("SELECT number FROM rooms WHERE building_symbol = ?")
            .param(target).query((rows, idx) -> rows.getString(1)).list();
        final List<String> labels = jdbc.sql("SELECT label FROM floors WHERE building_symbol = ?")
            .param(target).query(String.class).list();
        final String after = jdbc.sql(geomsql).params(target, "101").query(String.class).single();
        final List<String> kept = jdbc.sql("SELECT number FROM rooms WHERE building_symbol = ?")
            .param(other).query(String.class).list();
        Assertions.assertAll(
            () -> MatcherAssert.assertThat(
                "Poprawne numery bez duplikatów",
                numbers,
                Matchers.containsInAnyOrder("101", null, null)
            ),
            () -> MatcherAssert.assertThat("Nowe piętro", labels, Matchers.contains(updated)),
            () -> MatcherAssert.assertThat("Zaktualizowany kontur", after, Matchers.not(before)),
            () -> MatcherAssert.assertThat("Osobny budynek", kept, Matchers.contains("10"))
        );
    }

    private static String poly(final String lon) {
        return """
            {"type":"Polygon","coordinates":[[[17,51],[17,51.001],[%s,51.001],[%s,51],[17,51]]]}"""
            .formatted(lon, lon);
    }
}
