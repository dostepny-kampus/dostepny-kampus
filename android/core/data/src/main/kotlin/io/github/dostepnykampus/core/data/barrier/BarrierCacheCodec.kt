/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.barrier

import io.github.dostepnykampus.core.data.cache.ServerCopy
import io.github.dostepnykampus.core.data.cache.decodeCache
import io.github.dostepnykampus.core.data.cache.encodeCache
import io.github.dostepnykampus.core.database.cache.BARRIERS_COPY
import io.github.dostepnykampus.core.database.cache.ServerCopyDao
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.submission.BarrierType
import io.github.dostepnykampus.core.model.submission.Severity
import io.github.dostepnykampus.core.model.submission.SubmissionFloor
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import java.time.Instant
import kotlin.coroutines.CoroutineContext

private val cachedList = ListSerializer(CachedBarrier.serializer())

/** Kopia barier pod kluczem [BARRIERS_COPY] w [dao]. Wymagania: NFR-10 */
internal fun barrierCopy(dao: ServerCopyDao, ioContext: CoroutineContext): ServerCopy<List<AcceptedBarrier>> =
    ServerCopy(
        dao = dao,
        key = BARRIERS_COPY,
        encode = List<AcceptedBarrier>::toCacheJson,
        decode = String::toCachedBarriers,
        ioContext = ioContext,
    )

/** Bariery jako dokument JSON kopii. Wymagania: NFR-10 */
internal fun List<AcceptedBarrier>.toCacheJson(): String = encodeCache(cachedList, map(::cached))

/** Bariery z dokumentu kopii; `null` dla zepsutego dokumentu albo nieznanej uciążliwości. Wymagania: NFR-10 */
internal fun String.toCachedBarriers(): List<AcceptedBarrier>? =
    runCatching { decodeCache(cachedList, this).map(CachedBarrier::toBarrier) }.getOrNull()

@Serializable
private data class CachedBarrier(
    val id: String,
    val longitude: Double,
    val latitude: Double,
    val severity: Int,
    val type: String? = null,
    val buildingSymbol: String? = null,
    val verifiedAt: Long? = null,
    val entranceId: String? = null,
    val floor: CachedFloor? = null,
    val photoIds: List<String> = emptyList(),
    val description: String? = null,
    val isQuick: Boolean = false,
    val replacesSubmissionId: String? = null,
)

@Serializable
private data class CachedFloor(val floorId: String? = null, val number: Int? = null, val isMezzanine: Boolean? = null)

private fun cached(barrier: AcceptedBarrier): CachedBarrier =
    CachedBarrier(
        id = barrier.id,
        longitude = barrier.location.longitude,
        latitude = barrier.location.latitude,
        severity = barrier.severity.value,
        type = barrier.type?.code,
        buildingSymbol = barrier.buildingSymbol,
        verifiedAt = barrier.verifiedAt?.toEpochMilli(),
        entranceId = barrier.entranceId,
        floor = barrier.floor?.run { CachedFloor(floorId = floorId, number = number, isMezzanine = isMezzanine) },
        photoIds = barrier.photoIds,
        description = barrier.description,
        isQuick = barrier.isQuick,
        replacesSubmissionId = barrier.replacesSubmissionId,
    )

private fun CachedBarrier.toBarrier(): AcceptedBarrier =
    AcceptedBarrier(
        id = id,
        location = GeoPoint(longitude = longitude, latitude = latitude),
        severity = requireNotNull(Severity.fromValue(severity)),
        type = type?.let(BarrierType::fromCode),
        buildingSymbol = buildingSymbol,
        verifiedAt = verifiedAt?.let(Instant::ofEpochMilli),
        entranceId = entranceId,
        floor = floor?.run { SubmissionFloor(floorId = floorId, number = number, isMezzanine = isMezzanine) },
        photoIds = photoIds,
        description = description,
        isQuick = isQuick,
        replacesSubmissionId = replacesSubmissionId,
    )
