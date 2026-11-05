/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

/**
 * Karta budynku, zgodna ze schematem BuildingDetails (US2).
 *
 * @param symbol Symbol budynku, np. C-13
 * @param name Nazwa budynku albo null, gdy budynek jej nie ma
 * @param detailLevel Poziom szczegółowości: DETAILED albo BASIC
 * @param outline Kontur budynku jako GeoJSON
 * @param entrances Wejścia budynku; pusta lista, gdy dane nie mają wejść
 * @since 0.1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BuildingCard(
    String symbol,
    @Nullable String name,
    String detailLevel,
    JsonNode outline,
    List<Entrance> entrances
) {
}
