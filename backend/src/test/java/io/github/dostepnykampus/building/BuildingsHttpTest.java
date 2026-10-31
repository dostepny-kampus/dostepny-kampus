/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building;

import io.github.dostepnykampus.PostgisTestConfiguration;
import java.nio.charset.StandardCharsets;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

/**
 * Testy US1: GET /buildings zwraca budynki z konturem GeoJSON,
 * posortowane według symbolu, albo pustą listę z atrybucją kontraktu.
 *
 * @since 0.1.0
 */
@SpringBootTest
@Import(PostgisTestConfiguration.class)
@Transactional
final class BuildingsHttpTest {

    @Test
    void usOneListsBuildingsBySymbol(final WebApplicationContext context)
        throws Exception {
        final JdbcClient jdbc = context.getBean(JdbcClient.class);
        final ObjectMapper mapper = context.getBean(ObjectMapper.class);
        BuildingsHttpTest.insert(jdbc, "C-13", "DETAILED", BuildingsHttpTest.east());
        BuildingsHttpTest.insert(jdbc, "A-1", "BASIC", BuildingsHttpTest.west());
        MatcherAssert.assertThat(
            "Lista budynków nie jest posortowana według symbolu albo kontur nie jest GeoJSON",
            mapper.readTree(BuildingsHttpTest.body(context)),
            Matchers.equalTo(mapper.readTree(BuildingsHttpTest.listed()))
        );
    }

    @Test
    void usOneListsNoBuildingsWhenTableIsEmpty(final WebApplicationContext context)
        throws Exception {
        final ObjectMapper mapper = context.getBean(ObjectMapper.class);
        MatcherAssert.assertThat(
            "Pusta tabela budynków nie daje listy z atrybucją OpenStreetMap",
            mapper.readTree(BuildingsHttpTest.body(context)),
            Matchers.equalTo(mapper.readTree(BuildingsHttpTest.empty()))
        );
    }

    private static String body(final WebApplicationContext context) throws Exception {
        return MockMvcBuilders.webAppContextSetup(context).build()
            .perform(MockMvcRequestBuilders.get("/buildings"))
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    }

    private static void insert(
        final JdbcClient jdbc,
        final String symbol,
        final String level,
        final String outline
    ) {
        jdbc.sql(
            """
            INSERT INTO buildings (symbol, name, detail_level, outline, source)
            VALUES (:symbol, :name, :level,
                ST_SetSRID(ST_GeomFromGeoJSON(:outline), 4326),
                :source)
            """
        ).param("symbol", symbol)
            .param("name", "Budynek %s".formatted(symbol))
            .param("level", level)
            .param("outline", outline)
            .param("source", "© OpenStreetMap contributors")
            .update();
    }

    private static String listed() {
        return """
            {"source":"© OpenStreetMap contributors","buildings":[
            {"symbol":"A-1","name":"Budynek A-1","detailLevel":"BASIC","outline":%s},
            {"symbol":"C-13","name":"Budynek C-13","detailLevel":"DETAILED","outline":%s}
            ]}
            """.formatted(BuildingsHttpTest.west(), BuildingsHttpTest.east());
    }

    private static String empty() {
        return """
            {"source":"© OpenStreetMap contributors","buildings":[]}
            """;
    }

    private static String west() {
        return String.join(
            "",
            "{\"type\":\"MultiPolygon\",\"coordinates\":[[[",
            "[17.0583,51.1092],[17.0591,51.1092],[17.0591,51.1097],",
            "[17.0583,51.1097],[17.0583,51.1092]]]]}"
        );
    }

    private static String east() {
        return String.join(
            "",
            "{\"type\":\"MultiPolygon\",\"coordinates\":[[[",
            "[17.0613,51.1072],[17.0621,51.1072],[17.0621,51.1077],",
            "[17.0613,51.1077],[17.0613,51.1072]]]]}"
        );
    }
}
