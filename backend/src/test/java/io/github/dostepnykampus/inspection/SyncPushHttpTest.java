/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.inspection;

import io.github.dostepnykampus.IntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Znacznik piętra obejrzanego serwer zapisuje, a ponowne wysłanie tego samego znacznika nie
 * zapisuje go drugi raz.
 *
 * <p>Wymagania: US15.1, US15.3, BR-9, BR-20</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
@Transactional
final class SyncPushHttpTest {

    @Test
    void usFifteenOneSavesMarkerFromCollector(final WebApplicationContext context)
        throws Exception {
        final UUID id = UUID.randomUUID();
        Assertions.assertEquals(
            "200 APPLIED, zapisanych 1",
            String.format(
                "%s, zapisanych %d",
                SyncPushHttpTest.push(context, "COLLECTOR", id, "C-13"),
                SyncPushHttpTest.saved(context, id)
            ),
            "Zbierający nie zapisał znacznika piętra"
        );
    }

    @Test
    void brNineSavesRepeatedMarkerOnce(final WebApplicationContext context) throws Exception {
        final UUID id = UUID.randomUUID();
        SyncPushHttpTest.push(context, "COLLECTOR", id, "C-13");
        Assertions.assertEquals(
            "200 APPLIED, zapisanych 1",
            String.format(
                "%s, zapisanych %d",
                SyncPushHttpTest.push(context, "COLLECTOR", id, "C-13"),
                SyncPushHttpTest.saved(context, id)
            ),
            "Ponowne wysłanie znacznika zapisało go drugi raz albo nie zwróciło wyniku"
        );
    }

    @Test
    void usFifteenThreeRejectsMarkerOfUnknownBuilding(final WebApplicationContext context)
        throws Exception {
        final UUID id = UUID.randomUUID();
        Assertions.assertEquals(
            "200 REJECTED, zapisanych 0",
            String.format(
                "%s, zapisanych %d",
                SyncPushHttpTest.push(context, "COLLECTOR", id, "Z-99"),
                SyncPushHttpTest.saved(context, id)
            ),
            "Serwer zapisał znacznik w budynku, którego nie zna"
        );
    }

    private static String push(
        final WebApplicationContext context, final String role, final UUID id,
        final String building
    ) throws Exception {
        final MockHttpServletResponse response = context.getBean(MockMvc.class).perform(
            MockMvcRequestBuilders.post("/sync/push")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", role))
                .content(SyncPushHttpTest.body(id, building))
        ).andReturn().getResponse();
        String outcome = "";
        if (response.getContentAsString().contains("\"APPLIED\"")) {
            outcome = " APPLIED";
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

    private static int saved(final WebApplicationContext context, final UUID id) {
        return context.getBean(JdbcClient.class).sql(
            "SELECT count(*) FROM inspection_markers WHERE id = :id"
        ).param("id", id).query(Integer.class).single();
    }
}
