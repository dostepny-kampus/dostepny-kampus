/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.room;

import java.util.List;

/**
 * Plany pięter z warstw pięter, pomieszczeń i wejść usługi FeatureServer.
 *
 * <p>Wymagania: US16, US18.2, BR-24, BR-27, NFR-40</p>
 *
 * @since 0.1.0
 */
public interface PlanSource {

    /**
     * Piętra z warstwy pięter.
     *
     * @param service Adres usługi FeatureServer bez końcowego ukośnika
     * @param layer Numer warstwy pięter
     * @return Piętra w kolejności usługi
     */
    List<PlanFloor> floors(String service, int layer);

    /**
     * Pomieszczenia z warstwy pomieszczeń.
     *
     * @param service Adres usługi FeatureServer bez końcowego ukośnika
     * @param layer Numer warstwy pomieszczeń
     * @return Pomieszczenia z konturem w kolejności usługi
     */
    List<PlanRoom> rooms(String service, int layer);

    /**
     * Wpisy warstwy wejść; wpis bez piętra albo bez punktu jest pomijany. Czy_dostos „Tak”
     * daje ACCESSIBLE, „Nie” INACCESSIBLE, inna wartość NO_DATA. Szerokosc 0 albo nieliczbowa
     * nie daje szerokości drzwi. Nazwę, opis dojścia i wyjście ewakuacyjne daje pole Uwagi.
     *
     * @param service Adres usługi FeatureServer bez końcowego ukośnika
     * @param layer Numer warstwy wejść
     * @return Wpisy z piętrem w kolejności usługi
     */
    List<PlanEntrance> entrances(String service, int layer);
}
