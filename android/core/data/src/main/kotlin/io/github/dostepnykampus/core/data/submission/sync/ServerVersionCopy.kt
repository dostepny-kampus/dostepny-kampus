/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submission.sync

import io.github.dostepnykampus.core.data.cache.ServerCopy
import io.github.dostepnykampus.core.data.cache.decodeCache
import io.github.dostepnykampus.core.data.cache.encodeCache
import io.github.dostepnykampus.core.data.server.ServerEdit
import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.database.cache.ServerCopyDao
import io.github.dostepnykampus.core.database.cache.ServerCopyEntity
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.core.model.submission.BarrierGroup
import io.github.dostepnykampus.core.model.submission.Submission
import io.github.dostepnykampus.core.model.submission.SubmissionContent
import io.github.dostepnykampus.core.model.submission.SubmissionFloor
import io.github.dostepnykampus.core.model.submission.SubmissionKind
import io.github.dostepnykampus.core.model.submission.SubmissionStatus
import kotlinx.serialization.Serializable
import java.time.Instant
import kotlin.coroutines.CoroutineContext

/** Zgłoszenie w wersji z serwera z odpowiedzi 409 na edycję; `null` przy innym wyniku. Wymagania: US9.3, BR-10 */
internal fun serverVersionOf(result: ServerReply<*>): Submission? =
    ((result as? ServerReply.Ok)?.value as? ServerEdit.Conflict)?.server

/**
 * Wiersz kopii wersji [server] z chwilą odbioru [syncedAt], do zapisu w tej samej transakcji co odmowa.
 *
 * Wymagania: US9.3, BR-10
 */
internal fun serverVersionRow(server: Submission, syncedAt: Instant): ServerCopyEntity =
    ServerCopyEntity(
        key = serverVersionKey(server.id),
        documentJson = encodeCache(CachedVersion.serializer(), cached(server)),
        syncedAt = syncedAt.toEpochMilli(),
    )

/**
 * Kopia wersji z serwera zgłoszenia [id] w [dao]; zepsuty dokument czyta się jak brak kopii.
 *
 * Wymagania: US9.3, BR-10
 */
internal fun serverVersionCopy(dao: ServerCopyDao, id: String, ioContext: CoroutineContext): ServerCopy<Submission> =
    ServerCopy(
        dao = dao,
        key = serverVersionKey(id),
        encode = { encodeCache(CachedVersion.serializer(), cached(it)) },
        decode = { json -> runCatching { decodeCache(CachedVersion.serializer(), json).toSubmission() }.getOrNull() },
        ioContext = ioContext,
    )

/**
 * Treść własnego zgłoszenia z ostatniego zapisu i ścieżki jego zdjęć, trzymane w historii do poprawki bez sieci.
 *
 * Wymagania: US8.3, NFR-5
 *
 * @property content pełna treść
 * @property photoPaths ścieżki plików zdjęć w kolejności [SubmissionContent.photoIds]
 */
internal data class OwnCopy(val content: SubmissionContent, val photoPaths: List<String>)

/** Dokument [OwnCopy] zgłoszenia [id] do kolumny historii; w tym samym formacie co kopia wersji z serwera. */
internal fun ownCopyJson(id: String, copy: OwnCopy): String {
    val draft =
        Submission(
            id = id,
            version = 1,
            status = SubmissionStatus.SENT,
            createdAt = Instant.EPOCH,
            content = copy.content,
        )
    return encodeCache(StoredOwn.serializer(), StoredOwn(submission = cached(draft), photoPaths = copy.photoPaths))
}

/** [OwnCopy] z kolumny historii; brak albo zepsuty dokument to `null`. */
internal fun ownCopyOf(json: String?): OwnCopy? {
    val stored = json?.let { runCatching { decodeCache(StoredOwn.serializer(), it) }.getOrNull() }
    return stored?.let { OwnCopy(content = it.submission.toContent(), photoPaths = it.photoPaths) }
}

