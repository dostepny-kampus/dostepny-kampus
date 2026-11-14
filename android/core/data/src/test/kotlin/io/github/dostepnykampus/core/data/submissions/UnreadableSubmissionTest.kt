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
import io.github.dostepnykampus.core.database.OwnSubmissionState
import io.github.dostepnykampus.core.database.PendingEditDao
import io.github.dostepnykampus.core.database.PendingSubmissionWithPhotos
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Zgłoszenie, którego telefon nie odczyta, zostaje „Nie wysłano” bez kodu odmowy, a kolejka wysyła następne.
 *
 * Wymagania: BR-26, US9.5, NFR-5
 */
class UnreadableSubmissionTest {
    @TempDir
    lateinit var tempDir: File

    private val store = FakeStore()
    private val history = FakeHistory()
    private val created = mutableListOf<String>()
    private val ids = ArrayDeque(listOf(FIRST, SECOND))
    private val queue by lazy {
        SubmissionQueue(
            store = store,
            ioContext = EmptyCoroutineContext,
            server =
                SubmissionServer(
                    create = { id, content ->
                        created += id
                        ServerReply.Ok(server(id, content))
                    },
                    putPhoto = { _, _, _ -> ServerReply.Ok(Unit) },
                    update = { id, _, content -> ServerReply.Ok(ServerEdit.Saved(server(id, content))) },
                ),
            stamps = SubmissionStamps(newId = { ids.removeFirst() }),
            history = history,
        )
    }

    @Test
    fun `br26 brak pliku zdjęcia`() =
        runBlocking {
            val lost = photo(tempDir, "p1")
            queue.save(precise("p1"), lost)
            queue.save(precise("p2"), photo(tempDir, "p2"))
            File(lost.single().path).delete()

            assertTrue(queue.sendAll())

            assertEquals(listOf(FIRST, SECOND), created)
            assertUnreadable()
        }

    @Test
    fun `br26 nieznana wartość w bazie`() =
        runBlocking {
            queue.save(precise("p1"), photo(tempDir, "p1"))
            queue.save(precise("p2"), photo(tempDir, "p2"))
            val old = store.rows.first { it.submission.id == FIRST }
            store.rows[store.rows.indexOf(old)] = old.copy(submission = old.submission.copy(barrierGroup = "STARA"))

            assertTrue(queue.sendAll())

            assertEquals(listOf(SECOND), created)
            assertUnreadable()
            val dao =
                object : PendingEditDao {
                    override suspend fun find(id: String): PendingSubmissionWithPhotos? =
                        store.rows.find { it.submission.id == id }
                }
            assertNull(pendingEditsOf(dao).load(FIRST))
        }

    private fun assertUnreadable() {
        val first = history.rows.getValue(FIRST)
        assertEquals(OwnSubmissionState.REFUSED, first.state)
        assertNull(first.refusalCode)
        assertEquals(OwnSubmissionState.SENT, history.rows.getValue(SECOND).state)
        val left = store.rows.single()
        assertTrue(left.submission.isRefused)
    }

    private companion object {
        const val FIRST = "s-stare"
        const val SECOND = "s-nowe"
    }
}
