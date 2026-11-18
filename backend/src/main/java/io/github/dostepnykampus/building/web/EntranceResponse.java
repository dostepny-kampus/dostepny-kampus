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
import org.jspecify.annotations.Nullable;

/**
 * Wejście budynku w odpowiedzi HTTP, zgodne ze schematem Entrance.
 *
 * <p>Wymagania: US2, US17, BR-24, US18.2, BR-27</p>
 *
 * @param id Identyfikator wejścia, np. node/1167953448
 * @param buildingSymbol Budynek, do którego prowadzi wejście
 * @param name Opis wejścia albo null, gdy dane go nie podają
 * @param type Rodzaj wejścia z OpenStreetMap albo null
 * @param main Czy to wejście główne
 * @param location Położenie wejścia jako punkt GeoJSON z bazy, wstawiany do odpowiedzi bez zmian
 * @param floorId Piętro wejścia albo null, gdy dane go nie podają
 * @param marking Oznaczenie wejścia, w JSON pola obok pozostałych
 * @param plan Dane z planów PWr, w JSON pola obok pozostałych; null przy wejściu spoza warstwy
 * @since 0.1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EntranceResponse(
    String id,
    String buildingSymbol,
    @Nullable String name,
    @Nullable String type,
    boolean main,
    @JsonRawValue String location,
    @Nullable String floorId,
    @JsonUnwrapped Entrance.Marking marking,
    @JsonUnwrapped @Nullable Plan plan
) {

    /**
     * Odpowiedź z wejścia.
     *
     * @param entrance Wejście
     */
    public EntranceResponse(final Entrance entrance) {
        this(
            entrance.id(), entrance.buildingSymbol(), entrance.name(), entrance.type(),
            entrance.main(), entrance.location(), entrance.floorId(), entrance.marking(),
            Plan.of(entrance.plan())
        );
    }

    /**
     * Dane wejścia z planów PWr w odpowiedzi HTTP.
     *
     * <p>Wymagania: BR-27, US3.3, US17.3</p>
     *
     * @param approach Opis dojścia albo null, gdy dane go nie podają
     * @param doorWidthCm Szerokość drzwi w centymetrach albo null, gdy dane jej nie podają
     * @param source Nazwa źródła danych planów pięter
     * @since 0.1.0
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Plan(@Nullable String approach, @Nullable Integer doorWidthCm, String source) {

        /**
         * Odpowiedź z danych wejścia z planów PWr.
         *
         * @param plan Dane wejścia z planów PWr albo null
         * @return Odpowiedź albo null, gdy wejścia nie ma w warstwie
         */
        static @Nullable Plan of(final Entrance.@Nullable Plan plan) {
            final Plan result;
            if (plan == null) {
                result = null;
            } else {
                result = new Plan(plan.approach(), plan.doorWidthCm(), plan.source());
            }
            return result;
        }
    }
}
