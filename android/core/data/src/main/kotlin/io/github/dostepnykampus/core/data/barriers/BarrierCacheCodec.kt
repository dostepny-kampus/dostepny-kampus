/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.barriers

import io.github.dostepnykampus.core.model.BarrierType
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.Severity
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.time.Instant

private val cacheJson = Json { ignoreUnknownKeys = true }
private val cachedList = ListSerializer(CachedBarrier.serializer())

/** Bariery jako dokument JSON do wiersza `barrier_cache` (NFR-10). */
internal fun List<AcceptedBarrier>.toCacheJson(): String = cacheJson.encodeToString(cachedList, map(::cached))

/** Bariery z dokumentu kopii; `null` dla zepsutego dokumentu albo nieznanej uciążliwości (NFR-10). */
internal fun String.toCachedBarriers(): List<AcceptedBarrier>? =
    runCatching { cacheJson.decodeFromString(cachedList, this).map(CachedBarrier::toBarrier) }.getOrNull()

@Serializable
private data class CachedBarrier(
    val id: String,
    val longitude: Double,
    val latitude: Double,
    val severity: Int,
    val type: String? = null,
    val buildingSymbol: String? = null,
    val verifiedAt: Long? = null,
)

private fun cached(barrier: AcceptedBarrier): CachedBarrier =
    CachedBarrier(
        id = barrier.id,
        longitude = barrier.location.longitude,
        latitude = barrier.location.latitude,
        severity = barrier.severity.value,
        type = barrier.type?.code,
        buildingSymbol = barrier.buildingSymbol,
        verifiedAt = barrier.verifiedAt?.toEpochMilli(),
    )

private fun CachedBarrier.toBarrier(): AcceptedBarrier =
    AcceptedBarrier(
        id = id,
        location = GeoPoint(longitude = longitude, latitude = latitude),
        severity = requireNotNull(Severity.fromValue(severity)),
        type = type?.let(BarrierType::fromCode),
        buildingSymbol = buildingSymbol,
        verifiedAt = verifiedAt?.let(Instant::ofEpochMilli),
    )
