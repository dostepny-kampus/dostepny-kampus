/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building;

import io.github.dostepnykampus.IntegrationTest;
import io.github.dostepnykampus.SampleBuilding;
import java.nio.charset.StandardCharsets;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

/**
 * GET /buildings zwraca budynki z konturem GeoJSON,
 * posortowane według symbolu, z atrybucją wszystkich źródeł konturów,
 * albo pustą listę z pustą atrybucją.
 *
 * <p>Każdy test zaczyna od pustej tabeli, bez budynków z pliku konturów;
 * transakcja testu przywraca je po teście.</p>
 *
 * <p>Wymagania: US1, NFR-40</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
@Transactional
final class BuildingsHttpIT {

    @BeforeEach
    void clearBuildings(final WebApplicationContext context) {
        context.getBean(JdbcClient.class).sql("DELETE FROM entrances").update();
        context.getBean(JdbcClient.class).sql("DELETE FROM buildings").update();
    }

    @Test
    void usOneListsBuildingsBySymbol(final WebApplicationContext context)
        throws Exception {
        final JdbcClient jdbc = context.getBean(JdbcClient.class);
        final ObjectMapper mapper = context.getBean(ObjectMapper.class);
        BuildingsHttpIT.insert(jdbc, "C-13", "DETAILED", BuildingsHttpIT.east());
        BuildingsHttpIT.insert(jdbc, "A-1", "BASIC", BuildingsHttpIT.west());
        MatcherAssert.assertThat(
            "Lista budynków nie jest posortowana według symbolu albo kontur nie jest GeoJSON",
            mapper.readTree(BuildingsHttpIT.body(context)),
            Matchers.equalTo(mapper.readTree(BuildingsHttpIT.listed()))
        );
    }

    @Test
    void usOneOmitsNameOfUnnamedBuildingInList(final WebApplicationContext context)
        throws Exception {
        final JdbcClient jdbc = context.getBean(JdbcClient.class);
        final ObjectMapper mapper = context.getBean(ObjectMapper.class);
        BuildingsHttpIT.insert(jdbc, "A-1", "BASIC", BuildingsHttpIT.west());
        BuildingsHttpIT.insertUnnamed(jdbc, "C-13", "DETAILED", BuildingsHttpIT.east());
        MatcherAssert.assertThat(
            "Budynek bez nazwy ma w liście pole name, a budynek z nazwą go nie ma",
            mapper.readTree(BuildingsHttpIT.body(context)),
            Matchers.equalTo(mapper.readTree(BuildingsHttpIT.listedUnnamed()))
        );
    }

    @Test
    void usTwoOmitsNameOfUnnamedBuildingInCard(final WebApplicationContext context)
        throws Exception {
        final ObjectMapper mapper = context.getBean(ObjectMapper.class);
        BuildingsHttpIT.insertUnnamed(
            context.getBean(JdbcClient.class), "A-1", "BASIC", BuildingsHttpIT.west()
        );
        MatcherAssert.assertThat(
            "Karta budynku bez nazwy ma pole name",
            mapper.readTree(BuildingsHttpIT.card(context, "A-1")),
            Matchers.equalTo(mapper.readTree(BuildingsHttpIT.cardUnnamed()))
        );
    }

    @Test
    void usOneListsNoBuildingsWhenTableIsEmpty(final WebApplicationContext context)
        throws Exception {
        final ObjectMapper mapper = context.getBean(ObjectMapper.class);
        MatcherAssert.assertThat(
            "Pusta tabela budynków nie daje pustej listy z pustą atrybucją",
            mapper.readTree(BuildingsHttpIT.body(context)),
            Matchers.equalTo(mapper.readTree(BuildingsHttpIT.empty()))
        );
    }

    @Test
    void attributesEverySourceOfList(final WebApplicationContext context)
        throws Exception {
        final JdbcClient jdbc = context.getBean(JdbcClient.class);
        BuildingsHttpIT.insert(jdbc, "A-1", "BASIC", BuildingsHttpIT.west());
        BuildingsHttpIT.insert(jdbc, "C-13", "DETAILED", BuildingsHttpIT.east());
        BuildingsHttpIT.insert(jdbc, "C-14", "BASIC", BuildingsHttpIT.west());
        jdbc.sql("UPDATE buildings SET source = '© Politechnika Wrocławska' WHERE symbol = 'A-1'")
            .update();
        MatcherAssert.assertThat(
            "Atrybucja listy pomija któreś źródło konturów albo je powtarza",
            context.getBean(ObjectMapper.class)
                .readTree(BuildingsHttpIT.body(context))
                .get("source").asString(),
            Matchers.equalTo("© OpenStreetMap contributors; © Politechnika Wrocławska")
        );
    }

    @Test
    void usTwoReturnsBuildingCardWithoutEntrances(final WebApplicationContext context)
        throws Exception {
        final ObjectMapper mapper = context.getBean(ObjectMapper.class);
        BuildingsHttpIT.insert(
            context.getBean(JdbcClient.class), "A-1", "BASIC", BuildingsHttpIT.west()
        );
        MatcherAssert.assertThat(
            "Karta budynku nie ma konturu albo nie ma pustej listy wejść",
            mapper.readTree(BuildingsHttpIT.card(context, "A-1")),
            Matchers.equalTo(mapper.readTree(BuildingsHttpIT.cardBody()))
        );
    }

    @Test
    void usTwoReturnsNotFoundForUnknownSymbol(final WebApplicationContext context)
        throws Exception {
        MatcherAssert.assertThat(
            "Nieznany symbol budynku nie jest błędem 404",
            BuildingsHttpIT.failure(context, "/buildings/C-13"),
            Matchers.startsWith("404 application/problem+json")
        );
    }

    @Test
    void usSixTwoLocatesPointInsideOutline(final WebApplicationContext context)
        throws Exception {
        final ObjectMapper mapper = context.getBean(ObjectMapper.class);
        BuildingsHttpIT.insert(
            context.getBean(JdbcClient.class), "A-1", "BASIC", BuildingsHttpIT.yard()
        );
        MatcherAssert.assertThat(
            "Punkt w konturze nie wskazuje tego budynku",
            mapper.readTree(BuildingsHttpIT.locate(context, "51.1105", "17.0605")),
            Matchers.equalTo(mapper.readTree(BuildingsHttpIT.inside()))
        );
    }

    @Test
    void usSixTwoSuggestsNearestBuilding(final WebApplicationContext context)
        throws Exception {
        final ObjectMapper mapper = context.getBean(ObjectMapper.class);
        BuildingsHttpIT.insert(
            context.getBean(JdbcClient.class), "A-1", "BASIC", BuildingsHttpIT.yard()
        );
        MatcherAssert.assertThat(
            "Punkt tuż poza konturem nie podpowiada najbliższego budynku",
            mapper.readTree(BuildingsHttpIT.locate(context, "51.1113", "17.0605")),
            Matchers.equalTo(mapper.readTree(BuildingsHttpIT.nearby()))
        );
    }

    @Test
    void usSixTwoLeavesFarPointWithoutBuilding(final WebApplicationContext context)
        throws Exception {
        final ObjectMapper mapper = context.getBean(ObjectMapper.class);
        BuildingsHttpIT.insert(
            context.getBean(JdbcClient.class), "A-1", "BASIC", BuildingsHttpIT.yard()
        );
        MatcherAssert.assertThat(
            "Punkt daleko od budynków dostał podpowiedź budynku",
            mapper.readTree(BuildingsHttpIT.locate(context, "51.113", "17.0605")),
            Matchers.equalTo(mapper.readTree(BuildingsHttpIT.away()))
        );
    }

    @Test
    void usSixTwoDescribesPointOutsideRange(final WebApplicationContext context)
        throws Exception {
        MatcherAssert.assertThat(
            "Odpowiedź 400 podpowiedzi budynku nie ma opisu w detail",
            BuildingsHttpIT.detail(context, "/buildings/locate?lat=91&lon=17"),
            Matchers.not(Matchers.emptyOrNullString())
        );
    }

    @Test
    void usTwoDescribesMissingBuilding(final WebApplicationContext context)
        throws Exception {
        MatcherAssert.assertThat(
            "Odpowiedź 404 karty budynku nie ma opisu w detail",
            BuildingsHttpIT.detail(context, "/buildings/Z-404"),
            Matchers.containsString("Z-404")
        );
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(
        delimiter = ';',
        value = {
            "Szerokość powyżej 90 stopni nie jest błędem 400; /buildings/locate?lat=91&lon=17",
            "Szerokość poniżej -90 stopni nie jest błędem 400; /buildings/locate?lat=-91&lon=17",
            "Długość powyżej 180 stopni nie jest błędem 400; /buildings/locate?lat=51&lon=181",
            "Długość poniżej -180 stopni nie jest błędem 400; /buildings/locate?lat=51&lon=-181",
            "Szerokość NaN nie jest błędem 400; /buildings/locate?lat=NaN&lon=17",
            "Długość NaN nie jest błędem 400; /buildings/locate?lat=51&lon=NaN",
            "Tekst zamiast szerokości nie jest błędem 400; /buildings/locate?lat=abc&lon=17",
            "Brak długości nie jest błędem 400; /buildings/locate?lat=51"
        }
    )
    void usSixTwoRejectsInvalidCoordinates(
        final String message, final String url, final WebApplicationContext context
    ) throws Exception {
        MatcherAssert.assertThat(
            message,
            BuildingsHttpIT.failure(context, url),
            Matchers.startsWith("400 application/problem+json")
        );
    }

    private static String body(final WebApplicationContext context) throws Exception {
        return context.getBean(MockMvc.class)
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
        new SampleBuilding(symbol, "Budynek %s".formatted(symbol), level).geoJson(jdbc, outline);
    }

    private static void insertUnnamed(
        final JdbcClient jdbc,
        final String symbol,
        final String level,
        final String outline
    ) {
        new SampleBuilding(symbol, null, level).geoJson(jdbc, outline);
    }

    private static String listedUnnamed() {
        return """
            {"source":"© OpenStreetMap contributors","buildings":[
            {"symbol":"A-1","name":"Budynek A-1","detailLevel":"BASIC","outline":%s},
            {"symbol":"C-13","detailLevel":"DETAILED","outline":%s}
            ]}
            """.formatted(BuildingsHttpIT.west(), BuildingsHttpIT.east());
    }

    private static String cardUnnamed() {
        return String.join(
            "",
            "{\"symbol\":\"A-1\",\"detailLevel\":\"BASIC\",",
            "\"outline\":%s,\"entrances\":[]}"
        ).formatted(BuildingsHttpIT.west());
    }

    private static String listed() {
        return """
            {"source":"© OpenStreetMap contributors","buildings":[
            {"symbol":"A-1","name":"Budynek A-1","detailLevel":"BASIC","outline":%s},
            {"symbol":"C-13","name":"Budynek C-13","detailLevel":"DETAILED","outline":%s}
            ]}
            """.formatted(BuildingsHttpIT.west(), BuildingsHttpIT.east());
    }

    private static String empty() {
        return """
            {"source":"","buildings":[]}
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
        return context.getBean(MockMvc.class).perform(
            MockMvcRequestBuilders.get("/buildings/locate").param("lat", lat)
                .param("lon", lon)
        ).andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    }

    private static String failure(final WebApplicationContext context, final String path)
        throws Exception {
        final MockHttpServletResponse reply =
            context.getBean(MockMvc.class)
                .perform(MockMvcRequestBuilders.get(path))
                .andReturn()
                .getResponse();
        return "%d %s".formatted(reply.getStatus(), reply.getContentType());
    }

    private static String detail(final WebApplicationContext context, final String path)
        throws Exception {
        return context.getBean(ObjectMapper.class).readTree(
            context.getBean(MockMvc.class)
                .perform(MockMvcRequestBuilders.get(path))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8)
        ).path("detail").asString("");
    }

    private static String yard() {
        return String.join(
            "",
            "{\"type\":\"MultiPolygon\",\"coordinates\":[[[",
            "[17.06,51.11],[17.061,51.11],[17.061,51.111],",
            "[17.06,51.111],[17.06,51.11]]]]}"
        );
    }

    private static String card(final WebApplicationContext context, final String symbol)
        throws Exception {
        return context.getBean(MockMvc.class)
            .perform(MockMvcRequestBuilders.get("/buildings/{symbol}", symbol))
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    }

    private static String cardBody() {
        return String.join(
            "",
            "{\"symbol\":\"A-1\",\"name\":\"Budynek A-1\",",
            "\"detailLevel\":\"BASIC\",\"outline\":%s,\"entrances\":[]}"
        ).formatted(BuildingsHttpIT.west());
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
