/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.database

import androidx.room.Room
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Historia własnych zgłoszeń w bazie Room: od najnowszego, bez duplikatów, ze stanem wysyłki (US8, NFR-5). */
@RunWith(RobolectricTestRunner::class)
class OwnSubmissionDaoTest {
    private lateinit var database: DkDatabase
    private lateinit var dao: OwnSubmissionDao

    @Before
    fun open() {
        database =
            Room
                .inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), DkDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        dao = database.ownSubmissions()
    }

    @After
    fun close() {
        database.close()
    }

    /** Wiersz: C3 po odświeżeniu → status, powód i wersja z serwera, stan wysyłki zostaje, US8.2, BR-16. */
    @Test
    fun us8_2_server_status_saved() =
        runTest {
            dao.save(own(id = "a", savedAt = 1))
            dao.markSent(id = "a")
            dao.saveServerStatus(id = "a", status = "REJECTED", reason = "Zdjęcie nieostre", version = 3)

            val row = dao.observe().first().single()

            assertEquals(OwnSubmissionState.SENT, row.state)
            assertEquals("REJECTED", row.serverStatus)
            assertEquals("Zdjęcie nieostre", row.rejectionReason)
            assertEquals(3, row.serverVersion)
        }

    @Test
    fun us8_newest_first() =
        runTest {
            dao.save(own(id = "a", savedAt = 1))
            dao.save(own(id = "b", savedAt = 2))

            assertEquals(listOf("b", "a"), dao.observe().first().map { it.id })
        }

    @Test
    fun us8_edit_replaces_row() =
        runTest {
            dao.save(own(id = "a", savedAt = 1))
            dao.save(own(id = "a", savedAt = 5).copy(severity = 3))

            val rows = dao.observe().first()

            assertEquals(1, rows.size)
            assertEquals(3, rows.single().severity)
        }

    @Test
    fun nfr5_sent_keeps_server_version() =
        runTest {
            dao.save(own(id = "a", savedAt = 1))
            dao.markRefused(id = "a", code = 422)
            dao.saveVersion(id = "a", version = 4)
            dao.markSent(id = "a")

            val row = dao.observe().first().single()

            assertEquals(OwnSubmissionState.SENT, row.state)
            assertEquals(4, row.serverVersion)
            assertEquals(null, row.refusalCode)
        }

    @Test
    fun nfr5_refusal_keeps_code() =
        runTest {
            dao.save(own(id = "a", savedAt = 1))
            dao.markRefused(id = "a", code = 422)

            val row = dao.observe().first().single()

            assertEquals(OwnSubmissionState.REFUSED, row.state)
            assertEquals(422, row.refusalCode)
        }
}

private fun own(id: String, savedAt: Long) =
    OwnSubmissionEntity(id = id, savedAtMillis = savedAt, kind = "PRECISE", barrierType = "TB-1", severity = 2)
