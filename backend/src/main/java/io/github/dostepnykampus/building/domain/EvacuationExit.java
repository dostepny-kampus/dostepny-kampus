/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.domain;

import org.jspecify.annotations.Nullable;

/**
 * Wyjście ewakuacyjne budynku z warstwy wejść planów PWr; nie jest wejściem budynku.
 *
 * <p>Wymagania: US2.6, BR-27</p>
 *
 * @param id Identyfikator wyjścia
 * @param location Położenie wyjścia jako tekst punktu GeoJSON
 * @param floorId Piętro wyjścia z listy pięter budynku albo null, gdy dane go nie podają
 * @param description Opis wyjścia z danych, np. „Wyjście ewakuacyjne z pomieszczenia 41”
 * @param doorWidthCm Szerokość drzwi w centymetrach albo null, gdy dane jej nie podają
 * @since 0.1.0
 */
public record EvacuationExit(
    String id,
    String location,
    @Nullable String floorId,
    String description,
    @Nullable Integer doorWidthCm
) {
}
