/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.geo

import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.core.model.BuildingCatalog
import io.github.dostepnykampus.core.model.Entrance
import io.github.dostepnykampus.core.model.EntranceMarking
import io.github.dostepnykampus.core.model.GeoArea
import io.github.dostepnykampus.core.model.GeoMultiPolygon
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Geometry
import org.maplibre.spatialk.geojson.Point

/**
 * Czyta bez sieci katalog budynków z GeoJSON w formacie `data/buildings/pwr-buildings.geojson` (opis w
 * `data/README.md`). Kontury z tym samym symbolem tworzą jeden budynek z wielokątem złożonym i pierwszą podaną nazwą, a
 * kontur albo wejście z kilkoma symbolami należy do każdego z tych budynków. Nazwa to pole `display_name` bez
 * przeróbek. Rodzaj wejścia to tag `entrance` (`main` to wejście główne), opis to tag `ref`. Oznaczenie z tagu
 * `wheelchair`: `yes` i `designated` dają „dostępne”, `no` „niedostępne”, a inna wartość i brak tagu „brak danych”.
 * Kolejność jest z pliku.
 *
 * Wymagania: US1, US17.1, US2, NFR-18, BR-24
 *
 * @param geoJson treść pliku GeoJSON
 * @return katalog budynków
 * @throws IllegalArgumentException gdy treść nie jest kolekcją obiektów GeoJSON w tym formacie
 */
public fun parseBuildingCatalog(geoJson: String): BuildingCatalog {
    val collection = FeatureCollection.fromJson<Geometry?, JsonObject?>(geoJson)
    val attribution = collection.foreignMembers["attribution"]?.text()
    requireNotNull(attribution) { "Brak atrybucji źródła danych (pole attribution)" }
    val byKind = collection.features.groupBy { it.text(KIND) }
    val unknown = byKind.keys - setOf(BUILDING, ENTRANCE)
    require(unknown.isEmpty()) { "Nieznany rodzaj obiektu: $unknown" }
    return BuildingCatalog(
        buildings = buildings(byKind[BUILDING].orEmpty()),
        entrances = byKind[ENTRANCE].orEmpty().flatMap(::entrances),
        attribution = attribution,
    )
}

private const val KIND = "kind"
private const val BUILDING = "building"
private const val ENTRANCE = "entrance"

private typealias GeoFeature = Feature<Geometry?, JsonObject?>

private fun buildings(features: List<GeoFeature>): List<Building> =
    features
        .flatMap { feature -> feature.symbols().map { symbol -> symbol to feature } }
        .groupBy(keySelector = { it.first }, valueTransform = { it.second })
        .map { (symbol, outlines) ->
            val areas = outlines.map { it.geometry.toArea() }
            Building(
                symbol = symbol,
                name = outlines.firstNotNullOfOrNull { it.textOrNull("display_name") },
                outline = areas.singleOrNull() ?: GeoMultiPolygon(areas.flatMap(GeoArea::parts)),
            )
        }

private fun entrances(feature: GeoFeature): List<Entrance> {
    val point = feature.geometry as? Point
    requireNotNull(point) { "Wejście ${feature.text("osm_id")} nie jest punktem" }
    val type = feature.textOrNull("entrance")
    return feature.symbols().map { symbol ->
        Entrance(
            id = feature.text("osm_id"),
            buildingSymbol = symbol,
            name = feature.textOrNull("ref"),
            isMain = type == "main",
            type = type,
            location = point.coordinates.toModel(),
            marking =
                when (feature.textOrNull("wheelchair")) {
                    "yes", "designated" -> EntranceMarking.ACCESSIBLE
                    "no" -> EntranceMarking.INACCESSIBLE
                    else -> EntranceMarking.NO_DATA
                },
        )
    }
}

private fun GeoFeature.properties(): JsonObject = requireNotNull(properties) { "Obiekt bez właściwości" }

private fun GeoFeature.textOrNull(key: String): String? = properties()[key]?.text()

private fun JsonElement.text(): String? = jsonPrimitive.contentOrNull

private fun GeoFeature.text(key: String): String = requireNotNull(textOrNull(key)) { "Obiekt bez pola $key" }

private fun GeoFeature.symbols(): List<String> {
    val symbols = properties()["symbols"]?.jsonArray.orEmpty().map { it.text() }
    require(symbols.isNotEmpty() && null !in symbols) { "Obiekt ${text("osm_id")} bez symbolu budynku" }
    return symbols.filterNotNull()
}
