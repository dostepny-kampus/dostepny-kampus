/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.geo

import io.github.dostepnykampus.core.model.GeoArea
import io.github.dostepnykampus.core.model.GeoMultiPolygon
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import org.maplibre.spatialk.geojson.Geometry
import org.maplibre.spatialk.geojson.MultiPolygon
import org.maplibre.spatialk.geojson.Polygon
import org.maplibre.spatialk.geojson.Position

/** Kontur budynku z geometrii GeoJSON; inna geometria niż wielokąt daje [IllegalArgumentException]. */
internal fun Geometry?.toArea(): GeoArea {
    if (this is MultiPolygon) return GeoMultiPolygon(coordinates.map(::toModel))
    require(this is Polygon) { "Kontur budynku nie jest wielokątem: $this" }
    return toModel(coordinates)
}

private fun toModel(rings: List<List<Position>>): GeoPolygon =
    GeoPolygon(rings.map { ring -> ring.map { it.toModel() } })

/** Punkt modelu z pozycji GeoJSON (długość, szerokość). */
internal fun Position.toModel(): GeoPoint = GeoPoint(longitude = longitude, latitude = latitude)

/** Wielokąty składowe konturu: sam wielokąt albo części wielokąta złożonego. */
internal fun GeoArea.parts(): List<GeoPolygon> =
    when (this) {
        is GeoPolygon -> listOf(this)
        is GeoMultiPolygon -> polygons
    }
