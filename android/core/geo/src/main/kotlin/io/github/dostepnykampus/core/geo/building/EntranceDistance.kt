/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Yevhenii Lukianenko, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.geo.building

import io.github.dostepnykampus.core.geo.metersTo
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.building.Entrance

/**
 * Wejście z odległością od punktu pomiaru, do opisu miejsca słowami w C2.
 *
 * Wymagania: US7.4, NFR-19
 *
 * @property entrance wejście z danych
 * @property meters odległość w metrach na ziemi od punktu do wejścia
 */
public data class EntranceDistance(val entrance: Entrance, val meters: Double)

/**
 * Najbliższe wejście do [point]; obliczenia wykonuje Turf.
 *
 * @param point punkt pomiaru
 * @param entrances wejścia, spośród których szukamy, np. wejścia potwierdzonego budynku
 * @return najbliższe wejście albo `null`, gdy [entrances] jest puste
 */
public fun nearestEntrance(point: GeoPoint, entrances: List<Entrance>): EntranceDistance? =
    entrances
        .map { entrance -> EntranceDistance(entrance, meters = point.metersTo(entrance.location)) }
        .minByOrNull { it.meters }
