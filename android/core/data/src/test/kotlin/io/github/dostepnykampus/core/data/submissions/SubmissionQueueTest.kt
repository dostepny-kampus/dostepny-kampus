/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submissions

import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.Submission
import io.github.dostepnykampus.core.model.SubmissionContent
import io.github.dostepnykampus.core.model.SubmissionKind
import io.github.dostepnykampus.core.model.SubmissionStatus
import io.github.dostepnykampus.core.network.ApiResult
import io.github.dostepnykampus.core.network.SubmissionEdit
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException
import java.time.Instant
import kotlin.coroutines.EmptyCoroutineContext

/** Kolejka wysyła treść raz i tylko brakujące zdjęcia, a błędy zostawiają zgłoszenie w kolejce (NFR-5, US9.2). */
class SubmissionQueueTest {
    @TempDir
    lateinit var tempDir: File

    private val store = FakeStore()
    private val edits = mutableListOf<Pair<String, Int>>()
    private var creates = 0
    private val editResults = ArrayDeque<ApiResult<SubmissionEdit>>()

    @Test
    fun `wysyła treść i czyści kolejkę`() =
        runBlocking {
            saveTwoPhotos()
            assertTrue(queue().sendAll())

            assertEquals(listOf(SENT_ID to "p1", SENT_ID to "p2"), store.photos)
            assertTrue(store.rows.isEmpty())
        }

    @Test
    fun `po zerwaniu kontynuuje zdjęcia`() =
        runBlocking {
            saveTwoPhotos()
            store.photoResults.addLast(ApiResult.NetworkError(IOException("sieć")))
            assertFalse(queue().sendAll())

            assertEquals(SENT_ID, store.created.single())
            val row = store.rows.single()
            assertTrue(store.photos.isEmpty() && row.photos.none { it.isUploaded })

            assertTrue(queue().sendAll())

            assertEquals(1, store.created.size)
            assertEquals(listOf(SENT_ID to "p1", SENT_ID to "p2"), store.photos)
            assertTrue(store.rows.isEmpty())
        }

    @Test
    fun `odmowa zostawia w kolejce`() =
        runBlocking {
            saveTwoPhotos()
            store.createResults.addLast(ApiResult.ServerError(code = 400))
            queue().sendAll()

            assertTrue(store.created.isEmpty())
            assertTrue(store.photos.isEmpty())
            val left = store.rows.single()
            assertTrue(left.submission.isCreatedOnServer == false)
        }

    @Test
    fun `zapis planuje wysyłkę`() =
        runBlocking {
            var scheduled = 0
            saveTwoPhotos(queue(onSaved = { scheduled++ }))

            assertEquals(1, scheduled)
        }

    @Test
    fun `poprawka zastępuje wiersz`() =
        runBlocking {
            val target = queue()
            saveTwoPhotos(target)

            val edited = EditedSubmission(id = SENT_ID, baseVersion = null)
            target.saveEdit(edited, content = fullContent().copy(severity = 3), photos = drafts())

            assertEquals(listOf(SENT_ID), store.rows.map { it.submission.id })
            assertEquals(listOf(3), store.rows.map { it.submission.severity })
        }

    @Test
    fun `poprawka z wersją to edycja`() =
        runBlocking {
            val target = queue()
            target.saveEdit(EditedSubmission(id = "s1", baseVersion = 4), fullContent(), drafts())

            assertTrue(target.sendAll())

            assertEquals(listOf("s1" to 4), edits)
            assertEquals(0, creates)
            assertEquals(emptyList<String>(), store.rows.map { it.submission.id })
        }

    @Test
    fun `konflikt wersji zostaje`() =
        runBlocking {
            val target = queue()
            target.saveEdit(EditedSubmission(id = "s1", baseVersion = 4), fullContent(), drafts())
            val server = (serverSubmission(fullContent()) as ApiResult.Success).value
            editResults.addLast(ApiResult.Success(SubmissionEdit.Conflict(server)))

            target.sendAll()

            assertEquals(listOf("s1"), store.rows.map { it.submission.id })
            assertEquals(emptyList<Pair<String, String>>(), store.photos)
        }

    /** Dwa zapisy w tej samej chwili zegara idą na serwer w kolejności zapisu, nie identyfikatorów (US9.2, BR-9). */
    @Test
    fun `kolejność jak przy zapisie`() =
        runBlocking {
            val ids = ArrayDeque(listOf("z-pierwsze", "a-drugie"))
            val sent = mutableListOf<String>()
            val target =
                SubmissionQueue(
                    store = store,
                    ioContext = EmptyCoroutineContext,
                    server =
                        SubmissionServer(
                            create = { id, content ->
                                sent.add(id)
                                serverSubmission(content)
                            },
                            putPhoto = { _, _, _ -> ApiResult.Success(Unit) },
                            update = { _, _, _ -> ApiResult.NotFound },
                        ),
                    newId = { ids.removeFirst() },
                    nowMillis = { SAME_MILLIS },
                )
            val content = fullContent().copy(photoIds = emptyList())
            target.save(content, emptyList())
            target.save(content, emptyList())
            target.sendAll()

            assertEquals(listOf("z-pierwsze", "a-drugie"), sent)
        }

    private fun queue(onSaved: () -> Unit = {}): SubmissionQueue =
        SubmissionQueue(
            store = store,
            ioContext = EmptyCoroutineContext,
            server =
                SubmissionServer(
                    create = { _, content ->
                        creates++
                        store.createResults.removeFirstOrNull() ?: serverSubmission(content)
                    },
                    putPhoto = { _, _, _ -> store.photoResults.removeFirstOrNull() ?: ApiResult.Success(Unit) },
                    update = { id, version, content ->
                        edits.add(id to version)
                        editResults.removeFirstOrNull() ?: ApiResult.Success(SubmissionEdit.Saved(edited(id, content)))
                    },
                ),
            newId = { SENT_ID },
            onSaved = onSaved,
        )

    private fun edited(id: String, content: SubmissionContent): Submission =
        (serverSubmission(content) as ApiResult.Success).value.copy(id = id, version = 5)

    private fun saveTwoPhotos(target: SubmissionQueue = queue()) {
        runBlocking { target.save(fullContent(), drafts()) }
    }

    private fun drafts(): List<PendingPhotoDraft> {
        val drafts = listOf("p1", "p2").map { photo -> PendingPhotoDraft(photoId = photo, path = pathOf(photo)) }
        drafts.forEach { File(it.path).writeBytes(byteArrayOf(1, 2, 3)) }
        return drafts
    }

    private fun pathOf(photo: String): String = tempDir.resolve("$photo.jpg").path

    private fun serverSubmission(content: SubmissionContent): ApiResult<Submission> =
        ApiResult.Success(
            Submission(
                id = "s1",
                version = 1,
                status = SubmissionStatus.SENT,
                createdAt = Instant.parse("2026-01-01T00:00:00Z"),
                content = content,
            ),
        )

    private fun fullContent(): SubmissionContent =
        SubmissionContent(
            kind = SubmissionKind.QUICK,
            location = GeoPoint(longitude = 17.06209, latitude = 51.10785),
            photoIds = listOf("p1", "p2"),
            severity = 2,
            description = "Progi przy sekretariacie",
        )

    private companion object {
        const val SAME_MILLIS = 1_000L
    }
}
