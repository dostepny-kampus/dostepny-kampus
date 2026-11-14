/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.room;

import io.github.dostepnykampus.building.domain.Floor;

/**
 * Znaleziona sala z budynkiem i piętrem, do pokazania na planie piętra.
 *
 * <p>Wymagania: US18.1</p>
 *
 * @param building Symbol budynku z salą
 * @param floor Piętro z salą
 * @param room Sala do wyróżnienia na planie
 * @since 0.1.0
 */
public record RoomHit(String building, Floor floor, RoomList.Room room) {
}
