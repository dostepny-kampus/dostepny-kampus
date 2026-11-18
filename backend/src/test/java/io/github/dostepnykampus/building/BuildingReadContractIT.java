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
 * przed pozostałymi, dane wejść z planów PWr, próg podpowiedzi 50 m i piętra budynku.
 *
 * <p>Geometria przy równiku jest niezależna od danych kampusu. Punkty
 * oddalone o 49 i 51 m od północnej krawędzi powstają przez ST_Project;
 * margines jednego metra nie zależy od zaokrąglenia na granicy 50 m.</p>
 *
 * <p>Wymagania: US17.3, US2.1, BR-G4, US16.1, US16.2, NFR-40, BR-24, BR-27</p>
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
    void showsMetadataWithMainEntranceFirst(
        final WebApplicationContext context
    ) throws Exception {
        context.getBean(JdbcClient.class).sql(
            """
            INSERT INTO entrances (id, building_symbol, name, type, main, location,
                floor_id, marking, marking_date, marking_source)
            VALUES ('node/1', 'G-900', NULL, NULL, false,
                ST_SetSRID(ST_MakePoint(0, 0.0005), 4326), NULL,
                'NO_DATA', DATE '2026-10-01', 'IMPORT'),
                ('node/2', 'G-900', 'Wejście główne', 'main', true,
                ST_SetSRID(ST_MakePoint(0.0005, 0), 4326), 'K1.G900.P0',
                'ACCESSIBLE', DATE '2026-10-02', 'WALKTHROUGH'),
                ('node/3', 'G-900', 'Wejście boczne', 'service', false,
                ST_SetSRID(ST_MakePoint(0.001, 0.0005), 4326), NULL,
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
    void showsPwrEntranceDataWithWalkthroughMarkingFirst(
        final WebApplicationContext context
    ) throws Exception {
        BuildingReadContractIT.insertPwrEntrances(context);
        MatcherAssert.assertThat(
            "Dane wejść z planów PWr albo pierwszeństwo oznaczenia nie zgadzają się z kontraktem",
            BuildingReadContractIT.get(context, "/buildings/G-900").required("entrances"),
            Matchers.equalTo(
                context.getBean(ObjectMapper.class)
                    .readTree(BuildingReadContractIT.pwrEntrances())
            )
        );
    }

    @Test
    void showsPwrMarkingDateUnlessWalkthrough(final WebApplicationContext context)
        throws Exception {
        context.getBean(JdbcClient.class).sql(
            """
            INSERT INTO entrances (id, building_symbol, main, location, marking, marking_date,
                marking_source, pwr_name, pwr_marking, pwr_marking_date)
            VALUES ('node/8', 'G-900', false, ST_SetSRID(ST_MakePoint(0.0005, 0), 4326),
                'NO_DATA', DATE '2026-11-01', 'IMPORT', 'Wejście', 'ACCESSIBLE',
                DATE '2026-11-04'),
                ('node/9', 'G-900', false, ST_SetSRID(ST_MakePoint(0, 0.0005), 4326),
                'INACCESSIBLE', DATE '2026-11-02', 'WALKTHROUGH', 'Wejście', 'ACCESSIBLE',
                DATE '2026-11-04')
            """
        ).update();
        final JsonNode card = BuildingReadContractIT.get(context, "/buildings/G-900");
        MatcherAssert.assertThat(
            "Data oznaczenia nie jest datą warstwy PWr albo datą obchodu",
            String.join(
                " ", card.at("/entrances/0/markingDate").asString(),
                card.at("/entrances/1/markingDate").asString()
            ),
            Matchers.equalTo("2026-11-04 2026-11-02")
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
    void listsBuildingFloorsOrderedWithPwrSource(
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
    void returnsEmptyFloorsForBuildingWithoutFloors(
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
    void returnsNotFoundForUnknownBuildingFloors(
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

    private static void insertPwrEntrances(final WebApplicationContext context) {
        final JdbcClient jdbc = context.getBean(JdbcClient.class);
        jdbc.sql(
            """
            INSERT INTO entrances (id, building_symbol, name, main, location, marking,
                marking_date, marking_source, pwr_name, pwr_marking, approach, door_width_cm)
            VALUES ('node/5', 'G-900', 'W-1', true, ST_SetSRID(ST_MakePoint(0.0005, 0), 4326),
                'NO_DATA', DATE '2026-11-01', 'IMPORT', 'Wejście Cesarskie', 'ACCESSIBLE',
                'Od ul. Wybrzeże.', 120),
                ('node/6', 'G-900', NULL, false, ST_SetSRID(ST_MakePoint(0, 0.0005), 4326),
                'INACCESSIBLE', DATE '2026-11-02', 'WALKTHROUGH', 'Wejście', 'ACCESSIBLE',
                NULL, NULL),
                ('node/7', 'G-900', 'Wejście boczne', false,
                ST_SetSRID(ST_MakePoint(0.001, 0.0005), 4326), 'NO_DATA', DATE '2026-11-01',
                'IMPORT', NULL, NULL, NULL, NULL),
                ('pwr/s00000001', 'G-900', 'Wejście', false,
                ST_SetSRID(ST_MakePoint(0.0005, 0.001), 4326), 'INACCESSIBLE',
                DATE '2026-11-01', 'IMPORT', 'Wejście', 'INACCESSIBLE',
                'Do wejścia prowadzą schody.', 90)
            """
        ).update();
    }

    private static String pwrEntrances() {
        return """
            [{"id":"node/5","buildingSymbol":"G-900","name":"Wejście Cesarskie","main":true,
            "location":{"type":"Point","coordinates":[0.0005,0]},
            "marking":"ACCESSIBLE","markingDate":"2026-11-01","markingSource":"IMPORT",
            "approach":"Od ul. Wybrzeże.","doorWidthCm":120,
            "source":"Politechnika Wrocławska"},
            {"id":"node/6","buildingSymbol":"G-900","name":"Wejście","main":false,
            "location":{"type":"Point","coordinates":[0,0.0005]},
            "marking":"INACCESSIBLE","markingDate":"2026-11-02",
            "markingSource":"WALKTHROUGH","source":"Politechnika Wrocławska"},
            {"id":"node/7","buildingSymbol":"G-900","name":"Wejście boczne","main":false,
            "location":{"type":"Point","coordinates":[0.001,0.0005]},
            "marking":"NO_DATA","markingDate":"2026-11-01","markingSource":"IMPORT"},
            {"id":"pwr/s00000001","buildingSymbol":"G-900","name":"Wejście","main":false,
            "location":{"type":"Point","coordinates":[0.0005,0.001]},
            "marking":"INACCESSIBLE","markingDate":"2026-11-01","markingSource":"IMPORT",
            "approach":"Do wejścia prowadzą schody.","doorWidthCm":90,
            "source":"Politechnika Wrocławska"}]
            """;
    }

    private static String entrances() {
        return """
            [{"id":"node/2","buildingSymbol":"G-900","name":"Wejście główne",
            "type":"main","main":true,
            "location":{"type":"Point","coordinates":[0.0005,0]},"floorId":"K1.G900.P0",
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
