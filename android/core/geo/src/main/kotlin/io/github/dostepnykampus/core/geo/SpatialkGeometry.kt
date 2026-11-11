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
import org.maplibre.spatialk.geojson.MultiPolygon
import org.maplibre.spatialk.geojson.Polygon
import org.maplibre.spatialk.geojson.PolygonGeometry
import org.maplibre.spatialk.geojson.Position

/** Punkt modelu jako pozycja GeoJSON spatialk: długość, potem szerokość (#819). */
fun GeoPoint.toPosition(): Position = Position(longitude = longitude, latitude = latitude)

/** Kontur modelu jako geometria spatialk: `Polygon` albo `MultiPolygon` (#819). */
fun GeoArea.toGeometry(): PolygonGeometry =
    when (this) {
        is GeoPolygon -> Polygon(positions(this))
        is GeoMultiPolygon -> MultiPolygon(polygons.map(::positions))
    }

/** Wszystkie pierścienie konturu, także otwory i pierścienie części wielokąta złożonego. */
fun GeoArea.allRings(): List<List<GeoPoint>> =
    when (this) {
        is GeoPolygon -> rings
        is GeoMultiPolygon -> polygons.flatMap(GeoPolygon::rings)
    }

private fun positions(polygon: GeoPolygon): List<List<Position>> =
    polygon.rings.map { ring -> ring.map(GeoPoint::toPosition) }
