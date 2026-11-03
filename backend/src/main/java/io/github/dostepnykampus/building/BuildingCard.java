/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building;

import java.util.List;
import tools.jackson.databind.JsonNode;

/**
 * Karta budynku, zgodna ze schematem BuildingDetails (US2).
 *
 * @param symbol Symbol budynku, np. C-13
 * @param name Nazwa budynku
 * @param detailLevel Poziom szczegółowości: DETAILED albo BASIC
 * @param outline Kontur budynku jako GeoJSON
 * @param entrances Wejścia budynku; pusta lista, gdy dane nie mają wejść
 * @since 0.1.0
 */
public record BuildingCard(
    String symbol,
    String name,
    String detailLevel,
    JsonNode outline,
    List<Entrance> entrances
) {
}
