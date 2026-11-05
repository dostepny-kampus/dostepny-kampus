/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.buildings

import io.github.dostepnykampus.core.geo.parseGeoArea
import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.core.model.GeoArea
import io.github.dostepnykampus.core.model.GeoMultiPolygon
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.core.network.ApiBuilding
import io.github.dostepnykampus.core.network.BuildingList
import io.github.dostepnykampus.core.network.DetailLevel
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private val cacheJson = Json { ignoreUnknownKeys = true }

/** Lista budynków z serwera jako dokument JSON do wiersza `building_cache` (NFR-10). */
internal fun BuildingList.toCacheJson(): String =
    cacheJson.encodeToString(
        CachedList.serializer(),
        CachedList(
            source = source,
            buildings =
                buildings.map { item ->
                    CachedBuilding(
                        symbol = item.building.symbol,
                        name = item.building.name,
                        detailLevel = item.detailLevel,
                        outline = item.building.outline.toGeoJson(),
                    )
                },
        ),
    )

/** Lista budynków z dokumentu kopii; `null` dla zepsutego dokumentu albo złych konturów (NFR-10). */
internal fun String.toCachedBuildingList(): BuildingList? =
    runCatching {
        cacheJson.decodeFromString(CachedList.serializer(), this).let { cached ->
            BuildingList(
                source = cached.source,
                buildings = cached.buildings.map(CachedBuilding::toApi),
            )
        }
    }.getOrNull()

@Serializable
private data class CachedList(val source: String, val buildings: List<CachedBuilding>)

@Serializable
private data class CachedBuilding(
    val symbol: String,
    val name: String? = null,
    val detailLevel: DetailLevel,
    val outline: JsonElement,
)

private fun CachedBuilding.toApi(): ApiBuilding =
    ApiBuilding(
        building = Building(symbol = symbol, name = name, outline = parseGeoArea(outline.toString())),
        detailLevel = detailLevel,
    )

private fun GeoArea.toGeoJson(): JsonElement =
    when (this) {
        is GeoPolygon -> {
            polygonJson(rings)
        }

        is GeoMultiPolygon -> {
            JsonObject(
                mapOf(
                    "type" to JsonPrimitive("MultiPolygon"),
                    "coordinates" to JsonArray(polygons.map { polygon -> polygonRingsJson(polygon.rings) }),
                ),
            )
        }
    }

private fun polygonJson(rings: List<List<GeoPoint>>): JsonElement =
    JsonObject(
        mapOf(
            "type" to JsonPrimitive("Polygon"),
            "coordinates" to polygonRingsJson(rings),
        ),
    )

private fun polygonRingsJson(rings: List<List<GeoPoint>>): JsonArray = JsonArray(rings.map { ring -> ringJson(ring) })

private fun ringJson(ring: List<GeoPoint>): JsonArray =
    JsonArray(
        ring.map { point ->
            JsonArray(listOf(JsonPrimitive(point.longitude), JsonPrimitive(point.latitude)))
        },
    )
