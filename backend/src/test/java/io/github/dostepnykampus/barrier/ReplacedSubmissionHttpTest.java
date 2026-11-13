/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import io.github.dostepnykampus.AsCollector;
import io.github.dostepnykampus.IntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Zmiana zgłoszenia przyjętego wskazuje zgłoszenie, które istnieje i jest
 * przyjęte: brak zastępowanego zgłoszenia daje 404, zgłoszenie nieprzyjęte
 * 409, a w obu przypadkach nic się nie zapisuje (BR-3, BR-18).
 *
 * @since 0.1.0
 */
@IntegrationTest
@Transactional
final class ReplacedSubmissionHttpTest {

    /**
     * Pola zgłoszenia szybkiego.
     */
    private static final String QUICK = "\"kind\":\"QUICK\",\"barrierGroup\":\"OTHER\"";

    @Test
    void brEighteenRefusesChangeOfMissingSubmission(final WebApplicationContext context)
        throws Exception {
        Assertions.assertEquals(
            "404, zapisanych 0",
            ReplacedSubmissionHttpTest.change(context, UUID.randomUUID()),
            "Serwer przyjął zmianę zgłoszenia, którego nie ma"
        );
    }

    @Test
    void brEighteenRefusesChangeOfUnacceptedSubmission(final WebApplicationContext context)
        throws Exception {
        final UUID sent = UUID.randomUUID();
        ReplacedSubmissionHttpTest.send(context, sent, ReplacedSubmissionHttpTest.QUICK);
        Assertions.assertEquals(
            "409, zapisanych 0",
            ReplacedSubmissionHttpTest.change(context, sent),
            "Serwer przyjął zmianę zgłoszenia, którego Weryfikator nie przyjął"
        );
    }

    @Test
    void brNineReturnsSavedChangeAgain(final WebApplicationContext context) throws Exception {
        final UUID replaced = UUID.randomUUID();
        ReplacedSubmissionHttpTest.send(context, replaced, ReplacedSubmissionHttpTest.QUICK);
        final JdbcClient jdbc = context.getBean(JdbcClient.class);
        jdbc.sql("UPDATE submissions SET status = 'ACCEPTED' WHERE id = :id")
            .param("id", replaced).update();
        final UUID id = UUID.randomUUID();
        final String fields = ReplacedSubmissionHttpTest.replacing(replaced);
        ReplacedSubmissionHttpTest.send(context, id, fields);
        jdbc.sql("UPDATE submissions SET status = 'ARCHIVED' WHERE id = :id")
            .param("id", replaced).update();
        Assertions.assertEquals(
            200,
            ReplacedSubmissionHttpTest.send(context, id, fields),
            "Ponowne wysłanie zapisanej zmiany nie zwróciło zapisanego zgłoszenia"
        );
    }

    private static String change(final WebApplicationContext context, final UUID replaced)
        throws Exception {
        final UUID id = UUID.randomUUID();
        return String.format(
            "%d, zapisanych %d",
            ReplacedSubmissionHttpTest.send(
                context, id, ReplacedSubmissionHttpTest.replacing(replaced)
            ),
            context.getBean(JdbcClient.class)
                .sql("SELECT count(*) FROM submissions WHERE id = :id").param("id", id)
                .query(Integer.class).single()
        );
    }

    private static String replacing(final UUID replaced) {
        return String.format(
            "%s,\"replacesSubmissionId\":\"%s\"", ReplacedSubmissionHttpTest.QUICK, replaced
        );
    }

    private static int send(
        final WebApplicationContext context, final UUID id, final String fields
    ) throws Exception {
        return context.getBean(MockMvc.class).perform(
            MockMvcRequestBuilders.post("/submissions")
                .contentType(MediaType.APPLICATION_JSON).with(new AsCollector())
                .content(
                    String.join(
                        "",
                        "{\"id\":\"", id.toString(), "\",", fields,
                        ",\"location\":{\"type\":\"Point\",\"coordinates\":[17.06209,51.10785]},",
                        "\"photoIds\":[\"", UUID.randomUUID().toString(), "\"]}"
                    )
                )
        ).andReturn().getResponse().getStatus();
    }
}
