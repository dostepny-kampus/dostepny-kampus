/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building;

import tools.jackson.databind.JsonNode;

/**
 * Budynek PWr na mapie kampusu, zgodny ze schematem Building (US1).
 *
 * @param symbol Symbol budynku, np. C-13
 * @param name Nazwa budynku
 * @param detailLevel Poziom szczegółowości: DETAILED albo BASIC
 * @param outline Kontur budynku jako GeoJSON
 * @since 0.1.0
 */
public record Building(
    String symbol,
    String name,
    String detailLevel,
    JsonNode outline
) {
}
