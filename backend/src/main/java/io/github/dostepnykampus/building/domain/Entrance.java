/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.domain;

import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Wejście budynku z karty budynku.
 *
 * <p>Wymagania: US2, US17, BR-24, US18.2, BR-27</p>
 *
 * @param id Identyfikator wejścia, np. node/1167953448
 * @param name Opis wejścia albo null, gdy dane go nie podają
 * @param type Rodzaj wejścia z OpenStreetMap albo null
 * @param main Czy to wejście główne
 * @param location Położenie wejścia jako tekst punktu GeoJSON
 * @param floorId Piętro wejścia z listy pięter budynku albo null, gdy dane go nie podają
 * @param marking Oznaczenie wejścia
 * @param layer Obecność wejścia w warstwie wejść planów PWr z jej danymi
 * @since 0.1.0
 */
public record Entrance(
    String id,
    @Nullable String name,
    @Nullable String type,
    boolean main,
    String location,
    @Nullable String floorId,
    Marking marking,
    Layer layer
) {

    /**
     * Oznaczenie wejścia z datą i źródłem.
     *
     * <p>Wymagania: BR-24, US17.3</p>
     *
     * @param marking Oznaczenie: ACCESSIBLE, INACCESSIBLE albo NO_DATA
     * @param markingDate Dzień oznaczenia w zapisie RRRR-MM-DD
     * @param markingSource Pochodzenie oznaczenia: IMPORT albo WALKTHROUGH
     * @since 0.1.0
     */
    public record Marking(String marking, String markingDate, String markingSource) {
    }

    /**
     * Wejście w warstwie wejść planów PWr albo poza nią.
     *
     * <p>Wymagania: BR-27, US17.3</p>
     *
     * @since 0.1.0
     */
    public sealed interface Layer permits Layer.Listed, Layer.Unlisted {

        /**
         * Wejście jest w warstwie wejść planów PWr.
         *
         * @param approach Opis dojścia z warstwy
         * @param door Drzwi wejścia z warstwy
         * @since 0.1.0
         */
        record Listed(Approach approach, Door door) implements Layer {
        }

        /**
         * Wejścia nie ma w warstwie wejść planów PWr.
         *
         * @since 0.1.0
         */
        record Unlisted() implements Layer {
        }
    }

    /**
     * Opis dojścia do wejścia z danych albo jego brak.
     *
     * <p>Wymagania: BR-27, US3.3</p>
     *
     * @since 0.1.0
     */
    public sealed interface Approach permits Approach.Described, Approach.Undescribed {

        /**
         * Opis dojścia, jeśli dane go podają.
         *
         * @return Opis albo pusty wynik
         */
        Optional<String> text();

        /**
         * Dane podają opis dojścia.
         *
         * @param description Opis dojścia, np. „Do wejścia prowadzą schody.”
         * @since 0.1.0
         */
        record Described(String description) implements Approach {

            @Override
            public Optional<String> text() {
                return Optional.of(this.description);
            }
        }

        /**
         * Dane nie podają opisu dojścia.
         *
         * @since 0.1.0
         */
        record Undescribed() implements Approach {

            @Override
            public Optional<String> text() {
                return Optional.empty();
            }
        }
    }
}
