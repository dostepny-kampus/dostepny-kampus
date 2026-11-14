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
import java.time.Instant
import kotlin.coroutines.CoroutineContext

/**
 * Bariery z kopii razem z chwilą ich pobrania z serwera.
 *
 * Wymagania: NFR-10
 *
 * @property barriers bariery z kopii
 * @property syncedAt chwila pobrania z serwera
 */
data class SyncedBarriers(val barriers: List<AcceptedBarrier>, val syncedAt: Instant)

/**
 * Kopia ostatnich barier z serwera w jednym wierszu `barrier_cache`.
 *
 * Wymagania: NFR-10
 *
 * @param dao wiersz kopii w bazie
 * @param ioContext kontekst zapisu i odczytu bazy
 * @param now chwila pobrania w milisekundach epoki
 */
internal class BarrierListCopy(
    private val dao: BarrierCacheDao,
    private val ioContext: CoroutineContext = Dispatchers.IO,
    private val now: () -> Long = System::currentTimeMillis,
) {
    /** Zapisuje [barriers] z chwilą pobrania, zastępując poprzednią kopię, i zwraca tę chwilę. */
    suspend fun save(barriers: List<AcceptedBarrier>): Instant {
        val row =
            BarrierCacheEntity(id = BARRIER_CACHE_SINGLE_ROW, listJson = barriers.toCacheJson(), syncedAt = now())
        withContext(ioContext) { dao.save(row) }
        return Instant.ofEpochMilli(row.syncedAt)
    }

    /** Bariery z kopii; `null`, gdy kopii nie ma albo jest zepsuta. */
    suspend fun restore(): List<AcceptedBarrier>? = restoreSynced()?.barriers

    /** Bariery z kopii z chwilą pobrania; `null`, gdy kopii nie ma albo jest zepsuta. */
    suspend fun restoreSynced(): SyncedBarriers? {
        val row = withContext(ioContext) { dao.get() }
        val barriers = row?.run { listJson.toCachedBarriers() }
        return if (row == null || barriers == null) {
            null
        } else {
            SyncedBarriers(barriers, Instant.ofEpochMilli(row.syncedAt))
        }
    }
}
