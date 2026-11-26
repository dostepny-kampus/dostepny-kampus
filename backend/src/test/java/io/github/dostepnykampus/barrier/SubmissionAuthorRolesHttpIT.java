/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import io.github.dostepnykampus.IntegrationTest;
import java.util.List;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Serwer zapisuje przy pierwszym wysłaniu zgłoszenia role autora z tokenu, znane kontraktowi,
 * z rolą Odwiedzającego zawsze; ponowne wysłanie ról nie zmienia.
 *
 * <p>Wymagania: BR-5</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
@Transactional
final class SubmissionAuthorRolesHttpIT {

    @Test
    void storesRolesFromToken(final WebApplicationContext context) throws Exception {
        final UUID id = UUID.randomUUID();
        MatcherAssert.assertThat(
            "Serwer nie zapisał ról autora z tokenu w kolejności kontraktu",
            List.of(
                SubmissionAuthorRolesHttpIT.send(context, "VERIFIER+COLLECTOR.autor", id),
                SubmissionAuthorRolesHttpIT.roles(context, id)
            ),
            Matchers.contains(201, "VISITOR,COLLECTOR,VERIFIER")
        );
    }

    @Test
    void storesVisitorForAccountWithoutStaffRole(final WebApplicationContext context)
        throws Exception {
        final UUID id = UUID.randomUUID();
        MatcherAssert.assertThat(
            "Konto bez roli pracowniczej nie dostało roli Odwiedzającego",
            List.of(
                SubmissionAuthorRolesHttpIT.send(context, "VISITOR.autor", id),
                SubmissionAuthorRolesHttpIT.roles(context, id)
            ),
            Matchers.contains(201, "VISITOR")
        );
    }

    @Test
    void keepsRolesOfRepeatedSubmission(final WebApplicationContext context) throws Exception {
        final UUID id = UUID.randomUUID();
        SubmissionAuthorRolesHttpIT.send(context, "COLLECTOR.autor", id);
        MatcherAssert.assertThat(
            "Ponowne wysłanie zgłoszenia zmieniło zapisane role autora",
            List.of(
                SubmissionAuthorRolesHttpIT.send(context, "COORDINATOR.autor", id),
                SubmissionAuthorRolesHttpIT.roles(context, id)
            ),
            Matchers.contains(200, "VISITOR,COLLECTOR")
        );
    }

    private static int send(final WebApplicationContext context, final String token, final UUID id)
        throws Exception {
        return context.getBean(MockMvc.class).perform(
            MockMvcRequestBuilders.post("/submissions")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .content(SubmissionAuthorRolesHttpIT.body(id))
        ).andReturn().getResponse().getStatus();
    }

    private static String body(final UUID id) {
        return String.join(
            "",
            "{\"id\":\"", id.toString(), "\",\"kind\":\"QUICK\",\"barrierGroup\":\"OTHER\",",
            "\"location\":{\"type\":\"Point\",\"coordinates\":[17.06209,51.10785]},",
            "\"photoIds\":[\"", UUID.randomUUID().toString(), "\"]}"
        );
    }

    private static String roles(final WebApplicationContext context, final UUID id) {
        return context.getBean(JdbcClient.class)
            .sql("SELECT ARRAY_TO_STRING(author_roles, ',') FROM submissions WHERE id = :id")
            .param("id", id).query(String.class).single();
    }
}
