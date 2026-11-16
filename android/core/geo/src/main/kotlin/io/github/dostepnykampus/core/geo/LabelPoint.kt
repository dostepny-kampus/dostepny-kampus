/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.geo

import io.github.dostepnykampus.core.model.GeoArea
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import org.locationtech.jts.algorithm.construct.MaximumInscribedCircle
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Polygon
import org.maplibre.spatialk.turf.measurement.area
import org.maplibre.spatialk.units.International
import kotlin.math.cos

private const val LABEL_TOLERANCE_DEGREES = 1.0 / 111_195.0

private val FACTORY = GeometryFactory()

/**
 * Punkt podpisu konturu [area]: środek największego koła wpisanego w największą część konturu („pole of
 * inaccessibility”, `MaximumInscribedCircle` z JTS jak `ST_MaximumInscribedCircle` w PostGIS), z dokładnością
 * ok. 1 m. Środek ciężkości konturu z dziedzińcem, jak A-1, może wypaść poza kontur, a ten punkt nie. Jeden punkt na
 * kontur daje jeden podpis także przy wielokącie złożonym (reguła 4 wymagań wspólnych). Liczy w rzucie płaskim ze
 * stopniem długości skróconym o cosinus szerokości konturu.
 *
 * Wymagania: US1
 */
public fun labelPoint(area: GeoArea): GeoPoint {
    val polygon = area.parts().maxBy { part -> part.toGeometry().area().toDouble(International.SquareMeters) }
    val outer = polygon.rings.first()
    val scale = cos(Math.toRadians(outer.sumOf(GeoPoint::latitude) / outer.size))
    val center = MaximumInscribedCircle.getCenter(polygon.toFlat(scale), LABEL_TOLERANCE_DEGREES).coordinate
    return GeoPoint(longitude = center.x / scale, latitude = center.y)
}

private fun GeoPolygon.toFlat(scale: Double): Polygon {
    val flat = rings.map { ring -> FACTORY.createLinearRing(ring.map { it.toFlat(scale) }.toTypedArray()) }
    return FACTORY.createPolygon(flat.first(), flat.drop(1).toTypedArray())
}

private fun GeoPoint.toFlat(scale: Double): Coordinate = Coordinate(longitude * scale, latitude)
