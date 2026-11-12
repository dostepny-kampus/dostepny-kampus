/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.barriers

import io.github.dostepnykampus.core.database.BARRIER_CACHE_SINGLE_ROW
import io.github.dostepnykampus.core.database.BarrierCacheDao
import io.github.dostepnykampus.core.database.BarrierCacheEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * Kopia ostatnich barier z serwera w jednym wierszu `barrier_cache` (NFR-10).
 *
 * @param dao wiersz kopii w bazie
 * @param ioContext kontekst zapisu i odczytu bazy
 * @param now chwila pobrania w milisekundach epoki
 */
class BarrierListCopy(
    private val dao: BarrierCacheDao,
    private val ioContext: CoroutineContext = Dispatchers.IO,
    private val now: () -> Long = System::currentTimeMillis,
) {
    /** Zapisuje [barriers] z chwilą pobrania, zastępując poprzednią kopię. */
    suspend fun save(barriers: List<AcceptedBarrier>) {
        val row =
            BarrierCacheEntity(id = BARRIER_CACHE_SINGLE_ROW, listJson = barriers.toCacheJson(), syncedAt = now())
        withContext(ioContext) { dao.save(row) }
    }

    /** Bariery z kopii; `null`, gdy kopii nie ma albo jest zepsuta. */
    suspend fun restore(): List<AcceptedBarrier>? {
        val row = withContext(ioContext) { dao.get() }
        return row?.run { listJson.toCachedBarriers() }
    }
}
