/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.domain;

import java.util.List;
import java.util.Map;

/**
 * Lista budynków z atrybucją konturów i wejściami budynków.
 *
 * <p>Wymagania: US1, US1.4</p>
 *
 * @param source Atrybucja konturów budynków
 * @param buildings Budynki posortowane według symbolu
 * @param entrances Wejścia według symbolu budynku; budynku bez wejść nie ma w mapie
 * @since 0.1.0
 */
public record BuildingList(
    String source,
    List<Building> buildings,
    Map<String, List<Entrance>> entrances
) {

    /**
     * Wejścia budynku.
     *
     * @param symbol Symbol budynku, np. C-13
     * @return Wejścia budynku; pusta lista, gdy budynek ich nie ma
     */
    public List<Entrance> entrances(final String symbol) {
        return this.entrances.getOrDefault(symbol, List.of());
    }
}
