/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.specs;

import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.dostepnykampus.barrier.photo.PhotoStore;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Kroki scenariusza poprawy i ponownego wysłania zgłoszenia odrzuconego: autor wysyła edycję
 * z nowym zdjęciem przez {@code PUT /submissions/{id}}, a zgłoszenie wraca do kolejki
 * {@code GET /review/queue}.
 *
 * <p>Autorem jest konto testowe z tokenu Zbierającego, a kolejkę otwiera Weryfikator z innym
 * kontem. Po scenariuszu znika zgłoszenie, które założył.</p>
 *
 * <p>Wymagania: US8.3, BR-3, BR-16</p>
 *
 * @since 0.1.0
 */
public final class ResendSteps {

    /** Zgłoszenie scenariusza. */
    private static final UUID SUBMISSION =
        UUID.fromString("f2a3b4c5-0001-4d6e-8f70-8a9b0c1d2e3f");

    /** Zdjęcie, które Weryfikator uznał za złe. */
    private static final UUID OLD_PHOTO =
        UUID.fromString("f2a3b4c5-0002-4d6e-8f70-8a9b0c1d2e3f");

    /** Nowe zdjęcie progu. */
    private static final UUID NEW_PHOTO =
        UUID.fromString("f2a3b4c5-0003-4d6e-8f70-8a9b0c1d2e3f");

    /** Klient żądań HTTP do aplikacji. */
    private final MockMvc http;

    /** Baza aplikacji. */
    private final JdbcClient jdbc;

    /** Magazyn zdjęć zgłoszeń. */
    private final PhotoStore photos;

    /** Zdjęcie, które autor wyśle w zgłoszeniu. */
    private UUID photo;

    /**
     * Kroki na kontekście aplikacji.
     *
     * @param http Klient żądań HTTP do aplikacji
     * @param jdbc Baza aplikacji
     * @param photos Magazyn zdjęć zgłoszeń
     */
    public ResendSteps(final MockMvc http, final JdbcClient jdbc, final PhotoStore photos) {
        this.http = http;
        this.jdbc = jdbc;
        this.photos = photos;
        this.photo = ResendSteps.OLD_PHOTO;
    }

    /**
     * Zbierający wysłał zgłoszenie progu, a Weryfikator je odrzucił z powodem.
     *
     * @param reason Powód odrzucenia
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @Given("^zgłoszenie Zbierającego odrzucono z powodem „(.+)”$")
    public void rejected(final String reason) throws Exception {
        MatcherAssert.assertThat(
            "Serwer nie przyjął zgłoszenia Zbierającego, nie dał 201",
            this.send(
                MockMvcRequestBuilders.post("/submissions"),
                String.format("\"id\":\"%s\"", ResendSteps.SUBMISSION)
            ),
            Matchers.equalTo(201)
        );
        this.jdbc.sql(
            String.join(
                " ", "UPDATE submissions SET status = 'REJECTED', rejection_reason = :reason",
                "WHERE id = :id"
            )
        ).param("reason", reason).param("id", ResendSteps.SUBMISSION).update();
    }

    /** Zbierający robi nowe zdjęcie progu w miejsce starego. */
    @When("Zbierający zrobi nowe zdjęcie progu")
    public void newPhoto() {
        this.photo = ResendSteps.NEW_PHOTO;
    }

    /**
     * Aplikacja autora wysyła poprawione zgłoszenie z nowym zdjęciem.
     *
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @When("wyśle zgłoszenie ponownie")
    public void resends() throws Exception {
        MatcherAssert.assertThat(
            "Serwer nie przyjął poprawionego zgłoszenia odrzuconego, nie dał 200",
            this.send(
                MockMvcRequestBuilders.put("/submissions/{id}", ResendSteps.SUBMISSION),
                "\"version\":1"
            ),
            Matchers.equalTo(200)
        );
    }

    /** Poprawione zgłoszenie jest znowu wysłane. */
    @Then("zgłoszenie ma status „wysłane”")
    public void sentAgain() {
        MatcherAssert.assertThat(
            "Poprawione zgłoszenie odrzucone nie ma statusu „wysłane”",
            this.jdbc.sql("SELECT status FROM submissions WHERE id = :id")
                .param("id", ResendSteps.SUBMISSION).query(String.class).single(),
            Matchers.equalTo("SENT")
        );
    }

    /**
     * Weryfikator widzi poprawione zgłoszenie w kolejce.
     *
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @Then("trafia z powrotem do kolejki weryfikacji")
    public void backInQueue() throws Exception {
        MatcherAssert.assertThat(
            "Poprawionego zgłoszenia odrzuconego nie ma w kolejce weryfikacji",
            this.http.perform(
                MockMvcRequestBuilders.get("/review/queue")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer VERIFIER.weryfikator-kolejki")
            ).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8),
            Matchers.containsString(String.format("\"id\":\"%s\"", ResendSteps.SUBMISSION))
        );
    }

    /** Usuwa zgłoszenie scenariusza. */
    @After
    public void remove() {
        this.jdbc.sql("DELETE FROM submissions WHERE id = :id")
            .param("id", ResendSteps.SUBMISSION).update();
    }

    private int send(final MockHttpServletRequestBuilder request, final String head)
        throws Exception {
        final int status = this.http.perform(
            request.contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer COLLECTOR")
                .content(
                    String.join(
                        "",
                        "{", head, ",\"kind\":\"PRECISE\",\"location\":{\"type\":\"Point\",",
                        "\"coordinates\":[17.06209,51.10785]},\"buildingSymbol\":\"C-13\",",
                        "\"floor\":{\"number\":0},\"barrierGroup\":\"HORIZONTAL_CIRCULATION\",",
                        "\"barrierType\":\"TB-13\",\"severity\":2,\"photoIds\":[\"",
                        this.photo.toString(), "\"]}"
                    )
                )
        ).andReturn().getResponse().getStatus();
        this.photos.put(
            ResendSteps.SUBMISSION, this.photo, "zdjęcie".getBytes(StandardCharsets.UTF_8)
        );
        return status;
    }
}
