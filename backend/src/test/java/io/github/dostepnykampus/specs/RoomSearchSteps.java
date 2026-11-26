/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.specs;

import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.dostepnykampus.SampleBuilding;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Kroki scenariusza wyszukania sali w różnym zapisie: zapytanie idzie trasą
 * {@code GET /rooms}, a odpowiedź jest sprawdzana pod kątem budynku, piętra i funkcji sali.
 *
 * <p>Plan budynku A-1 z salą 329 powstaje w bazie, a budynek tylko wtedy, gdy go w bazie
 * nie ma. Po scenariuszu znika wszystko, co on założył.</p>
 *
 * <p>Wymagania: US18.1, NFR-40</p>
 *
 * @since 0.1.0
 */
public final class RoomSearchSteps {

    /** Piętro sali 329 założone przez scenariusz. */
    private static final String FLOOR = "SPEC-P2";

    /** Etykieta piętra sali 329. */
    private static final String LABEL = "2 piętro";

    /** Funkcja sali 329 z danych PWr. */
    private static final String FUNCTION = "Pracownia komputerowa";

    /** Klient żądań HTTP do aplikacji. */
    private final MockMvc http;

    /** Baza aplikacji. */
    private final JdbcClient jdbc;

    /** Czy budynek A-1 założył scenariusz. */
    private boolean created;

    /** Odpowiedź wyszukiwania. */
    private String body;

    /**
     * Kroki na kontekście aplikacji.
     *
     * @param http Klient żądań HTTP do aplikacji
     * @param jdbc Baza aplikacji
     */
    public RoomSearchSteps(final MockMvc http, final JdbcClient jdbc) {
        this.http = http;
        this.jdbc = jdbc;
        this.created = false;
        this.body = "";
    }

    /** Plan budynku A-1 ma salę 329 z funkcją i piętrem. */
    @Given("dane PWr budynku A-1 mają salę 329 z funkcją i piętrem")
    public void roomWithFunctionAndFloor() {
        final int present = this.jdbc.sql("SELECT count(*) FROM buildings WHERE symbol = 'A-1'")
            .query(Integer.class).single();
        if (present == 0) {
            new SampleBuilding("A-1", "A-1", "DETAILED")
                .wkt(this.jdbc, "POLYGON((0 0,0.001 0,0.001 0.001,0 0.001,0 0))");
            this.created = true;
        }
        this.jdbc.sql(
            String.join(
                " ",
                "INSERT INTO floors (building_symbol, id, label, sort_order)",
                "VALUES ('A-1', :id, :label, 2)"
            )
        ).param("id", RoomSearchSteps.FLOOR).param("label", RoomSearchSteps.LABEL).update();
        this.jdbc.sql(
            String.join(
                " ",
                "INSERT INTO rooms (building_symbol, floor_id, id, number, function, category,",
                "outline, label_point, labelled) VALUES ('A-1', :floor, 'spec-329', '329',",
                ":function, 'CLASSROOM', 'SRID=4326;MULTIPOLYGON(((0 0,1 0,1 1,0 0)))',",
                "'SRID=4326;POINT(0.5 0.25)', TRUE)"
            )
        ).param("floor", RoomSearchSteps.FLOOR)
            .param("function", RoomSearchSteps.FUNCTION).update();
    }

    /**
     * Mapa kampusu jest otwarta: lista budynków odpowiada.
     *
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @Given("Gość niezalogowany ma otwartą mapę kampusu")
    public void mapIsOpen() throws Exception {
        MatcherAssert.assertThat(
            "Serwer nie dał listy budynków ze statusem 200",
            this.http.perform(MockMvcRequestBuilders.get("/buildings"))
                .andReturn().getResponse().getStatus(),
            Matchers.equalTo(200)
        );
    }

    /**
     * Gość wyszukuje sali podanym zapisem.
     *
     * @param text Tekst zapytania, np. „A-1 329”
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @When("^wyszuka „(.+)”$")
    public void searches(final String text) throws Exception {
        this.body = this.http.perform(MockMvcRequestBuilders.get("/rooms").param("q", text))
            .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    /** Wynik jest jedną salą 329 budynku A-1 na piętrze z planu, do wyróżnienia na nim. */
    @Then("widzi plan piętra sali 329 w budynku A-1 z wyróżnioną salą")
    public void showsFloorPlan() {
        final JsonNode found = this.found();
        MatcherAssert.assertThat(
            String.format("Odpowiedź wyszukiwania sali: %s", this.body),
            List.of(
                found.path("buildingSymbol").asString(""),
                found.path("floor").path("id").asString(""),
                found.path("room").path("number").asString("")
            ),
            Matchers.contains("A-1", RoomSearchSteps.FLOOR, "329")
        );
    }

    /** Wynik podaje funkcję sali i etykietę piętra z danych PWr. */
    @Then("widzi funkcję sali i jej piętro z danych PWr")
    public void showsFunctionAndFloor() {
        final JsonNode found = this.found();
        MatcherAssert.assertThat(
            String.format("Odpowiedź wyszukiwania sali: %s", this.body),
            List.of(
                found.path("room").path("function").asString(""),
                found.path("floor").path("label").asString("")
            ),
            Matchers.contains(RoomSearchSteps.FUNCTION, RoomSearchSteps.LABEL)
        );
    }

    /** Usuwa wszystko, co założył scenariusz. */
    @After
    public void cleanUp() {
        this.jdbc.sql("DELETE FROM rooms WHERE building_symbol = 'A-1' AND floor_id = :floor")
            .param("floor", RoomSearchSteps.FLOOR).update();
        this.jdbc.sql("DELETE FROM floors WHERE building_symbol = 'A-1' AND id = :floor")
            .param("floor", RoomSearchSteps.FLOOR).update();
        if (this.created) {
            this.jdbc.sql("DELETE FROM buildings WHERE symbol = 'A-1'").update();
        }
    }

    private JsonNode found() {
        final JsonNode results = JsonMapper.builder().build().readTree(this.body)
            .path("results");
        MatcherAssert.assertThat(
            String.format("Wyszukiwanie nie dało dokładnie jednej sali: %s", this.body),
            results.size(), Matchers.equalTo(1)
        );
        return results.path(0);
    }
}
