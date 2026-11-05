/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building;

/**
 * Jedyne miejsce, w którym położenie punktu staje się odpowiedzią REST
 * LocateResult; brak symbolu pojawia się dopiero tutaj, na granicy (BR-G4).
 *
 * @since 0.1.0
 */
final class LocateResults {

    /**
     * Zamienia położenie na odpowiedź REST.
     *
     * @param place Położenie punktu
     * @return Odpowiedź: symbol i flaga konturu, bez symbolu poza budynkami
     */
    LocateResult result(final Placement place) {
        return switch (place) {
            case Placement.Inside inside ->
                new LocateResult(inside.symbol(), true);
            case Placement.Near near ->
                new LocateResult(near.symbol(), false);
            case Placement.None none ->
                new LocateResult(null, false);
        };
    }
}
