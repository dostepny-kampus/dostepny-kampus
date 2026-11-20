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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Kroki scenariusza „Inny Zbierający nie cofnie cudzego znacznika”: Zbierający stawia znacznik
 * piętra przez {@code POST /sync/push}, a inny Zbierający próbuje go cofnąć przez
 * {@code DELETE /inspection-markers/{markerId}}.
 *
 * <p>Token testowy ma zawsze to samo konto, więc po zapisie znacznika autorem w bazie zostaje
 * inne konto, a próbę cofnięcia wykonuje konto z tokenu.</p>
 *
 * <p>Wymagania: US15.2, BR-20</p>
 *
 * @since 0.1.0
 */
public final class ForeignMarkerSteps {

    /**
     * Znacznik piętra postawiony przez pierwszego Zbierającego.
     */
    private static final UUID MARKER =
        UUID.fromString("9a8b7c6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d");

    /**
     * Piętro budynku A-1 na potrzeby scenariusza.
     */
    private static final String FLOOR = "spec-a1-f0";

    /**
     * Klient żądań HTTP do aplikacji.
     */
    private final MockMvc http;

    /**
     * Baza aplikacji.
     */
    private final JdbcClient jdbc;

    /**
     * Status odpowiedzi na próbę cofnięcia, wspólny z krokiem „próba jest odrzucona”.
     */
    private final AttemptOutcome outcome;

    /**
     * Kroki na kontekście aplikacji.
     *
     * @param http Klient żądań HTTP do aplikacji
     * @param jdbc Baza aplikacji
     * @param outcome Status odpowiedzi na próbę, wspólny z innymi krokami
     */
    public ForeignMarkerSteps(
        final MockMvc http, final JdbcClient jdbc, final AttemptOutcome outcome
    ) {
        this.http = http;
        this.jdbc = jdbc;
        this.outcome = outcome;
    }

    /**
     * Zbierający oznacza piętro budynku A-1 przez {@code POST /sync/push}, a autorem znacznika
     * zostaje inne konto.
     *
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @Given("Zbierający oznaczył piętro budynku A-1 jako obejrzane")
    public void collectorMarkedFloor() throws Exception {
        this.jdbc.sql(
            """
            INSERT INTO floors (id, building_symbol, label, sort_order)
            VALUES (:floor, 'A-1', 'Parter', 0)
            ON CONFLICT (id, building_symbol) DO NOTHING
            """
        ).param("floor", ForeignMarkerSteps.FLOOR).update();
        MatcherAssert.assertThat(
            "Serwer nie zapisał znacznika piętra przez /sync/push",
            this.http.perform(
                MockMvcRequestBuilders.post("/sync/push")
                    .contentType(MediaType.APPLICATION_JSON)
                    .with(new AsCollector())
                    .content(
                        String.join(
                            "",
                            "{\"operations\":[{\"clientOperationId\":\"",
                            UUID.randomUUID().toString(),
                            "\",\"type\":\"SET_INSPECTION_MARKER\",\"marker\":{\"id\":\"",
                            ForeignMarkerSteps.MARKER.toString(),
                            "\",\"buildingSymbol\":\"A-1\",\"floorId\":\"",
                            ForeignMarkerSteps.FLOOR,
                            "\"}}]}"
                        )
                    )
            ).andReturn().getResponse().getContentAsString(),
            Matchers.containsString("\"outcome\":\"APPLIED\"")
        );
        this.jdbc.sql("UPDATE inspection_markers SET author = 'autor-znacznika' WHERE id = :id")
            .param("id", ForeignMarkerSteps.MARKER).update();
    }

    /**
     * Inny Zbierający próbuje cofnąć cudzy znacznik przez {@code DELETE /inspection-markers/{id}}.
     *
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @When("inny Zbierający spróbuje cofnąć ten znacznik")
    public void otherCollectorRevokes() throws Exception {
        this.outcome.record(
            this.http.perform(
                MockMvcRequestBuilders.delete(
                    "/inspection-markers/{id}", ForeignMarkerSteps.MARKER
                ).with(new AsCollector())
            ).andReturn().getResponse().getStatus()
        );
    }

    /**
     * Znacznik nadal nie jest cofnięty.
     */
    @Then("piętro jest nadal obejrzane")
    public void floorStillInspected() {
        MatcherAssert.assertThat(
            "Cudzy znacznik został cofnięty",
            this.jdbc.sql("SELECT revoked FROM inspection_markers WHERE id = :id")
                .param("id", ForeignMarkerSteps.MARKER).query(Boolean.class).single(),
            Matchers.is(false)
        );
    }

    /**
     * Usuwa znacznik i piętro scenariusza.
     */
    @After
    public void removeMarker() {
        this.jdbc.sql("DELETE FROM inspection_markers WHERE id = :id")
            .param("id", ForeignMarkerSteps.MARKER).update();
        this.jdbc.sql("DELETE FROM floors WHERE id = :floor AND building_symbol = 'A-1'")
            .param("floor", ForeignMarkerSteps.FLOOR).update();
    }
}
