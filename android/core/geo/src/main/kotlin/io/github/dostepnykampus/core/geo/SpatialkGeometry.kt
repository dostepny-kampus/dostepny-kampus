/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.geo

import io.github.dostepnykampus.core.model.GeoArea
import io.github.dostepnykampus.core.model.GeoMultiPolygon
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import org.maplibre.spatialk.geojson.LineString
import org.maplibre.spatialk.geojson.MultiPolygon
import org.maplibre.spatialk.geojson.Polygon
import org.maplibre.spatialk.geojson.PolygonGeometry
import org.maplibre.spatialk.geojson.Position
import org.maplibre.spatialk.turf.booleans.contains
import org.maplibre.spatialk.turf.measurement.distance
import org.maplibre.spatialk.units.International

/** Punkt modelu jako pozycja GeoJSON spatialk: długość, potem szerokość. */
public fun GeoPoint.toPosition(): Position = Position(longitude = longitude, latitude = latitude)

/** Kontur modelu jako geometria spatialk: `Polygon` albo `MultiPolygon`. */
public fun GeoArea.toGeometry(): PolygonGeometry =
    when (this) {
        is GeoPolygon -> Polygon(positions(this))
        is GeoMultiPolygon -> MultiPolygon(polygons.map(::positions))
    }

/** Wszystkie pierścienie konturu, także otwory i pierścienie części wielokąta złożonego. */
public fun GeoArea.allRings(): List<List<GeoPoint>> =
    when (this) {
        is GeoPolygon -> rings
        is GeoMultiPolygon -> polygons.flatMap(GeoPolygon::rings)
    }

/** Odległość w metrach na ziemi od [point] do najbliższej krawędzi konturu, także krawędzi otworu (Turf). */
internal fun GeoArea.edgeDistanceMeters(point: GeoPoint): Double {
    val position = point.toPosition()
    return allRings().minOf { ring ->
        distance(position, LineString(ring.map(GeoPoint::toPosition))).toDouble(International.Meters)
    }
}

private fun positions(polygon: GeoPolygon): List<List<Position>> =
    polygon.rings.map { ring -> ring.map(GeoPoint::toPosition) }

/** Czy [point] leży wewnątrz konturu albo na jego krawędzi, jak `ST_Covers`. */
internal fun GeoArea.covers(point: GeoPoint): Boolean = point.toPosition() in toGeometry() || hasPointOnEdge(point)

/** Czy [point] leży dokładnie na krawędzi konturu, także otworu, bez tolerancji. */
internal fun GeoArea.hasPointOnEdge(point: GeoPoint): Boolean =
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
