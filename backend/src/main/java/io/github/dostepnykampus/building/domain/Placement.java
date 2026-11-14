/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.domain;

import java.util.Objects;
import java.util.Optional;

/**
 * Położenie punktu względem budynków kampusu: w konturze,
 * blisko konturu albo poza zasięgiem podpowiedzi.
 *
 * <p>Wymagania: BR-G2, BR-G4</p>
 *
 * @since 0.1.0
 */
public sealed interface Placement
    permits Placement.Inside, Placement.Near, Placement.None {

    /**
     * Symbol budynku, jeśli jakiś jest w zasięgu podpowiedzi.
     *
     * @return Symbol budynku albo pusty wynik poza zasięgiem
     */
    Optional<String> found();

    /**
     * Czy punkt leży w konturze budynku.
     *
     * @return Prawda tylko dla punktu w konturze
     */
    boolean inside();

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

        @Override
        public Optional<String> found() {
            return Optional.of(this.symbol);
        }

        @Override
        public boolean inside() {
            return true;
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

        @Override
        public Optional<String> found() {
            return Optional.of(this.symbol);
        }

        @Override
        public boolean inside() {
            return false;
        }
    }

    /**
     * W promieniu podpowiedzi nie ma żadnego budynku.
     *
     * @since 0.1.0
     */
    record None() implements Placement {

        @Override
        public Optional<String> found() {
            return Optional.empty();
        }

        @Override
        public boolean inside() {
            return false;
        }
    }
}
