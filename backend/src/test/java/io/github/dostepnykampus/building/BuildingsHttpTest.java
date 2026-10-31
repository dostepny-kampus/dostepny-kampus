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
import org.springframework.mock.web.MockHttpServletResponse;
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

    @Test
    void usSixTwoLocatesPointInsideOutline(final WebApplicationContext context)
        throws Exception {
        final ObjectMapper mapper = context.getBean(ObjectMapper.class);
        BuildingsHttpTest.insert(
            context.getBean(JdbcClient.class), "A-1", "BASIC", BuildingsHttpTest.yard()
        );
        MatcherAssert.assertThat(
            "Punkt w konturze nie wskazuje tego budynku",
            mapper.readTree(BuildingsHttpTest.locate(context, "51.1105", "17.0605")),
            Matchers.equalTo(mapper.readTree(BuildingsHttpTest.inside()))
        );
    }

    @Test
    void usSixTwoSuggestsNearestBuilding(final WebApplicationContext context)
        throws Exception {
        final ObjectMapper mapper = context.getBean(ObjectMapper.class);
        BuildingsHttpTest.insert(
            context.getBean(JdbcClient.class), "A-1", "BASIC", BuildingsHttpTest.yard()
        );
        MatcherAssert.assertThat(
            "Punkt tuż poza konturem nie podpowiada najbliższego budynku",
            mapper.readTree(BuildingsHttpTest.locate(context, "51.1113", "17.0605")),
            Matchers.equalTo(mapper.readTree(BuildingsHttpTest.nearby()))
        );
    }

    @Test
    void usSixTwoLeavesFarPointWithoutBuilding(final WebApplicationContext context)
        throws Exception {
        final ObjectMapper mapper = context.getBean(ObjectMapper.class);
        BuildingsHttpTest.insert(
            context.getBean(JdbcClient.class), "A-1", "BASIC", BuildingsHttpTest.yard()
        );
        MatcherAssert.assertThat(
            "Punkt daleko od budynków dostał podpowiedź budynku",
            mapper.readTree(BuildingsHttpTest.locate(context, "51.113", "17.0605")),
            Matchers.equalTo(mapper.readTree(BuildingsHttpTest.away()))
        );
    }

    @Test
    void usSixTwoRejectsLatitudeAboveRange(final WebApplicationContext context)
        throws Exception {
        MatcherAssert.assertThat(
            "Szerokość powyżej 90 stopni nie jest błędem 400",
            BuildingsHttpTest.failure(context, "/buildings/locate?lat=91&lon=17"),
            Matchers.startsWith("400 application/problem+json")
        );
    }

    @Test
    void usSixTwoRejectsLatitudeBelowRange(final WebApplicationContext context)
        throws Exception {
        MatcherAssert.assertThat(
            "Szerokość poniżej -90 stopni nie jest błędem 400",
            BuildingsHttpTest.failure(context, "/buildings/locate?lat=-91&lon=17"),
            Matchers.startsWith("400 application/problem+json")
        );
    }

    @Test
    void usSixTwoRejectsLongitudeAboveRange(final WebApplicationContext context)
        throws Exception {
        MatcherAssert.assertThat(
            "Długość powyżej 180 stopni nie jest błędem 400",
            BuildingsHttpTest.failure(context, "/buildings/locate?lat=51&lon=181"),
            Matchers.startsWith("400 application/problem+json")
        );
    }

    @Test
    void usSixTwoRejectsLongitudeBelowRange(final WebApplicationContext context)
        throws Exception {
        MatcherAssert.assertThat(
            "Długość poniżej -180 stopni nie jest błędem 400",
            BuildingsHttpTest.failure(context, "/buildings/locate?lat=51&lon=-181"),
            Matchers.startsWith("400 application/problem+json")
        );
    }

    @Test
    void usSixTwoRejectsNonFiniteLatitude(final WebApplicationContext context)
        throws Exception {
        MatcherAssert.assertThat(
            "Szerokość NaN nie jest błędem 400",
            BuildingsHttpTest.failure(context, "/buildings/locate?lat=NaN&lon=17"),
            Matchers.startsWith("400 application/problem+json")
        );
    }

    @Test
    void usSixTwoRejectsNonFiniteLongitude(final WebApplicationContext context)
        throws Exception {
        MatcherAssert.assertThat(
            "Długość NaN nie jest błędem 400",
            BuildingsHttpTest.failure(context, "/buildings/locate?lat=51&lon=NaN"),
            Matchers.startsWith("400 application/problem+json")
        );
    }

    @Test
    void usSixTwoRejectsTextInsteadOfLatitude(final WebApplicationContext context)
        throws Exception {
        MatcherAssert.assertThat(
            "Tekst zamiast szerokości nie jest błędem 400",
            BuildingsHttpTest.failure(context, "/buildings/locate?lat=abc&lon=17"),
            Matchers.startsWith("400 application/problem+json")
        );
    }

    @Test
    void usSixTwoRejectsMissingLongitude(final WebApplicationContext context)
        throws Exception {
        MatcherAssert.assertThat(
            "Brak długości nie jest błędem 400",
            BuildingsHttpTest.failure(context, "/buildings/locate?lat=51"),
            Matchers.startsWith("400 application/problem+json")
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

    private static String locate(
        final WebApplicationContext context,
        final String lat,
        final String lon
    ) throws Exception {
        return MockMvcBuilders.webAppContextSetup(context).build().perform(
            MockMvcRequestBuilders.get("/buildings/locate").param("lat", lat)
                .param("lon", lon)
        ).andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    }

    private static String failure(final WebApplicationContext context, final String path)
        throws Exception {
        final MockHttpServletResponse reply =
            MockMvcBuilders.webAppContextSetup(context).build()
                .perform(MockMvcRequestBuilders.get(path))
                .andReturn()
                .getResponse();
        return "%d %s".formatted(reply.getStatus(), reply.getContentType());
    }

    private static String yard() {
        return String.join(
            "",
            "{\"type\":\"MultiPolygon\",\"coordinates\":[[[",
            "[17.06,51.11],[17.061,51.11],[17.061,51.111],",
            "[17.06,51.111],[17.06,51.11]]]]}"
        );
    }

    private static String inside() {
        return "{\"buildingSymbol\":\"A-1\",\"insideOutline\":true}";
    }

    private static String nearby() {
        return "{\"buildingSymbol\":\"A-1\",\"insideOutline\":false}";
    }

    private static String away() {
        return "{\"insideOutline\":false}";
    }
}
