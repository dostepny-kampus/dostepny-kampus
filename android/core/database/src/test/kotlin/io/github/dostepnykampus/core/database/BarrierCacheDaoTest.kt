/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.database

import androidx.room.Room
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Kopia barier ze zgłoszeń przyjętych w bazie Room: jeden wiersz, nowy zapis zastępuje poprzedni (NFR-10). */
@RunWith(RobolectricTestRunner::class)
class BarrierCacheDaoTest {
    private lateinit var database: DkDatabase
    private lateinit var dao: BarrierCacheDao

    @Before
    fun open() {
        database =
            Room
                .inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), DkDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        dao = database.barrierCache()
    }

    @After
    fun close() {
        database.close()
    }

    @Test
    fun nfr10_empty_before_first_save() =
        runTest {
            assertNull(dao.get())
        }

    @Test
    fun nfr10_saved_list_comes_back() =
        runTest {
            val entity = BarrierCacheEntity(id = BARRIER_CACHE_SINGLE_ROW, listJson = LIST_JSON, syncedAt = 42)
            dao.save(entity)

            assertEquals(entity, dao.get())
        }

    @Test
    fun nfr10_new_save_replaces_old() =
        runTest {
            dao.save(BarrierCacheEntity(id = BARRIER_CACHE_SINGLE_ROW, listJson = LIST_JSON, syncedAt = 1))
            val newer = BarrierCacheEntity(id = BARRIER_CACHE_SINGLE_ROW, listJson = "{}", syncedAt = 9)
            dao.save(newer)

            assertEquals(newer, dao.get())
        }
}

private const val LIST_JSON = """{"barriers":[{"id":"z1"},{"id":"z2"}]}"""
