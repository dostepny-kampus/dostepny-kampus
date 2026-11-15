/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.room.web;

import io.github.dostepnykampus.IntegrationTest;
import io.github.dostepnykampus.SampleBuilding;
import java.nio.charset.StandardCharsets;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

/**
 * Operacja listFloorRooms zgodna ze schematem RoomList kontraktu API.
 *
 * <p>Wymagania: US16.1, NFR-40</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
@Transactional
final class FloorRoomsContractIT {

    @Test
    void listsRoomsOfFloorWithOutlineAndPwrSource(final WebApplicationContext context)
        throws Exception {
        final ObjectMapper mapper = context.getBean(ObjectMapper.class);
        MatcherAssert.assertThat(
            "Odpowiedź pomieszczeń piętra z windą nie zgadza się ze schematem RoomList",
            mapper.readTree(
                FloorRoomsContractIT.reply(context, "/buildings/G-960/floors/P0/rooms")
                    .getContentAsString(StandardCharsets.UTF_8)
            ),
            Matchers.equalTo(
                mapper.readTree(
                    """
                    {"source":"Politechnika Wrocławska","floorOutline":%1$s,
                    "rooms":[{"id":"1","number":"101","function":"Winda","category":"ELEVATOR",
                    "geometry":%1$s,"labelPoint":%2$s,"labelled":true,
                    "liftId":"P0/1","liftFloors":["P0","P1"]},
                    {"id":"2","function":"Korytarz","category":"CORRIDOR","geometry":%1$s,
                    "labelPoint":%2$s,"labelled":false}]}
                    """.formatted(
                        "{\"type\":\"MultiPolygon\",\"coordinates\":[[[[0,0],[1,0],[1,1],[0,0]]]]}",
                        "{\"type\":\"Point\",\"coordinates\":[0.5,0.25]}"
                    )
                )
            )
        );
    }

    @Test
    void returnsNotFoundForUnknownFloor(final WebApplicationContext context) throws Exception {
        final MockHttpServletResponse reply =
            FloorRoomsContractIT.reply(context, "/buildings/G-960/floors/P9/rooms");
        MatcherAssert.assertThat(
            "Nieznane piętro nie daje 404 application/problem+json z piętrem w detail",
            String.format(
                "%d %s %s", reply.getStatus(), reply.getContentType(),
                context.getBean(ObjectMapper.class)
                    .readTree(reply.getContentAsString(StandardCharsets.UTF_8))
                    .path("detail").asString("")
            ),
            Matchers.allOf(
                Matchers.startsWith("404 application/problem+json"),
                Matchers.containsString("P9")
            )
        );
    }

    private static MockHttpServletResponse reply(
        final WebApplicationContext context, final String path
    ) throws Exception {
        FloorRoomsContractIT.insertPlan(context);
        return context.getBean(MockMvc.class).perform(MockMvcRequestBuilders.get(path))
            .andReturn().getResponse();
    }

    private static void insertPlan(final WebApplicationContext context) {
        final JdbcClient jdbc = context.getBean(JdbcClient.class);
        new SampleBuilding("G-960", "Budynek z planem", "DETAILED")
            .wkt(jdbc, "POLYGON((0 0,0.001 0,0.001 0.001,0 0.001,0 0))");
        jdbc.sql(
            """
            INSERT INTO floors (building_symbol, id, label, sort_order, outline) VALUES
                ('G-960', 'P0', 'Parter', 0, 'SRID=4326;MULTIPOLYGON(((0 0,1 0,1 1,0 0)))'),
                ('G-960', 'P1', '1 piętro', 1, NULL);
            INSERT INTO rooms (
                building_symbol, floor_id, id, number, function, category, outline,
                label_point, labelled, lift_id
            ) VALUES
                ('G-960', 'P1', '9', NULL, 'Winda', 'ELEVATOR',
                    'SRID=4326;MULTIPOLYGON(((0 0,1 0,1 1,0 0)))', 'SRID=4326;POINT(0.5 0.25)',
                    FALSE, 'P0/1'),
                ('G-960', 'P0', '1', '101', 'Winda', 'ELEVATOR',
                    'SRID=4326;MULTIPOLYGON(((0 0,1 0,1 1,0 0)))', 'SRID=4326;POINT(0.5 0.25)',
                    TRUE, 'P0/1'),
                ('G-960', 'P0', '2', NULL, 'Korytarz', 'CORRIDOR',
                    'SRID=4326;MULTIPOLYGON(((0 0,1 0,1 1,0 0)))', 'SRID=4326;POINT(0.5 0.25)',
                    FALSE, NULL)
            """
        ).update();
    }
}
