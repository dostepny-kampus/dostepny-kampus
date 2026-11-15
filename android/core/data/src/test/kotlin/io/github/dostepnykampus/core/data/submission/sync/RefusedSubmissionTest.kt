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
import io.github.dostepnykampus.core.model.submission.Submission
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
    fun refusesOn400() = refusedOnce(ServerReply.Fault(code = 400), code = 400)

    @Test
    fun refusesOn403() = refusedOnce(ServerReply.Fault(code = 403), code = 403)

    @Test
    fun refusesOn404() = refusedOnce(ServerReply.Missing, code = 404)

    @Test
    fun refusesOn413() = refusedOnce(ServerReply.Fault(code = 413), code = 413)

    @Test
    fun retriesOnServerFault() =
        runBlocking {
            queue.save(precise(PHOTO), photo(tempDir, PHOTO))
            store.createResults.addLast(ServerReply.Fault(code = 500))

            assertFalse(queue.sendAll(), "pierwszy przebieg kończy się odmową")
            assertTrue(queue.sendAll(), "drugi przebieg kończy się powodzeniem")

            assertEquals(2, creates, "zgłoszenie idzie na serwer drugi raz")
            assertEquals(OwnSubmissionState.SENT, history.rows.getValue(ID).state, "zgłoszenie jest wysłane")
        }

    @Test
    fun retriesOffline() =
        runBlocking {
            queue.save(precise(PHOTO), photo(tempDir, PHOTO))
            store.createResults.addLast(ServerReply.Offline)

            assertFalse(queue.sendAll(), "pierwszy przebieg kończy się odmową")
            assertEquals(
                OwnSubmissionState.LOCAL,
                history.rows.getValue(ID).state,
                "zgłoszenie wraca do stanu lokalnego",
            )
            assertTrue(queue.sendAll(), "drugi przebieg kończy się powodzeniem")

            assertEquals(2, creates, "zgłoszenie idzie na serwer drugi raz")
        }

    @Test
    fun waitsForSignInOn401() =
        runBlocking {
            queue.save(precise(PHOTO), photo(tempDir, PHOTO))
            store.createResults.addLast(ServerReply.Fault(code = 401))

            queue.sendAll()
            queue.sendAll()
            assertEquals(1, creates, "zgłoszenie idzie na serwer raz")
            assertTrue(queue.loginExpired.value, "wygasłe logowanie jest zgłoszone")
            assertEquals(
                OwnSubmissionState.LOCAL,
                history.rows.getValue(ID).state,
                "zgłoszenie zostaje lokalne",
            )

            queue.onLoggedIn()
            queue.sendAll()
            assertEquals(2, creates, "zgłoszenie idzie na serwer drugi raz")
        }

    @Test
    fun sendsAgainOnResave() =
        runBlocking {
            queue.save(precise(PHOTO), photo(tempDir, PHOTO))
            store.createResults.addLast(ServerReply.Fault(code = 400))
            queue.sendAll()

            queue.saveEdit(
                EditedSubmission(id = ID, baseVersion = null),
                precise(PHOTO, severity = 1),
                photo(tempDir, PHOTO),
            )
            assertEquals(
                OwnSubmissionState.LOCAL,
                history.rows.getValue(ID).state,
                "zgłoszenie zostaje lokalne",
            )
            queue.sendAll()

            assertEquals(2, creates, "zgłoszenie idzie na serwer drugi raz")
            assertEquals(OwnSubmissionState.SENT, history.rows.getValue(ID).state, "zgłoszenie jest wysłane")
        }

    private fun refusedOnce(result: ServerReply<Submission>, code: Int) =
        runBlocking {
            queue.save(precise(PHOTO), photo(tempDir, PHOTO))
            store.createResults.addLast(result)

            queue.sendAll()
            queue.sendAll()

            assertEquals(1, creates, "zgłoszenie idzie na serwer raz")
            val row = history.rows.getValue(ID)
            assertEquals(OwnSubmissionState.REFUSED, row.state, "zgłoszenie jest odrzucone")
            assertEquals(code, row.refusalCode, "kod odmowy z serwera")
            val left = store.rows.single()
            assertTrue(left.submission.isRefused, "odrzucone zgłoszenie zostaje w kolejce")
        }

    private companion object {
        const val ID = "s-odmowa"
        const val PHOTO = "p-odmowa"
    }
}
