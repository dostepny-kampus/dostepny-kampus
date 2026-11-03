/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import io.github.dostepnykampus.PostgisTestConfiguration;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

/**
 * Testy US8.3, US9.3 i BR-10: PUT /submissions/{id} zapisuje edycję tylko
 * z wersji, którą ma serwer; inaczej daje 409 z wersją z serwera.
 *
 * @since 0.1.0
 */
@SpringBootTest
@Import(PostgisTestConfiguration.class)
@Transactional
final class SubmissionsEditHttpTest {

    @Test
    void usEightSavesEditAsNextVersion(final WebApplicationContext context)
        throws Exception {
        MatcherAssert.assertThat(
            "Edycja wersji 1 nie daje wersji 2",
            SubmissionsEditHttpTest.field(context, "SENT", 1, "/version"),
            Matchers.equalTo("2")
        );
    }

    @Test
    void usEightReturnsRejectedToSent(final WebApplicationContext context)
        throws Exception {
        MatcherAssert.assertThat(
            "Poprawka odrzuconego zgłoszenia nie wraca do statusu „wysłane”",
            SubmissionsEditHttpTest.field(context, "REJECTED", 1, "/status"),
            Matchers.equalTo("SENT")
        );
    }

    @Test
    void brTenGivesServerVersionOnConflict(final WebApplicationContext context)
        throws Exception {
        MatcherAssert.assertThat(
            "Konflikt wersji nie zwraca wersji 2 z serwera",
            SubmissionsEditHttpTest.field(context, "SENT", 2, "/serverSubmission/version"),
            Matchers.equalTo("2")
        );
    }

    @Test
    void brTenRejectsStaleVersion(final WebApplicationContext context) throws Exception {
        MatcherAssert.assertThat(
            "Edycja starej wersji nie daje 409",
            SubmissionsEditHttpTest.edit(context, "SENT", 2, "1").getStatus(),
            Matchers.equalTo(409)
        );
    }

    @Test
    void brEighteenLocksAcceptedSubmission(final WebApplicationContext context)
        throws Exception {
        MatcherAssert.assertThat(
            "Edycja zgłoszenia przyjętego nie daje 409",
            SubmissionsEditHttpTest.edit(context, "ACCEPTED", 1, "1").getStatus(),
            Matchers.equalTo(409)
        );
    }

    @Test
    void usEightNeedsBaseVersion(final WebApplicationContext context) throws Exception {
        MatcherAssert.assertThat(
            "Edycja bez wersji bazowej nie daje 400",
            SubmissionsEditHttpTest.edit(context, "SENT", 1, "\"1\"").getStatus(),
            Matchers.equalTo(400)
        );
    }

    @Test
    void usEightFindsNoUnknownSubmission(final WebApplicationContext context)
        throws Exception {
        MatcherAssert.assertThat(
            "Edycja nieistniejącego zgłoszenia nie daje 404",
            SubmissionsEditHttpTest.put(context, UUID.randomUUID(), "1").getStatus(),
            Matchers.equalTo(404)
        );
    }

    private static String field(
        final WebApplicationContext context, final String status, final int version,
        final String pointer
    ) throws Exception {
        return context.getBean(ObjectMapper.class).readTree(
            SubmissionsEditHttpTest.edit(context, status, version, "1")
                .getContentAsString(StandardCharsets.UTF_8)
        ).at(pointer).asString();
    }

    private static MockHttpServletResponse edit(
        final WebApplicationContext context, final String status, final int version,
        final String base
    ) throws Exception {
        final UUID id = UUID.randomUUID();
        context.getBean(JdbcClient.class).sql(
            """
            INSERT INTO submissions (id, version, status, kind, location, content)
            VALUES (:id, :version, :status, 'QUICK',
                ST_SetSRID(ST_MakePoint(17.0, 51.0), 4326), '{"kind":"QUICK"}')
            """
        ).param("id", id).param("version", version).param("status", status).update();
        return SubmissionsEditHttpTest.put(context, id, base);
    }

    private static MockHttpServletResponse put(
        final WebApplicationContext context, final UUID id, final String base
    ) throws Exception {
        return MockMvcBuilders.webAppContextSetup(context).build().perform(
            MockMvcRequestBuilders.put("/submissions/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(SubmissionsEditHttpTest.body(base))
        ).andReturn().getResponse();
    }

    private static String body(final String base) {
        return String.join(
            "",
            "{\"version\":", base, ",\"kind\":\"QUICK\",\"barrierGroup\":\"OTHER\",",
            "\"location\":{\"type\":\"Point\",\"coordinates\":[17.0,51.0]},",
            "\"photoIds\":[\"5d1c2b3a-4e5f-4a6b-8c7d-9e0f1a2b3c4d\"]}"
        );
    }
}
