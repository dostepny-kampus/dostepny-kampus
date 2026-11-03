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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Kontrakt odczytu budynku: pełne metadane wejść (US17.3), wejście główne
 * przed pozostałymi (US2.1) i próg podpowiedzi 50 m (BR-G4).
 *
 * <p>Geometria przy równiku jest niezależna od danych kampusu. Punkty
 * oddalone o 49 i 51 m od północnej krawędzi powstają przez ST_Project;
 * margines jednego metra nie zależy od zaokrąglenia na granicy 50 m.</p>
 *
 * @since 0.1.0
 */
@SpringBootTest
@Import(PostgisTestConfiguration.class)
@Transactional
final class BuildingReadContractTest {

    @BeforeEach
    void insertBuilding(final WebApplicationContext context) {
        context.getBean(JdbcClient.class).sql(
            """
            INSERT INTO buildings (symbol, name, detail_level, outline, source)
            VALUES ('G-900', 'Budynek próbny', 'BASIC',
                ST_Multi(ST_GeomFromText(
                    'POLYGON((0 0,0.001 0,0.001 0.001,0 0.001,0 0))', 4326)),
                '© OpenStreetMap contributors')
            """
        ).update();
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
                'ACCESSIBLE', DATE '2026-10-02', 'WALKTHROUGH')
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
            "markingSource":"IMPORT"}]
            """;
    }

    private static JsonNode get(final WebApplicationContext context, final String path)
        throws Exception {
        return context.getBean(ObjectMapper.class).readTree(
            MockMvcBuilders.webAppContextSetup(context).build()
                .perform(MockMvcRequestBuilders.get(path))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8)
        );
    }
}
