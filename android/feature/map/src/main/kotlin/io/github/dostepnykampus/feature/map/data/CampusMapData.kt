/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.data

import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.core.model.BuildingCatalog
import io.github.dostepnykampus.core.model.GeoArea
import io.github.dostepnykampus.core.model.GeoMultiPolygon
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
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

/** Budynek w środku pierwszego widoku mapy: główny kampus (decyzja 80). */
const val START_BUILDING = "A-1"

/**
 * Dane warstw mapy V1 z katalogu budynków jako GeoJSON dla źródeł mapy, bez sieci (US1, US1.4, NFR-10).
 *
 * @property buildings kontury budynków; obiekt ma właściwość `label`, czyli podpis konturu na mapie, i listę
 *   `symbols`. Kontur wspólny kilku symboli to jeden obiekt z podpisem „C-3/C-4” (decyzja 79)
 * @property labels podpisy konturów, każdy raz, w kolejności katalogu; widok mapy rysuje z nich obrazy podpisów
 * @property entrances wejścia, każde raz po identyfikatorze, także wejście wspólne kilku budynków; obiekt ma
 *   właściwości `marking` (`ACCESSIBLE`, `INACCESSIBLE` albo `NO_DATA`) i `main`
 * @property start środek pierwszego widoku: środek budynku [START_BUILDING] (US1, US1.2, decyzja 80), a bez niego
 *   środek wszystkich konturów; `null` bez budynków
 */
data class CampusMapData(val buildings: String, val labels: List<String>, val entrances: String, val start: GeoPoint?)

/**
 * Dane warstw mapy z [catalog]. Katalog z repozytorium nie ma już wejść `emergency`, `exit` i `garage`
 * (BR-25, decyzja 76), więc mapa ich nie rysuje.
 */
fun campusMapData(catalog: BuildingCatalog): CampusMapData {
    val outlines = catalog.buildings.groupBy(keySelector = Building::outline, valueTransform = Building::symbol)
    val buildings =
        outlines.map { (outline, symbols) ->
            val properties =
                buildJsonObject {
                    put(key = "label", value = symbols.joinToString(separator = "/"))
                    put(key = "symbols", element = JsonArray(symbols.map { JsonPrimitive(it) }))
                }
            Feature(outline.toGeometry(), properties)
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
    val startBuildings = catalog.buildings.filter { it.symbol == START_BUILDING }.ifEmpty { catalog.buildings }
    return CampusMapData(
        buildings = GeoJsonObject.toJson(FeatureCollection(buildings)),
        labels = outlines.values.map { it.joinToString(separator = "/") },
        entrances = GeoJsonObject.toJson(FeatureCollection(entrances)),
        start = center(startBuildings.flatMap { it.outline.points() }),
    )
}

private fun center(points: List<GeoPoint>): GeoPoint? =
    points.takeIf { it.isNotEmpty() }?.let { all ->
        GeoPoint(
            longitude = (all.minOf(GeoPoint::longitude) + all.maxOf(GeoPoint::longitude)) / 2,
            latitude = (all.minOf(GeoPoint::latitude) + all.maxOf(GeoPoint::latitude)) / 2,
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
