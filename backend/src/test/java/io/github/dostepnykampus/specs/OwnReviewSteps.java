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
import java.util.UUID;
import java.util.regex.Pattern;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Kroki scenariusza „Osoba z rolami Zbierającego i Weryfikatora nie przyjmie
 * własnego zgłoszenia”.
 *
 * <p>Token testowy z dwiema rolami ma zawsze to samo konto, więc autorem
 * zgłoszenia i osobą próbującą je przyjąć jest jedno konto.</p>
 *
 * <p>Wymagania: BR-4, BR-1</p>
 *
 * @since 0.1.0
 */
public final class OwnReviewSteps {

    /**
     * Zgłoszenie scenariusza.
     */
    private static final UUID SUBMISSION =
        UUID.fromString("7a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c5e");

    /**
     * Separator ról w tokenie testowym.
     */
    private static final Pattern PLUS = Pattern.compile("\+");

    /**
     * Klient żądań HTTP do aplikacji.
     */
    private final MockMvc http;

    /**
     * Baza aplikacji.
     */
    private final JdbcClient jdbc;

    /**
     * Token osoby ze scenariusza.
     */
    private String token;

    /**
     * Status odpowiedzi na próbę, wspólny z krokiem odmowy.
     */
    private final AttemptOutcome outcome;

    /**
     * Kroki na kontekście aplikacji.
     *
     * @param http Klient żądań HTTP do aplikacji
     * @param jdbc Baza aplikacji
     * @param outcome Status odpowiedzi na próbę, wspólny z krokiem odmowy
     */
    public OwnReviewSteps(
        final MockMvc http, final JdbcClient jdbc, final AttemptOutcome outcome
    ) {
        this.http = http;
        this.jdbc = jdbc;
        this.outcome = outcome;
        this.token = "";
    }

    /**
     * Osoba ma obie role; token testowy je niesie.
     */
    @Given("osoba ma role Zbierającego i Weryfikatora")
    public void hasBothRoles() {
        this.token = "COLLECTOR+VERIFIER";
        MatcherAssert.assertThat(
            "Token scenariusza nie niesie obu ról",
            OwnReviewSteps.PLUS.split(this.token).length, Matchers.equalTo(2)
        );
    }

    /**
     * Osoba wysyła zgłoszenie jako Zbierający.
     *
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @Given("jako Zbierający wysłała zgłoszenie TB-1 „schody przy wejściu” przy A-1")
    public void collectorSent() throws Exception {
        MatcherAssert.assertThat(
            "Serwer nie przyjął zgłoszenia osoby",
            this.http.perform(
                MockMvcRequestBuilders.post("/submissions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer ".concat(this.token))
                    .content(OwnReviewSteps.body())
            ).andReturn().getResponse().getStatus(),
            Matchers.equalTo(201)
        );
    }

    /**
     * Osoba wysyła wprost do serwera przyjęcie własnego zgłoszenia.
     *
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @When("jako Weryfikator spróbuje przyjąć to zgłoszenie z pominięciem aplikacji")
    public void verifierAccepts() throws Exception {
        this.outcome.record(
            this.http.perform(
                MockMvcRequestBuilders.post("/submissions/{id}/review", OwnReviewSteps.SUBMISSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer ".concat(this.token))
                    .content("{\"decision\":\"ACCEPT\",\"version\":1}")
            ).andReturn().getResponse().getStatus()
        );
    }

    /**
     * Zgłoszenie nie zmieniło statusu.
     */
    @Then("zgłoszenie ma nadal status „wysłane”")
    public void stillSent() {
        MatcherAssert.assertThat(
            "Status zgłoszenia zmienił się po odmowie",
            this.jdbc.sql("SELECT status FROM submissions WHERE id = :id")
                .param("id", OwnReviewSteps.SUBMISSION).query(String.class).single(),
            Matchers.equalTo("SENT")
        );
    }

    /**
     * Usuwa zgłoszenie scenariusza.
     */
    @After
    public void removeSubmission() {
        this.jdbc.sql("DELETE FROM submissions WHERE id = :id")
            .param("id", OwnReviewSteps.SUBMISSION).update();
    }

    private static String body() {
        return String.join(
            "",
            "{\"id\":\"", OwnReviewSteps.SUBMISSION.toString(), "\",\"kind\":\"PRECISE\",",
            "\"location\":{\"type\":\"Point\",\"coordinates\":[17.06209,51.10785]},",
            "\"buildingSymbol\":\"A-1\",\"floor\":{\"number\":0},",
            "\"barrierGroup\":\"ENTRANCE_AND_DOORS\",\"barrierType\":\"TB-1\",",
            "\"severity\":3,\"photoIds\":[\"", UUID.randomUUID().toString(), "\"]}"
        );
    }
}
