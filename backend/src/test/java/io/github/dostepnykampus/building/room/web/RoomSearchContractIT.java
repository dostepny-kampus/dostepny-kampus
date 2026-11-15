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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

/**
 * Operacja searchRooms zgodna ze schematem RoomSearchResults kontraktu API.
 *
 * <p>Wymagania: US18.1, NFR-40</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
@Transactional
final class RoomSearchContractIT {

    @ParameterizedTest
    @ValueSource(strings = {"G-981 Q71", "g981 q71"})
    void findsRoomByBuildingAndNumber(final String text, final WebApplicationContext context)
        throws Exception {
        final ObjectMapper mapper = context.getBean(ObjectMapper.class);
        MatcherAssert.assertThat(
            "Odpowiedź wyszukiwania sali nie zgadza się ze schematem RoomSearchResults",
            mapper.readTree(
                RoomSearchContractIT.reply(context, text).getContentAsString(StandardCharsets.UTF_8)
            ),
            Matchers.equalTo(
                mapper.readTree(
                    """
                    {"source":"Politechnika Wrocławska","results":[{"buildingSymbol":"G-981",
                    "floor":{"id":"P1","label":"1 piętro","order":1},
                    "room":{"id":"7","number":"Q71","function":"Sala","category":"CLASSROOM",
                    "geometry":{"type":"MultiPolygon","coordinates":[[[[0,0],[1,0],[1,1],[0,0]]]]},
                    "labelPoint":{"type":"Point","coordinates":[0.5,0.25]},"labelled":true}}]}
                    """
                )
            )
        );
    }

    @Test
    void givesEmptyListWhenNothingMatches(final WebApplicationContext context) throws Exception {
        final MockHttpServletResponse reply = RoomSearchContractIT.reply(context, "G-981 Q79");
        MatcherAssert.assertThat(
            "Wyszukiwanie bez pasującej sali nie dało 200 z pustą listą",
            String.format(
                "%d %s", reply.getStatus(),
                context.getBean(ObjectMapper.class)
                    .readTree(reply.getContentAsString(StandardCharsets.UTF_8)).path("results")
            ),
            Matchers.equalTo("200 []")
        );
    }

    @Test
    void rejectsQueryWithoutRoomNumber(final WebApplicationContext context) throws Exception {
        final MockHttpServletResponse reply = RoomSearchContractIT.reply(context, "A-1 329 330");
        MatcherAssert.assertThat(
            "Zły parametr q nie daje 400 application/problem+json",
            String.format("%d %s", reply.getStatus(), reply.getContentType()),
            Matchers.equalTo("400 application/problem+json")
        );
    }

    private static MockHttpServletResponse reply(
        final WebApplicationContext context, final String text
    ) throws Exception {
        RoomSearchContractIT.insertPlan(context);
        return context.getBean(MockMvc.class)
            .perform(MockMvcRequestBuilders.get("/rooms").param("q", text))
            .andReturn().getResponse();
    }

    private static void insertPlan(final WebApplicationContext context) {
        final JdbcClient jdbc = context.getBean(JdbcClient.class);
        new SampleBuilding("G-981", "Budynek z salą", "DETAILED")
            .wkt(jdbc, "POLYGON((0 0,0.001 0,0.001 0.001,0 0.001,0 0))");
        jdbc.sql(
            """
            INSERT INTO floors (building_symbol, id, label, sort_order) VALUES
                ('G-981', 'P1', '1 piętro', 1);
            INSERT INTO rooms (
                building_symbol, floor_id, id, number, function, category, outline,
                label_point, labelled
            ) VALUES
                ('G-981', 'P1', '7', 'Q71', 'Sala', 'CLASSROOM',
                    'SRID=4326;MULTIPOLYGON(((0 0,1 0,1 1,0 0)))', 'SRID=4326;POINT(0.5 0.25)',
                    TRUE)
            """
        ).update();
    }
}
