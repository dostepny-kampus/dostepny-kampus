/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.path.web;

import io.github.dostepnykampus.IntegrationTest;
import java.nio.charset.StandardCharsets;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Operacja listAccessiblePaths zgodna ze schematem AccessiblePathList kontraktu API, bez
 * logowania.
 *
 * <p>Wymagania: US1.4, NFR-40</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
@Transactional
final class AccessiblePathsContractIT {

    @Test
    void listsPathsWithLineAndPwrSource(final WebApplicationContext context) throws Exception {
        context.getBean(JdbcClient.class).sql(
            """
            DELETE FROM accessible_paths;
            INSERT INTO accessible_paths (id, line)
                VALUES ('p-1', 'SRID=4326;LINESTRING(17.05 51.1,17.051 51.101)')
            """
        ).update();
        MatcherAssert.assertThat(
            "Odpowiedź ścieżek dostosowanych nie zgadza się ze schematem AccessiblePathList",
            AccessiblePathsContractIT.reply(context),
            Matchers.equalTo(
                context.getBean(ObjectMapper.class).readTree(
                    """
                    {"source":"Politechnika Wrocławska","paths":[{"id":"p-1",
                    "line":{"type":"LineString","coordinates":[[17.05,51.1],[17.051,51.101]]}}]}
                    """
                )
            )
        );
    }

    @Test
    void listsNoPathsBeforeImport(final WebApplicationContext context) throws Exception {
        context.getBean(JdbcClient.class).sql("DELETE FROM accessible_paths").update();
        MatcherAssert.assertThat(
            "Serwer bez danych ścieżek nie podaje pustej listy ze źródłem",
            AccessiblePathsContractIT.reply(context),
            Matchers.equalTo(
                context.getBean(ObjectMapper.class)
                    .readTree("{\"source\":\"Politechnika Wrocławska\",\"paths\":[]}")
            )
        );
    }

    private static JsonNode reply(final WebApplicationContext context) throws Exception {
        return context.getBean(ObjectMapper.class).readTree(
            context.getBean(MockMvc.class)
                .perform(MockMvcRequestBuilders.get("/accessible-paths"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8)
        );
    }
}
