/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.specs;

import io.cucumber.java.After;
import io.cucumber.java.en.When;
import io.github.dostepnykampus.AsVerifier;
import io.github.dostepnykampus.barrier.photo.PhotoStore;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * Przyjęcie zmiany oznaczenia wejścia przez Weryfikatora.
 *
 * @since 0.1.0
 */
public final class EntranceProgressSteps {

    /** Zgłoszenie oznaczenia. */
    private static final UUID SUBMISSION =
        UUID.fromString("1467d14f-8db6-4c36-9f27-bed095f1883d");

    /** Zdjęcie zgłoszenia. */
    private static final UUID PHOTO =
        UUID.fromString("c6bc0a95-535e-415c-8810-51e25286ab71");

    /** Klient HTTP. */
    private final MockMvc http;

    /** Baza zgłoszeń. */
    private final JdbcClient jdbc;

    /** Magazyn zdjęć zgłoszeń. */
    private final PhotoStore photos;

    /**
     * Tworzy kroki na kontekście aplikacji.
     *
     * @param http Klient HTTP
     * @param jdbc Baza zgłoszeń
     * @param photos Magazyn zdjęć zgłoszeń
     */
    public EntranceProgressSteps(
        final MockMvc http, final JdbcClient jdbc, final PhotoStore photos
    ) {
        this.http = http;
        this.jdbc = jdbc;
        this.photos = photos;
    }

    /**
     * Bierze do weryfikacji i przyjmuje zmianę jednego z dotąd niesprawdzonych wejść.
     *
     * @throws Exception Gdy przygotowanie zgłoszenia albo żądanie się nie powiedzie
     */
    @When("Weryfikator przyjmie zmianę oznaczenia jednego z pozostałych wejść A-1")
    public void accept() throws Exception {
        this.jdbc.sql(
            """
            INSERT INTO submissions (id, author, status, content)
            VALUES (:id, 'autor-zgloszenia', 'SENT', CAST(:content AS jsonb))
            """
        ).param("id", EntranceProgressSteps.SUBMISSION)
            .param("content", EntranceProgressSteps.content()).update();
        this.photos.put(
            EntranceProgressSteps.SUBMISSION, EntranceProgressSteps.PHOTO,
            "zdjęcie wejścia".getBytes(StandardCharsets.UTF_8)
        );
        this.http.perform(
            MockMvcRequestBuilders.post(
                "/submissions/{id}/claim", EntranceProgressSteps.SUBMISSION
            ).with(new AsVerifier())
        ).andExpect(MockMvcResultMatchers.status().isOk());
        this.http.perform(
            MockMvcRequestBuilders.post(
                "/submissions/{id}/review", EntranceProgressSteps.SUBMISSION
            ).with(new AsVerifier()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"ACCEPT\",\"version\":1}")
        ).andExpect(MockMvcResultMatchers.status().isOk());
    }

    /**
     * Usuwa zdjęcie z magazynu po scenariuszu.
     *
     * @throws IOException Gdy magazyn zdjęć odrzuci usunięcie
     */
    @After("@przyjecie-wejscia")
    public void cleanup() throws IOException {
        this.photos.delete(EntranceProgressSteps.SUBMISSION, EntranceProgressSteps.PHOTO);
    }

    private static String content() {
        return String.format(
            """
            {"kind":"ENTRANCE_MARKING","entranceId":"5","entranceMarking":"INACCESSIBLE",
                "buildingSymbol":"%s","location":{"type":"Point","coordinates":[17,51]},
                "photoIds":["%s"]}
            """,
            ProgressFixtures.key("A-1"), EntranceProgressSteps.PHOTO
        );
    }
}
