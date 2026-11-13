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
import io.github.dostepnykampus.AsCollector;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Kroki scenariusza „Zbierający nie edytuje cudzego zgłoszenia” (BR-3, US14.1).
 *
 * <p>Token testowy ma zawsze to samo konto, więc autorem zgłoszenia zostaje
 * inne konto w bazie, a próbę zmiany wykonuje konto z tokenu.</p>
 *
 * @since 0.1.0
 */
public final class ForeignEditSteps {

    /**
     * Zgłoszenie scenariusza.
     */
    private static final UUID SUBMISSION =
        UUID.fromString("3c1d2e3f-4a5b-4c6d-8e7f-9a0b1c2d3e4f");

    /**
     * Klient żądań HTTP do aplikacji.
     */
    private final MockMvc http;

    /**
     * Baza aplikacji.
     */
    private final JdbcClient jdbc;

    /**
     * Status odpowiedzi na próbę zmiany.
     */
    private int refused;

    /**
     * Kroki na kontekście aplikacji.
     *
     * @param http Klient żądań HTTP do aplikacji
     * @param jdbc Baza aplikacji
     */
    public ForeignEditSteps(final MockMvc http, final JdbcClient jdbc) {
        this.http = http;
        this.jdbc = jdbc;
    }

    /**
     * Zbierający wysyła zgłoszenie, a jego autorem zostaje inne konto.
     *
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @Given("Zbierający wysłał zgłoszenie TB-13 „próg” w budynku C-13")
    public void collectorSent() throws Exception {
        MatcherAssert.assertThat(
            "Serwer nie przyjął zgłoszenia autora",
            this.send(MockMvcRequestBuilders.post("/submissions"), "\"id\":\"%s\"", 2),
            Matchers.equalTo(201)
        );
        this.jdbc.sql("UPDATE submissions SET author = 'autor-zgloszenia' WHERE id = :id")
            .param("id", ForeignEditSteps.SUBMISSION).update();
    }

    /**
     * Inny Zbierający wysyła zmianę uciążliwości wprost do serwera.
     *
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @When(
        "inny Zbierający spróbuje zmienić uciążliwość w tym zgłoszeniu z pominięciem aplikacji"
    )
    public void otherCollectorEdits() throws Exception {
        this.refused = this.send(
            MockMvcRequestBuilders.put("/submissions/{id}", ForeignEditSteps.SUBMISSION),
            "\"version\":1",
            3
        );
    }

    /**
     * Serwer odmawia zmiany.
     */
    @Then("próba jest odrzucona")
    public void attemptRefused() {
        MatcherAssert.assertThat(
            "Zmiana cudzego zgłoszenia nie dała 403", this.refused, Matchers.equalTo(403)
        );
    }

    /**
     * Zgłoszenie ma uciążliwość z treści autora.
     */
    @Then("zgłoszenie ma nadal uciążliwość nadaną przez autora")
    public void severityKept() {
        MatcherAssert.assertThat(
            "Uciążliwość cudzego zgłoszenia się zmieniła",
            this.jdbc.sql("SELECT content ->> 'severity' FROM submissions WHERE id = :id")
                .param("id", ForeignEditSteps.SUBMISSION).query(String.class).single(),
            Matchers.equalTo("2")
        );
    }

    /**
     * Usuwa zgłoszenie scenariusza.
     */
    @After
    public void removeSubmission() {
        this.jdbc.sql("DELETE FROM submissions WHERE id = :id")
            .param("id", ForeignEditSteps.SUBMISSION).update();
    }

    private int send(
        final MockHttpServletRequestBuilder request, final String head, final int severity
    ) throws Exception {
        return this.http.perform(
            request.contentType(MediaType.APPLICATION_JSON).with(new AsCollector()).content(
                String.join(
                    "",
                    "{", head.formatted(ForeignEditSteps.SUBMISSION), ",\"kind\":\"PRECISE\",",
                    "\"location\":{\"type\":\"Point\",\"coordinates\":[17.06209,51.10785]},",
                    "\"buildingSymbol\":\"C-13\",\"floor\":{\"number\":0},",
                    "\"barrierGroup\":\"HORIZONTAL_CIRCULATION\",\"barrierType\":\"TB-13\",",
                    "\"severity\":", String.valueOf(severity),
                    ",\"photoIds\":[\"", UUID.randomUUID().toString(), "\"]}"
                )
            )
        ).andReturn().getResponse().getStatus();
    }
}
