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
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Kroki scenariusza „Przyjęte zgłoszenie szybkie zmienia dowolny Zbierający”
 * (BR-18).
 *
 * <p>Token testowy ma zawsze to samo konto, więc autorem zgłoszenia szybkiego
 * zostaje inne konto w bazie. Status „przyjęte” krok ustawia w bazie, bo
 * decyzje Weryfikatora serwer dostaje z modułem weryfikacji.</p>
 *
 * @since 0.1.0
 */
public final class QuickChangeSteps {

    /**
     * Przyjęte zgłoszenie szybkie Odwiedzającego.
     */
    private static final UUID QUICK =
        UUID.fromString("7a5b6c73-8e9f-4a01-9c23-3e4f5a6b7c83");

    /**
     * Zmiana zgłoszenia szybkiego od Zbierającego.
     */
    private static final UUID CHANGE =
        UUID.fromString("8b6c7d84-9fa0-4b12-8d34-4f5a6b7c8d94");

    /**
     * Klient żądań HTTP do aplikacji.
     */
    private final MockMvc http;

    /**
     * Baza aplikacji.
     */
    private final JdbcClient jdbc;

    /**
     * Budynek zgłoszenia szybkiego.
     */
    private String building;

    /**
     * Kroki na kontekście aplikacji.
     *
     * @param http Klient żądań HTTP do aplikacji
     * @param jdbc Baza aplikacji
     */
    public QuickChangeSteps(final MockMvc http, final JdbcClient jdbc) {
        this.http = http;
        this.jdbc = jdbc;
        this.building = "";
    }

    /**
     * Odwiedzający wysłał zgłoszenie szybkie z typem i uciążliwością 1,
     * a zgłoszenie jest już przyjęte.
     *
     * @param symbol Symbol budynku
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @Given("^przyjęte zgłoszenie szybkie Odwiedzającego w budynku (\\S+) ma typ TB-13 .+$")
    public void acceptedQuick(final String symbol) throws Exception {
        this.building = symbol;
        MatcherAssert.assertThat(
            "Serwer nie przyjął zgłoszenia szybkiego Odwiedzającego",
            this.send(
                "VISITOR",
                String.format("\"id\":\"%s\",\"severity\":1", QuickChangeSteps.QUICK)
            ),
            Matchers.equalTo(201)
        );
        this.jdbc.sql(
            "UPDATE submissions SET author = 'odwiedzajacy', status = 'ACCEPTED' WHERE id = :id"
        ).param("id", QuickChangeSteps.QUICK).update();
    }

    /**
     * Zbierający wysyła zmianę zgłoszenia szybkiego z uciążliwością 2.
     *
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @When("Zbierający, który nie jest autorem tego zgłoszenia, zmieni uciążliwość na 2")
    public void collectorChanges() throws Exception {
        MatcherAssert.assertThat(
            "Serwer nie przyjął zmiany zgłoszenia szybkiego od Zbierającego",
            this.send(
                "COLLECTOR",
                String.format(
                    "\"id\":\"%s\",\"replacesSubmissionId\":\"%s\",\"severity\":2",
                    QuickChangeSteps.CHANGE, QuickChangeSteps.QUICK
                )
            ),
            Matchers.equalTo(201)
        );
    }

    /**
     * Serwer zapisał zmianę jako nowe zgłoszenie wysłane, powiązane z poprzednim.
     */
    @Then("powstaje nowe zgłoszenie powiązane z poprzednim, ze statusem „wysłane”")
    public void linkedSubmission() {
        MatcherAssert.assertThat(
            "Nowe zgłoszenie nie jest wysłane albo nie wskazuje poprzedniego",
            this.jdbc.sql(
                String.join(
                    " ",
                    "SELECT status || ' ' || (content ->> 'replacesSubmissionId')",
                    "FROM submissions WHERE id = :id"
                )
            ).param("id", QuickChangeSteps.CHANGE).query(String.class).list(),
            Matchers.contains(String.format("SENT %s", QuickChangeSteps.QUICK))
        );
    }

    /**
     * Autorem zmiany jest konto z tokenu Zbierającego, a nie autor poprzedniego.
     */
    @Then("autorem nowego zgłoszenia jest ten Zbierający")
    public void authoredByCollector() {
        MatcherAssert.assertThat(
            "Autorem zmiany nie jest konto, które ją złożyło",
            this.jdbc.sql("SELECT author FROM submissions WHERE id = :id")
                .param("id", QuickChangeSteps.CHANGE).query(String.class).list(),
            Matchers.contains("konto-testowe")
        );
    }

    /**
     * Poprzednie zgłoszenie serwer nadal oddaje mapie.
     *
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @Then("poprzednie zgłoszenie nadal jest przyjęte i widoczne na mapie")
    public void previousStays() throws Exception {
        MatcherAssert.assertThat(
            "Mapa budynku nie pokazuje poprzedniego zgłoszenia",
            this.http.perform(
                MockMvcRequestBuilders.get("/submissions").param("buildingSymbol", this.building)
            ).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8),
            Matchers.containsString(QuickChangeSteps.QUICK.toString())
        );
    }

    /**
     * Usuwa zgłoszenia scenariusza.
     */
    @After
    public void removeSubmissions() {
        this.jdbc.sql("DELETE FROM submissions WHERE id IN (:ids)")
            .param("ids", List.of(QuickChangeSteps.QUICK, QuickChangeSteps.CHANGE)).update();
    }

    private int send(final String token, final String head) throws Exception {
        return this.http.perform(
            MockMvcRequestBuilders.post("/submissions").contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .content(this.body(head))
        ).andReturn().getResponse().getStatus();
    }

    private String body(final String head) {
        return String.join(
            "",
            "{", head, ",\"kind\":\"QUICK\",\"barrierType\":\"TB-13\",",
            "\"barrierGroup\":\"HORIZONTAL_CIRCULATION\",",
            "\"location\":{\"type\":\"Point\",\"coordinates\":", this.place(), "},",
            "\"photoIds\":[\"", UUID.randomUUID().toString(), "\"]}"
        );
    }

    private String place() {
        return this.jdbc.sql(
            String.join(
                " ",
                "SELECT '[' || ST_X(p) || ',' || ST_Y(p) || ']'",
                "FROM (SELECT ST_PointOnSurface(outline) AS p FROM buildings",
                "WHERE symbol = :symbol) AS inside"
            )
        ).param("symbol", this.building).query(String.class).single();
    }
}
