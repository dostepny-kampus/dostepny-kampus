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
import io.github.dostepnykampus.AsVerifier;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Kroki scenariusza „Status »w weryfikacji« po wzięciu zgłoszenia przez
 * Weryfikatora”.
 *
 * <p>Token testowy ma zawsze to samo konto, a Weryfikator nie bierze własnych
 * zgłoszeń, więc na czas wzięcia autorem zgłoszenia jest inne konto w bazie.
 * Przed odczytem listy autor wraca do konta z tokenu, żeby listę czytał
 * Zbierający, a nie obserwator.</p>
 *
 * <p>Wymagania: US8.2</p>
 *
 * @since 0.1.0
 */
public final class ReviewStatusSteps {

    /**
     * Konto, które w bazie jest autorem zgłoszenia z tokenem Zbierającego.
     */
    private static final String TOKEN_ACCOUNT = "konto-testowe";

    /**
     * Zgłoszenie scenariusza.
     */
    private static final UUID SUBMISSION =
        UUID.fromString("9b2c3d4e-5f60-4a71-8b92-0c1d2e3f4a5b");

    /**
     * Klient żądań HTTP do aplikacji.
     */
    private final MockMvc http;

    /**
     * Baza aplikacji.
     */
    private final JdbcClient jdbc;

    /**
     * Kroki na kontekście aplikacji.
     *
     * @param http Klient żądań HTTP do aplikacji
     * @param jdbc Baza aplikacji
     */
    public ReviewStatusSteps(final MockMvc http, final JdbcClient jdbc) {
        this.http = http;
        this.jdbc = jdbc;
    }

    /**
     * Zbierający wysyła zgłoszenie, a jego autorem zostaje inne konto.
     *
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @Given("zgłoszenie Zbierającego ma status „wysłane”")
    public void collectorSent() throws Exception {
        MatcherAssert.assertThat(
            "Serwer nie przyjął zgłoszenia Zbierającego",
            this.http.perform(
                MockMvcRequestBuilders.post("/submissions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .with(new AsCollector())
                    .content(ReviewStatusSteps.quick())
            ).andReturn().getResponse().getStatus(),
            Matchers.equalTo(201)
        );
        this.author("autor-zgloszenia");
    }

    /**
     * Weryfikator bierze zgłoszenie do weryfikacji.
     *
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @When("Weryfikator weźmie je z kolejki")
    public void verifierClaims() throws Exception {
        MatcherAssert.assertThat(
            "Weryfikator nie wziął zgłoszenia, serwer nie dał 200",
            this.http.perform(
                MockMvcRequestBuilders.post(
                    "/submissions/{id}/claim", ReviewStatusSteps.SUBMISSION
                ).with(new AsVerifier())
            ).andReturn().getResponse().getStatus(),
            Matchers.equalTo(200)
        );
    }

    /**
     * Lista własnych zgłoszeń Zbierającego pokazuje status „w weryfikacji”.
     *
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @Then("na liście Zbierającego zgłoszenie ma status „w weryfikacji”")
    public void listedInReview() throws Exception {
        this.author(ReviewStatusSteps.TOKEN_ACCOUNT);
        MatcherAssert.assertThat(
            "Lista Zbierającego nie pokazuje zgłoszenia ze statusem IN_REVIEW",
            this.http.perform(
                MockMvcRequestBuilders.get("/submissions").param("mine", "true")
                    .with(new AsCollector())
            ).andReturn().getResponse().getContentAsString(),
            Matchers.allOf(
                Matchers.containsString(ReviewStatusSteps.SUBMISSION.toString()),
                Matchers.containsString("\"status\":\"IN_REVIEW\"")
            )
        );
    }

    /**
     * Usuwa zgłoszenie scenariusza.
     */
    @After
    public void removeSubmission() {
        this.jdbc.sql("DELETE FROM submissions WHERE id = :id")
            .param("id", ReviewStatusSteps.SUBMISSION).update();
    }

    private void author(final String account) {
        this.jdbc.sql("UPDATE submissions SET author = :author WHERE id = :id")
            .param("author", account).param("id", ReviewStatusSteps.SUBMISSION).update();
    }

    private static String quick() {
        return String.join(
            "",
            "{\"id\":\"", ReviewStatusSteps.SUBMISSION.toString(), "\",",
            "\"kind\":\"QUICK\",\"barrierGroup\":\"OTHER\",",
            "\"location\":{\"type\":\"Point\",\"coordinates\":[17.06209,51.10785]},",
            "\"photoIds\":[\"", UUID.randomUUID().toString(), "\"]}"
        );
    }
}
