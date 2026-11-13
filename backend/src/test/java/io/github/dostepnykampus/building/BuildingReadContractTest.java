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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Kontrakt odczytu budynku: pełne metadane wejść, wejście główne
 * przed pozostałymi i próg podpowiedzi 50 m.
 *
 * <p>Geometria przy równiku jest niezależna od danych kampusu. Punkty
 * oddalone o 49 i 51 m od północnej krawędzi powstają przez ST_Project;
 * margines jednego metra nie zależy od zaokrąglenia na granicy 50 m.</p>
 *
 * <p>Wymagania: US17.3, US2.1, BR-G4</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
@Transactional
final class BuildingReadContractTest {

    @BeforeEach
    void insertBuilding(final WebApplicationContext context) {
        new SampleBuilding("G-900", "Budynek próbny", "BASIC").wkt(
            context.getBean(JdbcClient.class), "POLYGON((0 0,0.001 0,0.001 0.001,0 0.001,0 0))"
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
            BuildingReadContractTest.get(context, "/buildings/G-900").required("entrances"),
            Matchers.equalTo(
                context.getBean(ObjectMapper.class)
                    .readTree(BuildingReadContractTest.entrances())
            )
        );
    }

    @Test
    void brGFourSuggestsBuildingFortyNineMetersFromOutline(
        final WebApplicationContext context
    ) throws Exception {
        MatcherAssert.assertThat(
            "Punkt 49 m od konturu nie dostał podpowiedzi bez przypisania do wnętrza",
            BuildingReadContractTest.locate(context, 49),
            Matchers.equalTo(
                context.getBean(ObjectMapper.class).readTree(
                    "{\"buildingSymbol\":\"G-900\",\"insideOutline\":false}"
                )
            )
        );
    }

    @Test
    void brGFourLeavesPointFiftyOneMetersFromOutlineWithoutSuggestion(
        final WebApplicationContext context
    ) throws Exception {
        MatcherAssert.assertThat(
            "Punkt 51 m od konturu dostał podpowiedź spoza promienia 50 m",
            BuildingReadContractTest.locate(context, 51),
            Matchers.equalTo(
                context.getBean(ObjectMapper.class).readTree("{\"insideOutline\":false}")
            )
        );
    }

    private static JsonNode locate(final WebApplicationContext context, final int meters)
        throws Exception {
        return BuildingReadContractTest.get(
            context, "/buildings/locate?lat=%s&lon=0.0005"
                .formatted(BuildingReadContractTest.latitude(context, meters))
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
