/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.data

import io.github.dostepnykampus.core.model.GeoPoint

private const val EDGE_SLACK_DEGREES = 1e-5

/**
 * Krawędź mapy, przy której stoi widok na granicy terenu (US1.7).
 *
 * @property isReached czy punkt leży na tym brzegu prostokąta terenu albo za nim. Brzeg ma zapas około 1 m,
 *   bo widok zatrzymany gestem staje o ułamek stopnia przed nim
 */
enum class MapEdge(internal val isReached: (CampusBounds, GeoPoint) -> Boolean) {
    /** Górna krawędź mapy. */
    NORTH({ bounds, point -> point.latitude >= bounds.north - EDGE_SLACK_DEGREES }),

    /** Prawa krawędź mapy. */
    EAST({ bounds, point -> point.longitude >= bounds.east - EDGE_SLACK_DEGREES }),

    /** Dolna krawędź mapy. */
    SOUTH({ bounds, point -> point.latitude <= bounds.south + EDGE_SLACK_DEGREES }),

    /** Lewa krawędź mapy. */
    WEST({ bounds, point -> point.longitude <= bounds.west + EDGE_SLACK_DEGREES }),
}

/**
 * Sąsiedni teren dla widoku na granicy wybranego terenu (US1.7).
 *
 * @property name nazwa terenu, np. „Wittigowo”
 * @property kilometers odległość od wybranego terenu w pełnych kilometrach, co najmniej 1
 * @property edge krawędź mapy, za którą leży teren
 */
data class AreaNeighbor(val name: String, val kilometers: Int, val edge: MapEdge)

/**
 * Sąsiedni teren dla środka widoku [view] na terenie [current]: widok stoi na brzegu prostokąta terenu, a za tym
 * brzegiem leży środek innego terenu. Przy kilku wygrywa najbliższy. `null`, gdy widok nie stoi na brzegu albo
 * w tę stronę nie ma terenu (US1.7).
 */
fun CampusMapData.areaNeighbor(current: MapArea, view: GeoPoint): AreaNeighbor? =
    MapEdge.entries
        .asSequence()
        .filter { it.isReached(current.bounds, view) }
        .flatMap { edge -> (areas - current).filter { edge.isReached(current.bounds, it.bounds.center) }.map(edge::to) }
        .map { (edge, area) -> Triple(edge, area, current.bounds.kilometersTo(area.bounds)) }
        .minByOrNull { it.third }
        ?.let { (edge, area, kilometers) -> AreaNeighbor(area.name, fullKilometers(kilometers), edge) }
