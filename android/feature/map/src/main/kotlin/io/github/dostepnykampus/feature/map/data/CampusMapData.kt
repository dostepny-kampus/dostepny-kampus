/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.data

import io.github.dostepnykampus.core.geo.labelPoint
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
import org.maplibre.spatialk.geojson.BoundingBox
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.GeoJsonObject
import org.maplibre.spatialk.geojson.MultiPolygon
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Polygon
import org.maplibre.spatialk.geojson.PolygonGeometry
import org.maplibre.spatialk.geojson.Position
import org.maplibre.spatialk.turf.measurement.computeBbox
import org.maplibre.spatialk.turf.measurement.offset
import org.maplibre.spatialk.units.Bearing
import org.maplibre.spatialk.units.extensions.meters

/** Budynek w środku pierwszego widoku mapy: główny kampus (decyzja 80). */
const val START_BUILDING = "A-1"

private const val LABEL_SEPARATOR = "/"

private val CAMPUS_MARGIN = 500.meters

/**
 * Dane warstw mapy V1 z katalogu budynków jako GeoJSON dla źródeł mapy, bez sieci (US1, US1.4, NFR-10).
 *
 * @property buildings kontury budynków; obiekt ma właściwość `label`, czyli podpis konturu na mapie, i listę
 *   `symbols`. Kontur wspólny kilku symboli to jeden obiekt z podpisem „C-3/C-4” (decyzja 79)
 * @property labels podpisy konturów, każdy raz, w kolejności katalogu; widok mapy rysuje z nich obrazy podpisów
 * @property labelPoints punkty podpisów, jeden na kontur, wewnątrz konturu, z właściwością `label`. Podpis przy
 *   konturze dzielonym na kafelki pojawiałby się w każdym kawałku albo w żadnym (US1, reguła 4 wymagań wspólnych)
 * @property entrances wejścia, każde raz po identyfikatorze, także wejście wspólne kilku budynków; obiekt ma
 *   właściwości `id`, `marking` (`ACCESSIBLE`, `INACCESSIBLE` albo `NO_DATA`) i `main`
 * @property start środek pierwszego widoku: środek budynku [START_BUILDING] (US1, US1.2, decyzja 80), a bez niego
 *   środek wszystkich konturów; `null` bez budynków
 * @property focus symbol budynku zaznaczonego na mapie, np. po „Wstecz” z V2 (US1.1); `null` bez zaznaczenia
 * @property selectedEntrance wejście zaznaczone pinezką z podpisem, np. po „Pokaż wejście na mapie” (US3.3,
 *   decyzja 134); `null` bez zaznaczenia
 * @property bounds obszar kampusu: najmniejszy prostokąt ze wszystkimi konturami powiększony o 500 m z każdej strony
 *   (NFR-39); `null` bez budynków
 */
data class CampusMapData(
    val buildings: String,
    val labels: List<String>,
    val labelPoints: String,
    val entrances: String,
    val start: GeoPoint?,
    val focus: String? = null,
    val selectedEntrance: SelectedEntrance? = null,
    val bounds: CampusBounds? = null,
)

/**
 * Wejście zaznaczone na mapie pinezką z podpisem (US3.3, decyzja 134).
 *
 * @property id identyfikator wejścia
 * @property location punkt wejścia, w którym stoi czubek pinezki
 * @property label podpis pinezki: nazwa wejścia albo tekst zastępczy, gdy dane nie mają nazwy
 * @property buildingSymbol symbol budynku ekranu, z którego użyto „Pokaż wejście na mapie” (decyzja 135)
 */
data class SelectedEntrance(val id: String, val location: GeoPoint, val label: String, val buildingSymbol: String)

/**
 * Wejście [id] z [catalog] jako zaznaczenie na mapie, widziane z budynku [buildingSymbol]; [unnamed] to podpis
 * wejścia bez nazwy w danych. `null`, gdy katalog nie ma takiego wejścia (US3.3).
 */
