/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import io.github.dostepnykampus.IntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Zmiana zgłoszenia przyjętego wskazuje zgłoszenie, które istnieje i jest
 * przyjęte: brak zastępowanego zgłoszenia daje 404, zgłoszenie nieprzyjęte
 * 409, a w obu przypadkach nic się nie zapisuje.
 *
 * <p>Wymagania: BR-3, BR-18, BR-9</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
@Transactional
final class ReplacedSubmissionHttpIT {

    /**
     * Pola zgłoszenia szybkiego.
     */
    private static final String QUICK = "\"kind\":\"QUICK\",\"barrierGroup\":\"OTHER\"";

    @Test
    void refusesChangeOfMissingSubmission(final WebApplicationContext context)
        throws Exception {
        Assertions.assertEquals(
            "404, zapisanych 0",
            ReplacedSubmissionHttpIT.change(context, UUID.randomUUID()),
            "Serwer przyjął zmianę zgłoszenia, którego nie ma"
        );
    }

    @Test
    void refusesChangeOfUnacceptedSubmission(final WebApplicationContext context)
        throws Exception {
        final UUID sent = UUID.randomUUID();
        new SubmissionRequests(context).create(sent, ReplacedSubmissionHttpIT.QUICK);
        Assertions.assertEquals(
            "409, zapisanych 0",
            ReplacedSubmissionHttpIT.change(context, sent),
            "Serwer przyjął zmianę zgłoszenia, którego Weryfikator nie przyjął"
        );
    }

    @Test
    void returnsSavedChangeAgain(final WebApplicationContext context) throws Exception {
        final UUID replaced = UUID.randomUUID();
        new SubmissionRequests(context).create(replaced, ReplacedSubmissionHttpIT.QUICK);
        final JdbcClient jdbc = context.getBean(JdbcClient.class);
        jdbc.sql("UPDATE submissions SET status = 'ACCEPTED' WHERE id = :id")
            .param("id", replaced).update();
        final UUID id = UUID.randomUUID();
        final String fields = ReplacedSubmissionHttpIT.replacing(replaced);
        new SubmissionRequests(context).create(id, fields);
        jdbc.sql("UPDATE submissions SET status = 'ARCHIVED' WHERE id = :id")
            .param("id", replaced).update();
        Assertions.assertEquals(
            200,
            new SubmissionRequests(context).create(id, fields),
            "Ponowne wysłanie zapisanej zmiany nie zwróciło zapisanego zgłoszenia"
        );
    }

    private static String change(final WebApplicationContext context, final UUID replaced)
        throws Exception {
        final UUID id = UUID.randomUUID();
        return String.format(
            "%d, zapisanych %d",
            new SubmissionRequests(context).create(
                id, ReplacedSubmissionHttpIT.replacing(replaced)
            ),
            context.getBean(JdbcClient.class)
                .sql("SELECT count(*) FROM submissions WHERE id = :id").param("id", id)
                .query(Integer.class).single()
        );
    }

    private static String replacing(final UUID replaced) {
        return String.format(
            "%s,\"replacesSubmissionId\":\"%s\"", ReplacedSubmissionHttpIT.QUICK, replaced
        );
    }
}
