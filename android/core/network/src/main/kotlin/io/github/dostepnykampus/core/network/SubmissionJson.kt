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
    encodeToString(SubmissionRequest.serializer(), content.toRequest(id = id))

/** Treść `SubmissionUpdate` dla `PUT /submissions/{id}`: pola [content] i wersja bazowa [version] (BR-10). */
internal fun Json.encodeSubmissionUpdate(version: Int, content: SubmissionContent): String =
    encodeToString(SubmissionRequest.serializer(), content.toRequest(version = version))

/**
 * Zgłoszenie z odpowiedzi serwera (`Submission`). Zła treść, także wartość wyliczenia spoza kontraktu,
 * daje [IllegalArgumentException].
 *
 * @param body tekst odpowiedzi
 */
internal fun Json.decodeSubmission(body: String): Submission =
    decodeFromString(SubmissionDto.serializer(), body).toModel()

/**
 * Lista zgłoszeń z odpowiedzi serwera; zła treść jak w [decodeSubmission].
 *
 * @param body tekst odpowiedzi z tablicą `Submission`
 */
internal fun Json.decodeSubmissions(body: String): List<Submission> =
    decodeFromString(ListSerializer(SubmissionDto.serializer()), body).map(SubmissionDto::toModel)

/**
 * Zgłoszenie z serwera z odpowiedzi 409 (`VersionConflictProblem`). Brak `serverSubmission`
 * daje [IllegalArgumentException].
 *
 * @param body tekst odpowiedzi
 */
internal fun Json.decodeConflict(body: String): Submission =
    decodeFromString(VersionConflictDto.serializer(), body).serverSubmission.toModel()

@Serializable
private data class SubmissionRequest(
    val id: String? = null,
    val version: Int? = null,
    val kind: SubmissionKind,
    val location: GeoPointDto,
    val photoIds: List<String>,
    val accuracyMeters: Double? = null,
    @SerialName("manualCorrection") val isManualCorrection: Boolean = false,
    val buildingSymbol: String? = null,
    val floor: SubmissionFloorDto? = null,
    val barrierGroup: BarrierGroup? = null,
    val barrierType: String? = null,
    val severity: Int? = null,
    val description: String? = null,
    val replacesSubmissionId: String? = null,
    @SerialName("barrierRemoved") val isBarrierRemoved: Boolean = false,
    val entranceId: String? = null,
    val entranceMarking: EntranceMarking? = null,
)

@Serializable
private data class SubmissionDto(
    val id: String,
    val version: Int,
    val status: SubmissionStatus,
    val createdAt: String,
    val verifiedAt: String? = null,
    val rejectionReason: String? = null,
    val kind: SubmissionKind,
    val location: GeoPointDto,
    val photoIds: List<String>,
    val accuracyMeters: Double? = null,
    @SerialName("manualCorrection") val isManualCorrection: Boolean = false,
    val buildingSymbol: String? = null,
    val floor: SubmissionFloorDto? = null,
    val barrierGroup: BarrierGroup? = null,
    val barrierType: String? = null,
    val severity: Int? = null,
    val description: String? = null,
    val replacesSubmissionId: String? = null,
    @SerialName("barrierRemoved") val isBarrierRemoved: Boolean = false,
    val entranceId: String? = null,
    val entranceMarking: EntranceMarking? = null,
)

@Serializable
private data class VersionConflictDto(val serverSubmission: SubmissionDto)

@Serializable
private data class SubmissionFloorDto(
    val floorId: String? = null,
    val number: Int? = null,
    @SerialName("mezzanine") val isMezzanine: Boolean = false,
)

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
        floor = floor?.toDto(),
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
                floor = floor?.toModel(),
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

private fun SubmissionFloor.toDto(): SubmissionFloorDto =
    SubmissionFloorDto(floorId = floorId, number = number, isMezzanine = isMezzanine)

private fun SubmissionFloorDto.toModel(): SubmissionFloor =
    SubmissionFloor(floorId = floorId, number = number, isMezzanine = isMezzanine)

private fun instant(text: String): Instant =
    try {
        Instant.parse(text)
    } catch (error: DateTimeParseException) {
        throw IllegalArgumentException("Zła chwila w zgłoszeniu", error)
    }
