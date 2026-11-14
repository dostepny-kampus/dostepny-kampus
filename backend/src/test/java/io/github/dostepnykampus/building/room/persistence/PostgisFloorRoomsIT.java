/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.room.persistence;

import io.github.dostepnykampus.IntegrationTest;
import io.github.dostepnykampus.SampleBuilding;
import io.github.dostepnykampus.building.room.FloorRooms;
import io.github.dostepnykampus.building.room.RoomList;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Pomieszczenia piętra z bazy z obrysem piętra.
 *
 * <p>Wymagania: US16.1, NFR-40</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
final class PostgisFloorRoomsIT {

    @Test
    void usSixteenReadsRoomsOfFloorWithOutline(final ApplicationContext context) {
        PostgisFloorRoomsIT.plan(context, "G-951");
        final RoomList list = context.getBean(FloorRooms.class).rooms("G-951", "P0").orElseThrow();
        MatcherAssert.assertThat(
            "Pomieszczenia piętra albo obrys piętra nie zgadzają się z bazą",
            String.join(
                " | ", list.source(), list.outline().substring(0, 22),
                list.rooms().stream().map(
                    room -> String.join(
                        " ", room.id(), String.valueOf(room.number()), room.function(),
                        room.category(), room.label().substring(0, 15),
                        String.valueOf(room.labelled())
                    )
                ).toList().toString()
            ),
            Matchers.equalTo(
                String.join(
                    " | ", "Politechnika Wrocławska", "{\"type\":\"MultiPolygon\"",
                    String.join(
                        "", "[1 101 Sala dydaktyczna CLASSROOM {\"type\":\"Point\" true, ",
                        "2 null Korytarz CORRIDOR {\"type\":\"Point\" false]"
                    )
                )
            )
        );
    }

    @Test
    void usSixteenGivesEmptyOutlineForFloorWithoutRooms(final ApplicationContext context) {
        PostgisFloorRoomsIT.plan(context, "G-952");
        MatcherAssert.assertThat(
            "Piętro bez pomieszczeń nie ma pustego obrysu i pustej listy",
            context.getBean(FloorRooms.class).rooms("G-952", "P1").map(
                list -> String.join(" ", list.outline(), list.rooms().toString())
            ).orElseThrow(),
            Matchers.equalTo("{\"type\":\"MultiPolygon\",\"coordinates\":[]} []")
        );
    }

    @Test
    void usSixteenGivesNothingForUnknownBuildingOrFloor(final ApplicationContext context) {
        PostgisFloorRoomsIT.plan(context, "G-953");
        final FloorRooms rooms = context.getBean(FloorRooms.class);
        MatcherAssert.assertThat(
            "Nieznany budynek albo piętro dało pomieszczenia",
            String.join(
                " ", String.valueOf(rooms.rooms("G-999", "P0").isPresent()),
                String.valueOf(rooms.rooms("G-953", "P9").isPresent())
            ),
            Matchers.equalTo("false false")
        );
    }

    private static void plan(final ApplicationContext context, final String symbol) {
        final JdbcClient jdbc = context.getBean(JdbcClient.class);
        new SampleBuilding(symbol, "Budynek z planem", "DETAILED")
            .wkt(jdbc, "POLYGON((17 51,17.001 51,17.001 51.001,17 51.001,17 51))");
        jdbc.sql(
            """
            INSERT INTO floors (building_symbol, id, label, sort_order, outline) VALUES
                (:symbol, 'P0', 'Parter', 0,
                    'SRID=4326;MULTIPOLYGON(((17 51,17.001 51,17.001 51.001,17 51)))'),
                (:symbol, 'P1', '1 piętro', 1, NULL)
            """
        ).param("symbol", symbol).update();
        jdbc.sql(
            """
            INSERT INTO rooms (
                building_symbol, floor_id, id, number, function, category, outline,
                label_point, labelled
            )
            SELECT :symbol, 'P0', id, number, function, category, shape,
                ST_PointOnSurface(shape), labelled
            FROM (VALUES
                ('2', NULL, 'Korytarz', 'CORRIDOR', FALSE),
                ('1', '101', 'Sala dydaktyczna', 'CLASSROOM', TRUE)
            ) AS room (id, number, function, category, labelled),
            LATERAL (
                SELECT 'SRID=4326;MULTIPOLYGON(((17 51,17.0005 51,17.0005 51.0005,17 51)))'
                    ::geometry AS shape
            ) AS form
            """
        ).param("symbol", symbol).update();
    }
}
