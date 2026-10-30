/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.geo

import io.github.dostepnykampus.core.model.GeoArea
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot

private const val GRID_STEPS = 32
private const val CELL_CENTER = 0.5

/**
 * Punkt podpisu konturu [area]: wewnątrz największej części i najdalej od jej krawędzi, szukany w siatce 32 × 32
 * nad prostokątem tej części (przybliżony „pole of inaccessibility”). Środek ciężkości konturu z dziedzińcem, jak
 * A-1, może wypaść poza kontur, a ten punkt nie. Jeden punkt na kontur daje jeden podpis także przy wielokącie
 * złożonym (US1, reguła 4 wymagań wspólnych).
 */
fun labelPoint(area: GeoArea): GeoPoint {
    val polygon = area.parts().maxBy { abs(it.rings.first().signedArea()) }
    val outer = polygon.rings.first()
    val scale = cos(Math.toRadians(outer.first().latitude))
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
    return cells.filter { polygon.contains(it) }.maxByOrNull { polygon.edgeDistance(it, scale) } ?: outer.first()
}

private fun List<GeoPoint>.signedArea(): Double =
    zipWithNext { a, b -> a.longitude * b.latitude - b.longitude * a.latitude }.sum() / 2

// Parzysta liczba przecięć promienia z krawędziami wszystkich pierścieni: punkt poza konturem albo w dziedzińcu.
private fun GeoPolygon.contains(point: GeoPoint): Boolean =
    rings.sumOf { ring ->
        ring.zipWithNext().count { (a, b) ->
            a.latitude > point.latitude != b.latitude > point.latitude &&
                point.longitude <
                (b.longitude - a.longitude) * (point.latitude - a.latitude) / (b.latitude - a.latitude) + a.longitude
        }
    } % 2 == 1

private fun GeoPolygon.edgeDistance(point: GeoPoint, scale: Double): Double =
    rings.minOf { ring ->
        ring.zipWithNext().minOf { (a, b) -> segmentDistance(point = point, start = a, end = b, scale = scale) }
    }

// Odległość na płaszczyźnie, w której stopień długości jest krótszy o cosinus szerokości.
private fun segmentDistance(point: GeoPoint, start: GeoPoint, end: GeoPoint, scale: Double): Double {
    val dx = (end.longitude - start.longitude) * scale
    val dy = end.latitude - start.latitude
    val px = (point.longitude - start.longitude) * scale
    val py = point.latitude - start.latitude
    val length = dx * dx + dy * dy
    val along = if (length == 0.0) 0.0 else (px * dx + py * dy) / length
    val t = along.coerceIn(minimumValue = 0.0, maximumValue = 1.0)
    return hypot(x = px - t * dx, y = py - t * dy)
}
