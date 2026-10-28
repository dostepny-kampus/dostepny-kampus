/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.geo

import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.core.model.GeoArea
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import org.maplibre.spatialk.geojson.LineString
import org.maplibre.spatialk.geojson.MultiPolygon
import org.maplibre.spatialk.geojson.Position
import org.maplibre.spatialk.turf.booleans.contains
import org.maplibre.spatialk.turf.measurement.distance

/**
 * Podpowiedź budynku dla punktu, bez sieci (US6.2, to-be T3 i T4, BR-G2). Obliczenia wykonuje Turf.
 *
 * - Punkt w konturze budynku daje [LocateResult.InsideOutline]. Kontur może być wielokątem złożonym:
 *   wystarczy, że punkt leży w jednej z części.
 * - Punkt w otworze konturu, np. na dziedzińcu A-1, nie leży w konturze.
 * - Punkt na krawędzi konturu, także na krawędzi otworu, leży w konturze.
 * - Gdy punkt leży w konturach kilku budynków, wynikiem jest pierwszy z nich w kolejności [buildings].
 * - Punkt poza konturem każdego budynku daje stan „Poza budynkiem” ([LocateResult.OutsideBuilding]) bez
 *   wyjątku, z propozycją budynku o najmniejszej odległości od punktu do krawędzi konturu.
 *
 * @param point punkt, dla którego szukamy budynku
 * @param buildings budynki z konturami; pierścienie konturów są zamknięte, jak w GeoJSON
 * @return podpowiedź budynku
 * @throws IllegalArgumentException gdy kontur ma pierścień niezamknięty albo krótszy niż 4 punkty
 */
fun locateBuilding(point: GeoPoint, buildings: List<Building>): LocateResult {
    val position = point.toPosition()
    val inside = buildings.firstOrNull { position in it.outline.toTurf() }
    return if (inside != null) {
        LocateResult.InsideOutline(inside)
    } else {
        LocateResult.OutsideBuilding(
            buildings.minByOrNull { building ->
                building.outline.allRings().minOf { ring ->
                    distance(position, LineString(ring.map(GeoPoint::toPosition)))
                }
            },
        )
    }
}

private fun GeoPoint.toPosition(): Position = Position(longitude, latitude)

private fun GeoPolygon.toPositions(): List<List<Position>> = rings.map { ring -> ring.map(GeoPoint::toPosition) }

private fun GeoArea.toTurf(): MultiPolygon = MultiPolygon(polygons.map(GeoPolygon::toPositions))

private fun GeoArea.allRings(): List<List<GeoPoint>> = polygons.flatMap(GeoPolygon::rings)
