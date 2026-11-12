/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import io.github.dostepnykampus.AsCollector;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Zgłoszenia z jednym zdjęciem w bazie i żądania zdjęć przez MockMvc.
 *
 * @since 0.1.0
 */
final class PhotoRequests {

    /** Kontekst aplikacji. */
    private final WebApplicationContext context;

    PhotoRequests(final WebApplicationContext context) {
        this.context = context;
    }

    /**
     * Wstawia zgłoszenie z jednym zdjęciem na liście photoIds, zapisanym w JSON
     * dokładnie tak, jak podano, np. wielkimi literami.
     *
     * @param status Status zgłoszenia
     * @param photo Identyfikator zdjęcia tak, jak trafia do JSON
     * @return Identyfikator zgłoszenia
     */
    UUID add(final String status, final String photo) {
        final UUID id = new SampleSubmissions(this.context)
            .add(status, SampleSubmissions.quick(UUID.fromString(photo)));
        this.context.getBean(JdbcClient.class).sql(
            "UPDATE submissions SET content = jsonb_set(content, '{photoIds}', CAST(:photos AS JSONB))"
                .concat(" WHERE id = :id")
        ).param("photos", "[\"%s\"]".formatted(photo)).param("id", id).update();
        return id;
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
        return this.context.getBean(MockMvc.class).perform(
            MockMvcRequestBuilders.put("/submissions/{id}/photos/{photo}", id, photo)
                .contentType(MediaType.IMAGE_JPEG).content(body)
                .with(new AsCollector())
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
        return this.context.getBean(MockMvc.class).perform(
            MockMvcRequestBuilders.get("/submissions/{id}/photos/{photo}", id, photo)
        ).andReturn().getResponse();
    }
}
