/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.database

import androidx.room.Room
import io.github.dostepnykampus.core.database.cache.BUILDING_CACHE_SINGLE_ROW
import io.github.dostepnykampus.core.database.cache.BuildingCacheDao
import io.github.dostepnykampus.core.database.cache.BuildingCacheEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Kopia listy budynków w bazie Room: jeden wiersz, nowy zapis zastępuje poprzedni. Wymagania: NFR-10 */
@RunWith(RobolectricTestRunner::class)
class BuildingCacheDaoTest {
    private lateinit var database: DkDatabase
    private lateinit var dao: BuildingCacheDao

    @Before
    fun open() {
        database =
            Room
                .inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), DkDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        dao = database.buildingCache()
    }

    @After
    fun close() {
        database.close()
    }

    @Test
    fun staysEmptyBeforeFirstSave() =
        runTest {
            assertNull(dao.get())
        }

    @Test
    fun returnsSavedList() =
        runTest {
            val entity = BuildingCacheEntity(id = BUILDING_CACHE_SINGLE_ROW, listJson = LIST_JSON, syncedAt = 42)
            dao.save(entity)

            assertEquals(entity, dao.get())
        }

    @Test
    fun replacesOldOnNewSave() =
        runTest {
            dao.save(BuildingCacheEntity(id = BUILDING_CACHE_SINGLE_ROW, listJson = LIST_JSON, syncedAt = 1))
            val newer = BuildingCacheEntity(id = BUILDING_CACHE_SINGLE_ROW, listJson = "{}", syncedAt = 9)
            dao.save(newer)

            assertEquals(newer, dao.get())
        }
}

private const val LIST_JSON = """{"buildings":[{"symbol":"A-1"},{"symbol":"A-2"}]}"""
