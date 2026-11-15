/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.room.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.github.dostepnykampus.building.room.RoomList;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Pomieszczenia piętra w odpowiedzi HTTP, zgodne ze schematem RoomList.
 *
 * <p>Wymagania: US16.1, NFR-40</p>
 *
 * @param source Nazwa źródła danych planów pięter
 * @param floorOutline Obrys piętra jako GeoJSON
 * @param rooms Pomieszczenia piętra
 * @since 0.1.0
 */
public record RoomListResponse(String source, @JsonRawValue String floorOutline, List<Room> rooms) {

    /**
     * Odpowiedź z pomieszczeń piętra.
     *
     * @param list Pomieszczenia piętra
     */
    public RoomListResponse(final RoomList list) {
        this(list.source(), list.outline(), list.rooms().stream().map(Room::new).toList());
    }

    /**
     * Pomieszczenie w odpowiedzi HTTP, zgodne ze schematem Room.
     *
     * @param id Identyfikator pomieszczenia
     * @param number Numer pomieszczenia; pole znika, gdy plan go nie podaje
     * @param function Funkcja pomieszczenia z planu PWr
     * @param category Grupa funkcji
     * @param geometry Kontur pomieszczenia jako GeoJSON
     * @param labelPoint Punkt podpisu jako GeoJSON
     * @param labelled Czy numer jest podpisany na planie
     * @param lift Pola liftId i liftFloors windy; znikają poza windami
     * @since 0.1.0
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Room(
        String id, @Nullable String number, String function, String category,
        @JsonRawValue String geometry, @JsonRawValue String labelPoint, boolean labelled,
        @JsonUnwrapped @Nullable Lift lift
    ) {

        /**
         * Odpowiedź z pomieszczenia.
         *
         * @param room Pomieszczenie z planu piętra
         */
        public Room(final RoomList.Room room) {
            this(
                room.id(), room.number(), room.function(), room.category(), room.outline(),
                room.label(), room.labelled(),
                Optional.ofNullable(room.lift()).map(Lift::new).orElse(null)
            );
        }
    }

    /**
     * Winda pomieszczenia w odpowiedzi HTTP, pola liftId i liftFloors schematu Room.
     *
     * @param liftId Winda łącząca piętra
     * @param liftFloors Piętra windy od najniższego
     * @since 0.1.0
     */
    public record Lift(String liftId, List<String> liftFloors) {

        /**
         * Odpowiedź z windy.
         *
         * @param lift Winda z planu
         */
        public Lift(final RoomList.Lift lift) {
            this(lift.id(), lift.floors());
        }
    }
}
