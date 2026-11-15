/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.room.web;

import io.github.dostepnykampus.building.room.application.RoomReading;
import io.github.dostepnykampus.shared.error.MissingResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Operacje listFloorRooms (pomieszczenia piętra z obrysem) i searchRooms (sala po numerze).
 *
 * <p>Wymagania: US16.1, US18.1, NFR-40</p>
 *
 * @since 0.1.0
 */
@RestController
public final class RoomsController {

    /**
     * Pomieszczenia zapisanych planów pięter.
     */
    private final RoomReading rooms;

    RoomsController(final RoomReading rooms) {
        this.rooms = rooms;
    }

    /**
     * Zwraca pomieszczenia piętra według identyfikatora; nieznany budynek albo piętro daje 404.
     *
     * @param symbol Symbol budynku
     * @param floor Identyfikator piętra z listy pięter budynku
     * @return Pomieszczenia piętra z obrysem i źródłem danych
     */
    @GetMapping("/buildings/{symbol}/floors/{floorId}/rooms")
    RoomListResponse listFloorRooms(
        @PathVariable("symbol") final String symbol,
        @PathVariable("floorId") final String floor
    ) {
        return this.rooms.rooms(symbol, floor).map(RoomListResponse::new).orElseThrow(
            () -> new MissingResource("Nie ma piętra %s w budynku %s".formatted(floor, symbol))
        );
    }

    /**
     * Szuka sali po numerze, opcjonalnie w budynku podanym przed numerem.
     *
     * @param text Parametr {@code q}, np. {@code A-1 329}
     * @return Znalezione sale; pusta lista, gdy nic nie pasuje
     */
    @GetMapping("/rooms")
    RoomSearchResponse searchRooms(@RequestParam("q") final String text) {
        final RoomQuery query = new RoomQuery(text);
        final String number = query.number();
        return new RoomSearchResponse(
            query.building()
                .map(symbol -> this.rooms.search(number, symbol))
                .orElseGet(() -> this.rooms.search(number))
        );
    }
}
