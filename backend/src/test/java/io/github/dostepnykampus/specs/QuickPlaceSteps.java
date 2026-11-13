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
import java.util.Map;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Kroki scenariuszy zgłoszenia szybkiego, które rozstrzyga serwer: budynek
 * z konturu (BR-14) i odmowa poza terenem PWr (US4.9, BR-G5).
 *
 * <p>Miejsca ze scenariusza kroki wyliczają z konturów budynków w bazie.
 * Kroki o aplikacji nie mają tu działania: sprawdza je test aplikacji.</p>
 *
 * @since 0.1.0
 */
public final class QuickPlaceSteps {

    /**
     * Token testowy dla roli nazwanej w scenariuszu.
     */
    private static final Map<String, String> TOKENS = Map.of(
        "Odwiedzającego", "VISITOR", "Zbierającego", "COLLECTOR"
    );

    /**
     * Zgłoszenie scenariusza.
     */
    private static final UUID SUBMISSION =
        UUID.fromString("4d2e3f40-5b6c-4d7e-8f90-0b1c2d3e4f50");

    /**
     * Klient żądań HTTP do aplikacji.
     */
    private final MockMvc http;

    /**
     * Baza aplikacji.
     */
    private final JdbcClient jdbc;

    /**
     * Token zalogowanej osoby.
     */
    private String token;

    /**
     * Miejsce osoby jako współrzędne GeoJSON.
     */
    private String place;

    /**
     * Status odpowiedzi na wysłane zgłoszenie.
     */
    private int status;

    /**
     * Treść odpowiedzi na wysłane zgłoszenie.
     */
    private String answer;

    /**
     * Kroki na kontekście aplikacji.
     *
     * @param http Klient żądań HTTP do aplikacji
     * @param jdbc Baza aplikacji
     */
    public QuickPlaceSteps(final MockMvc http, final JdbcClient jdbc) {
        this.http = http;
        this.jdbc = jdbc;
        this.token = "";
        this.place = "";
        this.answer = "";
    }

    /**
     * Osoba ma jedną rolę.
     *
     * @param role Nazwa roli w dopełniaczu, jak w scenariuszu
     */
    @Given("^\\S+ jest zalogowany z rolą (\\S+)$")
    public void loggedInAs(final String role) {
        this.token = QuickPlaceSteps.TOKENS.get(role);
        MatcherAssert.assertThat(
            String.format("Scenariusz podaje nieznaną rolę: %s", role),
            this.token, Matchers.notNullValue()
        );
    }

    /**
     * Osoba stoi w konturze budynku.
     *
     * @param symbol Symbol budynku
     */
    @Given("^stoi w budynku (\\S+)$")
    public void standsInside(final String symbol) {
        this.place = this.jdbc.sql(
            String.join(
                " ",
                "SELECT '[' || ST_X(p) || ',' || ST_Y(p) || ']'",
                "FROM (SELECT ST_PointOnSurface(outline) AS p FROM buildings",
                "WHERE symbol = :symbol) AS inside"
            )
        ).param("symbol", symbol).query(String.class).single();
    }

    /**
     * Osoba stoi 3 m od konturu budynku, poza konturem każdego budynku.
     *
     * @param symbol Symbol budynku
     */
    @Given("^stoi na chodniku przed wejściem głównym (\\S+), poza konturem budynku$")
    public void standsOutside(final String symbol) {
        this.place = this.jdbc.sql(
            String.join(
                " ",
                "SELECT '[' || ST_X(p) || ',' || ST_Y(p) || ']' FROM (",
                "SELECT ST_Project(ST_PointN(ST_ExteriorRing(ST_GeometryN(outline, 1)), 1)",
                "::geography, 3, radians(a))::geometry AS p",
                "FROM buildings, generate_series(0, 315, 45) AS a WHERE symbol = :symbol",
                ") AS near WHERE NOT EXISTS (",
                "SELECT 1 FROM buildings WHERE ST_Contains(outline, p)) LIMIT 1"
            )
        ).param("symbol", symbol).query(String.class).single();
    }

