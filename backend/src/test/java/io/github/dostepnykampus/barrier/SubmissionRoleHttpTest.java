/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import io.github.dostepnykampus.IntegrationTest;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Zgłoszenie inne niż szybkie i zmianę zgłoszenia przyjętego serwer przyjmuje
 * tylko od Zbierającego, a zgłoszenie szybkie od każdej zalogowanej osoby
 * (US4, US7, US17.2, US8.4, BR-8, BR-18, BR-19).
 *
 * @since 0.1.0
 */
@IntegrationTest
@Transactional
final class SubmissionRoleHttpTest {

    /**
     * Pola pomiaru precyzyjnego TB-13 w budynku C-13.
     */
    private static final String PRECISE = String.join(
        "",
        "\"kind\":\"PRECISE\",\"buildingSymbol\":\"C-13\",\"floor\":{\"number\":0},",
        "\"barrierGroup\":\"HORIZONTAL_CIRCULATION\",\"barrierType\":\"TB-13\",\"severity\":2"
    );

    /**
     * Pola zgłoszenia szybkiego.
     */
    private static final String QUICK = "\"kind\":\"QUICK\",\"barrierGroup\":\"OTHER\"";

    @Test
    void usSevenRefusesCollectorWorkWithoutCollectorRole(final WebApplicationContext context) {
        final String replacing = String.format(
            "%s,\"replacesSubmissionId\":\"%s\"", SubmissionRoleHttpTest.QUICK, UUID.randomUUID()
        );
        Assertions.assertAll(
            Stream.of("VISITOR", "VERIFIER", "COORDINATOR", "VERIFIER+COORDINATOR").flatMap(
                role -> Stream.of(
                    SubmissionRoleHttpTest.PRECISE,
                    SubmissionRoleHttpTest.PRECISE.replace("PRECISE", "ENTRANCE_MARKING"),
                    replacing
                ).map(
                    fields -> () -> {
                        final UUID id = UUID.randomUUID();
                        Assertions.assertEquals(
                            "403, zapisanych 0",
                            SubmissionRoleHttpTest.outcome(context, role, id, fields),
                            String.format("Rola %s wysłała: %s", role, fields)
                        );
                    }
                )
            )
        );
    }

    @Test
    void usSevenAcceptsPreciseFromCollector(final WebApplicationContext context) throws Exception {
        Assertions.assertEquals(
            "201, zapisanych 1",
            SubmissionRoleHttpTest.outcome(
                context, "COLLECTOR", UUID.randomUUID(), SubmissionRoleHttpTest.PRECISE
            ),
            "Zbierający nie wysłał pomiaru precyzyjnego"
        );
    }

    @Test
    void usFourAcceptsQuickFromEveryRole(final WebApplicationContext context) {
        Assertions.assertAll(
            Stream.of("VISITOR", "COLLECTOR", "VERIFIER", "COORDINATOR").map(
                role -> () -> Assertions.assertEquals(
                    "201, zapisanych 1",
                    SubmissionRoleHttpTest.outcome(
                        context, role, UUID.randomUUID(), SubmissionRoleHttpTest.QUICK
                    ),
                    String.format("Rola %s nie wysłała zgłoszenia szybkiego", role)
                )
            )
        );
    }

    private static String outcome(
        final WebApplicationContext context, final String role, final UUID id, final String fields
    ) throws Exception {
        return String.format(
            "%d, zapisanych %d",
            context.getBean(MockMvc.class).perform(
                MockMvcRequestBuilders.post("/submissions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", role))
                    .content(SubmissionRoleHttpTest.body(id, fields))
            ).andReturn().getResponse().getStatus(),
            context.getBean(JdbcClient.class)
                .sql("SELECT count(*) FROM submissions WHERE id = :id").param("id", id)
                .query(Integer.class).single()
        );
    }

    private static String body(final UUID id, final String fields) {
        return String.join(
            "",
            "{\"id\":\"", id.toString(), "\",", fields,
            ",\"location\":{\"type\":\"Point\",\"coordinates\":[17.06209,51.10785]},",
            "\"photoIds\":[\"", UUID.randomUUID().toString(), "\"]}"
        );
    }
}
