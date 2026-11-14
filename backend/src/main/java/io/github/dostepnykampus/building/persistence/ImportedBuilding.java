/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.persistence;

import org.jspecify.annotations.Nullable;

/**
 * Budynek z pliku konturów OpenStreetMap gotowy do zapisu w tabeli buildings.
 *
 * <p>Wymagania: US1, US2</p>
 *
 * @param symbol Symbol budynku
 * @param name Nazwa z pola display_name albo null, gdy plik jej nie podaje
 * @param outline Wszystkie kontury symbolu jako MultiPolygon GeoJSON w WGS84
 * @since 0.1.0
 */
record ImportedBuilding(String symbol, @Nullable String name, String outline) {
}
