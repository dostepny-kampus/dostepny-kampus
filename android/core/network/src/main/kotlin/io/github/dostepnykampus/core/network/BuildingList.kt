/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network

import io.github.dostepnykampus.core.geo.buildingName
import io.github.dostepnykampus.core.geo.parseGeoArea
import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.core.model.Entrance
import io.github.dostepnykampus.core.model.EntranceMarking
import io.github.dostepnykampus.core.model.GeoPoint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Lista budynków z atrybucją konturów (`BuildingList`).
 *
 * @property source atrybucja, np. „© OpenStreetMap contributors”
 * @property buildings budynki z poziomem szczegółowości
 */
data class BuildingList(val source: String, val buildings: List<ApiBuilding>)

/**
 * Budynek z serwera: model mapy i poziom szczegółowości.
 *
 * @property building symbol, nazwa i kontur
 * @property detailLevel poziom szczegółowości z kontraktu
 */
data class ApiBuilding(val building: Building, val detailLevel: DetailLevel)

/**
 * Karta budynku (`BuildingDetails`).
 *
 * Kontrakt nie podaje rodzaju wejścia OSM, więc [Entrance.type] jest puste.
 * Dzień i źródło oznaczenia są sprawdzane; model wejścia ich nie trzyma.
 *
 * @property building symbol, nazwa i kontur
 * @property detailLevel poziom szczegółowości
 * @property entrances wejścia; pusta lista, dopóki serwer nie ma danych wejść
 */
data class BuildingDetails(val building: Building, val detailLevel: DetailLevel, val entrances: List<Entrance>)

/**
 * Podpowiedź budynku dla punktu (`LocateResult`).
 *
 * @property buildingSymbol budynek w konturze albo najbliższy; `null` poza budynkiem (BR-G4)
 * @property isInsideOutline czy punkt leży w konturze tego budynku (pole `insideOutline`)
 */
data class BuildingAtPoint(val buildingSymbol: String?, val isInsideOutline: Boolean)

/** Poziom szczegółowości budynku (`DetailLevel`). */
@Serializable
enum class DetailLevel {
    /** Budynek szczegółowy z planami pięter. */
    DETAILED,

    /** Budynek podstawowy, bez przełącznika pięter. */
    BASIC,
}

@Serializable
internal data class BuildingListDto(val source: String, val buildings: List<BuildingDto>)

@Serializable
internal data class BuildingDto(
    val symbol: String,
    val name: String,
    val detailLevel: DetailLevel,
    val outline: JsonElement,
)

@Serializable
internal data class BuildingDetailsDto(
    val symbol: String,
    val name: String,
    val detailLevel: DetailLevel,
    val outline: JsonElement,
    val entrances: List<EntranceDto>,
)

@Serializable
internal data class EntranceDto(
    val id: String,
    val buildingSymbol: String,
    val name: String? = null,
    val type: String? = null,
    @SerialName("main") val isMain: Boolean,
    val location: GeoPointDto,
    val marking: String,
    val markingDate: String,
    val markingSource: MarkingSource,
)

@Serializable
internal data class GeoPointDto(val type: String, val coordinates: List<Double>)

@Serializable
internal data class LocateResultDto(
    val buildingSymbol: String? = null,
    @SerialName("insideOutline") val isInsideOutline: Boolean,
)

@Serializable
internal enum class MarkingSource { IMPORT, WALKTHROUGH }

internal fun BuildingListDto.toModel(): BuildingList =
    BuildingList(source = source, buildings = buildings.map(BuildingDto::toModel))

internal fun BuildingDto.toModel(): ApiBuilding =
    ApiBuilding(
        building = Building(symbol = symbol, name = buildingName(name), outline = parseGeoArea(outline.toString())),
        detailLevel = detailLevel,
    )

internal fun BuildingDetailsDto.toModel(): BuildingDetails =
    BuildingDetails(
        building = Building(symbol = symbol, name = buildingName(name), outline = parseGeoArea(outline.toString())),
        detailLevel = detailLevel,
        entrances = entrances.map(EntranceDto::toModel),
    )

internal fun EntranceDto.toModel(): Entrance {
    require(ISO_DATE.matches(markingDate)) { "Zła data oznaczenia wejścia" }
    return Entrance(
        id = id,
        buildingSymbol = buildingSymbol,
        name = name,
        isMain = isMain,
        type = type,
        location = location.toModel(),
        marking = EntranceMarking.valueOf(marking),
    )
}

internal fun LocateResultDto.toModel(): BuildingAtPoint =
    BuildingAtPoint(buildingSymbol = buildingSymbol, isInsideOutline = isInsideOutline)

internal fun GeoPointDto.toModel(): GeoPoint {
    require(type == "Point" && coordinates.size >= MIN_COORDINATES) { "Zły punkt GeoJSON" }
    return GeoPoint(longitude = coordinates[LONGITUDE], latitude = coordinates[LATITUDE])
}

internal fun GeoPoint.toDto(): GeoPointDto = GeoPointDto(type = "Point", coordinates = listOf(longitude, latitude))

private val ISO_DATE = Regex("""\d{4}-\d{2}-\d{2}""")

private const val MIN_COORDINATES = 2
private const val LONGITUDE = 0
private const val LATITUDE = 1
