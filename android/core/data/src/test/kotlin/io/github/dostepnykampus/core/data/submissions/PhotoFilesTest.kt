/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submissions

import io.github.dostepnykampus.core.data.server.ServerEdit
import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.Submission
import io.github.dostepnykampus.core.model.SubmissionContent
import io.github.dostepnykampus.core.model.SubmissionKind
import io.github.dostepnykampus.core.model.SubmissionStatus
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.Instant
import kotlin.coroutines.EmptyCoroutineContext

/** Pliki zdjęć znikają z pamięci aplikacji po wysłaniu zgłoszenia i po zastąpieniu poprawką. Wymagania: NFR-5 */
class PhotoFilesTest {
    @TempDir
    lateinit var tempDir: File

    private val store = FakeStore()
    private var isOnline = true
    private val queue by lazy {
        SubmissionQueue(
            store = store,
            ioContext = EmptyCoroutineContext,
            server =
                SubmissionServer(
                    create = { id, content ->
                        if (isOnline) ServerReply.Ok(server(id, content)) else ServerReply.Offline
                    },
                    putPhoto = { _, _, _ -> ServerReply.Ok(Unit) },
                    update = { id, _, content -> ServerReply.Ok(ServerEdit.Saved(server(id, content))) },
                ),
            stamps = SubmissionStamps(newId = { ID }),
        )
    }

    @Test
    fun `nfr5 wysłane bez pliku`() =
        runBlocking {
            val photos = photos("p1", "p2")
            queue.save(precise("p1", "p2"), photos)

            queue.sendAll()

            photos.forEach { assertFalse(File(it.path).exists()) }
        }

    @Test
    fun `nfr5 czekające z plikiem`() =
        runBlocking {
            isOnline = false
            val photos = photos("p1")
            queue.save(precise("p1"), photos)

            queue.sendAll()

            assertTrue(File(photos.single().path).exists())
        }

    @Test
    fun `us8_3 poprawka usuwa stare`() =
        runBlocking {
            isOnline = false
            val first = photos("p1", "p2")
            queue.save(precise("p1", "p2"), first)
            val second = first.take(1) + photos("p3")

            queue.saveEdit(EditedSubmission(id = ID, baseVersion = null), precise("p1", "p3"), second)

            assertFalse(File(first[1].path).exists())
            second.forEach { assertTrue(File(it.path).exists()) }
        }

    private fun server(id: String, content: SubmissionContent): Submission =
        Submission(
            id = id,
            version = 1,
            status = SubmissionStatus.SENT,
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            content = content,
        )

    private fun photos(vararg ids: String): List<PendingPhotoDraft> =
        ids.map { id ->
            val file = tempDir.resolve("$id.jpg")
            file.writeBytes(byteArrayOf(1, 2, 3))
            PendingPhotoDraft(photoId = id, path = file.path)
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
        const val ID = "s-pliki"
    }
}
