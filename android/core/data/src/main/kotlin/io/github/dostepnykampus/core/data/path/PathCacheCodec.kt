/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.path

import io.github.dostepnykampus.core.data.cache.ServerCopy
import io.github.dostepnykampus.core.data.cache.decodeCache
import io.github.dostepnykampus.core.data.cache.encodeCache
import io.github.dostepnykampus.core.database.cache.ACCESSIBLE_PATHS_COPY
import io.github.dostepnykampus.core.database.cache.ServerCopyDao
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.path.AccessiblePath
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlin.coroutines.CoroutineContext

private val cachedList = ListSerializer(CachedPath.serializer())

/** Kopia ścieżek dostosowanych pod kluczem [ACCESSIBLE_PATHS_COPY] w [dao]. Wymagania: NFR-10 */
internal fun pathCopy(dao: ServerCopyDao, ioContext: CoroutineContext): ServerCopy<List<AccessiblePath>> =
    ServerCopy(
        dao = dao,
        key = ACCESSIBLE_PATHS_COPY,
        encode = { paths -> encodeCache(cachedList, paths.map(::cached)) },
        decode = { json -> runCatching { decodeCache(cachedList, json).map(CachedPath::toPath) }.getOrNull() },
        ioContext = ioContext,
    )

@Serializable
private data class CachedPath(val id: String, val line: List<CachedPoint>)

@Serializable
private data class CachedPoint(val longitude: Double, val latitude: Double)

private fun cached(path: AccessiblePath): CachedPath =
    CachedPath(id = path.id, line = path.line.map { CachedPoint(longitude = it.longitude, latitude = it.latitude) })

private fun CachedPath.toPath(): AccessiblePath =
    AccessiblePath(id = id, line = line.map { GeoPoint(longitude = it.longitude, latitude = it.latitude) })
