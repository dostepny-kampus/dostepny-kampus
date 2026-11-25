/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.inspection;

import io.github.dostepnykampus.IntegrationTest;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Pobranie zmian daje Zbierającemu znaczniki wszystkich Zbierających zmienione od kursora,
 * także cofnięte, z oznaczeniem własnych; inne role dostają pustą listę, a zły kursor 400.
 *
 * <p>Wymagania: US15.1, US15.2, US11.3, BR-20</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
@Transactional
final class SyncPullHttpIT {

    @Test
    void givesMarkersChangedSinceCursor(final WebApplicationContext context)
        throws Exception {
        final UUID old = SyncPullHttpIT.marker(context, "autor", "2026-01-01T00:00:00Z", false);
        final UUID own = SyncPullHttpIT.marker(context, "autor", "2026-03-01T00:00:00Z", false);
        final UUID foreign = SyncPullHttpIT.marker(
            context, "inne-konto", "2026-04-01T00:00:00Z", true
        );
        final JsonNode body = SyncPullHttpIT.pull(
            context, "COLLECTOR.autor", "?since=2026-02-01T00:00:00Z"
        );
        Assertions.assertEquals(
            String.join(
                " ", "2026-04-01T00:00:00Z", own.toString(), "C-13 false true",
                foreign.toString(), "C-13 true false", "stary false", "[] []"
            ),
            String.join(
                " ", body.path("cursor").asString(),
                SyncPullHttpIT.described(body.path("inspectionMarkers").path(0)),
                SyncPullHttpIT.described(body.path("inspectionMarkers").path(1)),
                "stary", String.valueOf(body.toString().contains(old.toString())),
                body.path("submissions").toString(), body.path("entrances").toString()
            ),
            "Pobranie nie dało znaczników zmienionych od kursora albo zły kursor"
        );
    }

    @Test
    void givesRevokedMarkerAgain(final WebApplicationContext context) throws Exception {
        final UUID id = SyncPullHttpIT.marker(context, "autor", "2026-01-01T00:00:00Z", false);
        final String cursor = SyncPullHttpIT.pull(context, "COLLECTOR.autor", "")
            .path("cursor").asString();
        context.getBean(MockMvc.class).perform(
            MockMvcRequestBuilders.delete("/inspection-markers/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer COORDINATOR.koordynator")
        );
        Assertions.assertEquals(
            String.format("%s C-13 true true", id),
            SyncPullHttpIT.described(
                SyncPullHttpIT.pull(context, "COLLECTOR.autor", "?since=".concat(cursor))
                    .path("inspectionMarkers").path(0)
            ),
            "Cofnięcie znacznika nie wróciło w pobraniu od poprzedniego kursora"
        );
    }

    @Test
    void givesNoMarkersToOtherRoles(final WebApplicationContext context) {
        SyncPullHttpIT.marker(context, "autor", "2026-01-01T00:00:00Z", false);
        Assertions.assertAll(
            Stream.of("VISITOR", "VERIFIER", "COORDINATOR").map(
                role -> () -> Assertions.assertEquals(
                    "[]",
                    SyncPullHttpIT.pull(context, role, "").path("inspectionMarkers").toString(),
                    String.format("Rola %s dostała znaczniki pięter", role)
                )
            )
        );
    }

    @Test
    void rejectsBrokenCursor(final WebApplicationContext context) throws Exception {
        Assertions.assertEquals(
            400,
            context.getBean(MockMvc.class).perform(
                MockMvcRequestBuilders.get("/sync/pull?since=wczoraj")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer COLLECTOR")
            ).andReturn().getResponse().getStatus(),
            "Kursor, który nie jest chwilą, nie dał 400"
        );
    }

    private static String described(final JsonNode marker) {
        return String.join(
            " ", marker.path("id").asString(), marker.path("buildingSymbol").asString(),
            String.valueOf(marker.path("revoked").asBoolean()),
            String.valueOf(marker.path("mine").asBoolean())
        );
    }

    private static UUID marker(
        final WebApplicationContext context, final String author, final String changed,
        final boolean revoked
    ) {
        final UUID id = UUID.randomUUID();
        context.getBean(JdbcClient.class).sql(
            """
            INSERT INTO inspection_markers (id, building_symbol, author, revoked, changed_at)
            VALUES (:id, 'C-13', :author, :revoked, CAST(:changed AS TIMESTAMPTZ))
            """
        ).param("id", id).param("author", author).param("revoked", revoked)
            .param("changed", changed).update();
        return id;
    }

    private static JsonNode pull(
        final WebApplicationContext context, final String token, final String query
    ) throws Exception {
        final MockHttpServletResponse response = context.getBean(MockMvc.class).perform(
            MockMvcRequestBuilders.get("/sync/pull".concat(query))
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
        ).andReturn().getResponse();
        return context.getBean(ObjectMapper.class).readTree(response.getContentAsString());
    }
}
