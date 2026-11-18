/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.room;

import org.jspecify.annotations.Nullable;

/**
 * Wpis warstwy wejść planu PWr: wejście budynku albo wyjście ewakuacyjne.
 *
 * <p>Wymagania: US18.2, BR-24, BR-27, NFR-40</p>
 *
 * @param floorId Identyfikator piętra wpisu, np. {@code K1.A1.P0}
 * @param point Położenie jako tekst GeoJSON typu Point w układzie WGS 84
 * @param marking Oznaczenie wejścia: ACCESSIBLE, INACCESSIBLE albo NO_DATA
 * @param name Nazwa wejścia z uwag albo null bez nazwy; przy wyjściu „Wyjście ewakuacyjne”
 * @param approach Opis dojścia z uwag albo null, gdy uwagi go nie podają
 * @param doorWidthCm Szerokość drzwi w centymetrach, większa od 0, albo null
 * @param evacuationExit Czy wpis jest wyjściem ewakuacyjnym, a nie wejściem
 * @since 0.1.0
 */
public record PlanEntrance(
    String floorId,
    String point,
    String marking,
    @Nullable String name,
    @Nullable String approach,
    @Nullable Integer doorWidthCm,
    boolean evacuationExit
) {
}
