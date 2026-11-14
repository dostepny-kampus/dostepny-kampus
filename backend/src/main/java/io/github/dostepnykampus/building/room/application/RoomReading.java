/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.room.application;

import io.github.dostepnykampus.building.room.FloorRooms;
import io.github.dostepnykampus.building.room.RoomList;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Odczyt pomieszczeń zapisanych planów pięter dla warstwy web.
 *
 * <p>Wymagania: US16.1</p>
 *
 * @since 0.1.0
 */
@Service
public final class RoomReading {

    /**
     * Port odczytu pomieszczeń piętra.
     */
    private final FloorRooms floors;

    RoomReading(final FloorRooms floors) {
        this.floors = floors;
    }

    /**
     * Pomieszczenia piętra budynku.
     *
     * @param building Symbol budynku
     * @param floor Identyfikator piętra
     * @return Pomieszczenia z obrysem piętra albo nic, gdy nie ma budynku lub piętra
     */
    public Optional<RoomList> rooms(final String building, final String floor) {
        return this.floors.rooms(building, floor);
    }
}
