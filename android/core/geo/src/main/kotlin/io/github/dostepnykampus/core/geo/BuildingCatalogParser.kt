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
 * - Nazwa złożona wyłącznie z symboli budynków, np. „C-13”, „C-3/C-4” albo „T-22” przy T-18, to brak nazwy
 *   (decyzja 75): ekran pokaże wtedy sam symbol.
 * - Kontur z kilkoma symbolami, np. `["C-3", "C-4"]`, daje osobny budynek dla każdego symbolu z tym samym konturem.
 * - Wejście z kilkoma symbolami daje wejście w każdym z tych budynków, z tym samym identyfikatorem `osm_id`.
 * - Rodzaj wejścia to tag `entrance`, a wejście główne ma `entrance` = `main`. Opis wejścia to tag `ref`, np. `W-1`.
 * - Oznaczenie wejścia pochodzi z tagu `wheelchair` (BR-24, decyzja 74): `yes` i `designated` dają „dostępne”,
 *   `no` daje „niedostępne”, a `limited`, każda inna wartość i brak tagu — „brak danych”.
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
                name = outlines.firstNotNullOfOrNull { it.textOrNull("name")?.takeUnless(::isSymbolsOnly) },
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

private val SYMBOLS_ONLY = Regex("""[A-Z]-\d+[a-z]?(\s*/\s*[A-Z]-\d+[a-z]?)*""")

private fun isSymbolsOnly(name: String): Boolean = SYMBOLS_ONLY.matches(name.trim())
