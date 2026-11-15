/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.room;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Pomieszczenia piętra ze źródłem danych.
 *
 * <p>Wymagania: US16.1, NFR-40</p>
 *
 * @param source Nazwa źródła danych planów pięter
 * @param outline Obrys piętra jako GeoJSON MultiPolygon; bez współrzędnych, gdy piętro nie ma
 *  pomieszczeń
 * @param rooms Pomieszczenia piętra według identyfikatora
 * @since 0.1.0
 */
public record RoomList(String source, String outline, List<RoomList.Room> rooms) {

    /**
     * Pomieszczenie z planu piętra.
     *
     * @param id Identyfikator pomieszczenia
     * @param number Numer pomieszczenia albo null, gdy plan go nie podaje
     * @param function Funkcja pomieszczenia z planu PWr
     * @param category Grupa funkcji z kontraktu API
     * @param outline Kontur jako GeoJSON
     * @param label Punkt podpisu jako GeoJSON Point
     * @param labelled Czy numer jest podpisany na planie
     * @param lift Winda łącząca piętra albo null poza windami
     * @since 0.1.0
     */
    public record Room(
        String id, @Nullable String number, String function, String category, String outline,
        String label, boolean labelled, @Nullable Lift lift
    ) {

        /**
         * Pomieszczenie bez windy.
         *
         * @param id Identyfikator pomieszczenia
         * @param number Numer pomieszczenia albo null, gdy plan go nie podaje
         * @param function Funkcja pomieszczenia z planu PWr
         * @param category Grupa funkcji z kontraktu API
         * @param outline Kontur jako GeoJSON
         * @param label Punkt podpisu jako GeoJSON Point
         * @param labelled Czy numer jest podpisany na planie
         */
        public Room(
            final String id, final @Nullable String number, final String function,
            final String category, final String outline, final String label, final boolean labelled
        ) {
            this(id, number, function, category, outline, label, labelled, null);
        }
    }

    /**
     * Winda łącząca piętra.
     *
     * @param id Identyfikator windy z pomieszczenia na najniższym piętrze szybu
     * @param floors Piętra windy od najniższego
     * @since 0.1.0
     */
    public record Lift(String id, List<String> floors) {
    }
}
