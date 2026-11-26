/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.database

import androidx.room.Room
import io.github.dostepnykampus.core.database.submission.OwnSubmissionDao
import io.github.dostepnykampus.core.database.submission.OwnSubmissionEntity
import io.github.dostepnykampus.core.database.submission.OwnSubmissionState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.IOException

/** Historia własnych zgłoszeń w bazie Room: od najnowszego, bez duplikatów, ze stanem wysyłki. Wymagania: US8, NFR-5 */
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

    /**
     * Wiersz: C3 po odświeżeniu → status, powód i wersja z serwera, stan wysyłki zostaje.
     * Scenariusz (częściowo): zgloszenia-zbierajacego.feature: Zgłoszenie odrzucone pokazuje powód odrzucenia
     *
     * Wymagania: US8.2, BR-16
     */
    @Test
    fun savesServerStatus() =
        runTest {
            dao.save(own(id = "a", savedAt = 1))
            dao.markSent(id = "a")
            dao.saveServerStatus(id = "a", status = "REJECTED", reason = "Zdjęcie nieostre", version = 3)

            val row = dao.observe(OWNER).first().single()

            assertEquals(OwnSubmissionState.SENT, row.state)
            assertEquals("REJECTED", row.serverStatus)
            assertEquals("Zdjęcie nieostre", row.rejectionReason)
            assertEquals(3, row.serverVersion)
        }

    /**
     * Zapis historii w [io.github.dostepnykampus.core.database.submission.PendingSubmissionDao.together]
     * jest cofany razem z transakcją kolejki.
     */
    @Test
    fun rollsBackTogether() =
        runTest {
            dao.save(own(id = "a", savedAt = 1))

            val failure =
                try {
                    database.pendingSubmissions().together {
                        dao.markSent(id = "a")
                        throw IOException("Proces zabity w środku zapisu")
                    }
                    null
                } catch (expected: IOException) {
                    expected
                }

            assertTrue(failure != null)
            val row = dao.observe(OWNER).first().single()
            assertEquals(OwnSubmissionState.LOCAL, row.state)
        }

    /** Scenariusz (częściowo): zgloszenia-zbierajacego.feature: Najnowsze zgłoszenia są na górze listy. */
    @Test
    fun listsNewestFirst() =
        runTest {
            dao.save(own(id = "a", savedAt = 1))
            dao.save(own(id = "b", savedAt = 2))

            assertEquals(listOf("b", "a"), dao.observe(OWNER).first().map { it.id })
        }

    @Test
    fun replacesRowOnEdit() =
        runTest {
            dao.save(own(id = "a", savedAt = 1))
            dao.save(own(id = "a", savedAt = 5).copy(severity = 3))

            val rows = dao.observe(OWNER).first()

            assertEquals(1, rows.size)
            assertEquals(3, rows.single().severity)
        }

    @Test
    fun keepsServerVersionWhenSent() =
        runTest {
            dao.save(own(id = "a", savedAt = 1))
            dao.markRefused(id = "a", code = 422)
            dao.saveVersion(id = "a", version = 4)
            dao.markSent(id = "a")

            val row = dao.observe(OWNER).first().single()

            assertEquals(OwnSubmissionState.SENT, row.state)
            assertEquals(4, row.serverVersion)
            assertEquals(null, row.refusalCode)
        }

    @Test
    fun keepsRefusalCode() =
        runTest {
            dao.save(own(id = "a", savedAt = 1))
            dao.markRefused(id = "a", code = 422)

            val row = dao.observe(OWNER).first().single()

            assertEquals(OwnSubmissionState.REFUSED, row.state)
            assertEquals(422, row.refusalCode)
        }
}

private fun own(id: String, savedAt: Long) =
    OwnSubmissionEntity(
        id = id,
        savedAtMillis = savedAt,
        kind = "PRECISE",
        barrierType = "TB-1",
        severity = 2,
        ownerSub = OWNER,
    )

private const val OWNER = "konto"
