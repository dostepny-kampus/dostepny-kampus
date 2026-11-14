/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Kontrakt odczytu budynku: pełne metadane wejść, wejście główne
 * przed pozostałymi, próg podpowiedzi 50 m i piętra budynku.
 *
 * <p>Geometria przy równiku jest niezależna od danych kampusu. Punkty
 * oddalone o 49 i 51 m od północnej krawędzi powstają przez ST_Project;
 * margines jednego metra nie zależy od zaokrąglenia na granicy 50 m.</p>
 *
 * <p>Wymagania: US17.3, US2.1, BR-G4, US16.1, US16.2, NFR-40</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
@Transactional
final class BuildingReadContractIT {

    /**
     * Przykładowy kontur budynku przy równiku w WKT.
     */
    private static final String OUTLINE = "POLYGON((0 0,0.001 0,0.001 0.001,0 0.001,0 0))";

    @BeforeEach
    void insertBuilding(final WebApplicationContext context) {
        new SampleBuilding("G-900", "Budynek próbny", "BASIC").wkt(
            context.getBean(JdbcClient.class), BuildingReadContractIT.OUTLINE
        );
    }

    @Test
    void usSeventeenShowsMetadataWithMainEntranceFirst(
        final WebApplicationContext context
    ) throws Exception {
        context.getBean(JdbcClient.class).sql(
            """
            INSERT INTO entrances (id, building_symbol, name, type, main, location,
                marking, marking_date, marking_source)
            VALUES ('node/1', 'G-900', NULL, NULL, false,
                ST_SetSRID(ST_MakePoint(0, 0.0005), 4326),
                'NO_DATA', DATE '2026-10-01', 'IMPORT'),
                ('node/2', 'G-900', 'Wejście główne', 'main', true,
                ST_SetSRID(ST_MakePoint(0.0005, 0), 4326),
                'ACCESSIBLE', DATE '2026-10-02', 'WALKTHROUGH'),
                ('node/3', 'G-900', 'Wejście boczne', 'service', false,
                ST_SetSRID(ST_MakePoint(0.001, 0.0005), 4326),
                'INACCESSIBLE', DATE '2026-10-03', 'WALKTHROUGH')
            """
        ).update();
        MatcherAssert.assertThat(
            "Metadane wejść albo kolejność wejścia głównego nie zgadzają się z kontraktem",
            BuildingReadContractIT.get(context, "/buildings/G-900").required("entrances"),
            Matchers.equalTo(
                context.getBean(ObjectMapper.class)
                    .readTree(BuildingReadContractIT.entrances())
            )
        );
    }

    @Test
    void suggestsBuildingFortyNineMetersFromOutline(
        final WebApplicationContext context
    ) throws Exception {
        MatcherAssert.assertThat(
            "Punkt 49 m od konturu nie dostał podpowiedzi bez przypisania do wnętrza",
            BuildingReadContractIT.locate(context, 49),
            Matchers.equalTo(
                context.getBean(ObjectMapper.class).readTree(
                    "{\"buildingSymbol\":\"G-900\",\"insideOutline\":false}"
                )
            )
        );
    }

    @Test
    void leavesPointFiftyOneMetersFromOutlineWithoutSuggestion(
        final WebApplicationContext context
    ) throws Exception {
        MatcherAssert.assertThat(
            "Punkt 51 m od konturu dostał podpowiedź spoza promienia 50 m",
            BuildingReadContractIT.locate(context, 51),
            Matchers.equalTo(
                context.getBean(ObjectMapper.class).readTree("{\"insideOutline\":false}")
            )
        );
    }

    @Test
    void usSixteenOneListsBuildingFloorsOrderedWithPwrSource(
        final WebApplicationContext context
    ) throws Exception {
        final JdbcClient jdbc = context.getBean(JdbcClient.class);
        new SampleBuilding("G-901", "Budynek szczegółowy", "DETAILED")
            .wkt(jdbc, BuildingReadContractIT.OUTLINE);
        new SampleBuilding("G-902", "Sąsiedni budynek", "DETAILED")
            .wkt(jdbc, BuildingReadContractIT.OUTLINE);
        jdbc.sql(
            """
            INSERT INTO floors (building_symbol, id, label, sort_order)
            VALUES ('G-901', 'f2', '2 piętro', 2),
                ('G-901', 'f-1', 'Poziom -1', -1),
                ('G-901', 'f1b', '1 piętro B', 1),
                ('G-901', 'f0', 'Parter', 0),
                ('G-901', 'f1a', '1 piętro A', 1),
                ('G-902', 'other', 'Parter G-902', 0)
            """
        ).update();
        MatcherAssert.assertThat(
            "Lista pięter, ich kolejność albo źródło nie zgadzają się z kontraktem",
            BuildingReadContractIT.get(context, "/buildings/G-901/floors"),
            Matchers.equalTo(
                context.getBean(ObjectMapper.class)
                    .readTree(BuildingReadContractIT.floors())
            )
        );
    }

