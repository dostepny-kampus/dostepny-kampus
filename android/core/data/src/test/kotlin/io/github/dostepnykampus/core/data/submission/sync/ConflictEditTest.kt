/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submission.sync

import io.github.dostepnykampus.core.data.server.ServerEdit
import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.data.submission.pending.pendingEditsOf
import io.github.dostepnykampus.core.data.submission.sync.SubmissionFixtures.photo
import io.github.dostepnykampus.core.data.submission.sync.SubmissionFixtures.precise
import io.github.dostepnykampus.core.data.submission.sync.SubmissionFixtures.server
import io.github.dostepnykampus.core.database.submission.OwnSubmissionState
import io.github.dostepnykampus.core.database.submission.PendingEditDao
import io.github.dostepnykampus.core.database.submission.PendingSubmissionWithPhotos
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.coroutines.EmptyCoroutineContext

/** Po 409 telefon pamięta wersję z serwera, a ponowna poprawka idzie od niej. Wymagania: US9.3, BR-10 */
class ConflictEditTest {
    @TempDir
    lateinit var tempDir: File

    private val store = FakeStore()
    private val history = FakeHistory()
    private val versions = mutableListOf<Int>()
    private val edits = ArrayDeque<ServerEdit>()
    private val queue by lazy {
        SubmissionQueue(
            store = store,
            ioContext = EmptyCoroutineContext,
            server =
                SubmissionServer(
                    create = { id, content -> ServerReply.Ok(server(id, content, version = 1)) },
                    putPhoto = { _, _, _ -> ServerReply.Ok(Unit) },
                    update = { id, version, content ->
                        versions += version
                        ServerReply.Ok(edits.removeFirstOrNull() ?: ServerEdit.Saved(server(id, content, 8)))
                    },
                ),
            history = history,
        )
    }

    @Test
    fun editsAgainAfter409() =
        runBlocking {
            queue.saveEdit(
                EditedSubmission(id = ID, baseVersion = 4),
                precise(PHOTO, severity = 2),
                photo(tempDir, PHOTO),
            )
            edits.addLast(ServerEdit.Conflict(server(ID, precise(PHOTO, severity = 3), version = 7)))

            queue.sendAll()

            val row = history.rows.getValue(ID)
            assertEquals(OwnSubmissionState.REFUSED, row.state)
            assertEquals(409, row.refusalCode)
            assertEquals(7, row.serverVersion)

            val dao =
                object : PendingEditDao {
                    override suspend fun find(id: String): PendingSubmissionWithPhotos? =
                        store.rows.find { it.submission.id == id }
                }
            val reopened = checkNotNull(pendingEditsOf(dao).load(ID))
            assertEquals(7, reopened.edited.baseVersion)
            queue.saveEdit(reopened.edited, reopened.content.copy(severity = 1), reopened.photos)
            queue.sendAll()

            assertEquals(listOf(4, 7), versions)
            assertEquals(OwnSubmissionState.SENT, history.rows.getValue(ID).state)
        }

    private companion object {
        const val ID = "s-konflikt"
        const val PHOTO = "p-konflikt"
    }
}
