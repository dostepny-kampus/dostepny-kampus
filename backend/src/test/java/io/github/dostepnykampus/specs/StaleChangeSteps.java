/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.specs;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.dostepnykampus.AsVerifier;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Kroki serwera dla dwóch równoległych zmian tego samego zgłoszenia przyjętego: po przyjęciu
 * jednej drugiej nie da się przyjąć, a odrzuca się ją z powodem „nieaktualne”.
 *
 * <p>Zmiany i przyjęte zgłoszenie zakłada {@link ChangeSteps}; zmiana autora ma identyfikator
 * zgłoszenia ze scenariuszy Weryfikatora. Drugi Weryfikator ma własne konto, pierwszy konto
 * z tokenu domyślnego.</p>
 *
 * <p>Wymagania: BR-23, BR-16, BR-18, BR-19</p>
 *
 * @since 0.1.0
 */
public final class StaleChangeSteps {

    /** Token drugiego Weryfikatora. */
    private static final String SECOND = "Bearer VERIFIER.drugi-weryfikator";

    /** Klient żądań HTTP do aplikacji. */
    private final MockMvc http;

    /** Baza aplikacji. */
    private final JdbcClient jdbc;

    /** Status odpowiedzi na próbę przyjęcia. */
    private final AttemptOutcome outcome;

    /** Zmiana, którą bierze pierwszy Weryfikator. */
    private UUID other;

    /**
     * Kroki na kontekście aplikacji.
     *
     * @param http Klient żądań HTTP do aplikacji
     * @param jdbc Baza aplikacji
     * @param outcome Status odpowiedzi na próbę, wspólny z innymi krokami
     */
    public StaleChangeSteps(
        final MockMvc http, final JdbcClient jdbc, final AttemptOutcome outcome
    ) {
        this.http = http;
        this.jdbc = jdbc;
        this.outcome = outcome;
        this.other = VerifierSteps.CHECKED;
    }

    /**
     * Drugi Weryfikator bierze i przyjmuje jedną ze zmian.
     *
     * @param change Zmiana ze scenariusza
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @Given("^drugi Weryfikator przyjął (zmianę autora z .+|zmianę „bariera usunięta”)$")
    void secondAccepted(final String change) throws Exception {
        UUID accepted = VerifierSteps.CHECKED;
        this.other = ChangeSteps.RIVAL;
        if (change.contains("usunięta")) {
            accepted = ChangeSteps.RIVAL;
            this.other = VerifierSteps.CHECKED;
        }
        MatcherAssert.assertThat(
            "Drugi Weryfikator nie wziął i nie przyjął zmiany, serwer nie dał 200",
            List.of(
                this.status(
                    MockMvcRequestBuilders.post("/submissions/{id}/claim", accepted)
                        .header(HttpHeaders.AUTHORIZATION, StaleChangeSteps.SECOND)
                ),
                this.status(
                    StaleChangeSteps.decision(accepted, "{\"decision\":\"ACCEPT\",\"version\":1}")
                        .header(HttpHeaders.AUTHORIZATION, StaleChangeSteps.SECOND)
                )
            ),
            Matchers.contains(200, 200)
        );
    }

    /**
     * Pierwszy Weryfikator bierze drugą zmianę.
     *
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @Given("^pierwszy Weryfikator wziął (?:zmianę „bariera usunięta”|zmianę autora)$")
    void firstClaimed() throws Exception {
        MatcherAssert.assertThat(
            "Pierwszy Weryfikator nie wziął drugiej zmiany, serwer nie dał 200",
            this.status(
                MockMvcRequestBuilders.post("/submissions/{id}/claim", this.other)
                    .with(new AsVerifier())
            ),
            Matchers.equalTo(200)
        );
    }

    /**
     * Pierwszy Weryfikator próbuje przyjąć wziętą zmianę.
     *
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @When("spróbuje ją przyjąć")
    void triesToAccept() throws Exception {
        this.outcome.record(
            this.status(
                StaleChangeSteps.decision(this.other, "{\"decision\":\"ACCEPT\",\"version\":1}")
                    .with(new AsVerifier())
            )
        );
    }

    /**
     * Pierwszy Weryfikator odrzuca wziętą zmianę z powodem.
     *
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @When("odrzuci ją z powodem „nieaktualne”")
    void rejectsStale() throws Exception {
        MatcherAssert.assertThat(
            "Serwer nie przyjął odrzucenia nieaktualnej zmiany, nie dał 200",
            this.status(
                StaleChangeSteps.decision(
                    this.other,
                    "{\"decision\":\"REJECT\",\"version\":1,\"rejectionReason\":\"nieaktualne\"}"
                ).with(new AsVerifier())
            ),
            Matchers.equalTo(200)
        );
    }

    /** Próba przyjęcia skończyła się konfliktem, a zmiana czeka dalej w weryfikacji. */
    @Then("ta zmiana nie zostaje przyjęta")
    void notAccepted() {
        MatcherAssert.assertThat(
            "Nieaktualna zmiana została przyjęta albo serwer nie dał 409",
            List.of(this.outcome.status(), this.stored(this.other)),
            Matchers.contains(409, "IN_REVIEW")
        );
    }

