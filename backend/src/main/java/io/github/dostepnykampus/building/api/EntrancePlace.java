/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.api;

import org.jspecify.annotations.Nullable;

/**
 * Miejsce wejścia dla innych modułów: punkt, budynek i piętro wejścia.
 *
 * <p>Wymagania: BR-8, BR-14</p>
 *
 * @param latitude Szerokość geograficzna w stopniach WGS84
 * @param longitude Długość geograficzna w stopniach WGS84
 * @param buildingSymbol Symbol budynku wejścia
 * @param floorId Identyfikator piętra wejścia albo null, gdy dane go nie podają
 * @since 0.1.0
 */
public record EntrancePlace(
    double latitude, double longitude, String buildingSymbol, @Nullable String floorId
) {
}
