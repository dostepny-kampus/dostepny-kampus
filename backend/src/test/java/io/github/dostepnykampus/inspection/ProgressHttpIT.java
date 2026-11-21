/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.inspection;

import io.github.dostepnykampus.IntegrationTest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Odczyt postępu budynków przez {@code GET /progress}: piętra, zgłoszenia bez archiwalnych
 * i spoza budynku, wejścia z obchodu oraz aktywne znaczniki z pseudonimem autora.
 *
 * <p>Wymagania: US11.1, US11.2, US11.3, US11.4, BR-5, BR-20, BR-21, BR-24</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
@Transactional
final class ProgressHttpIT {

    @Test
    void readsProgressForCoordinator(final WebApplicationContext context) throws Exception {
        ProgressHttpIT.seed(context.getBean(JdbcClient.class));
        final String body = context.getBean(MockMvc.class)
            .perform(MockMvcRequestBuilders.get("/progress")
                .header(HttpHeaders.AUTHORIZATION, "Bearer COORDINATOR"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        final JsonNode progress = context.getBean(ObjectMapper.class).readTree(body);
        Assertions.assertTrue(progress.isArray(), "Postęp zawiera tablicę budynków");
        ProgressHttpIT.detailed(ProgressHttpIT.find(progress, "T-D1"));
        ProgressHttpIT.basic(ProgressHttpIT.find(progress, "T-B1"));
    }

    @Test
    void deniesAccessWithoutCoordinatorRole(final MockMvc http) throws Exception {
        Assertions.assertEquals(
            List.of(401, 403, 403, 403),
            List.of(
                http.perform(MockMvcRequestBuilders.get("/progress"))
                    .andReturn().getResponse().getStatus(),
                ProgressHttpIT.status(http, "VISITOR"),
                ProgressHttpIT.status(http, "COLLECTOR"),
                ProgressHttpIT.status(http, "VERIFIER")
            ),
            "Postęp inwentaryzacji jest dostępny tylko Koordynatorowi"
        );
    }

    private static void detailed(final JsonNode item) {
        Assertions.assertEquals(
            List.of(2, 1, 2, 1, 2, 1, 2),
            List.of(
                item.path("floorsTotal").asInt(-1), item.path("floorsInspected").asInt(-1),
                item.path("pendingSubmissions").asInt(-1),
                item.path("acceptedSubmissions").asInt(-1),
                item.path("entrancesTotal").asInt(-1),
                item.path("entrancesCheckedOnSite").asInt(-1), item.path("markers").size()
            ),
            "Postęp i liczby dla budynku szczegółowego"
        );
        Assertions.assertEquals("DETAILED", item.path("detailLevel").asString(), "Rodzaj budynku");
        Assertions.assertFalse(item.has("inspected"), "Szczegółowy ma wyłącznie pokrycie pięter");
        final JsonNode mine = item.path("markers").valueStream()
            .filter(marker -> marker.path("mine").asBoolean(false)).findFirst().orElseThrow();
        Assertions.assertEquals(
            List.of("00000000-0000-4000-8000-000000000001", "T-D1", "f0", "false"),
            List.of(mine.path("id").asString(), mine.path("buildingSymbol").asString(),
                mine.path("floorId").asString(), mine.path("revoked").asString()),
            "Aktywny znacznik czytającego"
        );
        Assertions.assertTrue(
            mine.path("authorPseudonym").asString().startsWith("Autor "),
            "Odpowiedź używa pseudonimu autora"
        );
        Assertions.assertDoesNotThrow(
            () -> Instant.parse(mine.path("createdAt").asString()),
            "Chwila utworzenia jest datą i godziną w formacie ISO"
        );
    }

    private static void basic(final JsonNode item) {
        Assertions.assertEquals("BASIC", item.path("detailLevel").asString(), "Rodzaj budynku");
        Assertions.assertTrue(item.path("inspected").asBoolean(false), "Znacznik całego budynku");
        Assertions.assertFalse(
            item.has("floorsTotal") || item.has("floorsInspected"),
            "Podstawowy nie ma pokrycia pięter"
        );
        Assertions.assertFalse(
            item.path("markers").path(0).has("floorId"), "Znacznik całego budynku nie ma piętra"
        );
    }

    private static int status(final MockMvc http, final String role) throws Exception {
        return http.perform(MockMvcRequestBuilders.get("/progress")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ".concat(role)))
            .andReturn().getResponse().getStatus();
    }

    private static void seed(final JdbcClient jdbc) {
        jdbc.sql(
            """
            INSERT INTO buildings (symbol, name, detail_level, outline, source) VALUES
            ('T-D1', 'Det', 'DETAILED',
                ST_GeomFromText('MULTIPOLYGON(((17 51, 17.01 51, 17.01 51.01, 17 51)))', 4326),
                'OSM'),
            ('T-B1', 'Bas', 'BASIC',
                ST_GeomFromText('MULTIPOLYGON(((17 51, 17.01 51, 17.01 51.01, 17 51)))', 4326),
                'OSM')
            """
        ).update();
        jdbc.sql(
            """
            INSERT INTO floors (id, building_symbol, label, sort_order) VALUES
            ('f0', 'T-D1', 'Parter', 0), ('f1', 'T-D1', '1', 1)
            """
        ).update();
        jdbc.sql(
            """
            INSERT INTO inspection_markers (id, building_symbol, floor_id, author, revoked) VALUES
            ('00000000-0000-4000-8000-000000000001', 'T-D1', 'f0', 'konto-testowe', FALSE),
            ('00000000-0000-4000-8000-000000000002', 'T-D1', 'f0', 'inny', FALSE),
            ('00000000-0000-4000-8000-000000000003', 'T-D1', 'f1', 'inny', TRUE),
            ('00000000-0000-4000-8000-000000000004', 'T-B1', NULL, 'inny', FALSE)
            """
        ).update();
        jdbc.sql(
            """
            INSERT INTO submissions (id, status, content) VALUES
            (:a, 'SENT', CAST(:td1 AS jsonb)),
            (:b, 'IN_REVIEW', CAST(:td1 AS jsonb)),
            (:c, 'ACCEPTED', CAST(:td1 AS jsonb)),
            (:d, 'ARCHIVED', CAST(:td1 AS jsonb)),
            (:e, 'REJECTED', CAST(:td1 AS jsonb)),
            (:f, 'ACCEPTED', CAST(:out AS jsonb))
            """
        ).param("a", UUID.randomUUID()).param("b", UUID.randomUUID())
            .param("c", UUID.randomUUID()).param("d", UUID.randomUUID())
            .param("e", UUID.randomUUID()).param("f", UUID.randomUUID())
            .param("td1", ProgressHttpIT.quickJson("T-D1"))
            .param("out", "{\"kind\":\"QUICK\",\"location\":{\"coordinates\":[17,51]}}").update();
        jdbc.sql(
            """
            INSERT INTO entrances (id, building_symbol, main, location, marking,
                marking_date, marking_source) VALUES
            ('e1', 'T-D1', TRUE, ST_GeomFromText('POINT(17 51)', 4326), 'ACCESSIBLE',
                DATE '2026-11-10', 'WALKTHROUGH'),
            ('e2', 'T-D1', FALSE, ST_GeomFromText('POINT(17 51)', 4326), 'NO_DATA',
                DATE '2026-11-10', 'IMPORT')
            """
        ).update();
    }

    private static JsonNode find(final JsonNode list, final String symbol) {
        return list.valueStream().filter(item -> symbol.equals(item.path("buildingSymbol").asString()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(String.format("Brak %s", symbol)));
    }

    private static String quickJson(final String symbol) {
        return String.format(
            "{\"kind\":\"QUICK\",\"buildingSymbol\":\"%s\",\"location\":{\"coordinates\":[17,51]}}",
            symbol
        );
    }
}
