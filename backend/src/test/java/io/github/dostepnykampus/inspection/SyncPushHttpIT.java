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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Znacznik piętra obejrzanego serwer zapisuje, a ponowne wysłanie tego samego znacznika nie
 * zapisuje go drugi raz; znacznik piętra spoza budynku jest odrzucony, a błąd danych jednej
 * operacji (znak NUL) nie zatrzymuje pozostałych. Cofnięcie własnego znacznika jest udane, także
 * powtórzone, a cudzego odrzucone.
 *
 * <p>Wymagania: US15.1, US15.2, US15.3, BR-9, BR-20, BR-21, US9.2</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
@Transactional
final class SyncPushHttpIT {

    @Test
    void savesMarkerFromCollector(final WebApplicationContext context)
        throws Exception {
        Assertions.assertEquals(
            "200 APPLIED, zapisanych 1",
            SyncPushHttpIT.pushed(context, UUID.randomUUID(), "C-13"),
            "Zbierający nie zapisał znacznika piętra"
        );
    }

    @Test
    void rejectsMarkerWithoutCollectorRole(final WebApplicationContext context) {
        Assertions.assertAll(
            Stream.of("VISITOR", "VERIFIER", "COORDINATOR", "VERIFIER+COORDINATOR").map(
                role -> () -> {
                    final UUID id = UUID.randomUUID();
                    Assertions.assertEquals(
                        "200 REJECTED 403, zapisanych 0",
                        String.format(
                            "%s, zapisanych %d", SyncPushHttpIT.push(context, role, id, "C-13"),
                            SyncPushHttpIT.saved(context, id)
                        ),
                        String.format("Rola %s postawiła znacznik piętra", role)
                    );
                }
            )
        );
    }

    @Test
    void savesRepeatedMarkerOnce(final WebApplicationContext context) throws Exception {
        final UUID id = UUID.randomUUID();
        SyncPushHttpIT.push(context, "COLLECTOR", id, "C-13");
        Assertions.assertEquals(
            "200 APPLIED, zapisanych 1",
            SyncPushHttpIT.pushed(context, id, "C-13"),
            "Ponowne wysłanie znacznika zapisało go drugi raz albo nie zwróciło wyniku"
        );
    }

    @Test
    void rejectsMarkerOfUnknownBuilding(final WebApplicationContext context)
        throws Exception {
        Assertions.assertEquals(
            "200 REJECTED, zapisanych 0",
            SyncPushHttpIT.pushed(context, UUID.randomUUID(), "Z-99"),
            "Serwer zapisał znacznik w budynku, którego nie zna"
        );
    }

    @Test
    void rejectsMarkerOfFloorOutsideBuilding(final WebApplicationContext context)
        throws Exception {
        final UUID id = UUID.randomUUID();
        Assertions.assertEquals(
            "200 REJECTED, zapisanych 0",
            String.format(
                "%s, zapisanych %d",
                SyncPushHttpIT.send(
                    context, "COLLECTOR", SyncPushHttpIT.floor(id, "C-13", "nie-ma-pietra")
                ),
                SyncPushHttpIT.saved(context, id)
            ),
            "Serwer zapisał znacznik piętra, którego nie ma w budynku"
        );
    }

