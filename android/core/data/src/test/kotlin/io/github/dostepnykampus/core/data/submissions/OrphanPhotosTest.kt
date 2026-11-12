/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submissions

import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.SubmissionContent
import io.github.dostepnykampus.core.model.SubmissionKind
import io.github.dostepnykampus.core.network.ApiResult
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException
import kotlin.coroutines.EmptyCoroutineContext

/** Sprzątanie przy starcie usuwa tylko pliki zdjęć, których nie potrzebuje kolejka ani odtworzony szkic (#927). */
class OrphanPhotosTest {
    @TempDir
    lateinit var tempDir: File

    private val store = FakeStore()
    private val queue =
        SubmissionQueue(
            store = store,
            ioContext = EmptyCoroutineContext,
            server =
                SubmissionServer(
                    create = { _, _ -> ApiResult.NetworkError(IOException()) },
                    putPhoto = { _, _, _ -> ApiResult.NetworkError(IOException()) },
                    update = { _, _, _ -> ApiResult.NetworkError(IOException()) },
                ),
            stamps = SubmissionStamps(newId = { "s-sprzatanie" }, nowMillis = { NOW }),
        )

    @Test
    fun `nfr5 czekające zostaje`() =
        runBlocking {
            val file = oldPhoto("p1")
            queue.save(precise("p1"), listOf(PendingPhotoDraft(photoId = "p1", path = file.path)))

            queue.deleteOrphanPhotos(dir = tempDir, kept = emptySet())

            assertTrue(file.exists())
        }

    @Test
    fun `nfr5 szkic zostaje`() =
        runBlocking {
            val file = oldPhoto("p2")

            queue.deleteOrphanPhotos(dir = tempDir, kept = setOf(file.path))

            assertTrue(file.exists())
        }

    @Test
    fun `nfr5 osierocone znika`() =
        runBlocking {
            val file = oldPhoto("p3")

            queue.deleteOrphanPhotos(dir = tempDir, kept = emptySet())

            assertFalse(file.exists())
        }

    @Test
    fun `nfr5 świeże zostaje`() =
        runBlocking {
            val file = tempDir.resolve("p4.jpg").apply { writeBytes(byteArrayOf(1)) }
            file.setLastModified(NOW - FRESH_AGE_MILLIS)

            queue.deleteOrphanPhotos(dir = tempDir, kept = emptySet())

            assertTrue(file.exists())
        }

    private fun oldPhoto(id: String): File =
        tempDir.resolve("$id.jpg").apply {
            writeBytes(byteArrayOf(1, 2, 3))
            setLastModified(NOW - OLD_AGE_MILLIS)
        }

    private fun precise(vararg photoIds: String): SubmissionContent =
        SubmissionContent(
            kind = SubmissionKind.PRECISE,
            location = GeoPoint(longitude = 17.06209, latitude = 51.10785),
            photoIds = photoIds.toList(),
            barrierType = "TB-5",
            severity = 2,
        )

    private companion object {
        const val NOW = 1_800_000_000_000L
        const val OLD_AGE_MILLIS = 2 * 60 * 60 * 1000L
        const val FRESH_AGE_MILLIS = 10 * 60 * 1000L
    }
}
