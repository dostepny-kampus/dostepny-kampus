/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.specs;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.github.dostepnykampus.CampusBoundary;
import io.github.dostepnykampus.CampusPoint;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Kroki scenariuszy granicy terenu PWr: miejsce zgłoszenia względem granicy terenu.
 *
 * <p>Osobę i wysyłkę prowadzą kroki {@link QuickPlaceSteps} z tego samego scenariusza.</p>
 *
 * <p>Wymagania: US4.9, BR-G5</p>
 *
 * @since 0.1.0
 */
public final class QuickAreaSteps {

    /**
     * Baza aplikacji.
     */
    private final JdbcClient jdbc;

    /**
     * Kroki osoby i wysyłki z tego samego scenariusza.
     */
    private final QuickPlaceSteps sender;

    /**
     * Kroki na kontekście aplikacji.
     *
     * @param jdbc Baza aplikacji
     * @param sender Kroki osoby i wysyłki z tego samego scenariusza
     */
    public QuickAreaSteps(final JdbcClient jdbc, final QuickPlaceSteps sender) {
        this.jdbc = jdbc;
        this.sender = sender;
    }

    /**
     * Osoba przygotowała zdjęcie i grupę barier; wysyłka idzie z kroku o pozycji.
     */
    @Given("zrobił zdjęcie bariery i wybrał grupę barier „komunikacja pozioma”")
    public void preparesQuick() {
        MatcherAssert.assertThat(
            "Zgłoszenie przygotowuje zalogowana osoba",
            this.sender.loggedIn(), Matchers.is(true)
        );
    }

    /**
     * Zgłoszenie przychodzi z miejsca za granicą terenów PWr.
     *
     * @param meters Odległość od granicy w metrach
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @When("^aplikacja wyśle zgłoszenie z miejscem (\\d+) m za granicą terenu PWr$")
    public void sendsFromAfar(final int meters) throws Exception {
        this.sender.sendFrom(new CampusBoundary(this.jdbc).beyond(meters).json());
    }

    /**
     * Osoba ustala pozycję w jednym z miejsc szablonu granicy terenu i wysyła zgłoszenie.
     *
     * @param spot Miejsce słowami scenariusza
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @When("^telefon ustali pozycję (w przerwie 60 m .+|20 m za .+)$")
    public void sendsFromSpot(final String spot) throws Exception {
        final CampusPoint point;
        if (spot.startsWith("w przerwie")) {
            point = CampusPoint.GAP;
        } else if (spot.startsWith("20 m za obszarem")) {
            point = CampusPoint.MARGIN;
        } else {
            point = new CampusBoundary(this.jdbc).beyond(20);
        }
        this.sender.sendFrom(point.json());
    }
}
