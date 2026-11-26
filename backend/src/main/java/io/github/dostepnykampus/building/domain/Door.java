/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.domain;

import java.util.Optional;

/**
 * Drzwi wejścia albo wyjścia ewakuacyjnego z planów PWr: zmierzona szerokość albo jej brak.
 *
 * <p>Wymagania: BR-27, US2.1, US3.3</p>
 *
 * @since 0.1.0
 */
public sealed interface Door permits Door.Measured, Door.Unmeasured {

    /**
     * Szerokość drzwi w centymetrach, jeśli dane ją podają.
     *
     * @return Szerokość większa od 0 albo pusty wynik
     */
    Optional<Integer> widthCm();

    /**
     * Dane podają szerokość drzwi.
     *
     * @param centimeters Szerokość w centymetrach, większa od 0
     * @since 0.1.0
     */
    record Measured(int centimeters) implements Door {

        /**
         * Zmierzone drzwi; szerokość 0 albo ujemna kończy się wyjątkiem
         * IllegalArgumentException.
         *
         * @param centimeters Szerokość w centymetrach
         */
        public Measured {
            if (centimeters <= 0) {
                throw new IllegalArgumentException(
                    String.format("Szerokość drzwi %d cm nie jest dodatnia", centimeters)
                );
            }
        }

        @Override
        public Optional<Integer> widthCm() {
            return Optional.of(this.centimeters);
        }
    }

    /**
     * Dane nie podają szerokości drzwi.
     *
     * @since 0.1.0
     */
    record Unmeasured() implements Door {

        @Override
        public Optional<Integer> widthCm() {
            return Optional.empty();
        }
    }
}
