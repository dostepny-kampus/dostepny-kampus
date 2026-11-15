/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.database

import androidx.room.Room
import io.github.dostepnykampus.core.database.cache.BUILDING_CACHE_SINGLE_ROW
import io.github.dostepnykampus.core.database.cache.BuildingCacheDao
import io.github.dostepnykampus.core.database.cache.BuildingCacheEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Kopia listy budynków w bazie Room: jeden wiersz, nowy zapis zastępuje poprzedni. Wymagania: NFR-10 */
@RunWith(RobolectricTestRunner::class)
class BuildingCacheDaoTest {
    @Test
    fun staysEmptyBeforeFirstSave() =
        stored { dao ->
            assertNull(dao.get())
        }

    @Test
    fun returnsSavedList() =
        stored { dao ->
            val entity = BuildingCacheEntity(id = BUILDING_CACHE_SINGLE_ROW, listJson = LIST_JSON, syncedAt = 42)
            dao.save(entity)

            assertEquals(entity, dao.get())
        }

    @Test
    fun replacesOldOnNewSave() =
        stored { dao ->
            dao.save(BuildingCacheEntity(id = BUILDING_CACHE_SINGLE_ROW, listJson = LIST_JSON, syncedAt = 1))
            val newer = BuildingCacheEntity(id = BUILDING_CACHE_SINGLE_ROW, listJson = "{}", syncedAt = 9)
            dao.save(newer)

            assertEquals(newer, dao.get())
        }

    private fun stored(check: suspend (BuildingCacheDao) -> Unit) {
        val database =
            Room
                .inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), DkDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        try {
            runTest { check(database.buildingCache()) }
        } finally {
            database.close()
        }
    }
}

private const val LIST_JSON = """{"buildings":[{"symbol":"A-1"},{"symbol":"A-2"}]}"""
