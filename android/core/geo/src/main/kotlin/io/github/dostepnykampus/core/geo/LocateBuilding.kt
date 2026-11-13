/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.geo

import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.core.model.GeoArea
import io.github.dostepnykampus.core.model.GeoPoint
import org.maplibre.spatialk.turf.booleans.contains

/**
 * Podpowiedź budynku dla punktu, bez sieci (to-be T3 i T4). Obliczenia wykonuje Turf.
 *
 * - Punkt ściśle w konturze budynku daje [LocateResult.InsideOutline]. Kontur może być wielokątem złożonym:
 *   wystarczy, że punkt leży w jednej z części.
 * - Punkt w otworze konturu, np. na dziedzińcu A-1, nie leży w konturze.
 * - Punkt dokładnie na krawędzi konturu, także na krawędzi otworu i w dowolnej części wielokąta złożonego,
 *   leży poza budynkiem, jak w `ST_Contains` na serwerze. Propozycją jest ten budynek, 0 m od punktu.
 * - Gdy punkt leży w konturach kilku budynków, wynikiem jest pierwszy z nich w kolejności [buildings].
 * - Punkt poza konturem każdego budynku daje stan „Poza budynkiem” ([LocateResult.OutsideBuilding]) bez
 *   wyjątku, z propozycją budynku o najmniejszej odległości od punktu do krawędzi konturu.
 *
 * Wymagania: US6.2, BR-G2
 *
 * @param point punkt, dla którego szukamy budynku
 * @param buildings budynki z konturami jak w kontrakcie API: kontur ma co najmniej jeden wielokąt, a jego pierścienie
 *   są zamknięte, jak w GeoJSON
 * @return podpowiedź budynku
 * @throws IllegalArgumentException gdy kontur ma pierścień niezamknięty albo krótszy niż 4 punkty
 */
fun locateBuilding(point: GeoPoint, buildings: List<Building>): LocateResult {
    val inside = containingBuilding(point, buildings)
    return if (inside != null) {
        LocateResult.InsideOutline(inside)
    } else {
        LocateResult.OutsideBuilding(buildings.minByOrNull { building -> building.edgeDistanceMeters(point) })
    }
}

/** Pierwszy budynek, którego kontur zawiera punkt ściśle, bez punktów na krawędzi. */
fun containingBuilding(point: GeoPoint, buildings: List<Building>): Building? =
    buildings.firstOrNull { building -> building.outline.containsStrictly(point) }

/** Odległość w metrach na ziemi od [point] do najbliższej krawędzi konturu, także krawędzi otworu. */
internal fun Building.edgeDistanceMeters(point: GeoPoint): Double = outline.edgeDistanceMeters(point)

internal fun GeoArea.containsStrictly(point: GeoPoint): Boolean =
    point.toPosition() in toGeometry() && !hasPointOnEdge(point)

private fun GeoArea.hasPointOnEdge(point: GeoPoint): Boolean =
    allRings().any { ring -> ring.zipWithNext().any { (from, to) -> point.isOnSegment(from, to) } }

private fun GeoPoint.isOnSegment(from: GeoPoint, to: GeoPoint): Boolean {
    val alongX = (to.longitude - from.longitude) * (latitude - from.latitude)
    val alongY = (to.latitude - from.latitude) * (longitude - from.longitude)
    val isInLongitudes = longitude.isBetween(first = from.longitude, second = to.longitude)
    val isInLatitudes = latitude.isBetween(first = from.latitude, second = to.latitude)
    return alongX == alongY && isInLongitudes && isInLatitudes
}

private fun Double.isBetween(first: Double, second: Double): Boolean =
    if (first <= second) this in first..second else this in second..first
