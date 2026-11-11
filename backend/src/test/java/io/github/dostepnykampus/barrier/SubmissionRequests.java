/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Wysyła gotową treść JSON zgłoszenia do API przez MockMvc.
 *
 * @since 0.1.0
 */
final class SubmissionRequests {

    /**
     * Kontekst aplikacji, na którym działa MockMvc.
     */
    private final WebApplicationContext context;

    SubmissionRequests(final WebApplicationContext context) {
        this.context = context;
    }

    /**
     * Tworzy zgłoszenie.
     *
     * @param json Treść żądania
     * @return Odpowiedź POST /submissions
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    MockHttpServletResponse post(final String json) throws Exception {
        return this.send(MockMvcRequestBuilders.post("/submissions"), json);
    }

    /**
     * Edytuje zgłoszenie.
     *
     * @param id Identyfikator zgłoszenia
     * @param json Treść żądania
     * @return Odpowiedź PUT /submissions/{id}
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    MockHttpServletResponse put(final UUID id, final String json) throws Exception {
        return this.send(MockMvcRequestBuilders.put("/submissions/{id}", id), json);
    }

    /**
     * Pobiera zasób.
     *
     * @param uri Adres z zapytaniem
     * @return Odpowiedź GET
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    MockHttpServletResponse get(final String uri) throws Exception {
        return this.context.getBean(MockMvc.class)
            .perform(MockMvcRequestBuilders.get(uri))
            .andReturn().getResponse();
    }

    private MockHttpServletResponse send(
        final MockHttpServletRequestBuilder request, final String json
    ) throws Exception {
        return this.context.getBean(MockMvc.class).perform(
            request.contentType(MediaType.APPLICATION_JSON).content(json)
        ).andReturn().getResponse();
    }
}
