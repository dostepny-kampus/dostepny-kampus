/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.specs;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.dostepnykampus.AsCollector;
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Kroki postępu inwentaryzacji, mierzone przez odpowiedź dla Koordynatora.
 *
 * @since 0.1.0
 */
public final class ProgressSteps {

    /** Klient aplikacji. */
    private final MockMvc http;

    /** Baza danych testowych. */
    private final JdbcClient jdbc;

    /** Transakcje danych scenariusza. */
    private final PlatformTransactionManager manager;

    /** Dekodowanie odpowiedzi HTTP. */
    private final ObjectMapper mapper;

    /** Dane bieżącego scenariusza. */
    private ProgressFixtures data;

    /** Ostatni odczyt postępu. */
    private JsonNode progress;

    /**
     * Tworzy kroki na wspólnym kontekście aplikacji.
     *
     * @param http Klient aplikacji
     * @param jdbc Baza danych testowych
     * @param manager Transakcje danych scenariusza
     * @param mapper Dekodowanie odpowiedzi HTTP
     */
    public ProgressSteps(
        final MockMvc http, final JdbcClient jdbc, final PlatformTransactionManager manager,
        final ObjectMapper mapper
    ) {
        this.http = http;
        this.jdbc = jdbc;
        this.manager = manager;
        this.mapper = mapper;
        this.progress = mapper.createArrayNode();
    }

    /**
     * Izoluje dane jednego scenariusza.
     */
    @Before("@postep-http")
    public void start() {
        this.data = new ProgressFixtures(this.jdbc, this.manager);
    }

    /**
     * Wycofuje dane scenariusza i zmiany wykonane przez żądania.
     */
    @After("@postep-http")
    public void finish() {
        this.data.close();
    }

    @Given("budynek {string} ma {int} pięter z danych testowych i nie ma znaczników")
    public void emptyBuilding(final String symbol, final int floors) {
        this.data.building(symbol, floors);
    }

    @Given("w budynku A-1 obejrzano {int} z 5 pięter")
    public void inspectedFloors(final int covered) {
        this.data.building("A-1", 5);
        this.data.markedFloors("A-1", covered);
    }

    @Given("budynek B-8 ma znacznik całego budynku")
    public void inspectedBasic() {
        this.data.building("B-8", 0);
        this.data.marker("B-8", "");
    }

    @Given("w budynku A-1 są {int} wysłane, {int} w weryfikacji i {int} przyjęte zgłoszenia")
    public void submissionCounts(final int sent, final int reviewing, final int accepted) {
        this.data.submissions("A-1", "SENT", sent);
        this.data.submissions("A-1", "IN_REVIEW", reviewing);
        this.data.submissions("A-1", "ACCEPTED", accepted);
    }

    @When("Zbierający oznaczy budynek {string} na poziomie {int}")
    public void mark(final String symbol, final int floor) throws Exception {
        final String level;
        if (floor == 0) {
            level = "";
        } else {
            level = String.format(",\"floorId\":\"%d\"", floor);
        }
        this.send(String.format(
            """
            "type":"SET_INSPECTION_MARKER",
            "marker":{"id":"%s","buildingSymbol":"%s"%s}
            """,
            UUID.randomUUID(), ProgressFixtures.key(symbol), level
        ));
    }

    @When("do serwera dotrze cofnięcie znacznika z piętra {int} budynku A-1")
    public void revoke(final int floor) throws Exception {
        final UUID marker = this.jdbc.sql(
            """
            SELECT id FROM inspection_markers
            WHERE building_symbol = :symbol AND floor_id = :floor AND NOT revoked
            """
        ).param("symbol", ProgressFixtures.key("A-1")).param("floor", Integer.toString(floor))
            .query(UUID.class).single();
        this.send(String.format(
            "\"type\":\"REVOKE_INSPECTION_MARKER\",\"markerId\":\"%s\"", marker
        ));
    }

    @When("Koordynator otworzy postęp inwentaryzacji")
    public void read() throws Exception {
        this.progress = this.mapper.readTree(
            this.http.perform(
                MockMvcRequestBuilders.get("/progress")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer COORDINATOR")
            ).andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8)
        );
    }

    @Then("postęp {string} to {int} z {int} pięter, czyli {int} procent")
    public void coveredFloors(
        final String symbol, final int covered, final int total, final int percent
    ) {
        final JsonNode building = this.building(symbol);
        final int seen = building.path("floorsInspected").asInt(-1);
        final int listed = building.path("floorsTotal").asInt(-1);
        MatcherAssert.assertThat("Budynek ma piętra", listed, Matchers.greaterThan(0));
        MatcherAssert.assertThat(
            "Piętra obejrzane, wszystkie piętra i procent postępu",
            List.of(seen, listed, 100 * seen / listed),
            Matchers.contains(covered, total, percent)
        );
    }

    @Then("postęp {string} po oznaczeniu to {int} z {int}")
    public void marked(final String symbol, final int covered, final int total) {
        if (total == 0) {
            this.coveredWhole(symbol);
        } else {
            this.coveredFloors(symbol, covered, total, 100 * covered / total);
        }
    }

    @Then("budynek {string} jest obejrzany w całości")
    public void coveredWhole(final String symbol) {
        MatcherAssert.assertThat(
            "Znacznik obejrzenia całego budynku",
            this.building(symbol).path("inspected").asBoolean(false), Matchers.is(true)
        );
    }

    @Then("postęp A-1 pokazuje {int} zgłoszeń czekających i {int} przyjętych")
    public void countedSubmissions(final int pending, final int accepted) {
        final JsonNode building = this.building("A-1");
        MatcherAssert.assertThat(
            "Zgłoszenia czekające i przyjęte",
            List.of(
                building.path("pendingSubmissions").asInt(-1),
                building.path("acceptedSubmissions").asInt(-1)
            ),
            Matchers.contains(pending, accepted)
        );
    }

    private void send(final String operation) throws Exception {
        final JsonNode reply = this.mapper.readTree(
            this.http.perform(
                MockMvcRequestBuilders.post("/sync/push").with(new AsCollector())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(String.format(
                        "{\"operations\":[{\"clientOperationId\":\"%s\",%s}]}",
                        UUID.randomUUID(), operation
                    ))
            ).andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8)
        );
        MatcherAssert.assertThat(
            "Serwer przyjął operację znacznika",
            reply.path("results").path(0).path("outcome").asString(), Matchers.is("APPLIED")
        );
    }

    private JsonNode building(final String symbol) {
        return this.progress.valueStream().filter(
            item -> ProgressFixtures.key(symbol).equals(item.path("buildingSymbol").asString())
        ).findFirst().orElseThrow();
    }
}
