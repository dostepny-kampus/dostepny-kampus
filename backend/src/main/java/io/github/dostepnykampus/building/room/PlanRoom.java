/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.room;

/**
 * Pomieszczenie z planu piętra odczytane z usługi ArcGIS PWr.
 *
 * <p>Wymagania: US16, US18, NFR-40</p>
 *
 * @param floorId Identyfikator piętra z pomieszczeniem
 * @param number Numer pomieszczenia albo pusty tekst, gdy dane go nie podają
 * @param function Funkcja pomieszczenia z danych PWr; brak funkcji daje „Inne”
 * @param outline Kontur jako tekst GeoJSON typu MultiPolygon w układzie WGS 84
 * @since 0.1.0
 */
public record PlanRoom(String floorId, String number, String function, String outline) {
}
