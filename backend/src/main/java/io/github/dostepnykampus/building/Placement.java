/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building;

import java.util.Objects;

/**
 * Położenie punktu względem budynków kampusu (BR-G2, BR-G4): w konturze,
 * blisko konturu albo poza zasięgiem podpowiedzi.
 *
 * @since 0.1.0
 */
sealed interface Placement
    permits Placement.Inside, Placement.Near, Placement.None {

    /**
     * Punkt leży w konturze budynku.
     *
     * @param symbol Symbol budynku
     * @since 0.1.0
     */
    record Inside(String symbol) implements Placement {

        /**
         * Położenie w konturze; brak symbolu kończy się wyjątkiem
         * NullPointerException.
         *
         * @param symbol Symbol budynku
         */
        public Inside {
            Objects.requireNonNull(symbol, "symbol");
        }
    }

    /**
     * Punkt leży poza konturem, ale w promieniu podpowiedzi od budynku.
     *
     * @param symbol Symbol najbliższego budynku
     * @since 0.1.0
     */
    record Near(String symbol) implements Placement {

        /**
         * Położenie blisko konturu; brak symbolu kończy się wyjątkiem
         * NullPointerException.
         *
         * @param symbol Symbol budynku
         */
        public Near {
            Objects.requireNonNull(symbol, "symbol");
        }
    }

    /**
     * W promieniu podpowiedzi nie ma żadnego budynku.
     *
     * @since 0.1.0
     */
    record None() implements Placement {
    }
}
