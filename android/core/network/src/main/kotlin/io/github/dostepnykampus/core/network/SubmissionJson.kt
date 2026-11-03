/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network

import io.github.dostepnykampus.core.model.BarrierGroup
import io.github.dostepnykampus.core.model.EntranceMarking
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.Submission
import io.github.dostepnykampus.core.model.SubmissionContent
import io.github.dostepnykampus.core.model.SubmissionFloor
import io.github.dostepnykampus.core.model.SubmissionKind
import io.github.dostepnykampus.core.model.SubmissionStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import java.time.Instant
import java.time.format.DateTimeParseException

/**
 * Treść `NewSubmission` dla `POST /submissions`: pola [content] i identyfikator [id] z telefonu.
 * Puste pola nieobowiązkowe nie trafiają do JSON, bo serwer odrzuca `null` w miejscu tekstu.
 */
internal fun Json.encodeNewSubmission(id: String, content: SubmissionContent): String =
    withField(content, name = "id", value = JsonPrimitive(id))

/** Treść `SubmissionUpdate` dla `PUT /submissions/{id}`: pola [content] i wersja bazowa [version] (BR-10). */
internal fun Json.encodeSubmissionUpdate(version: Int, content: SubmissionContent): String =
    withField(content, name = "version", value = JsonPrimitive(version))

/**
 * Zgłoszenie z odpowiedzi serwera (`Submission`). Zła treść daje [IllegalArgumentException].
 *
 * @param element obiekt JSON zgłoszenia
 */
internal fun Json.decodeSubmission(element: JsonObject): Submission {
    val meta = decodeFromJsonElement(SubmissionMetaDto.serializer(), element)
    return Submission(
        id = meta.id,
        version = meta.version,
        status = SubmissionStatus.valueOf(meta.status),
        createdAt = instant(meta.createdAt),
        content = decodeFromJsonElement(SubmissionContentDto.serializer(), element).toModel(),
        rejectionReason = meta.rejectionReason,
        verifiedAt = meta.verifiedAt?.let(::instant),
    )
}

private fun Json.withField(content: SubmissionContent, name: String, value: JsonPrimitive): String {
    val fields = encodeToJsonElement(SubmissionContentDto.serializer(), content.toDto()).jsonObject
    return JsonObject(fields + (name to value)).toString()
}

@Serializable
private data class SubmissionMetaDto(
    val id: String,
    val version: Int,
    val status: String,
    val createdAt: String,
    val verifiedAt: String? = null,
    val rejectionReason: String? = null,
)

@Serializable
private data class SubmissionContentDto(
    val kind: String,
    val location: GeoPointDto,
    val photoIds: List<String>,
    val accuracyMeters: Double? = null,
    @SerialName("manualCorrection") val isManualCorrection: Boolean = false,
    val buildingSymbol: String? = null,
    val floor: SubmissionFloorDto? = null,
    val barrierGroup: String? = null,
    val barrierType: String? = null,
    val severity: Int? = null,
    val description: String? = null,
    val replacesSubmissionId: String? = null,
    @SerialName("barrierRemoved") val isBarrierRemoved: Boolean = false,
    val entranceId: String? = null,
    val entranceMarking: String? = null,
)

@Serializable
private data class SubmissionFloorDto(
    val floorId: String? = null,
    val number: Int? = null,
    @SerialName("mezzanine") val isMezzanine: Boolean = false,
)

private fun SubmissionContent.toDto(): SubmissionContentDto =
    SubmissionContentDto(
        kind = kind.name,
        location = GeoPointDto(type = "Point", coordinates = listOf(location.longitude, location.latitude)),
        photoIds = photoIds,
        accuracyMeters = accuracyMeters,
        isManualCorrection = isManualCorrection,
        buildingSymbol = buildingSymbol,
        floor = floor?.toFloorDto(),
        barrierGroup = barrierGroup?.name,
        barrierType = barrierType,
        severity = severity,
        description = description,
        replacesSubmissionId = replacesSubmissionId,
        isBarrierRemoved = isBarrierRemoved,
        entranceId = entranceId,
        entranceMarking = entranceMarking?.name,
    )

private fun SubmissionContentDto.toModel(): SubmissionContent {
    require(location.type == "Point" && location.coordinates.size >= 2) { "Zły punkt GeoJSON zgłoszenia" }
    return SubmissionContent(
        kind = SubmissionKind.valueOf(kind),
        location = GeoPoint(longitude = location.coordinates[0], latitude = location.coordinates[1]),
        photoIds = photoIds,
        accuracyMeters = accuracyMeters,
        isManualCorrection = isManualCorrection,
        buildingSymbol = buildingSymbol,
        floor = floor?.toFloor(),
        barrierGroup = barrierGroup?.let(BarrierGroup::valueOf),
        barrierType = barrierType,
        severity = severity,
        description = description,
        replacesSubmissionId = replacesSubmissionId,
        isBarrierRemoved = isBarrierRemoved,
        entranceId = entranceId,
        entranceMarking = entranceMarking?.let(EntranceMarking::valueOf),
    )
}

private fun SubmissionFloor.toFloorDto(): SubmissionFloorDto =
    SubmissionFloorDto(floorId = floorId, number = number, isMezzanine = isMezzanine)

private fun SubmissionFloorDto.toFloor(): SubmissionFloor =
    SubmissionFloor(floorId = floorId, number = number, isMezzanine = isMezzanine)

private fun instant(text: String): Instant =
    try {
        Instant.parse(text)
    } catch (error: DateTimeParseException) {
        throw IllegalArgumentException("Zła chwila w zgłoszeniu", error)
    }
