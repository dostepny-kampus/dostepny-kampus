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
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Cofnięcie znacznika przez {@code DELETE /inspection-markers/{markerId}}: Koordynator cofa
 * znacznik dowolnego Zbierającego (204), Zbierający wyłącznie własny (204), próba cofnięcia
 * cudzego znacznika przez Zbierającego daje 403, a brak znacznika 404.
 *
 * <p>Wymagania: US11.3, US15.2, BR-20</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
@Transactional
final class InspectionMarkerHttpIT {

    @Test
    void revokesAnyMarkerForCoordinator(final WebApplicationContext context) throws Exception {
        final UUID id = UUID.randomUUID();
        InspectionMarkerHttpIT.insert(context, id, "inne-konto");
        final int status = InspectionMarkerHttpIT.delete(context, "COORDINATOR", id);
        Assertions.assertEquals(
            "204, cofnięty true",
            String.format("%d, cofnięty %b", status, InspectionMarkerHttpIT.revoked(context, id)),
            "Koordynator nie cofnął cudzego znacznika"
        );
    }

    @Test
    void revokesOwnMarkerForCollector(final WebApplicationContext context) throws Exception {
        final UUID id = UUID.randomUUID();
        InspectionMarkerHttpIT.insert(context, id, "konto-testowe");
        final int status = InspectionMarkerHttpIT.delete(context, "COLLECTOR", id);
        Assertions.assertEquals(
            "204, cofnięty true",
            String.format("%d, cofnięty %b", status, InspectionMarkerHttpIT.revoked(context, id)),
            "Zbierający nie cofnął własnego znacznika"
        );
    }

    /**
     * Scenariusz: postep-inwentaryzacji.feature: Inny Zbierający nie cofnie cudzego znacznika.
     *
     * <p>Wymagania: US15.2, BR-20</p>
     *
     * @param context Kontekst testowy aplikacji
     * @throws Exception Błąd wywołania MockMvc
     */
    @Test
    void rejectsForeignMarkerRevokeForCollector(final WebApplicationContext context)
        throws Exception {
        final UUID id = UUID.randomUUID();
        InspectionMarkerHttpIT.insert(context, id, "inne-konto");
        final int status = InspectionMarkerHttpIT.delete(context, "COLLECTOR", id);
        Assertions.assertEquals(
            "403, cofnięty false",
            String.format("%d, cofnięty %b", status, InspectionMarkerHttpIT.revoked(context, id)),
            "Inny Zbierający cofnął cudzy znacznik"
        );
    }

    @Test
    void returnsNotFoundForMissingMarker(final WebApplicationContext context) throws Exception {
        final UUID missing = UUID.randomUUID();
        Assertions.assertEquals(
            "404, 404",
            String.format(
                "%d, %d",
                InspectionMarkerHttpIT.delete(context, "COORDINATOR", missing),
                InspectionMarkerHttpIT.delete(context, "COLLECTOR", missing)
            ),
            "Brak znacznika nie dał odpowiedzi 404"
        );
    }

    @Test
    void rejectsForbiddenRolesAndAnonymous(final WebApplicationContext context) throws Exception {
        final UUID id = UUID.randomUUID();
        final int anon = context.getBean(MockMvc.class)
            .perform(MockMvcRequestBuilders.delete("/inspection-markers/{id}", id))
            .andReturn().getResponse().getStatus();
        Assertions.assertEquals(
            "401, 403, 403",
            String.format(
                "%d, %d, %d",
                anon,
                InspectionMarkerHttpIT.delete(context, "VISITOR", id),
                InspectionMarkerHttpIT.delete(context, "VERIFIER", id)
            ),
            "Nieuprawnione konto dostało dostęp do cofnięcia znacznika"
        );
    }

    private static void insert(
        final WebApplicationContext context, final UUID id, final String author
    ) {
        context.getBean(JdbcClient.class).sql(
            "INSERT INTO inspection_markers (id, building_symbol, author) VALUES (:id, 'C-13', :a)"
        ).param("id", id).param("a", author).update();
    }

    private static int delete(
        final WebApplicationContext context, final String role, final UUID id
    ) throws Exception {
        return context.getBean(MockMvc.class).perform(
            MockMvcRequestBuilders.delete("/inspection-markers/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ".concat(role))
        ).andReturn().getResponse().getStatus();
    }

    private static boolean revoked(final WebApplicationContext context, final UUID id) {
        return context.getBean(JdbcClient.class).sql(
            "SELECT revoked FROM inspection_markers WHERE id = :id"
        ).param("id", id).query(Boolean.class).single();
    }
}
