/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.data

import io.github.dostepnykampus.core.model.BuildingCatalog
import io.github.dostepnykampus.core.model.GeoArea
import io.github.dostepnykampus.core.model.GeoMultiPolygon
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.GeoJsonObject
import org.maplibre.spatialk.geojson.MultiPolygon
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Polygon
import org.maplibre.spatialk.geojson.PolygonGeometry
import org.maplibre.spatialk.geojson.Position

/**
 * Dane warstw mapy V1 z katalogu budynków jako GeoJSON dla źródeł mapy, bez sieci (US1, US1.4, NFR-10).
 *
 * @property buildings kontury budynków; obiekt ma właściwość `symbol`, czyli podpis konturu na mapie
 * @property entrances wejścia, każde raz po identyfikatorze, także wejście wspólne kilku budynków; obiekt ma
 *   właściwości `marking` (`ACCESSIBLE`, `INACCESSIBLE` albo `NO_DATA`) i `main`
 * @property bounds prostokąt obejmujący wszystkie kontury, do pierwszego widoku mapy; `null` bez budynków
 */
data class CampusMapData(val buildings: String, val entrances: String, val bounds: GeoBounds?)

/**
 * Prostokąt na mapie w WGS84.
 *
 * @property southWest róg południowo-zachodni
 * @property northEast róg północno-wschodni
 */
data class GeoBounds(val southWest: GeoPoint, val northEast: GeoPoint)

/**
 * Dane warstw mapy z [catalog]. Katalog z repozytorium nie ma już wejść `emergency`, `exit` i `garage`
 * (BR-25, decyzja 76), więc mapa ich nie rysuje.
 */
fun campusMapData(catalog: BuildingCatalog): CampusMapData {
    val buildings =
        catalog.buildings.map { building ->
            Feature(building.outline.toGeometry(), buildJsonObject { put(key = "symbol", value = building.symbol) })
        }
    val entrances =
        catalog.entrances.distinctBy { it.id }.map { entrance ->
            val properties =
                buildJsonObject {
                    put(key = "marking", value = entrance.marking.name)
                    put(key = "main", value = entrance.isMain)
                }
            Feature(Point(entrance.location.toPosition()), properties)
        }
    return CampusMapData(
        buildings = GeoJsonObject.toJson(FeatureCollection(buildings)),
        entrances = GeoJsonObject.toJson(FeatureCollection(entrances)),
        bounds = bounds(catalog.buildings.flatMap { it.outline.points() }),
    )
}

private fun bounds(points: List<GeoPoint>): GeoBounds? =
    points.takeIf { it.isNotEmpty() }?.let { all ->
        GeoBounds(
            southWest = GeoPoint(longitude = all.minOf(GeoPoint::longitude), latitude = all.minOf(GeoPoint::latitude)),
            northEast = GeoPoint(longitude = all.maxOf(GeoPoint::longitude), latitude = all.maxOf(GeoPoint::latitude)),
        )
    }

private fun GeoPoint.toPosition(): Position = Position(longitude, latitude)

private fun GeoPolygon.toPositions(): List<List<Position>> = rings.map { ring -> ring.map(GeoPoint::toPosition) }

private fun GeoArea.toGeometry(): PolygonGeometry =
    when (this) {
        is GeoPolygon -> Polygon(toPositions())
        is GeoMultiPolygon -> MultiPolygon(polygons.map(GeoPolygon::toPositions))
    }

private fun GeoArea.points(): List<GeoPoint> =
    when (this) {
        is GeoPolygon -> rings.flatten()
        is GeoMultiPolygon -> polygons.flatMap { it.rings.flatten() }
    }
