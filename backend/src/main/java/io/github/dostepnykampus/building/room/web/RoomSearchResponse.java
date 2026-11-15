/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.room.web;

import io.github.dostepnykampus.building.domain.FloorList;
import io.github.dostepnykampus.building.room.RoomHit;
import io.github.dostepnykampus.building.web.FloorResponse;
import java.util.List;

/**
 * Wynik wyszukiwania sali w odpowiedzi HTTP, zgodny ze schematem RoomSearchResults.
 *
 * <p>Wymagania: US18.1, NFR-40</p>
 *
 * @param source Nazwa źródła danych planów pięter
 * @param results Znalezione sale, najpierw dokładne dopasowania numeru
 * @since 0.1.0
 */
public record RoomSearchResponse(String source, List<Hit> results) {

    /**
     * Odpowiedź ze znalezionych sal.
     *
     * @param hits Znalezione sale
     */
    public RoomSearchResponse(final List<RoomHit> hits) {
        this(FloorList.PWR_SOURCE, hits.stream().map(Hit::new).toList());
    }

    /**
     * Znaleziona sala z budynkiem i piętrem.
     *
     * @param buildingSymbol Budynek z salą
     * @param floor Piętro z salą
     * @param room Sala do wyróżnienia na planie piętra
     * @since 0.1.0
     */
    public record Hit(String buildingSymbol, FloorResponse floor, RoomListResponse.Room room) {

        /**
         * Odpowiedź ze znalezionej sali.
         *
         * @param hit Znaleziona sala
         */
        public Hit(final RoomHit hit) {
            this(
                hit.building(),
                new FloorResponse(hit.floor()),
                new RoomListResponse.Room(hit.room())
            );
        }
    }
}
