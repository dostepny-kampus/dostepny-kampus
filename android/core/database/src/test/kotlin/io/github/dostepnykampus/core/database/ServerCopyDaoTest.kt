/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.database

import androidx.room.Room
import io.github.dostepnykampus.core.database.cache.BARRIERS_COPY
import io.github.dostepnykampus.core.database.cache.BUILDINGS_COPY
import io.github.dostepnykampus.core.database.cache.ServerCopyDao
import io.github.dostepnykampus.core.database.cache.ServerCopyEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Kopie odpowiedzi serwera w bazie Room: jeden wiersz na klucz, nowy zapis zastępuje poprzedni pod tym samym kluczem
 * i nie rusza innych kluczy. Wymagania: NFR-10
 */
@RunWith(RobolectricTestRunner::class)
class ServerCopyDaoTest {
    private lateinit var database: DkDatabase
    private lateinit var dao: ServerCopyDao

    @Before
    fun open() {
        database =
            Room
                .inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), DkDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        dao = database.serverCopy()
    }

    @After
    fun close() {
        database.close()
    }

    @Test
    fun staysEmptyBeforeFirstSave() =
        runTest {
            assertNull(dao.get(BUILDINGS_COPY))
        }

    @Test
    fun returnsSavedCopy() =
        runTest {
            val entity = ServerCopyEntity(key = BUILDINGS_COPY, documentJson = DOCUMENT, syncedAt = 42)
            dao.save(entity)

            assertEquals(entity, dao.get(BUILDINGS_COPY))
        }

    @Test
    fun replacesOnlyCopyUnderSameKey() =
        runTest {
            val barriers = ServerCopyEntity(key = BARRIERS_COPY, documentJson = "[]", syncedAt = 3)
            dao.save(barriers)
            dao.save(ServerCopyEntity(key = BUILDINGS_COPY, documentJson = DOCUMENT, syncedAt = 1))
            val newer = ServerCopyEntity(key = BUILDINGS_COPY, documentJson = "{}", syncedAt = 9)
            dao.save(newer)

            assertEquals(listOf(newer, barriers), listOf(dao.get(BUILDINGS_COPY), dao.get(BARRIERS_COPY)))
        }
}

private const val DOCUMENT = """{"buildings":[{"symbol":"A-1"}]}"""
