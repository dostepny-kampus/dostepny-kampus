/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.dostepnykampus.building.Building;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Budynek w odpowiedzi HTTP, zgodny ze schematem Building.
 *
 * <p>Wymagania: US1</p>
 *
 * @param symbol Symbol budynku, np. C-13
 * @param name Nazwa budynku albo null, gdy budynek jej nie ma
 * @param detailLevel Poziom szczegółowości: DETAILED albo BASIC
 * @param outline Kontur budynku jako GeoJSON
 * @since 0.1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BuildingResponse(
    String symbol,
    @Nullable String name,
    String detailLevel,
    JsonNode outline
) {

    /**
     * Buduje odpowiedź z budynku.
     *
     * @param building Budynek
     * @param mapper Parser konturu GeoJSON
     * @return Odpowiedź z konturem jako drzewem JSON
     */
    public static BuildingResponse of(final Building building, final ObjectMapper mapper) {
        return new BuildingResponse(
            building.symbol(), building.name(), building.detailLevel(),
            mapper.readTree(building.outline())
        );
    }
}
