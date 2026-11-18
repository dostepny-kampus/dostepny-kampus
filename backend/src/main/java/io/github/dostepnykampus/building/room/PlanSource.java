/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.room;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
     * Wpisy warstwy wejść planu; wpis bez piętra albo bez punktu jest pomijany. Pola, których
     * wpis nie ma, bierze z wpisu warstwy wejść mapy kampusu w tym samym punkcie. Czy_dostos „Tak”
     * daje ACCESSIBLE, „Nie” INACCESSIBLE, inna wartość NO_DATA. Szerokosc 0 albo nieliczbowa
     * nie daje szerokości drzwi. Nazwę, opis dojścia i wyjście ewakuacyjne daje pole Uwagi.
     *
     * @param service Adres usługi FeatureServer planu bez końcowego ukośnika
     * @param layer Numer warstwy wejść planu
     * @param campus Adres usługi FeatureServer mapy kampusu bez końcowego ukośnika
     * @param notes Numer warstwy wejść mapy kampusu
     * @return Wpisy z piętrem w kolejności usługi planu
     */
    List<PlanEntrance> entrances(String service, int layer, String campus, int notes);

    /**
     * Dzień ostatniej edycji danych warstwy z opisu warstwy w usłudze.
     *
     * @param service Adres usługi FeatureServer bez końcowego ukośnika
     * @param layer Numer warstwy
     * @return Dzień edycji albo pusty, gdy opis warstwy go nie podaje
     */
    Optional<LocalDate> edited(String service, int layer);
}
