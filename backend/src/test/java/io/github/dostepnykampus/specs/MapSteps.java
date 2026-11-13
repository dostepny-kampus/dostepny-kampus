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
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Kroki szablonu „Na mapie są tylko zgłoszenia przyjęte”
 * i założeń pliku o przeglądaniu mapy.
 *
 * <p>Status zgłoszenia kroki ustawiają w bazie, bo decyzje Weryfikatora
 * serwer dostaje z modułem weryfikacji. Zgłoszenie „lokalne” istnieje tylko
 * w telefonie, więc serwer nie ma dla niego wiersza.</p>
 *
 * <p>Wymagania: US1.3, BR-6</p>
 *
 * @since 0.1.0
 */
public final class MapSteps {

    /**
     * Status na serwerze dla statusu nazwanego w scenariuszu.
     */
    private static final Map<String, String> STATUSES = Map.of(
        "wysłane", "SENT", "w weryfikacji", "IN_REVIEW", "odrzucone", "REJECTED",
        "archiwalne", "ARCHIVED", "przyjęte", "ACCEPTED"
    );

    /**
     * Przecinek albo spójnik „i” między symbolami budynków.
     */
    private static final Pattern BETWEEN = Pattern.compile(", | i ");

    /**
     * Zgłoszenie scenariusza.
     */
    private static final UUID SUBMISSION =
        UUID.fromString("5e3f4a51-6c7d-4e8f-9a01-1c2d3e4f5a61");

    /**
     * Klient żądań HTTP do aplikacji.
     */
    private final MockMvc http;

    /**
     * Baza aplikacji.
     */
    private final JdbcClient jdbc;

    /**
     * Zgłoszenia, które serwer oddał mapie.
     */
    private String shown;

    /**
     * Kroki na kontekście aplikacji.
     *
     * @param http Klient żądań HTTP do aplikacji
     * @param jdbc Baza aplikacji
     */
    public MapSteps(final MockMvc http, final JdbcClient jdbc) {
        this.http = http;
        this.jdbc = jdbc;
        this.shown = "";
    }

    /**
     * Serwer ma budynki wymienione w założeniach; plany pięter dochodzą
     * z modułem planów.
     *
     * @param symbols Symbole budynków rozdzielone przecinkami i spójnikiem „i”
     */
    @Given("^w aplikacji są między innymi budynki szczegółowe (.+)$")
    public void hasBuildings(final String symbols) {
        final List<String> wanted = List.of(MapSteps.BETWEEN.split(symbols));
        MatcherAssert.assertThat(
            "Serwer nie ma budynku z założeń scenariusza",
            this.jdbc.sql("SELECT symbol FROM buildings WHERE symbol IN (:symbols)")
                .param("symbols", wanted).query(String.class).list(),
            Matchers.containsInAnyOrder(wanted.toArray())
        );
    }

    /**
     * Dane wejść dla scenariuszy aplikacji; scenariusz serwera potrzebuje
     * z nich tylko budynku.
     *
     * @param symbol Symbol budynku
     */
    @Given(
        "^budynek (\\S+) ma 6 wejść, z których wejście główne ma schody, .+$"
    )
    @Given(
        "^w danych testowych budynek podstawowy (\\S+) ma wejścia wyłącznie .+$"
    )
    public void entranceData(final String symbol) {
        this.hasBuildings(symbol);
    }

    /**
     * Zbierający wysłał zgłoszenie TB-1 w budynku, a zgłoszenie ma już
     * podany status.
     *
     * @param symbol Symbol budynku
     * @param status Status słowami scenariusza
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @Given(
        "^przy wejściu głównym (\\S+) jest zgłoszenie TB-1 „.+” ze statusem „(.+)”$"
    )
    public void submissionWith(final String symbol, final String status) throws Exception {
        if (MapSteps.STATUSES.containsKey(status)) {
            MatcherAssert.assertThat(
                "Serwer nie przyjął zgłoszenia TB-1",
                this.http.perform(
                    MockMvcRequestBuilders.post("/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer COLLECTOR")
                        .content(this.body(symbol))
                ).andReturn().getResponse().getStatus(),
                Matchers.equalTo(201)
            );
            this.jdbc.sql("UPDATE submissions SET status = :status WHERE id = :id")
                .param("status", MapSteps.STATUSES.get(status))
                .param("id", MapSteps.SUBMISSION).update();
        } else {
            MatcherAssert.assertThat(
                "Scenariusz podaje nieznany status", status, Matchers.equalTo("lokalne")
            );
        }
    }

    /**
     * Mapa pobiera zgłoszenia budynku, bez logowania albo z tokenem.
     *
     * @param person Osoba ze scenariusza
     * @param symbol Symbol budynku
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @When("^(Gość niezalogowany|Odwiedzający) otworzy mapę kampusu przy budynku (\\S+)$")
    public void opensMap(final String person, final String symbol) throws Exception {
        final MockHttpServletRequestBuilder request =
            MockMvcRequestBuilders.get("/submissions").param("buildingSymbol", symbol);
        if ("Odwiedzający".equals(person)) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer VISITOR");
        }
        final MockHttpServletResponse response = this.http.perform(request)
            .andReturn().getResponse();
        this.shown = String.format(
            "%d %s", response.getStatus(), response.getContentAsString(StandardCharsets.UTF_8)
        );
    }

    /**
     * Serwer oddał mapie zgłoszenie albo je pominął.
     *
     * @param visible Słowa scenariusza: „jest” albo „nie jest”
     */
    @Then("^to zgłoszenie (jest|nie jest) widoczne na mapie$")
    public void visibility(final String visible) {
        MatcherAssert.assertThat(
            String.format("Serwer odpowiedział mapie: %s", this.shown),
            String.format(
                "%s, zgłoszenie %b",
                this.shown.substring(0, 3),
                this.shown.contains(MapSteps.SUBMISSION.toString())
            ),
            Matchers.equalTo(String.format("200, zgłoszenie %b", "jest".equals(visible)))
        );
    }

    /**
     * Usuwa zgłoszenie scenariusza.
     */
    @After
    public void removeSubmission() {
        this.jdbc.sql("DELETE FROM submissions WHERE id = :id")
            .param("id", MapSteps.SUBMISSION).update();
    }

    private String body(final String symbol) {
        return String.join(
            "",
            "{\"id\":\"", MapSteps.SUBMISSION.toString(), "\",\"kind\":\"PRECISE\",",
            "\"buildingSymbol\":\"", symbol, "\",\"floor\":{\"number\":0},",
            "\"barrierGroup\":\"ENTRANCE_AND_DOORS\",\"barrierType\":\"TB-1\",\"severity\":3,",
            "\"location\":{\"type\":\"Point\",\"coordinates\":",
            this.jdbc.sql(
                String.join(
                    " ",
                    "SELECT '[' || ST_X(p) || ',' || ST_Y(p) || ']'",
                    "FROM (SELECT ST_PointOnSurface(outline) AS p FROM buildings",
                    "WHERE symbol = :symbol) AS inside"
                )
            ).param("symbol", symbol).query(String.class).single(),
            "},\"photoIds\":[\"", UUID.randomUUID().toString(), "\"]}"
        );
    }
}
