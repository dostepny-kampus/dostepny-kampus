/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.domain;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Karta budynku z wejściami i wyjściami ewakuacyjnymi.
 *
 * <p>Wymagania: US2, US2.6, BR-27</p>
 *
 * @param symbol Symbol budynku, np. C-13
 * @param name Nazwa budynku albo null, gdy budynek jej nie ma
 * @param detailLevel Poziom szczegółowości: DETAILED albo BASIC
 * @param outline Kontur budynku jako tekst GeoJSON
 * @param entrances Wejścia budynku; pusta lista, gdy dane nie mają wejść
 * @param evacuationExits Wyjścia ewakuacyjne budynku; pusta lista, gdy dane ich nie podają
 * @since 0.1.0
 */
public record BuildingCard(
    String symbol,
    @Nullable String name,
    String detailLevel,
    String outline,
    List<Entrance> entrances,
    List<EvacuationExit> evacuationExits
) {
}
