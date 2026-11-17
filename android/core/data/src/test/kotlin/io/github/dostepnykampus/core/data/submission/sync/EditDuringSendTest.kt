/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submission.sync

import io.github.dostepnykampus.core.data.server.ServerEdit
import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.data.submission.sync.SubmissionFixtures.photo
import io.github.dostepnykampus.core.data.submission.sync.SubmissionFixtures.precise
import io.github.dostepnykampus.core.data.submission.sync.SubmissionFixtures.server
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
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
                    subject = { "konto-testowe" },
                    create = { _, id, content ->
                        calls += "create ${checkNotNull(content.severity)}"
                        entered.complete(Unit)
                        reply.await()
                        ServerReply.Ok(server(id, content))
                    },
                    putPhoto = { _, _, _, _ -> ServerReply.Ok(Unit) },
                    update = { _, id, version, content ->
                        calls += "update $version ${checkNotNull(content.severity)}"
                        ServerReply.Ok(ServerEdit.Saved(server(id, content)))
                    },
                ),
            stamps = SubmissionStamps(newId = { ID }),
        )
    }

    @Test
    fun savesWithoutWaiting() = runBlocking { editWhileSendWaitsForServer() }

    @Test
    fun sendsFixAsEdit() =
        runBlocking {
            editWhileSendWaitsForServer()

            assertEquals(listOf("create 2", "update 1 3"), calls, "poprawka z czasu wysyłki idzie jako edycja")
            assertEquals(emptyList<Any>(), store.rows, "kolejka po wysyłce jest pusta")
        }

    private suspend fun editWhileSendWaitsForServer() =
        coroutineScope {
            val photos = photo(tempDir, PHOTO)
            queue.save(precise(PHOTO, severity = 2), photos)
            val send = launch { queue.sendAll() }
            entered.await()
            withTimeout(TIMEOUT_MILLIS) {
                queue.saveEdit(EditedSubmission(id = ID, baseVersion = null), precise(PHOTO, severity = 3), photos)
            }
            reply.complete(Unit)
            send.join()
        }

    private companion object {
        const val ID = "s-w-trakcie"
        const val PHOTO = "p-w-trakcie"
        const val TIMEOUT_MILLIS = 1_000L
    }
}
