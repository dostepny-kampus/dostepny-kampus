/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.room;

import java.util.Optional;

/**
 * Pomieszczenia zapisanego planu piętra.
 *
 * <p>TODO(#1473): zwracać pomieszczenia w operacji listFloorRooms.</p>
 *
 * <p>Wymagania: US16.1, NFR-40</p>
 *
 * @since 0.1.0
 */
@FunctionalInterface
public interface FloorRooms {

    /**
     * Pomieszczenia piętra z obrysem piętra.
     *
     * @param building Symbol budynku
     * @param floor Identyfikator piętra z listy pięter budynku
     * @return Pomieszczenia piętra; brak, gdy nie ma budynku albo piętra
     */
    Optional<RoomList> rooms(String building, String floor);
}
