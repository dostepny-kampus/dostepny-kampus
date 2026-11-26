/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.api;

import java.util.List;
import java.util.Set;

/**
 * Stan katalogu budynków potrzebny do odczytu postępu inwentaryzacji.
 *
 * <p>Wymagania: US11.1, US11.4, BR-24</p>
 *
 * @since 0.1.0
 */
@FunctionalInterface
public interface BuildingInventory {

    /**
     * Odczytuje budynki wraz z aktualnymi piętrami i licznikami wejść.
     *
     * @return Budynki w kolejności symboli
     */
    List<Building> buildings();

    /**
     * Piętra i wejścia jednego budynku.
     *
     * @param symbol Symbol budynku
     * @param detailed Czy budynek ma poziom szczegółowy
     * @param floors Identyfikatory aktualnych pięter
     * @param entrances Wszystkie wejścia budynku
     * @param checked Wejścia sprawdzone na miejscu
     * @since 0.1.0
     */
    record Building(
        String symbol, boolean detailed, Set<String> floors, int entrances, int checked
    ) {

        /**
         * Utrwala niemodyfikowalny zbiór pięter.
         *
         * @param symbol Symbol budynku
         * @param detailed Czy budynek ma poziom szczegółowy
         * @param floors Identyfikatory aktualnych pięter
         * @param entrances Wszystkie wejścia budynku
         * @param checked Wejścia sprawdzone na miejscu
         */
        public Building {
            floors = Set.copyOf(floors);
        }
    }
}
