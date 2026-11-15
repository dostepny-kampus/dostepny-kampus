/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network.inspection

import io.github.dostepnykampus.core.model.inspection.BuildingProgress
import io.github.dostepnykampus.core.model.inspection.InspectionCoverage
import io.github.dostepnykampus.core.model.inspection.InspectionMarker
import io.github.dostepnykampus.core.model.inspection.MarkerPlace
import io.github.dostepnykampus.core.network.decodeContract
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import io.github.dostepnykampus.core.network.json.BuildingProgress as BuildingProgressJson
import io.github.dostepnykampus.core.network.json.DetailLevel as DetailLevelJson
import io.github.dostepnykampus.core.network.json.InspectionMarker as InspectionMarkerJson

/**
 * Postęp budynków z tekstu odpowiedzi [body] (`BuildingProgress[]`).
 *
 * Brak pola właściwego dla poziomu szczegółowości budynku albo zła chwila znacznika to błąd treści.
 *
 * Wymagania: US11.1, BR-21
 *
 * @throws IllegalArgumentException gdy treść nie pasuje do kontraktu
 */
internal fun Json.decodeProgress(body: String): List<BuildingProgress> =
    decodeContract(ListSerializer(BuildingProgressJson.serializer()), body).map { it.toBuilding() }

private fun BuildingProgressJson.toBuilding(): BuildingProgress =
    BuildingProgress(
        buildingSymbol = buildingSymbol,
        coverage = coverage(),
        pendingSubmissions = pendingSubmissions,
        acceptedSubmissions = acceptedSubmissions,
        entrancesTotal = entrancesTotal,
        entrancesCheckedOnSite = entrancesCheckedOnSite,
        markers = markers.map { it.toMarker() },
    )

private fun BuildingProgressJson.coverage(): InspectionCoverage =
    when (detailLevel) {
        DetailLevelJson.DETAILED -> {
            InspectionCoverage.Floors(
                floorsTotal = requireNotNull(floorsTotal) { "brak floorsTotal w budynku szczegółowym" },
                floorsInspected = requireNotNull(floorsInspected) { "brak floorsInspected w budynku szczegółowym" },
            )
        }

        DetailLevelJson.BASIC -> {
            InspectionCoverage.Whole(isInspected = requireNotNull(inspected) { "brak inspected w budynku podstawowym" })
        }
    }

private fun InspectionMarkerJson.toMarker(): InspectionMarker =
    InspectionMarker(
        id = id,
        buildingSymbol = buildingSymbol,
        place = floorId?.let { MarkerPlace.Floor(it) } ?: MarkerPlace.WholeBuilding,
        createdAt = receivedAt(createdAt),
        isRevoked = revoked,
        isMine = mine,
    )

private fun receivedAt(text: String): Instant =
    try {
        OffsetDateTime.parse(text).toInstant()
    } catch (error: DateTimeParseException) {
        throw IllegalArgumentException("Zła chwila odbioru znacznika", error)
    }
