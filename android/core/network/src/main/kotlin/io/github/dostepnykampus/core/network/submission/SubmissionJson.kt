/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network.submission

import io.github.dostepnykampus.core.model.Submission
import io.github.dostepnykampus.core.model.SubmissionContent
import io.github.dostepnykampus.core.model.SubmissionFloor
import io.github.dostepnykampus.core.network.building.toJson
import io.github.dostepnykampus.core.network.building.toModel
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import java.time.Instant
import java.time.format.DateTimeParseException
import io.github.dostepnykampus.core.network.json.Submission as SubmissionDto
import io.github.dostepnykampus.core.network.json.SubmissionContent as SubmissionContentJson
import io.github.dostepnykampus.core.network.json.SubmissionFloor as SubmissionFloorJson
import io.github.dostepnykampus.core.network.json.VersionConflictProblem as VersionConflictDto

/**
 * Treść `NewSubmission` dla `POST /submissions`: pola [content] i identyfikator [id] z telefonu.
 * Puste pola nieobowiązkowe i flagi równe `false` (wartość domyślna z kontraktu) nie trafiają do JSON.
 */
internal fun Json.encodeNewSubmission(id: String, content: SubmissionContent): String =
    encodeWith(content, "id" to JsonPrimitive(id))

/** Treść `SubmissionUpdate` dla `PUT /submissions/{id}`: pola [content] i wersja bazowa [version]. Wymagania: BR-10 */
internal fun Json.encodeSubmissionUpdate(version: Int, content: SubmissionContent): String =
    encodeWith(content, "version" to JsonPrimitive(version))

/**
 * Zgłoszenie z odpowiedzi serwera (`Submission`). Flaga nieobecna i `false` (wartość domyślna z kontraktu) dają
 * w modelu `null`. Zła treść, także wartość wyliczenia spoza kontraktu, daje [IllegalArgumentException].
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

private fun Json.withoutNulls(): Json = Json(from = this) { explicitNulls = false }

/** `SubmissionContent` z jednym polem nadanym poza treścią, jak `allOf` schematów żądań w kontrakcie. */
private fun Json.encodeWith(content: SubmissionContent, extra: Pair<String, JsonPrimitive>): String =
    JsonObject(
        withoutNulls().encodeToJsonElement(SubmissionContentJson.serializer(), content.toJson()).jsonObject + extra,
    ).toString()

private fun SubmissionContent.toJson(): SubmissionContentJson =
    SubmissionContentJson(
        kind = kind.into(),
        location = location.toJson(),
        photoIds = photoIds,
        accuracyMeters = accuracyMeters,
        manualCorrection = isManualCorrection,
        buildingSymbol = buildingSymbol,
        floor = floor?.toJson(),
        barrierGroup = barrierGroup?.into(),
        barrierType = barrierType,
        severity = severity,
        description = description,
        replacesSubmissionId = replacesSubmissionId,
        barrierRemoved = isBarrierRemoved,
        entranceId = entranceId,
        entranceMarking = entranceMarking?.into(),
    )

/** Flaga z kontraktu w modelu: `true` albo `null`, bo `false` to wartość domyślna pola nieobecnego. */
private fun Boolean?.flag(): Boolean? = takeIf { it == true }

private inline fun <reified T : Enum<T>> Enum<*>.into(): T = enumValueOf<T>(name)

private fun SubmissionFloor.toJson(): SubmissionFloorJson =
    SubmissionFloorJson(floorId = floorId, number = number, mezzanine = isMezzanine)

private fun SubmissionDto.toModel(): Submission =
    Submission(
        id = id,
        version = version,
        status = status.into(),
        createdAt = instant(createdAt),
        content =
            SubmissionContent(
                kind = kind.into(),
                location = location.toModel(),
                photoIds = photoIds,
                accuracyMeters = accuracyMeters,
                isManualCorrection = manualCorrection.flag(),
                buildingSymbol = buildingSymbol,
                floor = floor?.let { SubmissionFloor(it.floorId, it.number, it.mezzanine.flag()) },
                barrierGroup = barrierGroup?.into(),
                barrierType = barrierType,
                severity = severity,
                description = description,
                replacesSubmissionId = replacesSubmissionId,
                isBarrierRemoved = barrierRemoved.flag(),
                entranceId = entranceId,
                entranceMarking = entranceMarking?.into(),
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
