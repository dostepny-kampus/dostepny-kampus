/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network.building

import io.github.dostepnykampus.core.geo.GeoAreaJson
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.building.AssessmentSource
import io.github.dostepnykampus.core.model.building.Building
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceAssessment
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.core.model.building.Floor
import io.github.dostepnykampus.core.model.building.FloorList
import io.github.dostepnykampus.core.network.decodeContract
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeParseException
import io.github.dostepnykampus.core.network.json.Building as BuildingJson
import io.github.dostepnykampus.core.network.json.BuildingDetails as BuildingDetailsJson
import io.github.dostepnykampus.core.network.json.BuildingList as BuildingListJson
import io.github.dostepnykampus.core.network.json.Entrance as EntranceJson
import io.github.dostepnykampus.core.network.json.FloorList as FloorListJson
import io.github.dostepnykampus.core.network.json.GeoPoint as GeoPointJson
import io.github.dostepnykampus.core.network.json.LocateResult as LocateResultJson

/**
 * Lista budynków z atrybucją konturów (`BuildingList`).
 *
 * @property source atrybucja, np. „© OpenStreetMap contributors”
 * @property buildings budynki z poziomem szczegółowości
 */
public data class BuildingList(val source: String, val buildings: List<ApiBuilding>)

/**
 * Budynek z serwera: model mapy i poziom szczegółowości.
 *
 * @property building symbol, nazwa i kontur
 * @property detailLevel poziom szczegółowości z kontraktu
 */
public data class ApiBuilding(val building: Building, val detailLevel: DetailLevel)

/**
 * Karta budynku (`BuildingDetails`).
 *
 * Kontrakt nie podaje rodzaju wejścia OSM, więc [Entrance.type] jest puste.
 * Dzień i źródło oznaczenia trafiają do [Entrance.assessment]; nieistniejący dzień
 * albo nieznane źródło to błąd treści odpowiedzi. Import wejścia z polem `source` ma źródło
 * [AssessmentSource.PWR_PLANS]. Brak opisu dojścia to pusty tekst, a brak szerokości drzwi to 0.
 *
 * Wymagania: US17.3, BR-24, BR-27
 *
 * @property building symbol, nazwa i kontur
 * @property detailLevel poziom szczegółowości
 * @property entrances wejścia; pusta lista, dopóki serwer nie ma danych wejść
 */
public data class BuildingDetails(val building: Building, val detailLevel: DetailLevel, val entrances: List<Entrance>)

/**
 * Podpowiedź budynku dla punktu (`LocateResult`).
 *
 * Wymagania: BR-G4
 *
 * @property buildingSymbol budynek w konturze albo najbliższy; `null` poza budynkiem
 * @property isInsideOutline czy punkt leży w konturze tego budynku (pole `insideOutline`)
 */
public data class BuildingAtPoint(val buildingSymbol: String?, val isInsideOutline: Boolean)

/** Poziom szczegółowości budynku (`DetailLevel`). */
public enum class DetailLevel {
    /** Budynek szczegółowy z planami pięter. */
    DETAILED,

    /** Budynek podstawowy, bez przełącznika pięter. */
    BASIC,
}

/** Lista budynków z tekstu odpowiedzi [body] (`BuildingList`); zła treść daje [IllegalArgumentException]. */
internal fun Json.decodeBuildingList(body: String): BuildingList {
    val list = decodeContract(BuildingListJson.serializer(), body)
    return BuildingList(source = list.source, buildings = list.buildings.map { it.toModel(json = this) })
}

/** Karta budynku z tekstu odpowiedzi [body] (`BuildingDetails`); zła treść jak w [decodeBuildingList]. */
internal fun Json.decodeBuildingDetails(body: String): BuildingDetails {
    val card = decodeContract(BuildingDetailsJson.serializer(), body)
    val outline = decodeFromJsonElement(GeoAreaJson, card.outline)
    return BuildingDetails(
        building = Building(symbol = card.symbol, name = card.name, outline = outline),
        detailLevel = DetailLevel.valueOf(card.detailLevel.name),
        entrances = card.entrances.map(EntranceJson::toModel),
    )
}

/** Piętra z [body] (`FloorList`) od najniższego, także w innej kolejności z serwera. Wymagania: US16.1 */
internal fun Json.decodeFloorList(body: String): FloorList {
    val list = decodeContract(FloorListJson.serializer(), body)
    return FloorList(
        source = list.source.value,
        floors = list.floors.map { Floor(id = it.id, label = it.label, order = it.order) }.sortedBy(Floor::order),
    )
}

/** Podpowiedź budynku z tekstu odpowiedzi [body] (`LocateResult`); zła treść jak w [decodeBuildingList]. */
internal fun Json.decodeLocateResult(body: String): BuildingAtPoint {
    val result = decodeContract(LocateResultJson.serializer(), body)
    return BuildingAtPoint(buildingSymbol = result.buildingSymbol, isInsideOutline = result.insideOutline)
}

private fun EntranceJson.toModel(): Entrance =
    Entrance(
        id = id,
        buildingSymbol = buildingSymbol,
        name = name,
        isMain = main,
        type = type,
        location = location.toModel(),
        marking = EntranceMarking.valueOf(marking.name),
        assessment = EntranceAssessment(date = markingDay(markingDate), source = assessmentSource()),
        floorId = floorId,
        approach = approach.orEmpty(),
        doorWidthCm = doorWidthCm?.coerceAtLeast(0) ?: 0,
    )

private fun EntranceJson.assessmentSource(): AssessmentSource =
    when (markingSource) {
        EntranceJson.MarkingSource.IMPORT -> if (source == null) AssessmentSource.IMPORT else AssessmentSource.PWR_PLANS
        EntranceJson.MarkingSource.WALKTHROUGH -> AssessmentSource.WALKTHROUGH
    }

private fun BuildingJson.toModel(json: Json): ApiBuilding =
    ApiBuilding(
        building = Building(symbol = symbol, name = name, outline = json.decodeFromJsonElement(GeoAreaJson, outline)),
        detailLevel = DetailLevel.valueOf(detailLevel.name),
    )

private fun markingDay(text: String): LocalDate =
    try {
        LocalDate.parse(text)
    } catch (error: DateTimeParseException) {
        throw IllegalArgumentException("Zła data oznaczenia wejścia", error)
    }

internal fun GeoPointJson.toModel(): GeoPoint {
    require(type == "Point" && coordinates.size >= MIN_COORDINATES) { "Zły punkt GeoJSON" }
    return GeoPoint(longitude = coordinates[LONGITUDE], latitude = coordinates[LATITUDE])
}

internal fun GeoPoint.toJson(): GeoPointJson = GeoPointJson(type = "Point", coordinates = listOf(longitude, latitude))

private const val MIN_COORDINATES = 2
private const val LONGITUDE = 0
private const val LATITUDE = 1
