/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.database

import android.database.Cursor
import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Migracja 1→2: tabela kopii listy budynków powstaje pusta, a kolejka zgłoszeń zostaje (NFR-10). */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {
    private val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), DkDatabase::class.java)

    @Test
    fun migrate_1_to_2_keeps_queue() {
        helper.createDatabase(DB_NAME, 1).use { first ->
            first.execSQL(
                "INSERT INTO pending_submissions (id, saved_at, created_on_server, kind, longitude, latitude)" +
                    " VALUES ('a', 7, 0, 'QUICK', 17.0, 51.0)",
            )
            val before = first.query("SELECT COUNT(*) FROM pending_submissions").use { row -> readCount(row) }

            assertEquals(1, before)
        }

        helper
            .runMigrationsAndValidate(DB_NAME, 2, true, MIGRATION_1_2)
            .use { second ->
                val queued = second.query("SELECT COUNT(*) FROM pending_submissions").use { row -> readCount(row) }
                val cache = second.query("SELECT COUNT(*) FROM building_cache").use { row -> readCount(row) }

                assertEquals(1, queued)
                assertEquals(0, cache)
            }
    }
}

private fun readCount(cursor: Cursor): Int {
    cursor.moveToFirst()
    return cursor.getInt(0)
}

private const val DB_NAME = "migracja-test.db"