    @Test
    void usSixteenTwoReturnsEmptyFloorsForBuildingWithoutFloors(
        final WebApplicationContext context
    ) throws Exception {
        MatcherAssert.assertThat(
            "Budynek bez pięter nie zwraca pustej listy pięter ze źródłem PWr",
            BuildingReadContractIT.get(context, "/buildings/G-900/floors"),
            Matchers.equalTo(
                context.getBean(ObjectMapper.class).readTree(
                    "{\"source\":\"Politechnika Wrocławska\",\"floors\":[]}"
                )
            )
        );
    }

    @Test
    void usSixteenReturnsNotFoundForUnknownBuildingFloors(
        final WebApplicationContext context
    ) throws Exception {
        final MockHttpServletResponse reply = context.getBean(MockMvc.class)
            .perform(MockMvcRequestBuilders.get("/buildings/Z-404/floors"))
            .andExpect(MockMvcResultMatchers.status().isNotFound())
            .andReturn()
            .getResponse();
        MatcherAssert.assertThat(
            "Nieznany symbol budynku nie daje 404 application/problem+json z symbolem w detail",
            "%s %s".formatted(
                reply.getContentType(),
                context.getBean(ObjectMapper.class)
                    .readTree(reply.getContentAsString(StandardCharsets.UTF_8))
                    .path("detail").asString("")
            ),
            Matchers.allOf(
                Matchers.startsWith("application/problem+json"),
                Matchers.containsString("Z-404")
            )
        );
    }

    private static JsonNode locate(final WebApplicationContext context, final int meters)
        throws Exception {
        return BuildingReadContractIT.get(
            context, "/buildings/locate?lat=%s&lon=0.0005"
                .formatted(BuildingReadContractIT.latitude(context, meters))
        );
    }

    private static double latitude(final WebApplicationContext context, final int meters) {
        return context.getBean(JdbcClient.class).sql(
            """
            SELECT ST_Y(ST_Project(
                ST_SetSRID(ST_MakePoint(0.0005, 0.001), 4326)::geography,
                :meters, 0)::geometry)
            """
        ).param("meters", meters).query(Double.class).single();
    }

    private static String floors() {
        return """
            {"source":"Politechnika Wrocławska","floors":[
            {"id":"f-1","label":"Poziom -1","order":-1},
            {"id":"f0","label":"Parter","order":0},
            {"id":"f1a","label":"1 piętro A","order":1},
            {"id":"f1b","label":"1 piętro B","order":1},
            {"id":"f2","label":"2 piętro","order":2}
            ]}
            """;
    }

    private static String entrances() {
        return """
            [{"id":"node/2","buildingSymbol":"G-900","name":"Wejście główne",
            "type":"main","main":true,
            "location":{"type":"Point","coordinates":[0.0005,0]},
            "marking":"ACCESSIBLE","markingDate":"2026-10-02",
            "markingSource":"WALKTHROUGH"},
            {"id":"node/1","buildingSymbol":"G-900","main":false,
            "location":{"type":"Point","coordinates":[0,0.0005]},
            "marking":"NO_DATA","markingDate":"2026-10-01",
            "markingSource":"IMPORT"},
            {"id":"node/3","buildingSymbol":"G-900","name":"Wejście boczne",
            "type":"service","main":false,
            "location":{"type":"Point","coordinates":[0.001,0.0005]},
            "marking":"INACCESSIBLE","markingDate":"2026-10-03",
            "markingSource":"WALKTHROUGH"}]
            """;
    }

    private static JsonNode get(final WebApplicationContext context, final String path)
        throws Exception {
        return context.getBean(ObjectMapper.class).readTree(
            context.getBean(MockMvc.class)
                .perform(MockMvcRequestBuilders.get(path))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8)
        );
    }
}
