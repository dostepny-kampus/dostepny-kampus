/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.path;

/**
 * Odcinek ścieżki dostosowanej z mapy kampusu PWr.
 *
 * <p>Wymagania: US1.4, NFR-40</p>
 *
 * @param id Identyfikator odcinka z danych PWr
 * @param line Przebieg odcinka jako GeoJSON LineString w WGS84
 * @since 0.1.0
 */
public record AccessiblePath(String id, String line) {
}
