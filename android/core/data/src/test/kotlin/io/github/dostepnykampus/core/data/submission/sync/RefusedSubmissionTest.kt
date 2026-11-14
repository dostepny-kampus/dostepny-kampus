/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submission.sync

import io.github.dostepnykampus.core.data.server.ServerEdit
import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.data.submission.sync.SubmissionFixtures.photo
import io.github.dostepnykampus.core.data.submission.sync.SubmissionFixtures.precise
import io.github.dostepnykampus.core.data.submission.sync.SubmissionFixtures.server
import io.github.dostepnykampus.core.database.submission.OwnSubmissionState
import io.github.dostepnykampus.core.model.Submission
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
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
                        store.createResults.removeFirstOrNull() ?: ServerReply.Ok(server(id, content, version = 2))
                    },
                    putPhoto = { _, _, _ -> ServerReply.Ok(Unit) },
                    update = { id, _, content -> ServerReply.Ok(ServerEdit.Saved(server(id, content, version = 2))) },
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
            queue.save(precise(PHOTO), photo(tempDir, PHOTO))
            store.createResults.addLast(ServerReply.Fault(code = 500))

            assertFalse(queue.sendAll())
            assertTrue(queue.sendAll())

            assertEquals(2, creates)
            assertEquals(OwnSubmissionState.SENT, history.rows.getValue(ID).state)
        }

    @Test
    fun `br26 brak sieci ponawia`() =
        runBlocking {
            queue.save(precise(PHOTO), photo(tempDir, PHOTO))
            store.createResults.addLast(ServerReply.Offline)

            assertFalse(queue.sendAll())
            assertEquals(OwnSubmissionState.LOCAL, history.rows.getValue(ID).state)
            assertTrue(queue.sendAll())

            assertEquals(2, creates)
        }

    @Test
    fun `br26 401 czeka na zalogowanie`() =
        runBlocking {
            queue.save(precise(PHOTO), photo(tempDir, PHOTO))
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
            queue.save(precise(PHOTO), photo(tempDir, PHOTO))
            store.createResults.addLast(ServerReply.Fault(code = 400))
            queue.sendAll()

            queue.saveEdit(
                EditedSubmission(id = ID, baseVersion = null),
                precise(PHOTO, severity = 1),
                photo(tempDir, PHOTO),
            )
            assertEquals(OwnSubmissionState.LOCAL, history.rows.getValue(ID).state)
            queue.sendAll()

            assertEquals(2, creates)
            assertEquals(OwnSubmissionState.SENT, history.rows.getValue(ID).state)
        }

    private fun refusedOnce(result: ServerReply<Submission>, code: Int) =
        runBlocking {
            queue.save(precise(PHOTO), photo(tempDir, PHOTO))
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

    private companion object {
        const val ID = "s-odmowa"
        const val PHOTO = "p-odmowa"
    }
}
