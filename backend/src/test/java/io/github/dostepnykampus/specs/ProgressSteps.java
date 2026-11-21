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
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
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

    /** Transakcja otwarta na czas scenariusza. */
    private TransactionStatus transaction;

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
    }

    /**
     * Izoluje dane jednego scenariusza.
     */
    @Before("@postep-http")
    void start() {
        this.transaction = this.manager.getTransaction(new DefaultTransactionDefinition());
        this.data = new ProgressFixtures(this.jdbc);
        this.progress = this.mapper.createArrayNode();
    }

    /**
     * Wycofuje dane scenariusza i zmiany wykonane przez żądania.
     */
    @After("@postep-http")
    void finish() {
        this.manager.rollback(this.transaction);
    }

    @Given("budynek {string} ma {int} pięter z danych testowych i nie ma znaczników")
    void emptyBuilding(final String symbol, final int floors) {
        this.data.building(symbol, floors);
    }

    @Given("w budynku A-1 obejrzano {int} z 5 pięter")
    void inspectedFloors(final int covered) {
        this.data.building("A-1", 5);
        this.data.markedFloors("A-1", covered);
    }

    @Given("budynek B-8 ma znacznik całego budynku")
    void inspectedBasic() {
        this.data.building("B-8", 0);
        this.data.marker("B-8", "");
    }

    @Given("w budynku A-1 są {int} wysłane, {int} w weryfikacji i {int} przyjęte zgłoszenia")
    void submissionCounts(final int sent, final int reviewing, final int accepted) {
        this.data.submissions("A-1", "SENT", sent);
        this.data.submissions("A-1", "IN_REVIEW", reviewing);
        this.data.submissions("A-1", "ACCEPTED", accepted);
    }

    @When("Zbierający oznaczy budynek {string} na poziomie {int}")
    void mark(final String symbol, final int floor) throws Exception {
        this.recordMarker(symbol, String.format(",\"floorId\":\"%d\"", floor));
    }

    @When("Zbierający oznaczy budynek {string} w całości")
    void markWhole(final String symbol) throws Exception {
        this.recordMarker(symbol, "");
    }

    @When("do serwera dotrze cofnięcie znacznika z piętra {int} budynku A-1")
    void revoke(final int floor) throws Exception {
        this.send(
            String.format(
                "\"type\":\"REVOKE_INSPECTION_MARKER\",\"markerId\":\"%s\"",
                this.data.markerAt("A-1", floor)
            )
        );
    }

    @When("Koordynator otworzy postęp inwentaryzacji")
    void read() throws Exception {
        this.progress = this.response(
            MockMvcRequestBuilders.get("/progress")
                .header(HttpHeaders.AUTHORIZATION, "Bearer COORDINATOR")
        );
    }

    @Then("postęp {string} pokazuje {int} z {int} pięter")
    void coveredFloors(final String symbol, final int covered, final int total) {
        final JsonNode building = this.building(symbol);
        MatcherAssert.assertThat(
            "Piętra obejrzane i wszystkie piętra",
            List.of(
                building.path("floorsInspected").asInt(-1), building.path("floorsTotal").asInt(-1)
            ),
            Matchers.contains(covered, total)
        );
    }

    @Then("postęp {string} wynosi {int} procent")
    void percent(final String symbol, final int expected) {
        final JsonNode building = this.building(symbol);
        final int covered = building.path("floorsInspected").asInt(-1);
        final int total = building.path("floorsTotal").asInt(-1);
        MatcherAssert.assertThat("Budynek ma piętra", total, Matchers.greaterThan(0));
        MatcherAssert.assertThat("Procent postępu", 100 * covered / total, Matchers.is(expected));
    }

    @Then("budynek {string} jest obejrzany w całości")
    void coveredWhole(final String symbol) {
        MatcherAssert.assertThat(
            "Znacznik obejrzenia całego budynku",
            this.building(symbol).path("inspected").asBoolean(false), Matchers.is(true)
        );
    }

    @Then("postęp A-1 ma {int} w liczniku czekających i {int} w liczniku przyjętych")
    void countedSubmissions(final int pending, final int accepted) {
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

    private void recordMarker(final String symbol, final String level) throws Exception {
        this.send(
            String.format(
                """
                "type":"SET_INSPECTION_MARKER",
                "marker":{"id":"%s","buildingSymbol":"%s"%s}
                """,
                UUID.randomUUID(), ProgressFixtures.key(symbol), level
            )
        );
    }

    private void send(final String operation) throws Exception {
        MatcherAssert.assertThat(
            "Serwer przyjął operację znacznika",
            this.response(
                MockMvcRequestBuilders.post("/sync/push").with(new AsCollector())
                    .contentType(MediaType.APPLICATION_JSON).content(
                        String.format(
                            "{\"operations\":[{\"clientOperationId\":\"%s\",%s}]}",
                            UUID.randomUUID(), operation
                        )
                    )
            ).path("results").path(0).path("outcome").asString(),
            Matchers.is("APPLIED")
        );
    }

    private JsonNode response(final RequestBuilder request) throws Exception {
        return this.mapper.readTree(
            this.http.perform(request).andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8)
        );
    }

    private JsonNode building(final String symbol) {
        return this.progress.valueStream().filter(
            item -> ProgressFixtures.key(symbol).equals(item.path("buildingSymbol").asString())
        ).findFirst().orElseThrow();
    }
}
