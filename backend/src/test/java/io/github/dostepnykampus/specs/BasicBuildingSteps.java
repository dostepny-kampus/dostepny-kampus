/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.specs;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Kroki szablonu „W budynku podstawowym zgłoszenie można złożyć zawsze”
 * (BR-G1): serwer przyjmuje zgłoszenie w budynku bez planów pięter.
 *
 * <p>Osobę, jej miejsce i wysyłkę prowadzą kroki {@link QuickPlaceSteps}
 * tego samego scenariusza.</p>
 *
 * @since 0.1.0
 */
public final class BasicBuildingSteps {

    /**
     * Pola zgłoszenia szybkiego z grupą „wejście i drzwi”.
     */
    private static final String QUICK =
        "\"kind\":\"QUICK\",\"barrierGroup\":\"ENTRANCE_AND_DOORS\"";

    /**
     * Pola pomiaru precyzyjnego TB-13 z piętrem 0; brakuje symbolu budynku.
     */
    private static final String PRECISE = String.join(
        "",
        "\"kind\":\"PRECISE\",\"floor\":{\"number\":0},\"severity\":2,",
        "\"barrierGroup\":\"HORIZONTAL_CIRCULATION\",\"barrierType\":\"TB-13\",",
        "\"buildingSymbol\":\"%s\""
    );

    /**
     * Baza aplikacji.
     */
    private final JdbcClient jdbc;

    /**
     * Kroki osoby i wysyłki z tego samego scenariusza.
     */
    private final QuickPlaceSteps sender;

    /**
     * Budynek podstawowy z założeń pliku.
     */
    private String building;

    /**
     * Kroki na kontekście aplikacji.
     *
     * @param jdbc Baza aplikacji
     * @param sender Kroki osoby i wysyłki z tego samego scenariusza
     */
    public BasicBuildingSteps(final JdbcClient jdbc, final QuickPlaceSteps sender) {
        this.jdbc = jdbc;
        this.sender = sender;
        this.building = "";
    }

    /**
     * Serwer ma budynek z konturem i bez planów pięter.
     *
     * @param symbol Symbol budynku
     */
    @Given("^budynek (\\S+) jest budynkiem podstawowym z konturem i wejściami z OpenStreetMap$")
    public void basicBuilding(final String symbol) {
        MatcherAssert.assertThat(
            String.format("Serwer nie ma budynku podstawowego %s", symbol),
            this.jdbc.sql("SELECT detail_level FROM buildings WHERE symbol = :symbol")
                .param("symbol", symbol).query(String.class).list(),
            Matchers.contains("BASIC")
        );
        this.building = symbol;
    }

    /**
     * Osoba wysyła zgłoszenie szybkie albo pomiar precyzyjny ze swojego miejsca.
     *
     * @param kind Początek opisu zgłoszenia ze scenariusza
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @When("^złoży zgłoszenie (szybkie|TB-13) .+ z kompletem wymaganych danych$")
    public void submits(final String kind) throws Exception {
        if ("szybkie".equals(kind)) {
            this.sender.send(BasicBuildingSteps.QUICK);
        } else {
            this.sender.send(String.format(BasicBuildingSteps.PRECISE, this.building));
        }
    }

    /**
     * Serwer zapisał zgłoszenie.
     */
    @Then("zgłoszenie zapisuje się bez komunikatu o błędzie")
    public void saved() {
        MatcherAssert.assertThat(
            "Serwer nie zapisał zgłoszenia w budynku podstawowym",
            this.sender.outcome(), Matchers.startsWith("201 ")
        );
    }
}
