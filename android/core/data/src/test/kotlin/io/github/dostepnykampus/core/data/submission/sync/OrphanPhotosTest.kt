/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submission.sync

import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.submission.SubmissionContent
import io.github.dostepnykampus.core.model.submission.SubmissionKind
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Sprzątanie przy starcie usuwa tylko pliki zdjęć, których nie potrzebuje kolejka ani odtworzony szkic.
 *
 * Wymagania: NFR-5
 */
class OrphanPhotosTest {
    @TempDir
    lateinit var tempDir: File

    private val store = FakeStore()
    private val queue = queueOn(EmptyCoroutineContext)

    /** Katalog zdjęć (`filesDir`) jest czytany w kontekście operacji na plikach, nie w wątku wywołującym. */
    @Test
    fun readsDirectoryInIoContext() =
        Executors.newSingleThreadExecutor { Thread(it, IO_THREAD) }.asCoroutineDispatcher().use { io ->
            var thread = ""
            val dir = { tempDir.also { thread = Thread.currentThread().name } }
            runBlocking { queueOn(io).deleteOrphanPhotos(dir = dir, kept = emptySet()) }
            assertTrue(thread.startsWith(IO_THREAD), "katalog zdjęć czytany w kontekście plików, nie w wątku $thread")
        }

    @Test
    fun keepsWaitingPhoto() =
        runBlocking {
            val file = oldPhoto("p1")
            queue.save(precise("p1"), listOf(PendingPhotoDraft(photoId = "p1", path = file.path)))

            queue.deleteOrphanPhotos(dir = { tempDir }, kept = emptySet())

            assertTrue(file.exists(), "zdjęcie oczekujące w kolejce zostaje")
        }

    @Test
    fun keepsDraftPhoto() =
        runBlocking {
            val file = oldPhoto("p2")

            queue.deleteOrphanPhotos(dir = { tempDir }, kept = setOf(file.path))

            assertTrue(file.exists(), "zdjęcie odtworzonego szkicu zostaje")
        }

    @Test
    fun removesOrphanPhoto() =
        runBlocking {
            val file = oldPhoto("p3")

            queue.deleteOrphanPhotos(dir = { tempDir }, kept = emptySet())

            assertFalse(file.exists(), "zdjęcie bez właściciela jest usunięte")
        }

    @Test
    fun keepsFreshPhoto() =
        runBlocking {
            val file = tempDir.resolve("p4.jpg").apply { writeBytes(byteArrayOf(1)) }
            file.setLastModified(NOW - FRESH_AGE_MILLIS)

            queue.deleteOrphanPhotos(dir = { tempDir }, kept = emptySet())

            assertTrue(file.exists(), "świeże zdjęcie zostaje")
        }

    private fun queueOn(ioContext: CoroutineContext): SubmissionQueue =
        SubmissionQueue(
            store = store,
            ioContext = ioContext,
            server =
                SubmissionServer(
                    create = { _, _ -> ServerReply.Offline },
                    putPhoto = { _, _, _ -> ServerReply.Offline },
                    update = { _, _, _ -> ServerReply.Offline },
                ),
            stamps = SubmissionStamps(newId = { "s-sprzatanie" }, nowMillis = { NOW }),
        )

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
        const val IO_THREAD = "pliki-test"
    }
}
