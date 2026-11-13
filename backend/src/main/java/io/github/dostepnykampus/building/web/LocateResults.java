/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.web;

import io.github.dostepnykampus.building.domain.Placement;

/**
 * Jedyne miejsce, w którym położenie punktu staje się odpowiedzią REST
 * LocateResult; brak symbolu pojawia się dopiero tutaj, na granicy.
 *
 * <p>Wymagania: BR-G4</p>
 *
 * @since 0.1.0
 */
final class LocateResults {

    /**
     * Położenie punktu.
     */
    private final Placement place;

    LocateResults(final Placement place) {
        this.place = place;
    }

    /**
     * Zamienia położenie na odpowiedź REST.
     *
     * @return Odpowiedź: symbol i flaga konturu, bez symbolu poza budynkami
     */
    LocateResult result() {
        return new LocateResult(
            this.place.found().orElse(null),
            this.place.inside()
        );
    }
}
