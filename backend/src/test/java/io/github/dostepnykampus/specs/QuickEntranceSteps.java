/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.specs;

import io.cucumber.java.After;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.ArrayList;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Kroki zgłoszenia szybkiego przy wejściu, które rozstrzyga serwer: miejsce,
 * budynek i piętro od wejścia zamiast bieżącej pozycji.
 *
 * <p>Wejście główne scenariusza kroki wstawiają w punkcie wewnątrz konturu budynku,
 * z piętrem P0. Osobę i wysyłkę prowadzą kroki {@link QuickPlaceSteps}.</p>
 *
 * <p>Wymagania: US4.6, BR-8, BR-14</p>
 *
 * @since 0.1.0
 */
public final class QuickEntranceSteps {

    /**
     * Wejście główne scenariusza.
     */
    private static final String ENTRANCE = "node/spec-wejscie";

    /**
     * Baza aplikacji.
     */
    private final JdbcClient jdbc;

    /**
     * Kroki osoby i wysyłki zgłoszenia.
     */
    private final QuickPlaceSteps sender;

    /**
     * Kroki na kontekście aplikacji.
     *
     * @param jdbc Baza aplikacji
     * @param sender Kroki osoby i wysyłki zgłoszenia
     */
    public QuickEntranceSteps(final JdbcClient jdbc, final QuickPlaceSteps sender) {
        this.jdbc = jdbc;
        this.sender = sender;
    }

    /**
     * Osoba wysyła zgłoszenie szybkie z wejściem głównym budynku, bez pola location.
     *
     * @param symbol Symbol budynku
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @When(
        "^wyśle zgłoszenie szybkie ze zdjęciem, grupą barier i wejściem głównym (\\S+), "
            + "bez bieżącej pozycji$"
    )
    public void sendsAtEntrance(final String symbol) throws Exception {
        this.jdbc.sql(
            String.join(
                " ",
                "INSERT INTO entrances (id, building_symbol, main, location, floor_id, marking,",
                "marking_date, marking_source)",
                "SELECT :id, symbol, TRUE, ST_PointOnSurface(outline), 'P0', 'NO_DATA',",
                "DATE '2026-11-01', 'IMPORT' FROM buildings WHERE symbol = :symbol"
            )
        ).param("id", QuickEntranceSteps.ENTRANCE).param("symbol", symbol).update();
        this.sender.send(QuickEntranceSteps.fields(QuickEntranceSteps.ENTRANCE));
    }

    /**
     * Serwer zapisał zgłoszenie w punkcie wejścia, z jego budynkiem i piętrem.
     *
     * @param symbol Symbol budynku
     */
    @Then(
        "^zgłoszenie ma miejsce w punkcie wejścia głównego (\\S+), budynek \\1 "
            + "i piętro tego wejścia$"
    )
    public void placedAtEntrance(final String symbol) {
        MatcherAssert.assertThat(
            "Serwer nie przyjął zgłoszenia przy wejściu albo nie dał mu piętra wejścia",
            this.sender.outcome(),
            Matchers.allOf(
                Matchers.startsWith("201 "),
                Matchers.containsString("\"floorId\":\"P0\"")
            )
        );
        MatcherAssert.assertThat(
            "Zgłoszenie nie leży w punkcie wejścia albo ma inny budynek",
            this.jdbc.sql(
                String.join(
                    " ",
                    "SELECT s.building_symbol FROM submissions s JOIN entrances e",
                    "ON e.id = :entrance AND ST_Equals(s.location, e.location)",
                    "WHERE s.id = :id"
                )
            ).param("entrance", QuickEntranceSteps.ENTRANCE)
                .param("id", QuickPlaceSteps.SUBMISSION)
                .query(String.class).list(),
            Matchers.contains(symbol)
        );
    }

    /**
     * Serwer odrzuca zgłoszenie bez bieżącej pozycji i bez wejścia oraz z nieznanym wejściem.
     *
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @Then(
        "zgłoszenie bez bieżącej pozycji i bez wejścia albo z nieznanym wejściem serwer odrzuca"
    )
    public void refusesWithoutPlace() throws Exception {
        final List<String> outcomes = new ArrayList<>(2);
        for (final String fields : List.of(
            "\"kind\":\"QUICK\",\"barrierGroup\":\"ENTRANCE_AND_DOORS\"",
            QuickEntranceSteps.fields("node/0")
        )) {
            this.sender.send(fields);
            outcomes.add(this.sender.outcome().substring(0, 4));
        }
        MatcherAssert.assertThat(
            "Serwer przyjął zgłoszenie bez miejsca albo z nieznanym wejściem",
            outcomes, Matchers.contains("400 ", "400 ")
        );
    }

    /**
     * Usuwa wejście scenariusza po zgłoszeniu.
     */
    @After(order = 0)
    public void removeEntrance() {
        this.jdbc.sql("DELETE FROM entrances WHERE id = :id")
            .param("id", QuickEntranceSteps.ENTRANCE).update();
    }

    private static String fields(final String entrance) {
        return String.format(
            "\"kind\":\"QUICK\",\"barrierGroup\":\"ENTRANCE_AND_DOORS\",\"entranceId\":\"%s\"",
            entrance
        );
    }
}
