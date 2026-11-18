/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.domain;

import org.jspecify.annotations.Nullable;

/**
 * Wejście budynku.
 *
 * <p>Wymagania: US2, US17, BR-24, US18.2, BR-27</p>
 *
 * @param id Identyfikator wejścia, np. node/1167953448
 * @param buildingSymbol Budynek, do którego prowadzi wejście
 * @param name Opis wejścia albo null, gdy dane go nie podają
 * @param type Rodzaj wejścia z OpenStreetMap albo null
 * @param main Czy to wejście główne
 * @param place Położenie i piętro wejścia
 * @param marking Oznaczenie wejścia
 * @param plan Dane wejścia z warstwy wejść planów PWr albo null, gdy wejścia nie ma w warstwie
 * @since 0.1.0
 */
public record Entrance(
    String id,
    String buildingSymbol,
    @Nullable String name,
    @Nullable String type,
    boolean main,
    Place place,
    Marking marking,
    @Nullable Plan plan
) {

    /**
     * Położenie wejścia z piętrem.
     *
     * <p>Wymagania: US2, US18.2</p>
     *
     * @param location Położenie wejścia jako tekst punktu GeoJSON
     * @param floorId Piętro wejścia z listy pięter budynku albo null, gdy dane go nie podają
     * @since 0.1.0
     */
    public record Place(String location, @Nullable String floorId) {
    }

    /**
     * Dane wejścia z warstwy wejść planów PWr.
     *
     * <p>Wymagania: BR-27, US3.3, US17.3</p>
     *
     * @param source Nazwa źródła danych planów pięter
     * @param approach Opis dojścia albo null, gdy dane go nie podają
     * @param doorWidthCm Szerokość drzwi w centymetrach albo null, gdy dane jej nie podają
     * @since 0.1.0
     */
    public record Plan(
        String source, @Nullable String approach, @Nullable Integer doorWidthCm
    ) {
    }

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
