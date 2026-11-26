/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Odczyt budynków kampusu: lista na mapie, podpowiedź budynku dla punktu,
 * karta budynku z wejściami oraz piętra budynku.
 *
 * <p>Wymagania: US1, BR-G2, US2, US16</p>
 *
 * @since 0.1.0
 */
public interface Buildings {

    /**
     * Wszystkie budynki z atrybucją konturów.
     *
     * @return Budynki posortowane według symbolu
     */
    BuildingList listBuildings();

    /**
     * Położenie punktu względem budynków.
     *
     * @param lat Szerokość geograficzna WGS84
     * @param lon Długość geograficzna WGS84
     * @return Budynek, w którego konturze leży punkt, najbliższy budynek
     *  w zasięgu podpowiedzi albo brak budynku
     */
    Placement locate(double lat, double lon);

    /**
     * Karta budynku.
     *
     * @param symbol Symbol budynku, np. C-13
     * @return Karta z wejściami albo pusty wynik, gdy budynku nie ma
     */
    Optional<BuildingCard> building(String symbol);

    /**
     * Piętra budynku ze źródłem danych.
     *
     * @param symbol Symbol budynku, np. C-13
     * @return Piętra rosnąco według kolejności albo pusty wynik, gdy budynku nie ma
     */
    Optional<FloorList> floors(String symbol);

    /**
     * Wejścia, których oznaczenie zmieniło przyjęte zgłoszenie z obchodu od {@code since}
     * włącznie, od najdawniej zmienionego.
     *
     * @param since Chwila, od której liczą się zmiany
     * @return Wejścia z budynkiem
     */
    List<MarkedEntrance> markedSince(Instant since);
}