    @Test
    void savesMarkerOfFloorInBuilding(final WebApplicationContext context)
        throws Exception {
        context.getBean(JdbcClient.class).sql(
            """
            INSERT INTO floors (building_symbol, id, label, sort_order)
            VALUES ('C-13', 'test-f0', 'Parter', 0)
            """
        ).update();
        final UUID id = UUID.randomUUID();
        Assertions.assertEquals(
            "200 APPLIED, zapisanych 1",
            String.format(
                "%s, zapisanych %d",
                SyncPushHttpIT.send(
                    context, "COLLECTOR", SyncPushHttpIT.floor(id, "C-13", "test-f0")
                ),
                SyncPushHttpIT.saved(context, id)
            ),
            "Serwer nie zapisał znacznika piętra, które jest w budynku"
        );
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "{\"id\":\"%s\",\"buildingSymbol\":\"C-13%s\"}",
            "{\"id\":\"%s\",\"buildingSymbol\":\"C-13\",\"floorId\":\"f%s\"}"
        }
    )
    void rejectsOnlyBrokenOperationInBatch(
        final String broken, final WebApplicationContext context
    ) throws Exception {
        final UUID good = UUID.randomUUID();
        final MockHttpServletResponse response = context.getBean(MockMvc.class).perform(
            MockMvcRequestBuilders.post("/sync/push")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer COLLECTOR")
                .content(
                    String.join(
                        "",
                        "{\"operations\":[{\"clientOperationId\":\"",
                        UUID.randomUUID().toString(), "\",",
                        "\"type\":\"SET_INSPECTION_MARKER\",\"marker\":",
                        broken.formatted(UUID.randomUUID(), String.join("", "\\", "u0000")), "},",
                        SyncPushHttpIT.body(good, "C-13").substring(15)
                    )
                )
        ).andReturn().getResponse();
        final JsonNode results = context.getBean(ObjectMapper.class)
            .readTree(response.getContentAsString()).path("results");
        Assertions.assertEquals(
            "200 REJECTED 400 APPLIED, zapisanych 1",
            String.format(
                "%d %s %d %s, zapisanych %d", response.getStatus(),
                results.path(0).path("outcome").asString(),
                results.path(0).path("problem").path("status").asInt(),
                results.path(1).path("outcome").asString(), SyncPushHttpIT.saved(context, good)
            ),
            "Błąd jednej operacji zatrzymał pozostałe albo nie dał jej wyniku REJECTED 400"
        );
    }

    @Test
    void revokesOwnMarkerAlsoWhenRepeated(final WebApplicationContext context)
        throws Exception {
        final UUID id = UUID.randomUUID();
        SyncPushHttpIT.push(context, "COLLECTOR", id, "C-13");
        Assertions.assertEquals(
            "200 APPLIED, 200 APPLIED, cofnięty true",
            String.format(
                "%s, %s, cofnięty %b",
                SyncPushHttpIT.revoke(context, id), SyncPushHttpIT.revoke(context, id),
                SyncPushHttpIT.revoked(context, id)
            ),
            "Zbierający nie cofnął własnego znacznika albo powtórzone cofnięcie nie jest udane"
        );
    }

    @Test
    void rejectsRevokeOfForeignAndUnknownMarker(final WebApplicationContext context)
        throws Exception {
        final UUID foreign = UUID.randomUUID();
        context.getBean(JdbcClient.class).sql(
            "INSERT INTO inspection_markers (id, building_symbol, author) VALUES (:id, 'C-13', 'inne-konto')"
        ).param("id", foreign).update();
        Assertions.assertEquals(
            "200 REJECTED, 200 REJECTED, cofnięty false",
            String.format(
                "%s, %s, cofnięty %b",
                SyncPushHttpIT.revoke(context, foreign),
                SyncPushHttpIT.revoke(context, UUID.randomUUID()),
                SyncPushHttpIT.revoked(context, foreign)
            ),
            "Cofnięcie cudzego albo nieistniejącego znacznika nie zostało odrzucone"
        );
    }

    private static String revoke(final WebApplicationContext context, final UUID id)
        throws Exception {
        return SyncPushHttpIT.send(
            context, "COLLECTOR",
            String.join(
                "",
                "{\"operations\":[{\"clientOperationId\":\"", UUID.randomUUID().toString(),
                "\",\"type\":\"REVOKE_INSPECTION_MARKER\",\"markerId\":\"", id.toString(),
                "\"}]}"
            )
        );
    }

    private static boolean revoked(final WebApplicationContext context, final UUID id) {
        return context.getBean(JdbcClient.class).sql(
            "SELECT revoked FROM inspection_markers WHERE id = :id"
        ).param("id", id).query(Boolean.class).single();
    }

    private static String pushed(
        final WebApplicationContext context, final UUID id, final String building
    ) throws Exception {
        return String.format(
            "%s, zapisanych %d",
            SyncPushHttpIT.push(context, "COLLECTOR", id, building),
            SyncPushHttpIT.saved(context, id)
        );
    }

    private static String push(
        final WebApplicationContext context, final String role, final UUID id,
        final String building
    ) throws Exception {
        return SyncPushHttpIT.send(context, role, SyncPushHttpIT.body(id, building));
    }

    private static String send(
        final WebApplicationContext context, final String role, final String content
    ) throws Exception {
        final MockHttpServletResponse response = context.getBean(MockMvc.class).perform(
            MockMvcRequestBuilders.post("/sync/push")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", role))
                .content(content)
        ).andReturn().getResponse();
        String outcome = "";
        if (response.getContentAsString().contains("\"APPLIED\"")) {
            outcome = " APPLIED";
        } else if (response.getContentAsString().contains("\"status\":403")) {
            outcome = " REJECTED 403";
        } else if (response.getContentAsString().contains("\"REJECTED\"")) {
            outcome = " REJECTED";
        }
        return String.format("%d%s", response.getStatus(), outcome);
    }

    private static String body(final UUID id, final String building) {
        return String.join(
            "",
            "{\"operations\":[{\"clientOperationId\":\"", UUID.randomUUID().toString(),
            "\",\"type\":\"SET_INSPECTION_MARKER\",\"marker\":{\"id\":\"",
            id.toString(), "\",\"buildingSymbol\":\"", building, "\"}}]}"
        );
    }

    private static String floor(final UUID id, final String building, final String floor) {
        return String.join(
            "",
            "{\"operations\":[{\"clientOperationId\":\"", UUID.randomUUID().toString(),
            "\",\"type\":\"SET_INSPECTION_MARKER\",\"marker\":{\"id\":\"",
            id.toString(), "\",\"buildingSymbol\":\"", building, "\",\"floorId\":\"",
            floor, "\"}}]}"
        );
    }

    private static int saved(final WebApplicationContext context, final UUID id) {
        return context.getBean(JdbcClient.class).sql(
            "SELECT count(*) FROM inspection_markers WHERE id = :id"
        ).param("id", id).query(Integer.class).single();
    }
}
