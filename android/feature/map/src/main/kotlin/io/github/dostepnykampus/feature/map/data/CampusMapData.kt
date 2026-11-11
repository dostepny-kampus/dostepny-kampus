/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.data

import io.github.dostepnykampus.core.geo.allRings
import io.github.dostepnykampus.core.geo.campusAreas
import io.github.dostepnykampus.core.geo.labelPoint
import io.github.dostepnykampus.core.geo.toGeometry
import io.github.dostepnykampus.core.geo.toPosition
import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.core.model.BuildingCatalog
import io.github.dostepnykampus.core.model.GeoArea
import io.github.dostepnykampus.core.model.GeoPoint
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.maplibre.spatialk.geojson.BoundingBox
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Geometry
import org.maplibre.spatialk.geojson.Point
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
 * Dane warstw mapy V1 z katalogu budynków jako kolekcje GeoJSON w elementach JSON dla źródeł mapy, bez sieci
 * (US1, US1.4, NFR-10). Styl wstawia je do źródeł bez parsowania napisu (#835).
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
 * @property bounds teren startowy mapy: najmniejszy prostokąt z konturami budynków terenu, na którym stoi
 *   [START_BUILDING], powiększony o 500 m z każdej strony (BR-G5, NFR-39). Bez tego budynku to pierwszy teren
 *   katalogu; `null` bez budynków
 * @property areas tereny PWr z katalogu, każdy z prostokątem jak [bounds] (BR-G5)
 * @property labelPointsBySymbol punkt podpisu konturu każdego symbolu, ten sam co w [labelPoints]; symbole konturu
 *   wspólnego mają jeden punkt. Kamera bierze punkt stąd i nie liczy go drugi raz na wątku interfejsu (NFR-11)
 */
data class CampusMapData(
    val buildings: JsonElement,
    val labels: List<String>,
    val labelPoints: JsonElement,
    val entrances: JsonElement,
    val start: GeoPoint?,
    val bounds: CampusBounds? = null,
    val areas: List<MapArea> = emptyList(),
    val labelPointsBySymbol: Map<String, GeoPoint> = emptyMap(),
)

/**
 * Prostokąt terenu budynku [symbol]: budynek pokazany z V2 albo z wyszukiwania może stać na innym terenie niż
 * [START_BUILDING] (US1.8, BR-G5). Bez symbolu i dla symbolu spoza katalogu to teren startowy.
 */
fun CampusMapData.boundsOf(symbol: String?): CampusBounds? =
    areas.firstOrNull { symbol in it.symbols }?.bounds ?: bounds

/**
 * Teren PWr na mapie (BR-G5).
 *
 * @property name nazwa terenu, np. „Kampus główny”
 * @property symbols symbole budynków terenu
 * @property bounds prostokąt konturów budynków terenu powiększony o 500 m z każdej strony (NFR-39)
 */
data class MapArea(val name: String, val symbols: List<String>, val bounds: CampusBounds)

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
 * (BR-25, decyzja 76), więc mapa ich nie rysuje. Liczenie można przerwać przerwaniem wątku: sprawdza je przed każdym
 * punktem podpisu, najcięższym krokiem, i rzuca [InterruptedException] (NFR-11).
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
    val labelled =
        outlines.map { (outline, symbols) ->
            if (Thread.interrupted()) throw InterruptedException("liczenie geometrii przerwane")
            symbols to labelPoint(outline)
        }
    val labelPoints =
        labelled.map { (symbols, point) ->
            val properties = buildJsonObject { put(key = "label", value = symbols.joinToString(LABEL_SEPARATOR)) }
            Feature(Point(point.toPosition()), properties)
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
    val areas =
        campusAreas(catalog.buildings).mapNotNull { area ->
            campusBounds(area.buildings.flatMap { it.outline.points() })?.let { MapArea(area.name, area.symbols, it) }
        }
    val startArea = areas.firstOrNull { START_BUILDING in it.symbols } ?: areas.firstOrNull()
    return CampusMapData(
        buildings = featureCollection(buildings),
        labels = outlines.values.map { it.joinToString(separator = LABEL_SEPARATOR) },
        labelPoints = featureCollection(labelPoints),
        entrances = featureCollection(entrances),
        start = center(startBuildings.flatMap { it.outline.points() }),
        bounds = startArea?.bounds,
        areas = areas,
        labelPointsBySymbol = labelled.flatMap { (symbols, point) -> symbols.map { it to point } }.toMap(),
    )
}

/**
 * Symbole budynków z podpisów [labels] konturów trafionych jednym dotknięciem, każdy raz. Kontur wspólny
 * „C-3/C-4” daje oba symbole, a wtedy ekran pyta „Który budynek?” (decyzja 85).
 */
fun symbolsOf(labels: List<String>): List<String> = labels.flatMap { it.split(LABEL_SEPARATOR) }.distinct()

private fun center(points: List<GeoPoint>): GeoPoint? {
    if (points.isEmpty()) return null
    val box = computeBbox(points.map(GeoPoint::toPosition))
    return GeoPoint(longitude = (box.west + box.east) / 2, latitude = (box.south + box.north) / 2)
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

private fun GeoArea.points(): List<GeoPoint> = allRings().flatten()

/**
 * Kolekcja GeoJSON z obiektów [features] jako element JSON, bez napisu po drodze. Napis powstaje raz, gdy styl
 * mapy idzie do MapLibre (#835).
 */
internal fun featureCollection(features: List<Feature<Geometry, JsonObject?>>): JsonElement =
    Json.encodeToJsonElement(
        FeatureCollection.serializer(Geometry.serializer(), JsonObject.serializer().nullable),
        FeatureCollection(features),
    )
