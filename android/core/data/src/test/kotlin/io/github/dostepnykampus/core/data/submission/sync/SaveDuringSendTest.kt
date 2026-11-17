/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submission.sync

import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.data.submission.sync.SubmissionFixtures.acceptingServer
import io.github.dostepnykampus.core.data.submission.sync.SubmissionFixtures.photo
import io.github.dostepnykampus.core.data.submission.sync.SubmissionFixtures.precise
import io.github.dostepnykampus.core.data.submission.sync.SubmissionFixtures.server
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
            subject = { "konto-testowe" },
            ioContext = EmptyCoroutineContext,
            server =
                acceptingServer { id, content ->
                    created += id
                    val pending = saveDuringSend
                    saveDuringSend = null
                    pending?.invoke()
                    ServerReply.Ok(server(id, content))
                },
            stamps = SubmissionStamps(newId = { ids.removeFirst() }),
        )
    }

    @Test
    fun savesDuringSend() =
        runBlocking {
            queue.save(precise("p1"), photo(tempDir, "p1"))
            saveDuringSend = { queue.save(precise("p2"), photo(tempDir, "p2")) }

            assertTrue(queue.sendAll(), "przebieg wysyłki kończy się powodzeniem")

            assertEquals(listOf(FIRST, SECOND), created, "oba zgłoszenia idą w jednym przebiegu")
            assertEquals(emptyList<Any>(), store.rows, "kolejka po wysyłce jest pusta")
        }

    private companion object {
        const val FIRST = "s-pierwsze"
        const val SECOND = "s-w-trakcie"
    }
}
