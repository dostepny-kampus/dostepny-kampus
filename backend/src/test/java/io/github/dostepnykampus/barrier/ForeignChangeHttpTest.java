/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
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
 * Zwykłą zmianę przyjętego zgłoszenia z pomiaru precyzyjnego składa tylko
 * autor, a zmianę zgłoszenia szybkiego i zmianę „bariera usunięta” dowolny
 * Zbierający (BR-3, BR-18, BR-19).
 *
 * @since 0.1.0
 */
@IntegrationTest
@Transactional
final class ForeignChangeHttpTest {

    /**
     * Pola pomiaru precyzyjnego TB-13 w budynku C-13.
     */
    private static final String PRECISE = String.join(
        "",
        "\"kind\":\"PRECISE\",\"buildingSymbol\":\"C-13\",\"floor\":{\"number\":0},",
        "\"barrierGroup\":\"HORIZONTAL_CIRCULATION\",\"barrierType\":\"TB-13\",\"severity\":2"
    );

    @Test
    void brEighteenRefusesOrdinaryChangeOfForeignPrecise(final WebApplicationContext context)
        throws Exception {
        Assertions.assertEquals(
            "403, zapisanych 0",
            ForeignChangeHttpTest.change(
                context, ForeignChangeHttpTest.accepted(context, "inne-konto"),
                ForeignChangeHttpTest.PRECISE.replace("\"severity\":2", "\"severity\":3")
            ),
            "Serwer przyjął zwykłą zmianę cudzego zgłoszenia z pomiaru precyzyjnego"
        );
    }

    @Test
    void brNineteenAcceptsRemovalOfForeignPrecise(final WebApplicationContext context)
        throws Exception {
        Assertions.assertEquals(
            "201, zapisanych 1",
            ForeignChangeHttpTest.change(
                context, ForeignChangeHttpTest.accepted(context, "inne-konto"),
                ForeignChangeHttpTest.PRECISE.replace("\"severity\":2", "\"barrierRemoved\":true")
            ),
            "Serwer odrzucił zmianę „bariera usunięta” od innego Zbierającego"
        );
    }

    @Test
    void brEighteenAcceptsOrdinaryChangeFromAuthor(final WebApplicationContext context)
        throws Exception {
        final UUID own = ForeignChangeHttpTest.accepted(context, "konto-testowe");
        Assertions.assertEquals(
            "201, zapisanych 1",
            ForeignChangeHttpTest.change(
                context, own,
                ForeignChangeHttpTest.PRECISE.replace("\"severity\":2", "\"severity\":3")
            ),
            "Serwer odrzucił zwykłą zmianę od autora zgłoszenia"
        );
    }

    @Test
    void brEighteenAcceptsChangeOfForeignQuick(final WebApplicationContext context)
        throws Exception {
        final UUID quick = UUID.randomUUID();
        ForeignChangeHttpTest.send(context, quick, "\"kind\":\"QUICK\",\"barrierGroup\":\"OTHER\"");
        context.getBean(JdbcClient.class)
            .sql("UPDATE submissions SET author = 'inne-konto', status = 'ACCEPTED' WHERE id = :id")
            .param("id", quick).update();
        Assertions.assertEquals(
            "201, zapisanych 1",
            ForeignChangeHttpTest.change(context, quick, ForeignChangeHttpTest.PRECISE),
            "Serwer odrzucił zmianę cudzego zgłoszenia szybkiego"
        );
    }

    private static UUID accepted(final WebApplicationContext context, final String author)
        throws Exception {
        final UUID id = UUID.randomUUID();
        ForeignChangeHttpTest.send(context, id, ForeignChangeHttpTest.PRECISE);
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
            ForeignChangeHttpTest.send(
                context, id, String.format("%s,\"replacesSubmissionId\":\"%s\"", fields, replaced)
            ),
            context.getBean(JdbcClient.class)
                .sql("SELECT count(*) FROM submissions WHERE id = :id").param("id", id)
                .query(Integer.class).single()
        );
    }

    private static int send(
        final WebApplicationContext context, final UUID id, final String fields
    ) throws Exception {
        return context.getBean(MockMvc.class).perform(
            MockMvcRequestBuilders.post("/submissions")
                .contentType(MediaType.APPLICATION_JSON).with(new AsCollector())
                .content(ForeignChangeHttpTest.body(id, fields))
        ).andReturn().getResponse().getStatus();
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
