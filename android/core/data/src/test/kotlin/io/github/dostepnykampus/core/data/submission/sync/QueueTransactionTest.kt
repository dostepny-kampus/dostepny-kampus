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
import io.github.dostepnykampus.core.database.submission.OwnSubmissionDao
import io.github.dostepnykampus.core.database.submission.OwnSubmissionEntity
import io.github.dostepnykampus.core.database.submission.OwnSubmissionState
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Błąd zapisu historii cofa zapis kolejki z tej samej pary, więc tabele nie zostają w sprzeczności.
 *
 * Wymagania: NFR-5, BR-10, US8.1
 */
class QueueTransactionTest {
    @TempDir
    lateinit var tempDir: File

    private val history = FakeHistory()
    private val store = FakeStore().also { it.history = history }
    private var failing = ""

    @Test
    fun rollsBackWithoutHistory() =
        runBlocking {
            failing = "save"

            assertThrows<IllegalStateException> { runBlocking { queue().save(precise(PHOTO), photo(tempDir, PHOTO)) } }

            assertEquals(emptyList<Any>(), store.rows)
        }

    @Test
    fun createsWithoutVersion() =
        runBlocking {
            queue().save(precise(PHOTO), photo(tempDir, PHOTO))
            failing = "saveVersion"

            assertThrows<IllegalStateException> { runBlocking { queue().sendAll() } }

            val row = store.rows.single()
            assertFalse(row.submission.isCreatedOnServer)
        }

    @Test
    fun removesWithoutSending() =
        runBlocking {
            queue().save(precise(PHOTO), photo(tempDir, PHOTO))
            failing = "markSent"

            assertThrows<IllegalStateException> { runBlocking { queue().sendAll() } }

            val row = store.rows.single()
            assertEquals(ID, row.submission.id)
            assertEquals(OwnSubmissionState.LOCAL, history.rows.getValue(ID).state)
        }

    private fun queue(): SubmissionQueue =
        SubmissionQueue(
            store = store,
            ioContext = EmptyCoroutineContext,
            server =
                SubmissionServer(
                    create = { id, content -> ServerReply.Ok(server(id, content)) },
                    putPhoto = { _, _, _ -> ServerReply.Ok(Unit) },
                    update = { id, _, content -> ServerReply.Ok(ServerEdit.Saved(server(id, content))) },
                ),
            stamps = SubmissionStamps(newId = { ID }),
            history = FailingHistory(),
        )

    /** Historia, której wybrany zapis [failing] kończy się błędem, np. po zabiciu procesu. */
    private inner class FailingHistory : OwnSubmissionDao by history {
        override suspend fun save(submission: OwnSubmissionEntity) {
            check(failing != "save")
            history.save(submission)
        }

        override suspend fun saveVersion(id: String, version: Int) {
            check(failing != "saveVersion")
            history.saveVersion(id, version)
        }

        override suspend fun markSent(id: String) {
            check(failing != "markSent")
            history.markSent(id)
        }
    }

    private companion object {
        const val ID = "s-transakcja"
        const val PHOTO = "p-transakcja"
    }
}
