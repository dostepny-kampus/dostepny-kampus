/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.geo

import io.github.dostepnykampus.core.model.GeoArea
import io.github.dostepnykampus.core.model.GeoPoint
import org.maplibre.spatialk.turf.booleans.contains
import org.maplibre.spatialk.turf.measurement.area
import org.maplibre.spatialk.units.International

private const val GRID_STEPS = 32
private const val CELL_CENTER = 0.5

/**
 * Punkt podpisu konturu [area]: wewnątrz największej części i najdalej od jej krawędzi, szukany w siatce 32 × 32
 * nad prostokątem tej części (przybliżony „pole of inaccessibility”). Środek ciężkości konturu z dziedzińcem, jak
 * A-1, może wypaść poza kontur, a ten punkt nie. Jeden punkt na kontur daje jeden podpis także przy wielokącie
 * złożonym (reguła 4 wymagań wspólnych).
 *
 * Pole części, test „wewnątrz” i odległość do krawędzi liczy Turf, jak w [locateBuilding]. Własne jest tylko
 * przeszukanie siatki, bo spatialk 0.8.0 nie ma `pointOnFeature` ani „polylabel”.
 *
 * Wymagania: US1
 */
public fun labelPoint(area: GeoArea): GeoPoint {
    val polygon = area.parts().maxBy { part -> part.toGeometry().area().toDouble(International.SquareMeters) }
    val geometry = polygon.toGeometry()
    val outer = polygon.rings.first()
    val west = outer.minOf { it.longitude }
    val south = outer.minOf { it.latitude }
    val width = (outer.maxOf { it.longitude } - west) / GRID_STEPS
    val height = (outer.maxOf { it.latitude } - south) / GRID_STEPS
    val cells =
        (0 until GRID_STEPS).flatMap { column ->
            (0 until GRID_STEPS).map { row ->
                val longitude = west + (column + CELL_CENTER) * width
                GeoPoint(longitude = longitude, latitude = south + (row + CELL_CENTER) * height)
            }
        }
    return cells.filter { it.toPosition() in geometry }.maxByOrNull(polygon::edgeDistanceMeters) ?: outer.first()
}
