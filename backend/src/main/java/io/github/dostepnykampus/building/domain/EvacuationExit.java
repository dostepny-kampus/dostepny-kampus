/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.domain;

/**
 * Wyjście ewakuacyjne budynku z warstwy wejść planów PWr; nie jest wejściem budynku.
 *
 * <p>Wymagania: US2.6, BR-27</p>
 *
 * @param id Identyfikator wyjścia
 * @param location Położenie wyjścia jako tekst punktu GeoJSON
 * @param floorId Piętro wyjścia z listy pięter budynku
 * @param description Opis wyjścia z danych, np. „Wyjście ewakuacyjne z pomieszczenia 41”
 * @param door Drzwi wyjścia
 * @since 0.1.0
 */
public record EvacuationExit(
    String id,
    String location,
    String floorId,
    String description,
    Door door
) {
}
