/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.buildings

import io.github.dostepnykampus.core.geo.GeoAreaJson
import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.core.model.GeoArea
import io.github.dostepnykampus.core.network.ApiBuilding
import io.github.dostepnykampus.core.network.BuildingList
import io.github.dostepnykampus.core.network.DetailLevel
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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
                        outline = item.building.outline,
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
    @Serializable(with = GeoAreaJson::class) val outline: GeoArea,
)

private fun CachedBuilding.toApi(): ApiBuilding =
    ApiBuilding(
        building = Building(symbol = symbol, name = name, outline = outline),
        detailLevel = detailLevel,
    )
