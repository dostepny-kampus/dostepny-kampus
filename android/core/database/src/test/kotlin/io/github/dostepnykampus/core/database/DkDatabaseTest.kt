/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.database

import androidx.room.Room
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Schemat bazy lokalnej: tabele kolejki zgłoszeń i ich zdjęć (NFR-5). */
@RunWith(RobolectricTestRunner::class)
class DkDatabaseTest {
    @Test
    fun nfr5_queue_tables() {
        val database =
            Room
                .inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), DkDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        val tables =
            database.openHelper.readableDatabase
                .query("SELECT name FROM sqlite_master WHERE type = 'table' AND name LIKE 'pending_%' ORDER BY name")
                .use { cursor -> generateSequence { if (cursor.moveToNext()) cursor.getString(0) else null }.toList() }
        database.close()
        assertEquals(listOf("pending_photos", "pending_submissions"), tables)
    }
}
