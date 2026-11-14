/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.inspection

import io.github.dostepnykampus.core.database.inspection.PendingMarkerChangeDao
import io.github.dostepnykampus.core.database.inspection.PendingMarkerChangeEntity
import io.github.dostepnykampus.core.database.inspection.PendingMarkerChangeType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Znacznik piętra obejrzanego i jego cofnięcie zapisują się w kolejce telefonu. Zapis nie sięga do serwera, więc
 * postęp liczony na serwerze zostaje bez zmian do synchronizacji.
 *
 * Wymagania: US15.3, BR-9
 */
class FloorMarkersTest {
    private val queue = FakeMarkerQueue()
    private val ids = generateSequence(1) { it + 1 }.map { "id-$it" }.iterator()
    private val markers = floorMarkersOf(queue = queue, newId = { ids.next() }, nowMillis = { queue.rows.size + 1L })

    /**
     * Przykład „oznaczy kolejne piętro jako obejrzane”.
     *
     * Scenariusz: postep-inwentaryzacji.feature: Znacznik i jego cofnięcie bez sieci czekają w telefonie
     */
    @Test
    fun `us153 nowy znacznik czeka w telefonie`() =
        runBlocking {
            val markerId = markers.mark(buildingSymbol = "A-1", floorId = "p4")
            val waiting =
                PendingMarkerChange(markerId = markerId, buildingSymbol = "A-1", floorId = "p4", isRevoke = false)
            assertEquals(listOf(waiting), markers.pending().first())
        }

    /**
     * Przykład „cofnie swój znacznik jednego z obejrzanych pięter”: znacznik jest już na serwerze.
     *
     * Scenariusz: postep-inwentaryzacji.feature: Znacznik i jego cofnięcie bez sieci czekają w telefonie
     */
    @Test
    fun `us153 cofniecie znacznika czeka w telefonie`() =
        runBlocking {
            markers.revoke(markerId = "na-serwerze", buildingSymbol = "A-1", floorId = "p2")
            val waiting =
                PendingMarkerChange(markerId = "na-serwerze", buildingSymbol = "A-1", floorId = "p2", isRevoke = true)
            assertEquals(listOf(waiting), markers.pending().first())
        }

    @Test
    fun `us153 cofniecie czekajacego znacznika oproznia kolejke`() =
        runBlocking {
            val markerId = markers.mark(buildingSymbol = "B-8", floorId = null)
            markers.revoke(markerId = markerId, buildingSymbol = "B-8", floorId = null)
            assertEquals(emptyList<PendingMarkerChange>(), markers.pending().first())
        }

    @Test
    fun `br9 kazda zmiana ma wlasna operacje`() =
        runBlocking {
            markers.mark(buildingSymbol = "A-1", floorId = "p4")
            markers.revoke(markerId = "na-serwerze", buildingSymbol = "A-1", floorId = "p2")
            assertEquals(2, queue.rows.keys.size)
        }
}

/** Kolejka zmian znaczników w pamięci, z regułą cofnięcia z prawdziwego DAO. */
private class FakeMarkerQueue : PendingMarkerChangeDao {
    val rows = linkedMapOf<String, PendingMarkerChangeEntity>()

    override fun observe(): Flow<List<PendingMarkerChangeEntity>> =
        flow { emit(rows.values.sortedBy { it.savedAtMillis }) }

    override suspend fun save(change: PendingMarkerChangeEntity) {
        rows[change.operationId] = change
    }

    override suspend fun deleteSet(markerId: String): Int {
        val waiting = rows.values.filter { it.markerId == markerId && it.type == PendingMarkerChangeType.SET }
        waiting.forEach { rows.remove(it.operationId) }
        return waiting.size
    }

    override suspend fun delete(operationId: String) {
        rows.remove(operationId)
    }
}
