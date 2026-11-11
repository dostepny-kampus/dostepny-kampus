/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.buildings

import io.github.dostepnykampus.core.database.BUILDING_CACHE_SINGLE_ROW
import io.github.dostepnykampus.core.database.BuildingCacheDao
import io.github.dostepnykampus.core.database.BuildingCacheEntity
import io.github.dostepnykampus.core.network.BuildingList
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * Lista budynków z serwera razem z chwilą jej pobrania (NFR-10).
 *
 * @property list lista budynków
 * @property syncedAtMillis chwila pobrania z serwera w milisekundach epoki
 */
internal data class SyncedBuildingList(val list: BuildingList, val syncedAtMillis: Long)

/**
 * Kopia ostatniej listy budynków z serwera w jednym wierszu `building_cache` (NFR-10).
 *
 * @param dao wiersz kopii w bazie
 * @param ioContext kontekst zapisu i odczytu bazy
 */
internal class BuildingListCopy(private val dao: BuildingCacheDao, private val ioContext: CoroutineContext) {
    /** Zapisuje listę z chwilą pobrania, zastępując poprzednią kopię. */
    suspend fun save(synced: SyncedBuildingList) {
        val row =
            BuildingCacheEntity(
                id = BUILDING_CACHE_SINGLE_ROW,
                listJson = synced.list.toCacheJson(),
                syncedAt = synced.syncedAtMillis,
            )
        withContext(ioContext) { dao.save(row) }
    }

    /** Lista z kopii z chwilą pobrania; `null`, gdy kopii nie ma albo jest zepsuta. */
    suspend fun restore(): SyncedBuildingList? {
        val row = withContext(ioContext) { dao.get() }
        val list = row?.run { listJson.toCachedBuildingList() }
        return if (row == null || list == null) null else SyncedBuildingList(list = list, syncedAtMillis = row.syncedAt)
    }
}
