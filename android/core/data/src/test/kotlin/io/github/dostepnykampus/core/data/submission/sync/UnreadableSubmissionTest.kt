/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submission.sync

import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.data.submission.pending.pendingEditsOf
import io.github.dostepnykampus.core.data.submission.sync.SubmissionFixtures.acceptingServer
import io.github.dostepnykampus.core.data.submission.sync.SubmissionFixtures.photo
import io.github.dostepnykampus.core.data.submission.sync.SubmissionFixtures.precise
import io.github.dostepnykampus.core.data.submission.sync.SubmissionFixtures.server
import io.github.dostepnykampus.core.database.submission.OwnSubmissionState
import io.github.dostepnykampus.core.database.submission.PendingEditDao
import io.github.dostepnykampus.core.database.submission.PendingSubmissionWithPhotos
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
                acceptingServer { id, content ->
                    created += id
                    ServerReply.Ok(server(id, content))
                },
            stamps = SubmissionStamps(newId = { ids.removeFirst() }),
            history = history,
        )
    }

    @Test
    fun reportsMissingPhotoFile() =
        runBlocking {
            val lost = photo(tempDir, "p1")
            queue.save(precise("p1"), lost)
            queue.save(precise("p2"), photo(tempDir, "p2"))
            File(lost.single().path).delete()

            assertTrue(queue.sendAll(), "przebieg wysyłki kończy się powodzeniem")

            assertEquals(listOf(FIRST, SECOND), created, "oba zgłoszenia próbują iść na serwer")
            assertUnreadable()
        }

    @Test
    fun reportsUnknownStoredValue() =
        runBlocking {
            queue.save(precise("p1"), photo(tempDir, "p1"))
            queue.save(precise("p2"), photo(tempDir, "p2"))
            val old = store.rows.first { it.submission.id == FIRST }
            store.rows[store.rows.indexOf(old)] = old.copy(submission = old.submission.copy(barrierGroup = "STARA"))

            assertTrue(queue.sendAll(), "przebieg wysyłki kończy się powodzeniem")

            assertEquals(listOf(SECOND), created, "nieczytelne zgłoszenie nie idzie na serwer")
            assertUnreadable()
            val dao =
                object : PendingEditDao {
                    override suspend fun find(id: String): PendingSubmissionWithPhotos? =
                        store.rows.find { it.submission.id == id }
                }
            assertNull(pendingEditsOf(dao).load(FIRST), "nieczytelnego zgłoszenia nie da się poprawić")
        }

    private fun assertUnreadable() {
        val first = history.rows.getValue(FIRST)
        assertEquals(OwnSubmissionState.REFUSED, first.state, "nieczytelne zgłoszenie jest odrzucone")
        assertNull(first.refusalCode, "nieczytelne zgłoszenie nie ma kodu odmowy")
        assertEquals(OwnSubmissionState.SENT, history.rows.getValue(SECOND).state, "kolejne zgłoszenie jest wysłane")
        val left = store.rows.single()
        assertTrue(left.submission.isRefused, "nieczytelne zgłoszenie zostaje w kolejce jako odrzucone")
    }

    private companion object {
        const val FIRST = "s-stare"
        const val SECOND = "s-nowe"
    }
}
