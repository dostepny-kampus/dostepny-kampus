/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.github.dostepnykampus.building.domain.Entrance;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Wejście budynku w odpowiedzi HTTP, zgodne ze schematem Entrance.
 *
 * <p>Wymagania: US2, US17, BR-24</p>
 *
 * @param id Identyfikator wejścia, np. node/1167953448
 * @param buildingSymbol Budynek, do którego prowadzi wejście
 * @param name Opis wejścia albo null, gdy dane go nie podają
 * @param type Rodzaj wejścia z OpenStreetMap albo null
 * @param main Czy to wejście główne
 * @param location Położenie wejścia jako punkt GeoJSON
 * @param marking Oznaczenie wejścia, w JSON pola obok pozostałych
 * @since 0.1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EntranceResponse(
    String id,
    String buildingSymbol,
    @Nullable String name,
    @Nullable String type,
    boolean main,
    JsonNode location,
    @JsonUnwrapped Entrance.Marking marking
) {

    /**
     * Odpowiedź z wejścia.
     *
     * @param entrance Wejście
     * @param mapper Parser położenia GeoJSON
     */
    public EntranceResponse(final Entrance entrance, final ObjectMapper mapper) {
        this(
            entrance.id(), entrance.buildingSymbol(), entrance.name(), entrance.type(),
            entrance.main(), mapper.readTree(entrance.location()), entrance.marking()
        );
    }
}
