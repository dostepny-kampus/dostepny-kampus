/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
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
 * Zwykłą zmianę przyjętego zgłoszenia z pomiaru precyzyjnego składa tylko
 * autor, a zmianę zgłoszenia szybkiego i zmianę „bariera usunięta” dowolny
 * Zbierający.
 *
 * <p>Wymagania: BR-3, BR-18, BR-19</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
@Transactional
final class ForeignChangeHttpIT {

    /**
     * Pola pomiaru precyzyjnego TB-13 w budynku C-13.
     */
    private static final String PRECISE = String.join(
        "",
        "\"kind\":\"PRECISE\",\"buildingSymbol\":\"C-13\",\"floor\":{\"number\":0},",
        "\"barrierGroup\":\"HORIZONTAL_CIRCULATION\",\"barrierType\":\"TB-13\",\"severity\":2"
    );

    @Test
    void refusesOrdinaryChangeOfForeignPrecise(final WebApplicationContext context)
        throws Exception {
        Assertions.assertEquals(
            "403, zapisanych 0",
            ForeignChangeHttpIT.change(
                context, ForeignChangeHttpIT.accepted(context, "inne-konto"),
                ForeignChangeHttpIT.PRECISE.replace("\"severity\":2", "\"severity\":3")
            ),
            "Serwer przyjął zwykłą zmianę cudzego zgłoszenia z pomiaru precyzyjnego"
        );
    }

    @Test
    void acceptsRemovalOfForeignPrecise(final WebApplicationContext context)
        throws Exception {
        Assertions.assertEquals(
            "201, zapisanych 1",
            ForeignChangeHttpIT.change(
                context, ForeignChangeHttpIT.accepted(context, "inne-konto"),
                ForeignChangeHttpIT.PRECISE.replace("\"severity\":2", "\"barrierRemoved\":true")
            ),
            "Serwer odrzucił zmianę „bariera usunięta” od innego Zbierającego"
        );
    }

    @Test
    void acceptsOrdinaryChangeFromAuthor(final WebApplicationContext context)
        throws Exception {
        Assertions.assertEquals(
            "201, zapisanych 1",
            ForeignChangeHttpIT.change(
                context, ForeignChangeHttpIT.accepted(context, "konto-testowe"),
                ForeignChangeHttpIT.PRECISE.replace("\"severity\":2", "\"severity\":3")
            ),
            "Serwer odrzucił zwykłą zmianę od autora zgłoszenia"
        );
    }

    @Test
    void acceptsChangeOfForeignQuick(final WebApplicationContext context)
        throws Exception {
        final UUID quick = UUID.randomUUID();
        new SubmissionRequests(context).create(
            quick, "\"kind\":\"QUICK\",\"barrierGroup\":\"OTHER\""
        );
        context.getBean(JdbcClient.class)
            .sql("UPDATE submissions SET author = 'inne-konto', status = 'ACCEPTED' WHERE id = :id")
            .param("id", quick).update();
        Assertions.assertEquals(
            "201, zapisanych 1",
            ForeignChangeHttpIT.change(context, quick, ForeignChangeHttpIT.PRECISE),
            "Serwer odrzucił zmianę cudzego zgłoszenia szybkiego"
        );
    }

    private static UUID accepted(final WebApplicationContext context, final String author)
        throws Exception {
        final UUID id = UUID.randomUUID();
        new SubmissionRequests(context).create(id, ForeignChangeHttpIT.PRECISE);
        context.getBean(JdbcClient.class)
            .sql("UPDATE submissions SET author = :author, status = 'ACCEPTED' WHERE id = :id")
            .param("author", author).param("id", id).update();
        return id;
    }

    private static String change(
        final WebApplicationContext context, final UUID replaced, final String fields
    ) throws Exception {
        final UUID id = UUID.randomUUID();
        return String.format(
            "%d, zapisanych %d",
            new SubmissionRequests(context).create(
                id, String.format("%s,\"replacesSubmissionId\":\"%s\"", fields, replaced)
            ),
            context.getBean(JdbcClient.class)
                .sql("SELECT count(*) FROM submissions WHERE id = :id").param("id", id)
                .query(Integer.class).single()
        );
    }
}
