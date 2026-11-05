/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

/**
 * Budynek PWr na mapie kampusu, zgodny ze schematem Building (US1).
 *
 * @param symbol Symbol budynku, np. C-13
 * @param name Nazwa budynku albo null, gdy budynek jej nie ma
 * @param detailLevel Poziom szczegółowości: DETAILED albo BASIC
 * @param outline Kontur budynku jako GeoJSON
 * @since 0.1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Building(
    String symbol,
    @Nullable String name,
    String detailLevel,
    JsonNode outline
) {
}
