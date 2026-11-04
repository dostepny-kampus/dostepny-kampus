/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network

import io.github.dostepnykampus.core.model.BarrierGroup
import io.github.dostepnykampus.core.model.EntranceMarking
import io.github.dostepnykampus.core.model.Submission
import io.github.dostepnykampus.core.model.SubmissionContent
import io.github.dostepnykampus.core.model.SubmissionFloor
import io.github.dostepnykampus.core.model.SubmissionKind
import io.github.dostepnykampus.core.model.SubmissionStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.format.DateTimeParseException

/**
 * Treść `NewSubmission` dla `POST /submissions`: pola [content] i identyfikator [id] z telefonu.
 * Puste pola nieobowiązkowe nie trafiają do JSON, bo serwer odrzuca `null` w miejscu tekstu.
 */
internal fun Json.encodeNewSubmission(id: String, content: SubmissionContent): String =
    withoutNulls().encodeToString(SubmissionRequest.serializer(), content.toRequest(id = id))

/** Treść `SubmissionUpdate` dla `PUT /submissions/{id}`: pola [content] i wersja bazowa [version] (BR-10). */
internal fun Json.encodeSubmissionUpdate(version: Int, content: SubmissionContent): String =
    withoutNulls().encodeToString(SubmissionRequest.serializer(), content.toRequest(version = version))

/**
 * Zgłoszenie z odpowiedzi serwera (`Submission`). Zła treść, także wartość wyliczenia spoza kontraktu,
 * daje [IllegalArgumentException].
 *
 * @param body tekst odpowiedzi
 */
internal fun Json.decodeSubmission(body: String): Submission =
    withoutNulls().decodeFromString(SubmissionDto.serializer(), body).toModel()

/**
 * Lista zgłoszeń z odpowiedzi serwera; zła treść jak w [decodeSubmission].
 *
 * @param body tekst odpowiedzi z tablicą `Submission`
 */
internal fun Json.decodeSubmissions(body: String): List<Submission> =
    withoutNulls().decodeFromString(ListSerializer(SubmissionDto.serializer()), body).map(SubmissionDto::toModel)

/**
 * Zgłoszenie z serwera z odpowiedzi 409 (`VersionConflictProblem`). Brak `serverSubmission`
 * daje [IllegalArgumentException].
 *
 * @param body tekst odpowiedzi
 */
internal fun Json.decodeConflict(body: String): Submission =
    withoutNulls().decodeFromString(VersionConflictDto.serializer(), body).serverSubmission.toModel()

@Serializable
private data class SubmissionRequest(
    val id: String?,
    val version: Int?,
    val kind: SubmissionKind,
    val location: GeoPointDto,
    val photoIds: List<String>,
    val accuracyMeters: Double?,
    @SerialName("manualCorrection") val isManualCorrection: Boolean?,
    val buildingSymbol: String?,
    val floor: SubmissionFloorDto?,
    val barrierGroup: BarrierGroup?,
    val barrierType: String?,
    val severity: Int?,
    val description: String?,
    val replacesSubmissionId: String?,
    @SerialName("barrierRemoved") val isBarrierRemoved: Boolean?,
    val entranceId: String?,
    val entranceMarking: EntranceMarking?,
)

@Serializable
private data class SubmissionDto(
    val id: String,
    val version: Int,
    val status: SubmissionStatus,
    val createdAt: String,
    val verifiedAt: String?,
    val rejectionReason: String?,
    val kind: SubmissionKind,
    val location: GeoPointDto,
    val photoIds: List<String>,
    val accuracyMeters: Double?,
    @SerialName("manualCorrection") val isManualCorrection: Boolean?,
    val buildingSymbol: String?,
    val floor: SubmissionFloorDto?,
    val barrierGroup: BarrierGroup?,
    val barrierType: String?,
    val severity: Int?,
    val description: String?,
    val replacesSubmissionId: String?,
    @SerialName("barrierRemoved") val isBarrierRemoved: Boolean?,
    val entranceId: String?,
    val entranceMarking: EntranceMarking?,
)

@Serializable
private data class VersionConflictDto(val serverSubmission: SubmissionDto)

@Serializable
private data class SubmissionFloorDto(
    val floorId: String?,
    val number: Int?,
    @SerialName("mezzanine") val isMezzanine: Boolean?,
)

private fun Json.withoutNulls(): Json = Json(from = this) { explicitNulls = false }

private fun SubmissionContent.toRequest(id: String? = null, version: Int? = null): SubmissionRequest =
    SubmissionRequest(
        id = id,
        version = version,
        kind = kind,
        location = location.toDto(),
        photoIds = photoIds,
        accuracyMeters = accuracyMeters,
        isManualCorrection = isManualCorrection,
        buildingSymbol = buildingSymbol,
        floor = floor?.let { SubmissionFloorDto(it.floorId, it.number, it.isMezzanine) },
        barrierGroup = barrierGroup,
        barrierType = barrierType,
        severity = severity,
        description = description,
        replacesSubmissionId = replacesSubmissionId,
        isBarrierRemoved = isBarrierRemoved,
        entranceId = entranceId,
        entranceMarking = entranceMarking,
    )

private fun SubmissionDto.toModel(): Submission =
    Submission(
        id = id,
        version = version,
        status = status,
        createdAt = instant(createdAt),
        content =
            SubmissionContent(
                kind = kind,
                location = location.toModel(),
                photoIds = photoIds,
                accuracyMeters = accuracyMeters,
                isManualCorrection = isManualCorrection,
                buildingSymbol = buildingSymbol,
                floor = floor?.let { SubmissionFloor(it.floorId, it.number, it.isMezzanine) },
                barrierGroup = barrierGroup,
                barrierType = barrierType,
                severity = severity,
                description = description,
                replacesSubmissionId = replacesSubmissionId,
                isBarrierRemoved = isBarrierRemoved,
                entranceId = entranceId,
                entranceMarking = entranceMarking,
            ),
        rejectionReason = rejectionReason,
        verifiedAt = verifiedAt?.let(::instant),
    )

private fun instant(text: String): Instant =
    try {
        Instant.parse(text)
    } catch (error: DateTimeParseException) {
        throw IllegalArgumentException("Zła chwila w zgłoszeniu", error)
    }
