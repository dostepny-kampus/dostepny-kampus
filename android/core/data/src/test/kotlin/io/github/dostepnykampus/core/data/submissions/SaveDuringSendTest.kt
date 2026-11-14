/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submissions

import io.github.dostepnykampus.core.data.server.ServerEdit
import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.data.submissions.SubmissionFixtures.photo
import io.github.dostepnykampus.core.data.submissions.SubmissionFixtures.precise
import io.github.dostepnykampus.core.data.submissions.SubmissionFixtures.server
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.coroutines.EmptyCoroutineContext

/** Zgłoszenie zapisane w trakcie wysyłki kolejki idzie w tym samym przebiegu, bez kolejnego zapisu. Wymagania: NFR-6 */
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
                        ServerReply.Ok(server(id, content))
                    },
                    putPhoto = { _, _, _ -> ServerReply.Ok(Unit) },
                    update = { id, _, content -> ServerReply.Ok(ServerEdit.Saved(server(id, content))) },
                ),
            stamps = SubmissionStamps(newId = { ids.removeFirst() }),
        )
    }

    @Test
    fun `nfr6 zapis w trakcie wysyłki`() =
        runBlocking {
            queue.save(precise("p1"), photo(tempDir, "p1"))
            saveDuringSend = { queue.save(precise("p2"), photo(tempDir, "p2")) }

            assertTrue(queue.sendAll())

            assertEquals(listOf(FIRST, SECOND), created)
            assertEquals(emptyList<Any>(), store.rows)
        }

    private companion object {
        const val FIRST = "s-pierwsze"
        const val SECOND = "s-w-trakcie"
    }
}
