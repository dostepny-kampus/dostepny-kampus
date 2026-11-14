/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submissions

import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.database.OwnSubmissionState
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
import java.time.Instant
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Odmowa serwera (4xx poza 401) daje stan końcowy „Nie wysłano” z kodem i nie wraca do wysyłki, aż osoba zapisze
 * zgłoszenie ponownie; 5xx i brak sieci ponawiają się, a 401 czeka na zalogowanie.
 *
 * Wymagania: BR-26, US9.5
 */
class RefusedSubmissionTest {
    @TempDir
    lateinit var tempDir: File

    private val store = FakeStore()
    private val history = FakeHistory()
    private var creates = 0
    private val queue by lazy {
        SubmissionQueue(
            store = store,
            ioContext = EmptyCoroutineContext,
            server =
                SubmissionServer(
                    create = { id, content ->
                        creates++
                        store.createResults.removeFirstOrNull() ?: ServerReply.Ok(server(id, content))
                    },
                    putPhoto = { _, _, _ -> ServerReply.Ok(Unit) },
                    update = { id, _, content -> ApiResult.Success(SubmissionEdit.Saved(server(id, content))) },
                ),
            stamps = SubmissionStamps(newId = { ID }),
            history = history,
        )
    }

    @Test
    fun `br26 odmowa 400`() = refusedOnce(ServerReply.Fault(code = 400), code = 400)

    @Test
    fun `br26 odmowa 403`() = refusedOnce(ServerReply.Fault(code = 403), code = 403)

    @Test
    fun `br26 odmowa 404`() = refusedOnce(ServerReply.Missing, code = 404)

    @Test
    fun `br26 odmowa 413`() = refusedOnce(ServerReply.Fault(code = 413), code = 413)

    @Test
    fun `br26 awaria serwera ponawia`() =
        runBlocking {
            queue.save(precise(), photo())
            store.createResults.addLast(ServerReply.Fault(code = 500))

            assertFalse(queue.sendAll())
            assertTrue(queue.sendAll())

            assertEquals(2, creates)
            assertEquals(OwnSubmissionState.SENT, history.rows.getValue(ID).state)
        }

    @Test
    fun `br26 brak sieci ponawia`() =
        runBlocking {
            queue.save(precise(), photo())
            store.createResults.addLast(ServerReply.Offline)

            assertFalse(queue.sendAll())
            assertEquals(OwnSubmissionState.LOCAL, history.rows.getValue(ID).state)
            assertTrue(queue.sendAll())

            assertEquals(2, creates)
        }

    @Test
    fun `br26 401 czeka na zalogowanie`() =
        runBlocking {
            queue.save(precise(), photo())
            store.createResults.addLast(ServerReply.Fault(code = 401))

            queue.sendAll()
            queue.sendAll()
            assertEquals(1, creates)
            assertTrue(queue.loginExpired.value)
            assertEquals(OwnSubmissionState.LOCAL, history.rows.getValue(ID).state)

            queue.onLoggedIn()
            queue.sendAll()
            assertEquals(2, creates)
        }

    @Test
    fun `us9_5 ponowny zapis wysyła`() =
        runBlocking {
            queue.save(precise(), photo())
            store.createResults.addLast(ServerReply.Fault(code = 400))
            queue.sendAll()

            queue.saveEdit(EditedSubmission(id = ID, baseVersion = null), precise().copy(severity = 1), photo())
            assertEquals(OwnSubmissionState.LOCAL, history.rows.getValue(ID).state)
            queue.sendAll()

            assertEquals(2, creates)
            assertEquals(OwnSubmissionState.SENT, history.rows.getValue(ID).state)
        }

    private fun refusedOnce(result: ServerReply<Submission>, code: Int) =
        runBlocking {
            queue.save(precise(), photo())
            store.createResults.addLast(result)

            queue.sendAll()
            queue.sendAll()

            assertEquals(1, creates)
            val row = history.rows.getValue(ID)
            assertEquals(OwnSubmissionState.REFUSED, row.state)
            assertEquals(code, row.refusalCode)
            val left = store.rows.single()
            assertTrue(left.submission.isRefused)
        }

    private fun server(id: String, content: SubmissionContent): Submission =
        Submission(
            id = id,
            version = 2,
            status = SubmissionStatus.SENT,
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            content = content,
        )

    private fun photo(): List<PendingPhotoDraft> {
        val file = tempDir.resolve("$PHOTO.jpg")
        file.writeBytes(byteArrayOf(1, 2, 3))
        return listOf(PendingPhotoDraft(photoId = PHOTO, path = file.path))
    }

    private fun precise(): SubmissionContent =
        SubmissionContent(
            kind = SubmissionKind.PRECISE,
            location = GeoPoint(longitude = 17.06209, latitude = 51.10785),
            photoIds = listOf(PHOTO),
            barrierType = "TB-5",
            severity = 2,
        )

    private companion object {
        const val ID = "s-odmowa"
        const val PHOTO = "p-odmowa"
    }
}