    /**
     * Na mapie jest tylko przyjęta zmiana autora.
     *
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @Then("na mapie jest tylko bariera TB-9 z uciążliwością 2")
    void onlyAuthorChange() throws Exception {
        final String map = this.map();
        MatcherAssert.assertThat(
            "Mapa nie pokazuje tylko przyjętej zmiany autora z uciążliwością 2",
            List.of(
                map.contains(StaleChangeSteps.shown(VerifierSteps.CHECKED)),
                map.contains(StaleChangeSteps.shown(ChangeSteps.ORIGINAL)),
                map.contains(StaleChangeSteps.shown(ChangeSteps.RIVAL))
            ),
            Matchers.contains(true, false, false)
        );
    }

    /**
     * Po przyjętym usunięciu żadnej wersji windy nie ma na mapie.
     *
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @Then("niesprawnej windy nie ma na mapie")
    void elevatorGone() throws Exception {
        MatcherAssert.assertThat(
            "Mapa pokazuje windę po przyjętej zmianie „bariera usunięta”",
            this.map(),
            Matchers.not(
                Matchers.anyOf(
                    Matchers.containsString(StaleChangeSteps.shown(VerifierSteps.CHECKED)),
                    Matchers.containsString(StaleChangeSteps.shown(ChangeSteps.ORIGINAL)),
                    Matchers.containsString(StaleChangeSteps.shown(ChangeSteps.RIVAL))
                )
            )
        );
    }

    /**
     * Autor zmiany widzi ją jako odrzuconą z powodem „nieaktualne”.
     *
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @Then("^osoba, która złożyła tę zmianę, widzi ją ze statusem „odrzucone” i powodem .+$")
    void authorSeesStale() throws Exception {
        MatcherAssert.assertThat(
            "Autor nie widzi zmiany jako odrzuconej z powodem „nieaktualne”",
            this.http.perform(
                MockMvcRequestBuilders.get("/submissions/{id}", this.other)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer COLLECTOR.autor-zgloszenia")
            ).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8),
            Matchers.allOf(
                Matchers.containsString("\"status\":\"REJECTED\""),
                Matchers.containsString("\"rejectionReason\":\"nieaktualne\"")
            )
        );
    }

    private static MockHttpServletRequestBuilder decision(final UUID id, final String body) {
        return MockMvcRequestBuilders.post("/submissions/{id}/review", id)
            .contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static String shown(final UUID id) {
        return String.format("\"id\":\"%s\"", id);
    }

    private int status(final MockHttpServletRequestBuilder request) throws Exception {
        return this.http.perform(request).andReturn().getResponse().getStatus();
    }

    private String stored(final UUID id) {
        return this.jdbc.sql("SELECT status FROM submissions WHERE id = :id")
            .param("id", id).query(String.class).single();
    }

    private String map() throws Exception {
        return this.http.perform(MockMvcRequestBuilders.get("/submissions"))
            .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
