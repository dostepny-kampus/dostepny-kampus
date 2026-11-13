/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

/**
 * Wejście budynku na karcie, zgodne ze schematem Entrance.
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
public record Entrance(
    String id,
    String buildingSymbol,
    @Nullable String name,
    @Nullable String type,
    boolean main,
    JsonNode location,
    @JsonUnwrapped Marking marking
) {

    /**
     * Oznaczenie wejścia z datą i źródłem.
     *
     * <p>Wymagania: BR-24, US17.3</p>
     *
     * @param marking Oznaczenie: ACCESSIBLE, INACCESSIBLE albo NO_DATA
     * @param markingDate Dzień oznaczenia w zapisie RRRR-MM-DD
     * @param markingSource Pochodzenie oznaczenia: IMPORT albo WALKTHROUGH
     * @since 0.1.0
     */
    public record Marking(String marking, String markingDate, String markingSource) {
    }
}
