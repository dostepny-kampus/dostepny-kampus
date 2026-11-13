/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building;

import io.github.dostepnykampus.building.locate.Placement;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Położenie punktu staje się odpowiedzią REST bez
 * pomyłki między konturem, sąsiedztwem i brakiem budynku.
 *
 * <p>Wymagania: BR-G2, BR-G4</p>
 *
 * @since 0.1.0
 */
final class LocateResultsTest {

    @Test
    void brGTwoMarksPointInsideOutline() {
        MatcherAssert.assertThat(
            "Punkt w konturze musi mieć insideOutline=true",
            new LocateResults(new Placement.Inside("A-1")).result(),
            Matchers.equalTo(new LocateResult("A-1", true))
        );
    }

    @Test
    void brGTwoMarksNearPointOutsideOutline() {
        MatcherAssert.assertThat(
            "Punkt blisko konturu musi mieć symbol i insideOutline=false",
            new LocateResults(new Placement.Near("A-1")).result(),
            Matchers.equalTo(new LocateResult("A-1", false))
        );
    }

    @Test
    void brGFourLeavesFarPointWithoutSymbol() {
        MatcherAssert.assertThat(
            "Punkt poza budynkami nie ma symbolu i insideOutline=false",
            new LocateResults(new Placement.None()).result(),
            Matchers.equalTo(new LocateResult(null, false))
        );
    }
}
