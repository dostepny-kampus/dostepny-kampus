/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.sync;

import io.github.dostepnykampus.IntegrationTest;
import io.github.dostepnykampus.OpenApiContract;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Pobranie zmian daje od kursora własne zgłoszenia w każdym statusie i cudze przyjęte
 * i archiwalne, wejścia z oznaczeniem z obchodu, a Zbierającemu znaczniki wszystkich
 * Zbierających, także cofnięte, z oznaczeniem własnych; inne role dostają pustą listę
 * znaczników, a zły kursor 400. Kursor odpowiedzi jest cofnięty o margines dłuższy niż limit
 * czasu transakcji zapisu, więc zmiana zatwierdzona po pobraniu z wcześniejszą chwilą
 * przychodzi w następnym.
 *
 * <p>Wymagania: US15.1, US15.2, US11.3, US8.1, US1.3, US10.6, US17.2, BR-20</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
@Transactional
final class SyncPullHttpIT {

    @Test
    void givesMarkersChangedSinceCursor(final WebApplicationContext context)
        throws Exception {
        final UUID old = SyncPullHttpIT.marker(context, "autor", "2999-01-01T00:00:00Z", false);
        final UUID own = SyncPullHttpIT.marker(context, "autor", "2999-03-01T00:00:00Z", false);
        final UUID foreign = SyncPullHttpIT.marker(
            context, "inne-konto", "2999-04-01T00:00:00Z", true
        );
        final JsonNode body = SyncPullHttpIT.pull(
            context, "COLLECTOR.autor", "?since=2999-02-01T00:00:00Z"
        );
        Assertions.assertEquals(
            String.join(
                " ", "2999-02-01T00:00:00Z", own.toString(), "C-13 false true",
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
    void givesOwnAndPublicSubmissionsSinceCursor(final WebApplicationContext context)
        throws Exception {
        final UUID old = SyncPullHttpIT.submission(context, "autor", "ACCEPTED");
        final Instant cursor = context.getBean(JdbcClient.class).sql("SELECT clock_timestamp()")
            .query(OffsetDateTime.class).single().toInstant();
        final List<UUID> ids = List.of(
            old,
            SyncPullHttpIT.submission(context, "autor", "SENT"),
            SyncPullHttpIT.submission(context, "autor", "IN_REVIEW"),
            SyncPullHttpIT.submission(context, "autor", "ACCEPTED"),
            SyncPullHttpIT.submission(context, "autor", "REJECTED"),
            SyncPullHttpIT.submission(context, "autor", "ARCHIVED"),
            SyncPullHttpIT.submission(context, "inne-konto", "SENT"),
            SyncPullHttpIT.submission(context, "inne-konto", "IN_REVIEW"),
            SyncPullHttpIT.submission(context, "inne-konto", "REJECTED"),
            SyncPullHttpIT.submission(context, "inne-konto", "ACCEPTED"),
            SyncPullHttpIT.submission(context, "inne-konto", "ARCHIVED")
        );
        final JsonNode body = SyncPullHttpIT.pull(
            context, "VISITOR.autor", "?since=".concat(cursor.toString())
        );
        MatcherAssert.assertThat(
            "Pobranie nie dało własnych i publicznych zgłoszeń od kursora w kształcie kontraktu",
            List.of(
                body.path("submissions").valueStream()
                    .map(node -> UUID.fromString(node.path("id").asString()))
                    .filter(ids::contains).map(ids::indexOf).sorted().toList(),
                Instant.parse(body.path("cursor").asString()).equals(cursor),
                new OpenApiContract().violations("SyncPullResponse", body.toString())
            ),
            Matchers.contains(List.of(1, 2, 3, 4, 5, 9, 10), true, List.of())
        );
    }

    @Test
    void givesEntrancesMarkedOnWalkthroughSinceCursor(final WebApplicationContext context)
        throws Exception {
        final JdbcClient jdbc = context.getBean(JdbcClient.class);
        SyncPullHttpIT.entrance(jdbc, "node/pobranie-stare");
        final Instant cursor = jdbc.sql("SELECT clock_timestamp()")
            .query(OffsetDateTime.class).single().toInstant();
        SyncPullHttpIT.entrance(jdbc, "node/pobranie-nowe");
        final JsonNode body = SyncPullHttpIT.pull(
            context, "VISITOR.autor", "?since=".concat(cursor.toString())
        );
        MatcherAssert.assertThat(
            "Pobranie nie dało wejścia z oznaczeniem z obchodu od kursora w kształcie kontraktu",
            List.of(
                body.path("entrances").valueStream()
                    .map(node -> node.path("id").asString())
                    .filter(id -> id.startsWith("node/pobranie-")).toList(),
                Instant.parse(body.path("cursor").asString()).equals(cursor),
                new OpenApiContract().violations("SyncPullResponse", body.toString())
            ),
            Matchers.contains(List.of("node/pobranie-nowe"), true, List.of())
        );
    }

    @Test
    void givesChangeCommittedAfterPull(final WebApplicationContext context) throws Exception {
        final String cursor = SyncPullHttpIT.pull(context, "COLLECTOR.autor", "")
            .path("cursor").asString();
        MatcherAssert.assertThat(
            "Kursor nie jest cofnięty albo spóźniona zmiana nie przyszła w następnym pobraniu",
            List.of(
                Instant.parse(cursor).isBefore(Instant.now().minusSeconds(60)),
                SyncPullHttpIT.pulledAfter(
                    context, cursor,
                    SyncPullHttpIT.marker(
                        context, "autor", Instant.now().minusSeconds(1).toString(), false
                    )
                )
            ),
            Matchers.contains(true, true)
        );
    }

    @Test
    void keepsMarginLongerThanWriteTimeout(final WebApplicationContext context) {
        final int timeout = context.getBean(AbstractPlatformTransactionManager.class)
            .getDefaultTimeout();
        MatcherAssert.assertThat(
            "Limit czasu transakcji zapisu nie jest ustawiony albo przekracza margines kursora",
            List.of(
                timeout > 0,
                context.getEnvironment().getRequiredProperty(
                    "dk.sync.cursor-margin", Duration.class
                ).compareTo(Duration.ofSeconds(timeout)) > 0
            ),
            Matchers.contains(true, true)
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

    private static void entrance(final JdbcClient jdbc, final String id) {
        jdbc.sql(
            """
            INSERT INTO entrances (id, building_symbol, name, main, location, marking,
                marking_date, marking_source, walkthrough_at)
            VALUES (:id, 'C-13', 'Wejście', false, ST_SetSRID(ST_MakePoint(17.06, 51.11), 4326),
                'ACCESSIBLE', CURRENT_DATE, 'WALKTHROUGH', statement_timestamp())
            """
        ).param("id", id).update();
    }

    private static UUID submission(
        final WebApplicationContext context, final String author, final String status
    ) throws Exception {
        final UUID id = UUID.randomUUID();
        context.getBean(MockMvc.class).perform(
            MockMvcRequestBuilders.post("/submissions")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer COLLECTOR.%s", author))
                .content(SyncPullHttpIT.quick(id))
        );
        context.getBean(JdbcClient.class)
            .sql("UPDATE submissions SET status = :status WHERE id = :id")
            .param("status", status).param("id", id).update();
        return id;
    }

    private static String quick(final UUID id) {
        return String.join(
            "",
            "{\"id\":\"", id.toString(), "\",\"kind\":\"QUICK\",\"barrierGroup\":\"OTHER\",",
            "\"location\":{\"type\":\"Point\",\"coordinates\":[17.06209,51.10785]},",
            "\"photoIds\":[\"", UUID.randomUUID().toString(), "\"]}"
        );
    }

    private static boolean pulledAfter(
        final WebApplicationContext context, final String cursor, final UUID marker
    ) throws Exception {
        return SyncPullHttpIT.pull(context, "COLLECTOR.autor", "?since=".concat(cursor))
            .path("inspectionMarkers").valueStream()
            .anyMatch(node -> marker.toString().equals(node.path("id").asString()));
    }

    private static JsonNode pull(
        final WebApplicationContext context, final String token, final String query
    ) throws Exception {
        return context.getBean(ObjectMapper.class).readTree(
            context.getBean(MockMvc.class).perform(
                MockMvcRequestBuilders.get("/sync/pull".concat(query))
                    .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
            ).andReturn().getResponse().getContentAsString()
        );
    }
}
