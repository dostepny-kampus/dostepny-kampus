/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.database

import androidx.room.Room
import io.github.dostepnykampus.core.database.inspection.PendingMarkerChangeDao
import io.github.dostepnykampus.core.database.inspection.PendingMarkerChangeEntity
import io.github.dostepnykampus.core.database.inspection.PendingMarkerChangeType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Kolejka zmian znaczników piętra obejrzanego w bazie telefonu. Wymagania: US15.3, BR-9, NFR-5 */
@RunWith(RobolectricTestRunner::class)
class PendingMarkerChangeDaoTest {
    /** Zmiany wracają w kolejności zapisu, a ponowny zapis operacji nie dodaje drugiego wiersza. */
    @Test
    fun keepsChangesInOrder() =
        stored { dao ->
            dao.save(change(operation = "o2", marker = "m2", savedAt = 9))
            dao.save(change(operation = "o1", marker = "m1", savedAt = 5))
            dao.save(change(operation = "o1", marker = "m1", savedAt = 5))
            assertEquals(listOf("o1", "o2"), dao.observe().first().map { it.operationId })
        }

    /** Cofnięcie znacznika, który serwer już ma, czeka w kolejce obok innych zmian. */
    @Test
    fun queuesRevokeOfSent() =
        stored { dao ->
            dao.save(change(operation = "o1", marker = "m1", savedAt = 5))
            dao.saveRevoke(revoke(operation = "o2", marker = "m0", savedAt = 7))
            assertEquals(
                listOf("m1" to PendingMarkerChangeType.SET, "m0" to PendingMarkerChangeType.REVOKE),
                dao.observe().first().map { it.markerId to it.type },
            )
        }

    /** Cofnięcie znacznika, którego postawienie jeszcze czeka, usuwa to postawienie i nic nie dodaje. */
    @Test
    fun dropsRevokeOfWaiting() =
        stored { dao ->
            dao.save(change(operation = "o1", marker = "m1", savedAt = 5))
            dao.saveRevoke(revoke(operation = "o2", marker = "m1", savedAt = 7))
            assertEquals(emptyList<PendingMarkerChangeEntity>(), dao.observe().first())
        }

    /** Zmiana odebrana przez serwer znika z kolejki, pozostałe zostają. */
    @Test
    fun removesSentChange() =
        stored { dao ->
            dao.save(change(operation = "o1", marker = "m1", savedAt = 5))
            dao.save(change(operation = "o2", marker = "m2", savedAt = 9))
            dao.delete("o1")
            assertEquals(listOf("o2"), dao.observe().first().map { it.operationId })
        }

    private fun change(operation: String, marker: String, savedAt: Long): PendingMarkerChangeEntity =
        PendingMarkerChangeEntity(
            operationId = operation,
            type = PendingMarkerChangeType.SET,
            markerId = marker,
            buildingSymbol = "A-1",
            floorId = "p1",
            savedAtMillis = savedAt,
        )

    private fun revoke(operation: String, marker: String, savedAt: Long): PendingMarkerChangeEntity =
        change(operation = operation, marker = marker, savedAt = savedAt).copy(type = PendingMarkerChangeType.REVOKE)

    private fun stored(check: suspend (PendingMarkerChangeDao) -> Unit) {
        val database =
            Room
                .inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), DkDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        try {
            runTest { check(database.pendingMarkerChanges()) }
        } finally {
            database.close()
        }
    }
}
