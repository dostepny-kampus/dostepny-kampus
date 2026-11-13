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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.Instant
import kotlin.coroutines.EmptyCoroutineContext

/** Zapis poprawki nie czeka na wywołania serwera, a poprawka z czasu wysyłki idzie jako edycja. Wymagania: US8.3 */
class EditDuringSendTest {
    @TempDir
    lateinit var tempDir: File

    private val store = FakeStore()
    private val calls = mutableListOf<String>()
    private val entered = CompletableDeferred<Unit>()
    private val reply = CompletableDeferred<Unit>()
    private val queue by lazy {
        SubmissionQueue(
            store = store,
            ioContext = EmptyCoroutineContext,
            server =
                SubmissionServer(
                    create = { id, content ->
                        calls += "create ${checkNotNull(content.severity)}"
                        entered.complete(Unit)
                        reply.await()
                        ApiResult.Success(server(id, content))
                    },
                    putPhoto = { _, _, _ -> ApiResult.Success(Unit) },
                    update = { id, version, content ->
                        calls += "update $version ${checkNotNull(content.severity)}"
                        ApiResult.Success(SubmissionEdit.Saved(server(id, content)))
                    },
                ),
            stamps = SubmissionStamps(newId = { ID }),
        )
    }

    @Test
    fun `us8_3 zapis nie czeka`() =
        runBlocking {
            val photos = photo()
            queue.save(precise(severity = 2), photos)
            val send = launch { queue.sendAll() }
            entered.await()

            withTimeout(TIMEOUT_MILLIS) {
                queue.saveEdit(EditedSubmission(id = ID, baseVersion = null), precise(severity = 3), photos)
            }

            reply.complete(Unit)
            send.join()
        }

    @Test
    fun `us8_3 poprawka jako edycja`() =
        runBlocking {
            val photos = photo()
            queue.save(precise(severity = 2), photos)
            val send = launch { queue.sendAll() }
            entered.await()
            withTimeout(TIMEOUT_MILLIS) {
                queue.saveEdit(EditedSubmission(id = ID, baseVersion = null), precise(severity = 3), photos)
            }

            reply.complete(Unit)
            send.join()

            assertEquals(listOf("create 2", "update 1 3"), calls)
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

    private fun photo(): List<PendingPhotoDraft> {
        val file = tempDir.resolve("$PHOTO.jpg")
        file.writeBytes(byteArrayOf(1, 2, 3))
        return listOf(PendingPhotoDraft(photoId = PHOTO, path = file.path))
    }

    private fun precise(severity: Int): SubmissionContent =
        SubmissionContent(
            kind = SubmissionKind.PRECISE,
            location = GeoPoint(longitude = 17.06209, latitude = 51.10785),
            photoIds = listOf(PHOTO),
            barrierType = "TB-5",
            severity = severity,
        )

    private companion object {
        const val ID = "s-w-trakcie"
        const val PHOTO = "p-w-trakcie"
        const val TIMEOUT_MILLIS = 1_000L
    }
}
