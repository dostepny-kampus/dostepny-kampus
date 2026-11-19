/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Yevhenii Lukianenko, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.building

import io.github.dostepnykampus.core.data.cache.decodeCache
import io.github.dostepnykampus.core.data.cache.encodeCache
import io.github.dostepnykampus.core.geo.GeoAreaJson
import io.github.dostepnykampus.core.model.GeoArea
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.building.AssessmentSource
import io.github.dostepnykampus.core.model.building.Building
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceAssessment
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.core.network.building.ApiBuilding
import io.github.dostepnykampus.core.network.building.BuildingList
import io.github.dostepnykampus.core.network.building.DetailLevel
import kotlinx.serialization.Serializable
import java.time.LocalDate

/** Lista budynków z serwera z wejściami jako dokument JSON do wiersza `building_cache`. Wymagania: NFR-10 */
internal fun BuildingList.toCacheJson(): String =
    encodeCache(
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
                        entrances = item.entrances.map(::CachedEntrance),
                    )
                },
        ),
    )

/**
 * Lista budynków z dokumentu kopii; `null` dla zepsutego dokumentu albo złych konturów. Dokument bez wejść daje
 * budynki bez wejść.
 *
 * Wymagania: NFR-10
 */
internal fun String.toCachedBuildingList(): BuildingList? =
    runCatching {
        decodeCache(CachedList.serializer(), this).let { cached ->
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
    val entrances: List<CachedEntrance> = emptyList(),
)

@Serializable
private data class CachedEntrance(
    val id: String,
    val buildingSymbol: String,
    val name: String? = null,
    val isMain: Boolean,
    val type: String? = null,
    val longitude: Double,
    val latitude: Double,
    val marking: EntranceMarking,
    val assessment: CachedAssessment? = null,
    val floorId: String? = null,
    val approach: String = "",
    val doorWidthCm: Int = 0,
    val isNameGenerated: Boolean = false,
    val placeDescription: String = "",
) {
    constructor(entrance: Entrance) : this(
        id = entrance.id,
        buildingSymbol = entrance.buildingSymbol,
        name = entrance.name,
        isMain = entrance.isMain,
        type = entrance.type,
        longitude = entrance.location.longitude,
        latitude = entrance.location.latitude,
        marking = entrance.marking,
        assessment = entrance.assessment?.let { CachedAssessment(date = it.date.toString(), source = it.source) },
        floorId = entrance.floorId,
        approach = entrance.approach,
        doorWidthCm = entrance.doorWidthCm,
        isNameGenerated = entrance.isNameGenerated,
        placeDescription = entrance.placeDescription,
    )

    fun toModel(): Entrance =
        Entrance(
            id = id,
            buildingSymbol = buildingSymbol,
            name = name,
            isMain = isMain,
            type = type,
            location = GeoPoint(longitude = longitude, latitude = latitude),
            marking = marking,
            assessment = assessment?.let { EntranceAssessment(date = LocalDate.parse(it.date), source = it.source) },
            floorId = floorId,
            approach = approach,
            doorWidthCm = doorWidthCm,
            isNameGenerated = isNameGenerated,
            placeDescription = placeDescription,
        )
}

@Serializable
private data class CachedAssessment(val date: String, val source: AssessmentSource)

private fun CachedBuilding.toApi(): ApiBuilding =
    ApiBuilding(
        building = Building(symbol = symbol, name = name, outline = outline),
        detailLevel = detailLevel,
        entrances = entrances.map(CachedEntrance::toModel),
    )