    /**
     * Osoba wysyła zgłoszenie szybkie z grupą „inne” ze swojego miejsca.
     *
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @When("wyśle zgłoszenie szybkie z grupą barier „inne”")
    public void sendsQuick() throws Exception {
        this.send();
    }

    /**
     * Zgłoszenie przychodzi z miejsca oddalonego od każdego budynku PWr.
     *
     * @param meters Odległość od najbliższego konturu w metrach
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @When("^aplikacja wyśle zgłoszenie z miejscem (\\d+) m od konturu najbliższego budynku PWr$")
    public void sendsFromAfar(final int meters) throws Exception {
        this.place = this.jdbc.sql(
            String.join(
                " ",
                "SELECT '[' || ST_X(p) || ',' || ST_Y(p) || ']' FROM (",
                "SELECT ST_Project(v::geography, :meters, radians(270))::geometry AS p FROM (",
                "SELECT (ST_DumpPoints(outline)).geom AS v FROM buildings) AS vertices",
                "ORDER BY ST_X(v) LIMIT 1) AS west"
            )
        ).param("meters", meters).query(String.class).single();
        this.send();
    }

    /**
     * Serwer zapisał zgłoszenie z budynkiem z konturu.
     *
     * @param symbol Symbol budynku
     */
    @Then("^zgłoszenie ma budynek (\\S+)$")
    public void hasBuilding(final String symbol) {
        MatcherAssert.assertThat(
            "Serwer nie przypisał budynku z konturu",
            String.format("%d %s", this.status, this.answer),
            Matchers.allOf(
                Matchers.startsWith("201 "),
                Matchers.containsString(String.format("\"buildingSymbol\":\"%s\"", symbol))
            )
        );
    }

    /**
     * Serwer zapisał zgłoszenie bez budynku.
     */
    @Then("zgłoszenie jest „poza budynkiem”")
    public void hasNoBuilding() {
        MatcherAssert.assertThat(
            "Serwer przypisał budynek miejscu poza konturem",
            String.format("%d %s", this.status, this.answer),
            Matchers.allOf(
                Matchers.startsWith("201 "),
                Matchers.not(Matchers.containsString("\"buildingSymbol\":\""))
            )
        );
    }

    /**
     * Zgłoszenie nie ma piętra.
     */
    @Then("zgłoszenie nie ma piętra")
    public void hasNoFloor() {
        MatcherAssert.assertThat(
            "Zgłoszenie szybkie z grupą „inne” dostało piętro",
            this.answer, Matchers.not(Matchers.containsString("\"floor\":{"))
        );
    }

    /**
     * Serwer odmawia zapisu poza terenem PWr.
     */
    @Then("serwer odrzuca zgłoszenie")
    public void refused() {
        MatcherAssert.assertThat(
            "Serwer przyjął zgłoszenie spoza terenu PWr",
            String.format("%d %s", this.status, this.answer),
            Matchers.allOf(
                Matchers.startsWith("400 "),
                Matchers.containsString("urn:dostepny-kampus:poza-terenem-pwr")
            )
        );
    }

    /**
     * Krok aplikacji; serwer nie ma tu działania.
     */
    @Then("aplikacja nie pyta o budynek ani o piętro")
    public void appDoesNotAsk() {
        // Pytania aplikacji sprawdza test aplikacji tego scenariusza.
    }

    /**
     * Krok aplikacji; serwer nie ma tu działania.
     */
    @Then("zgłoszenie zostaje w telefonie ze statusem „Nie wysłano”")
    public void staysInPhone() {
        // Stan w telefonie sprawdza test aplikacji tego scenariusza.
    }

    /**
     * Usuwa zgłoszenie scenariusza.
     */
    @After
    public void removeSubmission() {
        this.jdbc.sql("DELETE FROM submissions WHERE id = :id")
            .param("id", QuickPlaceSteps.SUBMISSION).update();
    }

    private void send() throws Exception {
        final MockHttpServletResponse response = this.http.perform(
            MockMvcRequestBuilders.post("/submissions").contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", this.token))
                .content(this.body())
        ).andReturn().getResponse();
        this.status = response.getStatus();
        this.answer = response.getContentAsString();
    }

    private String body() {
        return String.join(
            "",
            "{\"id\":\"", QuickPlaceSteps.SUBMISSION.toString(),
            "\",\"kind\":\"QUICK\",\"barrierGroup\":\"OTHER\",",
            "\"location\":{\"type\":\"Point\",\"coordinates\":", this.place, "},",
            "\"photoIds\":[\"", UUID.randomUUID().toString(), "\"]}"
        );
    }
}
