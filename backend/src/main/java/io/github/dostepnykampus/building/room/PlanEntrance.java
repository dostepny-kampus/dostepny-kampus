/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.room;

/**
 * Wejście budynku z warstwy wejść planu PWr, źródło piętra wejścia.
 *
 * <p>Wymagania: US18.2, NFR-40</p>
 *
 * @param floorId Identyfikator piętra wejścia, np. {@code K1.A1.P0}
 * @param point Położenie jako tekst GeoJSON typu Point w układzie WGS 84
 * @since 0.1.0
 */
public record PlanEntrance(String floorId, String point) {
}
