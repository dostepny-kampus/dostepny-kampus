/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Zgłoszenia z listą zdjęć w bazie oraz żądania zdjęć przez MockMvc.
 *
 * @since 0.1.0
 */
final class PhotoRequests {

    /**
     * Kontekst aplikacji, na którym działa MockMvc.
     */
    private final WebApplicationContext context;

    PhotoRequests(final WebApplicationContext context) {
        this.context = context;
    }

    /**
     * Wstawia zgłoszenie z jednym zdjęciem na liście photoIds.
     *
     * @param status Status zgłoszenia
     * @param photo Identyfikator zdjęcia tak, jak trafia do JSON
     * @return Identyfikator zgłoszenia
     */
    UUID add(final String status, final String photo) {
        final UUID id = UUID.randomUUID();
        this.context.getBean(JdbcClient.class).sql(
            """
            INSERT INTO submissions (id, status, kind, location, content)
            VALUES (:id, :status, 'QUICK', ST_SetSRID(ST_MakePoint(17.0, 51.0), 4326),
                CAST(:content AS JSONB))
            """
        ).param("id", id).param("status", status).param(
            "content",
            String.join(
                "",
                "{\"kind\":\"QUICK\",\"location\":{\"type\":\"Point\",",
                "\"coordinates\":[17.0,51.0]},\"barrierGroup\":\"OTHER\",",
                "\"photoIds\":[\"%s\"]}"
            ).formatted(photo)
        ).update();
        return id;
    }

    /**
     * Zmienia status zgłoszenia bezpośrednio w bazie.
     *
     * @param id Identyfikator zgłoszenia
     * @param status Nowy status
     */
    void status(final UUID id, final String status) {
        this.context.getBean(JdbcClient.class)
            .sql("UPDATE submissions SET status = :status WHERE id = :id")
            .param("status", status).param("id", id).update();
    }

    /**
     * Wysyła zdjęcie.
     *
     * @param id Identyfikator zgłoszenia
     * @param photo Identyfikator zdjęcia
     * @param body Treść żądania
     * @return Odpowiedź PUT
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    MockHttpServletResponse put(final UUID id, final UUID photo, final byte[] body)
        throws Exception {
        return MockMvcBuilders.webAppContextSetup(this.context).build().perform(
            MockMvcRequestBuilders.put("/submissions/{id}/photos/{photo}", id, photo)
                .contentType(MediaType.IMAGE_JPEG).content(body)
        ).andReturn().getResponse();
    }

    /**
     * Pobiera zdjęcie.
     *
     * @param id Identyfikator zgłoszenia
     * @param photo Identyfikator zdjęcia w adresie
     * @return Odpowiedź GET
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    MockHttpServletResponse get(final UUID id, final String photo) throws Exception {
        return MockMvcBuilders.webAppContextSetup(this.context).build().perform(
            MockMvcRequestBuilders.get("/submissions/{id}/photos/{photo}", id, photo)
        ).andReturn().getResponse();
    }
}
