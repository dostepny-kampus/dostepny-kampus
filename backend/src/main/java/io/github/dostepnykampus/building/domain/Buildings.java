/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.domain;

import java.util.Optional;

/**
 * Odczyt budynków kampusu: lista na mapie, podpowiedź budynku dla punktu
 * i karta budynku z wejściami.
 *
 * <p>Wymagania: US1, BR-G2, US2</p>
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
}
