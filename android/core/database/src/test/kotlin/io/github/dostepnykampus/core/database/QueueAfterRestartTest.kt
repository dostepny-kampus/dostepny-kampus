/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.database

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

/** Kolejka i historia w pliku bazy z [DatabaseModule] po zamknięciu i ponownym otwarciu. Wymagania: NFR-5, BR-9 */
@RunWith(RobolectricTestRunner::class)
class QueueAfterRestartTest {
    private val app = RuntimeEnvironment.getApplication()

    /**
     * Restart to nowa instancja bazy na tym samym pliku, jak po uruchomieniu procesu od nowa.
     * Scenariusz: synchronizacja.feature: Zgłoszenia zapisane bez sieci przetrwają restart telefonu
     */
    @Test
    fun us9_1_local_after_restart() =
        runTest {
            val before = DatabaseModule.database(app)
            IDS.forEachIndexed { index, id ->
                before.pendingSubmissions().save(submission(id, savedAt = index.toLong()), photos(of = id))
                before.ownSubmissions().save(OwnSubmissionEntity(id = id, savedAtMillis = index.toLong(), kind = KIND))
            }
            before.close()

            val after = DatabaseModule.database(app)
            val pending = after.pendingSubmissions().all()
            val own = after.ownSubmissions().observe().first()
            after.close()

            assertEquals(IDS, pending.map { it.submission.id })
            pending.forEach { saved ->
                assertEquals(PHOTOS_EACH, saved.photos.size)
                assertTrue(saved.photos.all { File(it.path).isFile && !it.isUploaded })
            }
            assertEquals(IDS.toSet(), own.map { it.id }.toSet())
            assertTrue(own.all { it.state == OwnSubmissionState.LOCAL })
        }

    private fun photos(of: String): List<PendingPhotoEntity> =
        (1..PHOTOS_EACH).map { number ->
            val file = File(app.filesDir, "zdjecia/$of-$number.jpg").apply { parentFile?.mkdirs() }
            file.writeBytes(byteArrayOf(1, 2, 3))
            PendingPhotoEntity(submissionId = of, photoId = "$of-$number", path = file.path, isUploaded = false)
        }

    private fun submission(id: String, savedAt: Long): PendingSubmissionEntity =
        PendingSubmissionEntity(
            id = id,
            baseVersion = null,
            savedAtMillis = savedAt,
            isCreatedOnServer = false,
            kind = KIND,
            longitude = 17.06209,
            latitude = 51.10785,
            accuracyMeters = 4.0,
            isManualCorrection = null,
            buildingSymbol = "C-13",
            floorId = null,
            floorNumber = 0,
            isMezzanine = null,
            barrierGroup = "OTHER",
            barrierType = "TB-8",
            severity = 3,
            description = null,
            replacesSubmissionId = null,
            isBarrierRemoved = null,
            entranceId = null,
            entranceMarking = null,
        )

    private companion object {
        const val KIND = "PRECISE"
        const val PHOTOS_EACH = 2
        val IDS = listOf("a", "b", "c")
    }
}
