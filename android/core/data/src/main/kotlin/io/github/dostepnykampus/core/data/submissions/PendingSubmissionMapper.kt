/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submissions

import io.github.dostepnykampus.core.database.PendingSubmissionEntity
import io.github.dostepnykampus.core.database.PendingSubmissionWithPhotos
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.SubmissionContent
import io.github.dostepnykampus.core.model.SubmissionFloor
import kotlin.reflect.KClass

/**
 * Treść zgłoszenia z wiersza kolejki razem ze zdjęciami.
 *
 * Encja przechowuje enumy jako tekst, więc odczyt sprawdza wartości: baza nie jest kontraktem,
 * a zła wartość (np. po awarii) ma wywołać błąd głośno, nie po cichu.
 */
internal fun PendingSubmissionWithPhotos.toSubmissionContent(): SubmissionContent {
    val row = submission
    return SubmissionContent(
        kind = row.kind.toEnum(),
        location = GeoPoint(longitude = row.longitude, latitude = row.latitude),
        photoIds = photos.map { it.photoId },
        accuracyMeters = row.accuracyMeters,
        isManualCorrection = row.isManualCorrection,
        buildingSymbol = row.buildingSymbol,
        floor = row.floorOf(),
        barrierGroup = row.barrierGroup?.toEnum(),
        barrierType = row.barrierType,
        severity = row.severity,
        description = row.description,
        replacesSubmissionId = row.replacesSubmissionId,
        isBarrierRemoved = row.isBarrierRemoved,
        entranceId = row.entranceId,
        entranceMarking = row.entranceMarking?.toEnum(),
    )
}

/** Wiersz kolejki z treści zgłoszenia; identyfikator i chwila zapisu powstają w telefonie. */
internal fun SubmissionContent.toPendingSubmissionEntity(id: String, savedAtMillis: Long): PendingSubmissionEntity =
    PendingSubmissionEntity(
        id = id,
        baseVersion = null,
        savedAtMillis = savedAtMillis,
        isCreatedOnServer = false,
        kind = kind.name,
        longitude = location.longitude,
        latitude = location.latitude,
        accuracyMeters = accuracyMeters,
        isManualCorrection = isManualCorrection,
        buildingSymbol = buildingSymbol,
        floorId = floor?.floorId,
        floorNumber = floor?.number,
        isMezzanine = floor?.isMezzanine,
        barrierGroup = barrierGroup?.name,
        barrierType = barrierType,
        severity = severity,
        description = description,
        replacesSubmissionId = replacesSubmissionId,
        isBarrierRemoved = isBarrierRemoved,
        entranceId = entranceId,
        entranceMarking = entranceMarking?.name,
    )

private fun PendingSubmissionEntity.floorOf(): SubmissionFloor? =
    if (floorId == null && floorNumber == null && isMezzanine == null) {
        null
    } else {
        SubmissionFloor(floorId, floorNumber, isMezzanine)
    }

private inline fun <reified E : Enum<E>> String.toEnum(): E =
    enumValues<E>().singleOrNull { it.name == this } ?: unknownEnum(this, E::class)

private fun unknownEnum(value: String, type: KClass<*>): Nothing =
    throw IllegalArgumentException("W bazie kolejki jest nieznana wartość ${type.java.name}: $value")
