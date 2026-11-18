/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRawValue;
import io.github.dostepnykampus.building.domain.Building;
import io.github.dostepnykampus.building.domain.Entrance;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Budynek w odpowiedzi HTTP, zgodny ze schematem Building.
 *
 * <p>Wymagania: US1, US1.4</p>
 *
 * @param symbol Symbol budynku, np. C-13
 * @param name Nazwa budynku albo null, gdy budynek jej nie ma
 * @param detailLevel Poziom szczegółowości: DETAILED albo BASIC
 * @param outline Kontur budynku jako GeoJSON z bazy, wstawiany do odpowiedzi bez zmian
 * @param entrances Wejścia budynku; pusta lista, gdy budynek ich nie ma
 * @since 0.1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BuildingResponse(
    String symbol,
    @Nullable String name,
    String detailLevel,
    @JsonRawValue String outline,
    List<EntranceResponse> entrances
) {

    /**
     * Odpowiedź z budynku.
     *
     * @param building Budynek
     * @param entrances Wejścia budynku
     */
    public BuildingResponse(final Building building, final List<Entrance> entrances) {
        this(
            building.symbol(), building.name(), building.detailLevel(),
            building.outline(),
            entrances.stream()
                .map(entrance -> new EntranceResponse(building.symbol(), entrance)).toList()
        );
    }
}
