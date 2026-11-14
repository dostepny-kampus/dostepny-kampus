/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.persistence;

import org.jspecify.annotations.Nullable;

/**
 * Wejście z pliku konturów OpenStreetMap przypisane do jednego budynku, gotowe do zapisu
 * w tabeli entrances.
 *
 * <p>Wymagania: US17.1, BR-24</p>
 *
 * @param id Identyfikator węzła OpenStreetMap
 * @param building Symbol budynku
 * @param name Opis z tagu ref albo null
 * @param type Rodzaj z tagu entrance albo null
 * @param main Czy tag entrance ma wartość main
 * @param location Punkt wejścia jako GeoJSON w WGS84
 * @param marking Oznaczenie dostępności: ACCESSIBLE, INACCESSIBLE albo NO_DATA
 * @since 0.1.0
 */
record ImportedEntrance(
    String id,
    String building,
    @Nullable String name,
    @Nullable String type,
    boolean main,
    String location,
    String marking
) {
}