fun selectedEntrance(catalog: BuildingCatalog, id: String, buildingSymbol: String, unnamed: String): SelectedEntrance? =
    catalog.entrances.firstOrNull { it.id == id }?.let { found ->
        SelectedEntrance(
            id = found.id,
            location = found.location,
            label = found.name?.takeIf(String::isNotBlank) ?: unnamed,
            buildingSymbol = buildingSymbol,
        )
    }

/**
 * Prostokąt na mapie w stopniach (NFR-39).
 *
 * @property west długość geograficzna zachodniego brzegu
 * @property south szerokość geograficzna południowego brzegu
 * @property east długość geograficzna wschodniego brzegu
 * @property north szerokość geograficzna północnego brzegu
 */
data class CampusBounds(val west: Double, val south: Double, val east: Double, val north: Double)

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
                    put(key = "label", value = symbols.joinToString(separator = LABEL_SEPARATOR))
                    put(key = "symbols", element = JsonArray(symbols.map { JsonPrimitive(it) }))
                }
            Feature(outline.toGeometry(), properties)
        }
    val labelPoints =
        outlines.map { (outline, symbols) ->
            val properties = buildJsonObject { put(key = "label", value = symbols.joinToString(LABEL_SEPARATOR)) }
            Feature(Point(labelPoint(outline).toPosition()), properties)
        }
    val entrances =
        catalog.entrances.distinctBy { it.id }.map { entrance ->
            val properties =
                buildJsonObject {
                    put(key = "id", value = entrance.id)
                    put(key = "marking", value = entrance.marking.name)
                    put(key = "main", value = entrance.isMain)
                }
            Feature(Point(entrance.location.toPosition()), properties)
        }
    val startBuildings = catalog.buildings.filter { it.symbol == START_BUILDING }.ifEmpty { catalog.buildings }
    val allPoints = catalog.buildings.flatMap { it.outline.points() }
    return CampusMapData(
        buildings = GeoJsonObject.toJson(FeatureCollection(buildings)),
        labels = outlines.values.map { it.joinToString(separator = LABEL_SEPARATOR) },
        labelPoints = GeoJsonObject.toJson(FeatureCollection(labelPoints)),
        entrances = GeoJsonObject.toJson(FeatureCollection(entrances)),
        start = center(startBuildings.flatMap { it.outline.points() }),
        bounds = campusBounds(allPoints),
    )
}

/**
 * Symbole budynków z podpisów [labels] konturów trafionych jednym dotknięciem, każdy raz. Kontur wspólny
 * „C-3/C-4” daje oba symbole, a wtedy ekran pyta „Który budynek?” (decyzja 85).
 */
fun symbolsOf(labels: List<String>): List<String> = labels.flatMap { it.split(LABEL_SEPARATOR) }.distinct()

private fun center(points: List<GeoPoint>): GeoPoint? =
    points.takeIf { it.isNotEmpty() }?.let { all ->
        GeoPoint(
            longitude = (all.minOf(GeoPoint::longitude) + all.maxOf(GeoPoint::longitude)) / 2,
            latitude = (all.minOf(GeoPoint::latitude) + all.maxOf(GeoPoint::latitude)) / 2,
        )
    }

private fun campusBounds(points: List<GeoPoint>): CampusBounds? =
    points.takeIf { it.isNotEmpty() }?.let { all -> computeBbox(all.map(GeoPoint::toPosition)).withCampusMargin() }

private fun BoundingBox.withCampusMargin(): CampusBounds {
    val middleLatitude = (south + north) / 2
    return CampusBounds(
        west = Position(longitude = west, latitude = middleLatitude).offset(CAMPUS_MARGIN, Bearing.West).longitude,
        south = southwest.offset(CAMPUS_MARGIN, Bearing.South).latitude,
        east = Position(longitude = east, latitude = middleLatitude).offset(CAMPUS_MARGIN, Bearing.East).longitude,
        north = northeast.offset(CAMPUS_MARGIN, Bearing.North).latitude,
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