/** Ścieżki zdjęć z dokumentów historii [contents]; zepsute dokumenty pomija. */
internal fun ownPhotoPaths(contents: List<String>): List<String> =
    contents.flatMap { ownCopyOf(it)?.photoPaths.orEmpty() }

/** Klucz kopii wersji z serwera zgłoszenia [id] w tabeli kopii. */
internal fun serverVersionKey(id: String): String = "submission_server_version:$id"

/** Wersja i status w [submission] nie są czytane: liczy się treść. */
@Serializable
private data class StoredOwn(val submission: CachedVersion, val photoPaths: List<String>)

@Serializable
private data class CachedVersion(
    val id: String,
    val version: Int,
    val status: SubmissionStatus,
    val createdAtMillis: Long,
    val rejectionReason: String? = null,
    val verifiedAtMillis: Long? = null,
    val kind: SubmissionKind,
    val longitude: Double,
    val latitude: Double,
    val photoIds: List<String>,
    val accuracyMeters: Double? = null,
    val isManualCorrection: Boolean? = null,
    val buildingSymbol: String? = null,
    val floorId: String? = null,
    val floorNumber: Int? = null,
    val isMezzanine: Boolean? = null,
    val barrierGroup: BarrierGroup? = null,
    val barrierType: String? = null,
    val severity: Int? = null,
    val description: String? = null,
    val replacesSubmissionId: String? = null,
    val isBarrierRemoved: Boolean? = null,
    val entranceId: String? = null,
    val entranceMarking: EntranceMarking? = null,
)

private fun cached(server: Submission): CachedVersion {
    val content = server.content
    return CachedVersion(
        id = server.id,
        version = server.version,
        status = server.status,
        createdAtMillis = server.createdAt.toEpochMilli(),
        rejectionReason = server.rejectionReason,
        verifiedAtMillis = server.verifiedAt?.toEpochMilli(),
        kind = content.kind,
        longitude = content.location.longitude,
        latitude = content.location.latitude,
        photoIds = content.photoIds,
        accuracyMeters = content.accuracyMeters,
        isManualCorrection = content.isManualCorrection,
        buildingSymbol = content.buildingSymbol,
        floorId = content.floor?.floorId,
        floorNumber = content.floor?.number,
        isMezzanine = content.floor?.isMezzanine,
        barrierGroup = content.barrierGroup,
        barrierType = content.barrierType,
        severity = content.severity,
        description = content.description,
        replacesSubmissionId = content.replacesSubmissionId,
        isBarrierRemoved = content.isBarrierRemoved,
        entranceId = content.entranceId,
        entranceMarking = content.entranceMarking,
    )
}

private fun CachedVersion.toSubmission(): Submission =
    Submission(
        id = id,
        version = version,
        status = status,
        createdAt = Instant.ofEpochMilli(createdAtMillis),
        content = toContent(),
        rejectionReason = rejectionReason,
        verifiedAt = verifiedAtMillis?.let(Instant::ofEpochMilli),
    )

private fun CachedVersion.toContent(): SubmissionContent =
    SubmissionContent(
        kind = kind,
        location = GeoPoint(longitude = longitude, latitude = latitude),
        photoIds = photoIds,
        accuracyMeters = accuracyMeters,
        isManualCorrection = isManualCorrection,
        buildingSymbol = buildingSymbol,
        floor = floorOf(),
        barrierGroup = barrierGroup,
        barrierType = barrierType,
        severity = severity,
        description = description,
        replacesSubmissionId = replacesSubmissionId,
        isBarrierRemoved = isBarrierRemoved,
        entranceId = entranceId,
        entranceMarking = entranceMarking,
    )

private fun CachedVersion.floorOf(): SubmissionFloor? =
    if (floorId == null && floorNumber == null && isMezzanine == null) {
        null
    } else {
        SubmissionFloor(floorId = floorId, number = floorNumber, isMezzanine = isMezzanine)
    }
