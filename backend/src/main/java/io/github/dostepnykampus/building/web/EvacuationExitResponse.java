/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRawValue;
import io.github.dostepnykampus.building.domain.EvacuationExit;
import io.github.dostepnykampus.building.domain.FloorList;
import org.jspecify.annotations.Nullable;

/**
 * Wyjście ewakuacyjne w odpowiedzi HTTP, zgodne ze schematem EvacuationExit.
 *
 * <p>Wymagania: US2.6, BR-27, NFR-40</p>
 *
 * @param id Identyfikator wyjścia
 * @param location Położenie wyjścia jako punkt GeoJSON z bazy, wstawiany do odpowiedzi bez zmian
 * @param floorId Piętro wyjścia albo null, gdy dane go nie podają
 * @param description Opis wyjścia z danych
 * @param doorWidthCm Szerokość drzwi w centymetrach albo null, gdy dane jej nie podają
 * @param source Nazwa źródła danych planów pięter
 * @since 0.1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EvacuationExitResponse(
    String id,
    @JsonRawValue String location,
    @Nullable String floorId,
    String description,
    @Nullable Integer doorWidthCm,
    String source
) {

    /**
     * Odpowiedź z wyjścia ewakuacyjnego.
     *
     * @param exit Wyjście ewakuacyjne
     */
    public EvacuationExitResponse(final EvacuationExit exit) {
        this(
            exit.id(), exit.location(), exit.floorId(), exit.description(), exit.doorWidthCm(),
            FloorList.PWR_SOURCE
        );
    }
}
