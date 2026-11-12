/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.Instant
import kotlin.coroutines.EmptyCoroutineContext

/** Zgłoszenie zapisane w trakcie wysyłki kolejki idzie w tym samym przebiegu, bez kolejnego zapisu (NFR-6, #854). */
class SaveDuringSendTest {
    @TempDir
    lateinit var tempDir: File

    private val store = FakeStore()
    private val created = mutableListOf<String>()
    private val ids = ArrayDeque(listOf(FIRST, SECOND))
    private var saveDuringSend: (suspend () -> Unit)? = null
    private val queue: SubmissionQueue by lazy {
        SubmissionQueue(
            store = store,
            ioContext = EmptyCoroutineContext,
            server =
                SubmissionServer(
                    create = { id, content ->
                        created += id
                        val pending = saveDuringSend
                        saveDuringSend = null
                        pending?.invoke()
                        ApiResult.Success(server(id, content))
                    },
                    putPhoto = { _, _, _ -> ApiResult.Success(Unit) },
                    update = { id, _, content -> ApiResult.Success(SubmissionEdit.Saved(server(id, content))) },
                ),
            stamps = SubmissionStamps(newId = { ids.removeFirst() }),
        )
    }

    @Test
    fun `nfr6 zapis w trakcie wysyłki`() =
        runBlocking {
            queue.save(precise("p1"), photo("p1"))
            saveDuringSend = { queue.save(precise("p2"), photo("p2")) }

            assertTrue(queue.sendAll())

            assertEquals(listOf(FIRST, SECOND), created)
            assertEquals(emptyList<Any>(), store.rows)
        }

    private fun server(id: String, content: SubmissionContent): Submission =
        Submission(
            id = id,
            version = 1,
            status = SubmissionStatus.SENT,
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            content = content,
        )

    private fun photo(id: String): List<PendingPhotoDraft> {
        val file = tempDir.resolve("$id.jpg")
        file.writeBytes(byteArrayOf(1, 2, 3))
        return listOf(PendingPhotoDraft(photoId = id, path = file.path))
    }

    private fun precise(photoId: String): SubmissionContent =
        SubmissionContent(
            kind = SubmissionKind.PRECISE,
            location = GeoPoint(longitude = 17.06209, latitude = 51.10785),
            photoIds = listOf(photoId),
            barrierType = "TB-5",
            severity = 2,
        )

    private companion object {
        const val FIRST = "s-pierwsze"
        const val SECOND = "s-w-trakcie"
    }
}
