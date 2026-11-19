/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.barrier

import io.github.dostepnykampus.core.geo.allRings
import io.github.dostepnykampus.core.geo.toGeometry
import io.github.dostepnykampus.core.geo.toPosition
import io.github.dostepnykampus.core.model.GeoArea
import io.github.dostepnykampus.core.model.GeoPoint
import org.maplibre.spatialk.geojson.LineString
import org.maplibre.spatialk.turf.booleans.contains
import org.maplibre.spatialk.turf.measurement.bearingTo
import org.maplibre.spatialk.turf.measurement.offset
import org.maplibre.spatialk.turf.misc.nearestPointTo
import org.maplibre.spatialk.units.Bearing
import org.maplibre.spatialk.units.International
import org.maplibre.spatialk.units.extensions.degrees
import org.maplibre.spatialk.units.extensions.inDegrees
import org.maplibre.spatialk.units.extensions.meters

private val SIDE_STEP = 0.5.meters

/**
 * Kierunek na zewnątrz konturu przy [point], w stopniach od północy zgodnie z ruchem wskazówek zegara:
 * prostopadły do krawędzi konturu najbliższej [point], także krawędzi otworu. `null`, gdy kontur nie ma
 * krawędzi albo z najbliższego punktu krawędzi obie strony leżą wewnątrz konturu (wierzchołek wklęsły).
 *
 * Wymagania: US2.2, NFR-17
 */
internal fun GeoArea.outwardBearing(point: GeoPoint): Double? {
    val position = point.toPosition()
    val geometry = toGeometry()
    val nearest =
        allRings()
            .filter { it.size > 1 }
            .map { ring -> LineString(ring.map(GeoPoint::toPosition)) }
            .map { line -> line to nearestPointTo(line, position) }
            .minByOrNull { (_, found) -> found.properties.distance.toDouble(International.Meters) }
    return nearest?.let { (line, found) ->
        val edge = line.coordinates
        val start = found.properties.index.coerceAtMost(edge.lastIndex - 1)
        val along = edge[start].bearingTo(edge[start + 1])
        listOf(along + 90.degrees, along - 90.degrees)
            .firstOrNull { side -> found.geometry.coordinates.offset(SIDE_STEP, side) !in geometry }
            ?.let { side -> (side - Bearing.North).inDegrees }
    }
}
