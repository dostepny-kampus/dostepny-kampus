/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.github.dostepnykampus.building.domain.Entrance;
import io.github.dostepnykampus.building.domain.FloorList;
import org.jspecify.annotations.Nullable;

/**
 * Wejście budynku w odpowiedzi HTTP, zgodne ze schematem Entrance.
 *
 * <p>Wymagania: US2, US17, BR-24, US18.2, BR-27, BR-25</p>
 *
 * @param id Identyfikator wejścia, np. node/1167953448
 * @param buildingSymbol Budynek, do którego prowadzi wejście
 * @param name Opis wejścia albo null, gdy dane go nie podają
 * @param generatedName Czy nazwę nadał import; w JSON tylko wartość true
 * @param type Rodzaj wejścia z OpenStreetMap albo null
 * @param main Czy to wejście główne
 * @param place Położenie i piętro wejścia, w JSON pola obok pozostałych
 * @param marking Oznaczenie wejścia, w JSON pola obok pozostałych
 * @param plan Dane z planów PWr, w JSON pola obok pozostałych; null przy wejściu spoza warstwy
 * @since 0.1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EntranceResponse(
    String id,
    String buildingSymbol,
    @Nullable String name,
    @JsonInclude(JsonInclude.Include.NON_DEFAULT) boolean generatedName,
    @Nullable String type,
    boolean main,
    @JsonUnwrapped Place place,
    @JsonUnwrapped Entrance.Marking marking,
    @JsonUnwrapped @Nullable Plan plan
) {

    /**
     * Odpowiedź z wejścia budynku.
     *
     * @param building Symbol budynku, do którego prowadzi wejście
     * @param entrance Wejście
     */
    public EntranceResponse(final String building, final Entrance entrance) {
        this(
            entrance.id(), building, entrance.name(), entrance.generatedName(),
            entrance.type(), entrance.main(),
            new Place(entrance.location(), entrance.floorId()), entrance.marking(),
            Plan.of(entrance.layer())
        );
    }

    /**
     * Położenie i piętro wejścia w odpowiedzi HTTP.
     *
     * <p>Wymagania: US2, US18.2</p>
     *
     * @param location Położenie wejścia jako punkt GeoJSON z bazy, wstawiany bez zmian
     * @param floorId Piętro wejścia albo null, gdy dane go nie podają
     * @since 0.1.0
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Place(@JsonRawValue String location, @Nullable String floorId) {
    }

    /**
     * Dane wejścia z planów PWr w odpowiedzi HTTP.
     *
     * <p>Wymagania: BR-27, US3.3, US17.3, NFR-40</p>
     *
     * @param approach Opis dojścia albo null, gdy dane go nie podają
     * @param doorWidthCm Szerokość drzwi w centymetrach albo null, gdy dane jej nie podają
     * @param source Nazwa źródła danych planów pięter
     * @since 0.1.0
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Plan(@Nullable String approach, @Nullable Integer doorWidthCm, String source) {

        /**
         * Odpowiedź z obecności wejścia w warstwie wejść planów PWr.
         *
         * @param layer Obecność wejścia w warstwie
         * @return Dane z warstwy albo null przy wejściu spoza warstwy
         */
        static @Nullable Plan of(final Entrance.Layer layer) {
            return switch (layer) {
                case Entrance.Layer.Listed listed -> new Plan(
                    listed.approach().text().orElse(null),
                    listed.door().widthCm().orElse(null),
                    FloorList.PWR_SOURCE
                );
                case Entrance.Layer.Unlisted _ -> null;
                case null -> throw new IllegalStateException("Brak obecności wejścia w warstwie");
            };
        }
    }
}
