/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Yevhenii Lukianenko, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.database

import android.database.Cursor
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Migracje bazy: nowa tabela powstaje pusta, a kolejka zgłoszeń zostaje.
 * 1→2: kopia listy budynków; 2→3: historia własnych zgłoszeń; 3→4: znacznik odrzuconego;
 * 4→5: status z serwera w historii; 5→6: kopia barier ze zgłoszeń przyjętych.
 *
 * Wymagania: NFR-10, US8, BR-26, US8.2
 */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {
    private val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), DkDatabase::class.java)

    @Test
    fun keepsQueueFrom1To2() {
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

    @Test
    fun keepsQueueFrom2To3() {
        helper.createDatabase(DB_NAME, 2).use { second ->
            second.execSQL(
                "INSERT INTO pending_submissions (id, saved_at, created_on_server, kind, longitude, latitude)" +
                    " VALUES ('a', 7, 0, 'PRECISE', 17.0, 51.0)",
            )
        }

        helper
            .runMigrationsAndValidate(DB_NAME, 3, true, MIGRATION_2_3)
            .use { third ->
                val queued = third.query("SELECT COUNT(*) FROM pending_submissions").use { row -> readCount(row) }
                val own = third.query("SELECT COUNT(*) FROM own_submissions").use { row -> readCount(row) }

                assertEquals(1, queued)
                assertEquals(0, own)
            }
    }

    @Test
    fun keepsQueueFrom3To4() {
        helper.createDatabase(DB_NAME, 3).use { third ->
            third.execSQL(
                "INSERT INTO pending_submissions (id, saved_at, created_on_server, kind, longitude, latitude)" +
                    " VALUES ('a', 7, 0, 'PRECISE', 17.0, 51.0)",
            )
        }

        helper
            .runMigrationsAndValidate(DB_NAME, 4, true, MIGRATION_3_4)
            .use { fourth ->
                val waiting =
                    fourth.query("SELECT COUNT(*) FROM pending_submissions WHERE refused = 0").use { row ->
                        readCount(row)
                    }

                assertEquals(1, waiting)
            }
    }

    @Test
    fun keepsHistoryFrom4To5() {
        helper.createDatabase(DB_NAME, 4).use { fourth ->
            fourth.execSQL("INSERT INTO own_submissions (id, saved_at, kind, state) VALUES ('a', 7, 'PRECISE', 'SENT')")
        }

        helper
            .runMigrationsAndValidate(DB_NAME, 5, true, MIGRATION_4_5)
            .use { fifth ->
                val unknown =
                    fifth.query("SELECT COUNT(*) FROM own_submissions WHERE server_status IS NULL").use { row ->
                        readCount(row)
                    }

                assertEquals(1, unknown)
            }
    }

    @Test
    fun keepsHistoryFrom5To6() {
        helper.createDatabase(DB_NAME, 5).use { fifth ->
            fifth.execSQL("INSERT INTO own_submissions (id, saved_at, kind, state) VALUES ('a', 7, 'PRECISE', 'SENT')")
            fifth.execSQL("INSERT INTO building_cache (id, list_json, synced_at) VALUES (1, '{}', 7)")
        }

        helper
            .runMigrationsAndValidate(DB_NAME, 6, true, MIGRATION_5_6)
            .use { sixth ->
                val history = sixth.query("SELECT COUNT(*) FROM own_submissions").use { row -> readCount(row) }
                val buildings = sixth.query("SELECT COUNT(*) FROM building_cache").use { row -> readCount(row) }
                val barriers = sixth.query("SELECT COUNT(*) FROM barrier_cache").use { row -> readCount(row) }

                assertEquals(listOf(1, 1, 0), listOf(history, buildings, barriers))
            }
    }

    @Test
    fun keepsQueueFrom6To7() {
        helper.createDatabase(DB_NAME, 6).use { sixth ->
            sixth.execSQL(
                "INSERT INTO pending_submissions (id, saved_at, created_on_server, kind, longitude, latitude)" +
                    " VALUES ('a', 7, 0, 'PRECISE', 17.0, 51.0)",
            )
        }

        helper
            .runMigrationsAndValidate(DB_NAME, 7, true, MIGRATION_6_7)
            .use { seventh ->
                val queued = seventh.query("SELECT COUNT(*) FROM pending_submissions").use { row -> readCount(row) }
                val markers = seventh.query("SELECT COUNT(*) FROM pending_marker_changes").use { row -> readCount(row) }

                assertEquals(listOf(1, 0), listOf(queued, markers))
            }
    }

    @Test
    fun keepsQueueFrom7To8() {
        helper.createDatabase(DB_NAME, 7).use { seventh ->
            seventh.execSQL(
                "INSERT INTO pending_submissions (id, saved_at, created_on_server, kind, longitude, latitude)" +
                    " VALUES ('a', 7, 0, 'PRECISE', 17.0, 51.0)",
            )
        }

        helper
            .runMigrationsAndValidate(DB_NAME, 8, true, MIGRATION_7_8)
            .use { eighth ->
                val queued = eighth.query("SELECT COUNT(*) FROM pending_submissions").use { row -> readCount(row) }
                val plans = eighth.query("SELECT COUNT(*) FROM floor_plans").use { row -> readCount(row) }
                val rooms = eighth.query("SELECT COUNT(*) FROM floor_rooms").use { row -> readCount(row) }

                assertEquals(listOf(1, 0, 0), listOf(queued, plans, rooms))
            }
    }

    @Test
    fun keepsRoomsFrom8To9() {
        helper.createDatabase(DB_NAME, 8).use { eighth ->
            eighth.execSQL(
                "INSERT INTO floor_rooms (building_symbol, floor_id, id, number, function, category, geometry_json," +
                    " label_longitude, label_latitude, labelled) VALUES ('A-1', 'P0', 'r', '1', 'Winda', 'ELEVATOR'," +
                    " '{}', 17.0, 51.0, 1)",
            )
        }

        helper
            .runMigrationsAndValidate(DB_NAME, 9, true, MIGRATION_8_9)
            .use { ninth ->
                val lift =
                    ninth.query("SELECT lift_id, lift_floors_json FROM floor_rooms").use { row ->
                        row.moveToFirst()
                        listOf(row.isNull(0), row.isNull(1))
                    }

                assertEquals(listOf(true, true), lift)
            }
    }

    @Test
    fun keepsBothQueuesFrom9To10() {
        helper.createDatabase(DB_NAME, 9).use { ninth ->
            ninth.execSQL(
                "INSERT INTO pending_submissions (id, saved_at, created_on_server, kind, longitude, latitude)" +
                    " VALUES ('a', 7, 0, 'PRECISE', 17.0, 51.0)",
            )
            ninth.execSQL(
                "INSERT INTO pending_marker_changes (operation_id, type, marker_id, building_symbol, saved_at)" +
                    " VALUES ('b', 'SET', 'm', 'A-1', 8)",
            )
        }

        val database = databaseAfterFirstAccount()
        try {
            runBlocking {
                val submission =
                    database
                        .pendingSubmissions()
                        .all()
                        .single()
                        .submission
                val marker =
                    database
                        .pendingMarkerChanges()
                        .observe()
                        .first()
                        .single()
                assertEquals("zgłoszenie należy do pierwszego konta", "pierwsze", submission.ownerSub)
                assertEquals("zmiana znacznika należy do pierwszego konta", "pierwsze", marker.ownerSub)
            }
        } finally {
            database.close()
        }
    }

    @Test
    fun addsMarkersFrom10To11() {
        helper.createDatabase(DB_NAME, 10).use { tenth ->
            tenth.execSQL("INSERT INTO building_cache (id, list_json, synced_at) VALUES (1, '{}', 7)")
        }

        helper
            .runMigrationsAndValidate(DB_NAME, 11, true)
            .use { eleventh ->
                val buildings = eleventh.query("SELECT COUNT(*) FROM building_cache").use { row -> readCount(row) }
                val markers = eleventh.query("SELECT COUNT(*) FROM inspection_markers").use { row -> readCount(row) }

                assertEquals("kopia budynków i pusta tabela znaczników", listOf(1, 0), listOf(buildings, markers))
            }
    }

    @Test
    fun movesCopiesToOneTableIn12() {
        helper.createDatabase(DB_NAME, 11).use { eleventh ->
            eleventh.execSQL("INSERT INTO building_cache (id, list_json, synced_at) VALUES (1, '{\"b\":1}', 7)")
            eleventh.execSQL("INSERT INTO barrier_cache (id, list_json, synced_at) VALUES (1, '[]', 9)")
        }

        helper
            .runMigrationsAndValidate(DB_NAME, 12, true, MIGRATION_11_12)
            .use { twelfth ->
                val copies =
                    twelfth.query("SELECT `key`, document_json, synced_at FROM server_copy ORDER BY `key`").use { row ->
                        generateSequence { if (row.moveToNext()) row else null }
                            .map { "${it.getString(0)} ${it.getString(1)} ${it.getLong(2)}" }
                            .toList()
                    }
                val oldTables =
                    twelfth
                        .query(
                            "SELECT COUNT(*) FROM sqlite_master WHERE name IN ('building_cache', 'barrier_cache')",
                        ).use { row -> readCount(row) }

                assertEquals(
                    "kopie pod kluczami i bez starych tabel",
                    listOf("barriers [] 9", "buildings {\"b\":1} 7", "0"),
                    copies + oldTables.toString(),
                )
            }
    }

    @Test
    fun adoptsOwnerlessHistory() {
        helper.createDatabase(DB_NAME, 12).use { twelfth ->
            twelfth.execSQL(
                "INSERT INTO own_submissions (id, saved_at, kind, state) VALUES ('h', 7, 'PRECISE', 'SENT')",
            )
        }

        val database = databaseAfterFirstAccount()
        try {
            runBlocking {
                val owner = database.ownSubmissions().find("h")?.ownerSub
                assertEquals("historia bez właściciela należy do pierwszego konta", "pierwsze", owner)
            }
        } finally {
            database.close()
        }
    }

    /** Wersja 13→14: historia zostaje, nowe kolumny są puste. Wymagania: US8.4, US17.2 */
    @Test
    fun keepsHistoryFrom13To14() {
        helper.createDatabase(DB_NAME, 13).use { thirteenth ->
            thirteenth.execSQL(
                "INSERT INTO own_submissions (id, saved_at, kind, state) VALUES ('h', 7, 'PRECISE', 'SENT')",
            )
        }

        helper
            .runMigrationsAndValidate(DB_NAME, 14, true, MIGRATION_13_14)
            .use { fourteenth ->
                val empty =
                    "SELECT COUNT(*) FROM own_submissions WHERE barrier_removed IS NULL " +
                        "AND entrance_id IS NULL AND entrance_marking IS NULL"
                val history = fourteenth.query(empty).use { row -> readCount(row) }
                assertEquals("wiersz historii zostaje z pustymi nowymi kolumnami", 1, history)
            }
    }

    /** Wersja 14→15: historia zostaje, treść do poprawki jest pusta. Wymagania: US8.3, NFR-5 */
    @Test
    fun keepsHistoryFrom14To15() {
        helper.createDatabase(DB_NAME, 14).use { fourteenth ->
            fourteenth.execSQL(
                "INSERT INTO own_submissions (id, saved_at, kind, state) VALUES ('h', 7, 'PRECISE', 'SENT')",
            )
        }

        helper
            .runMigrationsAndValidate(DB_NAME, 15, true, MIGRATION_14_15)
            .use { fifteenth ->
                val empty = "SELECT COUNT(*) FROM own_submissions WHERE id = 'h' AND content_json IS NULL"
                val history = fifteenth.query(empty).use { row -> readCount(row) }
                assertEquals("wiersz historii zostaje bez treści do poprawki", 1, history)
            }
    }
}

private fun databaseAfterFirstAccount(): DkDatabase {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val database =
        Room
            .databaseBuilder(context, DkDatabase::class.java, DB_NAME)
            .addMigrations(*ALL_MIGRATIONS.toTypedArray())
            .build()
    runBlocking {
        database.queueOwnership().adopt("pierwsze")
        database.queueOwnership().adopt("drugie")
    }
    return database
}

private fun readCount(cursor: Cursor): Int {
    cursor.moveToFirst()
    return cursor.getInt(0)
}

private const val DB_NAME = "migracja-test.db"
