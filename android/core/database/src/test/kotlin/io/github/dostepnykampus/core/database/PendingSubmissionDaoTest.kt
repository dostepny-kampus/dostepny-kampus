/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.database

import androidx.room.Room
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

/** Kolejka zgłoszeń w bazie Room w pamięci: zapis, ponowny zapis, postęp wysyłki i usunięcie (NFR-5, BR-9). */
@RunWith(RobolectricTestRunner::class)
class PendingSubmissionDaoTest {
    private lateinit var database: DkDatabase
    private lateinit var dao: PendingSubmissionDao

    @Before
    fun open() {
        database =
            Room
                .inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), DkDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        dao = database.pendingSubmissions()
    }

    @After
    fun close() {
        database.close()
    }

    @Test
    fun nfr5_saved_with_photos() =
        runTest {
            dao.save(submission(id = ID, savedAt = 2), photos("p1", "p2", of = ID))
            val saved = dao.observe().first().single()
            assertEquals(submission(id = ID, savedAt = 2), saved.submission)
            assertEquals(setOf("p1", "p2"), saved.photos.map { it.photoId }.toSet())
        }

    @Test
    fun nfr5_oldest_first() =
        runTest {
            dao.save(submission(id = "b", savedAt = 5), photos("p", of = "b"))
            dao.save(submission(id = "a", savedAt = 9), photos("p", of = "a"))
            assertEquals(listOf("b", "a"), dao.all().map { it.submission.id })
        }

    @Test
    fun br9_resave_replaces() =
        runTest {
            dao.save(submission(id = ID, savedAt = 1), photos("p1", "p2", of = ID))
            dao.save(submission(id = ID, savedAt = 1).copy(severity = 3), photos("p3", of = ID))
            val saved = dao.all().single()
            assertEquals(3, saved.submission.severity)
            assertEquals(listOf("p3"), saved.photos.map { it.photoId })
        }

    @Test
    fun us9_2_progress_kept() =
        runTest {
            dao.save(submission(id = ID, savedAt = 1), photos("p1", "p2", of = ID))
            dao.markCreated(ID)
            dao.markUploaded(submissionId = ID, photoId = "p1")
            val saved = dao.all().single()
            assertTrue(saved.submission.isCreatedOnServer)
            assertEquals(listOf("p1"), saved.photos.filter { it.isUploaded }.map { it.photoId })
        }

    @Test
    fun us9_2_sent_removed() =
        runTest {
            dao.save(submission(id = ID, savedAt = 1), photos("p1", of = ID))
            dao.delete(ID)
            assertEquals(emptyList<PendingSubmissionWithPhotos>(), dao.all())
        }

    /** Odrzucone zgłoszenie zostaje w telefonie, ale kolejka go nie oddaje do wysyłki (BR-26). */
    @Test
    fun br26_refused_not_queued() =
        runTest {
            dao.save(submission(id = "a", savedAt = 1), photos("p1", of = "a"))
            dao.save(submission(id = "b", savedAt = 2), photos("p2", of = "b"))
            dao.markRefused("a")
            assertEquals(listOf("b"), dao.all().map { it.submission.id })
            assertEquals(listOf("b"), dao.observe().first().map { it.submission.id })
            dao.save(submission(id = "a", savedAt = 3), photos("p1", of = "a"))
            assertEquals(listOf("b", "a"), dao.all().map { it.submission.id })
        }

    private fun submission(id: String, savedAt: Long): PendingSubmissionEntity =
        PendingSubmissionEntity(
            id = id,
            baseVersion = null,
            savedAtMillis = savedAt,
            isCreatedOnServer = false,
            kind = "QUICK",
            longitude = 17.06209,
            latitude = 51.10785,
            accuracyMeters = 4.0,
            isManualCorrection = null,
            buildingSymbol = null,
            floorId = null,
            floorNumber = null,
            isMezzanine = null,
            barrierGroup = "OTHER",
            barrierType = null,
            severity = null,
            description = null,
            replacesSubmissionId = null,
            isBarrierRemoved = null,
            entranceId = null,
            entranceMarking = null,
        )

    private fun photos(vararg photoIds: String, of: String): List<PendingPhotoEntity> =
        photoIds.map { photo(submissionId = of, photoId = it) }

    private fun photo(submissionId: String, photoId: String): PendingPhotoEntity =
        PendingPhotoEntity(
            submissionId = submissionId,
            photoId = photoId,
            path = "zdjecia/$photoId.jpg",
            isUploaded = false,
        )

    private companion object {
        const val ID = "0b6f6c1e-8a43-4d5e-9f43-2f1d3c4b5a69"
    }
}
