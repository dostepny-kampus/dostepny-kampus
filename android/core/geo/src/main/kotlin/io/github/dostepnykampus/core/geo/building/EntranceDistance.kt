/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Yevhenii Lukianenko, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.geo.building

import io.github.dostepnykampus.core.geo.metersTo
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceDistance

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
