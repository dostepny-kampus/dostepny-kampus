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
 * Czyta katalog budynków z GeoJSON w formacie pliku `data/buildings/pwr-buildings.geojson` (opis formatu
 * w `data/README.md`), bez sieci (US1, US17.1).
 *
 * - Kontury z tym samym symbolem, np. dwa obiekty OSM `H-14`, tworzą jeden budynek z wielokątem złożonym.
 *   Nazwą budynku jest pierwsza nazwa podana w danych.
 * - Kontur z kilkoma symbolami, np. `["C-3", "C-4"]`, daje osobny budynek dla każdego symbolu z tym samym konturem.
 * - Wejście z kilkoma symbolami daje wejście w każdym z tych budynków, z tym samym identyfikatorem `osm_id`.
 * - Wejście główne to wejście z tagiem `entrance` = `main`.
 * - Opis wejścia to tag `ref`, np. `W-1`, albo pusty napis, gdy tagu brak.
 * - Oznaczenie wejścia pochodzi z tagu `wheelchair` (BR-24): `yes` daje „dostępne”, `no` daje „niedostępne”,
 *   a każda inna wartość i brak tagu — „brak danych”.
 *
 * Budynki i wejścia mają kolejność z pliku.
 *
 * @param geoJson treść pliku GeoJSON
 * @return katalog budynków
 * @throws IllegalArgumentException gdy treść nie jest kolekcją obiektów GeoJSON w tym formacie
 */
fun parseBuildingCatalog(geoJson: String): BuildingCatalog {
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
                name = outlines.firstNotNullOfOrNull { it.textOrNull("name") },
                outline = areas.singleOrNull() ?: GeoMultiPolygon(areas.flatMap(GeoArea::parts)),
            )
        }

private fun entrances(feature: GeoFeature): List<Entrance> {
    val point = feature.geometry as? Point
    requireNotNull(point) { "Wejście ${feature.text("osm_id")} nie jest punktem" }
    return feature.symbols().map { symbol ->
        Entrance(
            id = feature.text("osm_id"),
            buildingSymbol = symbol,
            name = feature.textOrNull("ref").orEmpty(),
            isMain = feature.textOrNull("entrance") == "main",
            location = point.coordinates.toModel(),
            marking =
                when (feature.textOrNull("wheelchair")) {
                    "yes" -> EntranceMarking.ACCESSIBLE
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
    val symbols = properties()["symbols"]?.jsonArray.orEmpty().map { it.jsonPrimitive.content }
    require(symbols.isNotEmpty()) { "Obiekt ${text("osm_id")} bez symbolu budynku" }
    return symbols
}
