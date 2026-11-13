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
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

/**
 * Testy US1: GET /buildings zwraca budynki z konturem GeoJSON,
 * posortowane według symbolu, z atrybucją wszystkich źródeł konturów,
 * albo pustą listę z pustą atrybucją.
 *
 * <p>Każdy test zaczyna od pustej tabeli, bez budynków z pliku konturów;
 * transakcja testu przywraca je po teście.</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
@Transactional
final class BuildingsHttpTest {

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
        BuildingsHttpTest.insert(jdbc, "C-13", "DETAILED", BuildingsHttpTest.east());
        BuildingsHttpTest.insert(jdbc, "A-1", "BASIC", BuildingsHttpTest.west());
        MatcherAssert.assertThat(
            "Lista budynków nie jest posortowana według symbolu albo kontur nie jest GeoJSON",
            mapper.readTree(BuildingsHttpTest.body(context)),
            Matchers.equalTo(mapper.readTree(BuildingsHttpTest.listed()))
        );
    }

    @Test
    void usOneOmitsNameOfUnnamedBuildingInList(final WebApplicationContext context)
        throws Exception {
        final JdbcClient jdbc = context.getBean(JdbcClient.class);
        final ObjectMapper mapper = context.getBean(ObjectMapper.class);
        BuildingsHttpTest.insert(jdbc, "A-1", "BASIC", BuildingsHttpTest.west());
        BuildingsHttpTest.insertUnnamed(jdbc, "C-13", "DETAILED", BuildingsHttpTest.east());
        MatcherAssert.assertThat(
            "Budynek bez nazwy ma w liście pole name, a budynek z nazwą go nie ma",
            mapper.readTree(BuildingsHttpTest.body(context)),
            Matchers.equalTo(mapper.readTree(BuildingsHttpTest.listedUnnamed()))
        );
    }

    @Test
    void usTwoOmitsNameOfUnnamedBuildingInCard(final WebApplicationContext context)
        throws Exception {
        final ObjectMapper mapper = context.getBean(ObjectMapper.class);
        BuildingsHttpTest.insertUnnamed(
            context.getBean(JdbcClient.class), "A-1", "BASIC", BuildingsHttpTest.west()
        );
        MatcherAssert.assertThat(
            "Karta budynku bez nazwy ma pole name",
            mapper.readTree(BuildingsHttpTest.card(context, "A-1")),
            Matchers.equalTo(mapper.readTree(BuildingsHttpTest.cardUnnamed()))
        );
    }

    @Test
    void usOneListsNoBuildingsWhenTableIsEmpty(final WebApplicationContext context)
        throws Exception {
        final ObjectMapper mapper = context.getBean(ObjectMapper.class);
        MatcherAssert.assertThat(
            "Pusta tabela budynków nie daje pustej listy z pustą atrybucją",
            mapper.readTree(BuildingsHttpTest.body(context)),
            Matchers.equalTo(mapper.readTree(BuildingsHttpTest.empty()))
        );
    }

    @Test
    void nfrFortyAttributesEverySourceOfList(final WebApplicationContext context)
        throws Exception {
        final JdbcClient jdbc = context.getBean(JdbcClient.class);
        BuildingsHttpTest.insert(jdbc, "A-1", "BASIC", BuildingsHttpTest.west());
        BuildingsHttpTest.insert(jdbc, "C-13", "DETAILED", BuildingsHttpTest.east());
        BuildingsHttpTest.insert(jdbc, "C-14", "BASIC", BuildingsHttpTest.west());
        jdbc.sql("UPDATE buildings SET source = '© Politechnika Wrocławska' WHERE symbol = 'A-1'")
            .update();
        MatcherAssert.assertThat(
            "Atrybucja listy pomija któreś źródło konturów albo je powtarza",
            context.getBean(ObjectMapper.class)
                .readTree(BuildingsHttpTest.body(context))
                .get("source").asString(),
            Matchers.equalTo("© OpenStreetMap contributors; © Politechnika Wrocławska")
        );
    }

    @Test
    void usTwoReturnsBuildingCardWithoutEntrances(final WebApplicationContext context)
        throws Exception {
        final ObjectMapper mapper = context.getBean(ObjectMapper.class);
        BuildingsHttpTest.insert(
            context.getBean(JdbcClient.class), "A-1", "BASIC", BuildingsHttpTest.west()
        );
        MatcherAssert.assertThat(
            "Karta budynku nie ma konturu albo nie ma pustej listy wejść",
            mapper.readTree(BuildingsHttpTest.card(context, "A-1")),
            Matchers.equalTo(mapper.readTree(BuildingsHttpTest.cardBody()))
        );
    }

    @Test
    void usTwoReturnsNotFoundForUnknownSymbol(final WebApplicationContext context)
        throws Exception {
        MatcherAssert.assertThat(
            "Nieznany symbol budynku nie jest błędem 404",
            BuildingsHttpTest.failure(context, "/buildings/C-13"),
            Matchers.startsWith("404 application/problem+json")
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
    void usSixTwoDescribesPointOutsideRange(final WebApplicationContext context)
        throws Exception {
        MatcherAssert.assertThat(
            "Odpowiedź 400 podpowiedzi budynku nie ma opisu w detail",
            BuildingsHttpTest.detail(context, "/buildings/locate?lat=91&lon=17"),
            Matchers.not(Matchers.emptyOrNullString())
        );
    }

    @Test
    void usTwoDescribesMissingBuilding(final WebApplicationContext context)
        throws Exception {
        MatcherAssert.assertThat(
            "Odpowiedź 404 karty budynku nie ma opisu w detail",
            BuildingsHttpTest.detail(context, "/buildings/Z-404"),
            Matchers.containsString("Z-404")
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
            """.formatted(BuildingsHttpTest.west(), BuildingsHttpTest.east());
    }

    private static String cardUnnamed() {
        return String.join(
            "",
            "{\"symbol\":\"A-1\",\"detailLevel\":\"BASIC\",",
            "\"outline\":%s,\"entrances\":[]}"
        ).formatted(BuildingsHttpTest.west());
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
        ).formatted(BuildingsHttpTest.west());
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
