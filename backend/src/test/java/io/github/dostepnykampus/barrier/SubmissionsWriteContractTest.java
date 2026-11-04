/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import io.github.dostepnykampus.PostgisTestConfiguration;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Zapis przez HTTP bez transakcji testowej: retry i konflikty zachowują
 * treść, a poprawna edycja zapisuje treść i wersję (BR-3, BR-9, BR-10).
 *
 * @since 0.1.0
 */
@SpringBootTest
@Import(PostgisTestConfiguration.class)
final class SubmissionsWriteContractTest {

    /** Wiersz zarezerwowany dla sekwencyjnych scenariuszy tej klasy, usuwany po każdym teście. */
    private static final UUID ID = UUID.fromString("3ade39ac-7364-4bd7-ab83-2644ef3636cc");

    @AfterEach
    void clean(final WebApplicationContext context) {
        context.getBean(JdbcClient.class).sql("DELETE FROM submissions WHERE id = :id")
            .param("id", SubmissionsWriteContractTest.ID).update();
    }

    @Test
    void brNineRetryKeepsContentAndVersion(final WebApplicationContext context) throws Exception {
        final UUID id = this.create(context);
        Assertions.assertEquals(
            List.of(200, "original", 1, "SENT", "original", 1),
            this.observed(context, id, this.post(context, id, "changed"))
        );
    }

    @Test
    void usEightEditAndStaleConflict(final WebApplicationContext context) throws Exception {
        final UUID id = this.create(context);
        final List<Object> saved = this.observed(
            context, id, this.put(context, id, "edited", ",\"version\":1")
        );
        final MockHttpServletResponse conflict = this.put(context, id, "stale", ",\"version\":1");
        Assertions.assertEquals(
            List.of(
                List.of(200, "edited", 2, "SENT", "edited", 2),
                List.of(409, "edited", 2, "SENT", "edited", 2), "application/problem+json"
            ),
            List.of(saved, this.observed(context, id, conflict), conflict.getContentType())
        );
    }

    @Test
    void usEightEditPreservesStatusRules(final WebApplicationContext context) throws Exception {
        final List<List<Object>> actual = new ArrayList<>(3);
        for (final String status : List.of("SENT", "IN_REVIEW", "REJECTED")) {
            final UUID id = this.create(context);
            this.status(context, id, status);
            actual.add(
                this.observed(context, id, this.put(context, id, "edited", ",\"version\":1"))
            );
        }
        Assertions.assertEquals(
            List.of(
                List.of(200, "edited", 2, "SENT", "edited", 2),
                List.of(200, "edited", 2, "IN_REVIEW", "edited", 2),
                List.of(200, "edited", 2, "SENT", "edited", 2)
            ), actual
        );
    }

    @Test
    void brEighteenLocksAcceptedAndArchived(final WebApplicationContext context) throws Exception {
        final List<List<Object>> actual = new ArrayList<>(2);
        for (final String status : List.of("ACCEPTED", "ARCHIVED")) {
            final UUID id = this.create(context);
            this.status(context, id, status);
            actual.add(
                this.observed(
                    context, id, this.put(context, id, "blocked", ",\"version\":1")
                )
            );
        }
        Assertions.assertEquals(
            List.of(
                List.of(409, "original", 1, "ACCEPTED", "original", 1),
                List.of(409, "original", 1, "ARCHIVED", "original", 1)
            ), actual
        );
    }

    @Test
    void usEightRejectsInvalidVersion(final WebApplicationContext context) throws Exception {
        final UUID id = this.create(context);
        final List<List<Object>> actual = new ArrayList<>(4);
        for (final String version : List.of(
            "", ",\"version\":\"1\"", ",\"version\":0", ",\"version\":null"
        )) {
            actual.add(
                List.of(
                    this.put(context, id, "invalid", version).getStatus(),
                    this.stored(context, id).path("description").asString(),
                    this.version(context, id)
                )
            );
        }
        Assertions.assertEquals(
            List.of(
                List.of(400, "original", 1), List.of(400, "original", 1),
                List.of(400, "original", 1), List.of(400, "original", 1)
            ), actual
        );
    }

    private UUID create(final WebApplicationContext context) throws Exception {
        this.clean(context);
        Assertions.assertEquals(
            201, this.post(context, SubmissionsWriteContractTest.ID, "original").getStatus()
        );
        return SubmissionsWriteContractTest.ID;
    }

    private MockHttpServletResponse post(
        final WebApplicationContext context, final UUID id, final String description
    ) throws Exception {
        return new SubmissionRequests(context).post(this.body(id, description, ""));
    }

    private MockHttpServletResponse put(
        final WebApplicationContext context, final UUID id, final String description,
        final String version
    ) throws Exception {
        return new SubmissionRequests(context).put(id, this.body(id, description, version));
    }

    private String body(final UUID id, final String description, final String version) {
        return String.format(
            """
            {"id":"%s","kind":"QUICK","barrierGroup":"OTHER","description":"%s",
            "location":{"type":"Point","coordinates":[17.0,51.0]},"photoIds":["%s"]%s}
            """, id, description, UUID.randomUUID(), version
        );
    }

    private JsonNode json(
        final WebApplicationContext context, final MockHttpServletResponse response
    ) throws Exception {
        return context.getBean(ObjectMapper.class).readTree(
            response.getContentAsString(StandardCharsets.UTF_8)
        );
    }

    private List<Object> observed(
        final WebApplicationContext context, final UUID id, final MockHttpServletResponse response
    ) throws Exception {
        final JsonNode body = this.json(context, response);
        final JsonNode saved;
        if (response.getStatus() == 409) {
            saved = body.path("serverSubmission");
        } else {
            saved = body;
        }
        return List.of(
            response.getStatus(), saved.path("description").asString(),
            saved.path("version").asInt(), saved.path("status").asString(),
            this.stored(context, id).path("description").asString(), this.version(context, id)
        );
    }

    private JsonNode stored(final WebApplicationContext context, final UUID id) {
        return context.getBean(ObjectMapper.class).readTree(
            context.getBean(JdbcClient.class)
                .sql("SELECT content::text FROM submissions WHERE id = :id")
                .param("id", id).query(String.class).single()
        );
    }

    private int version(final WebApplicationContext context, final UUID id) {
        return context.getBean(JdbcClient.class)
            .sql("SELECT version FROM submissions WHERE id = :id")
            .param("id", id).query(Integer.class).single();
    }

    private void status(final WebApplicationContext context, final UUID id, final String status) {
        context.getBean(JdbcClient.class)
            .sql("UPDATE submissions SET status = :status WHERE id = :id")
            .param("status", status).param("id", id).update();
    }
}
