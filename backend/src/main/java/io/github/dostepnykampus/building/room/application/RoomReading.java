/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.room.application;

import io.github.dostepnykampus.building.room.FloorRooms;
import io.github.dostepnykampus.building.room.RoomHit;
import io.github.dostepnykampus.building.room.RoomList;
import io.github.dostepnykampus.building.room.RoomSearch;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Odczyt i wyszukiwanie pomieszczeń zapisanych planów pięter dla warstwy web.
 *
 * <p>Wymagania: US16.1, US18.1</p>
 *
 * @since 0.1.0
 */
@Service
public final class RoomReading {

    /**
     * Port odczytu pomieszczeń piętra.
     */
    private final FloorRooms floors;

    /**
     * Port wyszukiwania sali po numerze.
     */
    private final RoomSearch search;

    RoomReading(final FloorRooms floors, final RoomSearch search) {
        this.floors = floors;
        this.search = search;
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

    /**
     * Sale o numerze zaczynającym się od {@code number}, najpierw dokładne dopasowania.
     *
     * @param number Numer sali albo jego początek
     * @return Znalezione sale z budynkiem i piętrem we wszystkich budynkach
     */
    public List<RoomHit> search(final String number) {
        return this.search.rooms(number);
    }

    /**
     * Sale budynku o numerze zaczynającym się od {@code number}, najpierw dokładne dopasowania.
     *
     * @param number Numer sali albo jego początek
     * @param building Symbol budynku
     * @return Znalezione sale z budynkiem i piętrem
     */
    public List<RoomHit> search(final String number, final String building) {
        return this.search.rooms(number, building);
    }
}
