/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submissions

import io.github.dostepnykampus.core.database.OwnSubmissionState
import io.github.dostepnykampus.core.database.PendingEditDao
import io.github.dostepnykampus.core.database.PendingSubmissionWithPhotos
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.Submission
import io.github.dostepnykampus.core.model.SubmissionContent
import io.github.dostepnykampus.core.model.SubmissionKind
import io.github.dostepnykampus.core.model.SubmissionStatus
import io.github.dostepnykampus.core.network.ApiResult
import io.github.dostepnykampus.core.network.SubmissionEdit
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.Instant
import kotlin.coroutines.EmptyCoroutineContext

/** Po 409 telefon pamięta wersję z serwera, a ponowna poprawka idzie od niej (US9.3, BR-10, #858). */
class ConflictEditTest {
    @TempDir
    lateinit var tempDir: File

    private val store = FakeStore()
    private val history = FakeHistory()
    private val versions = mutableListOf<Int>()
    private val edits = ArrayDeque<SubmissionEdit>()
    private val queue by lazy {
        SubmissionQueue(
            store = store,
            ioContext = EmptyCoroutineContext,
            server =
                SubmissionServer(
                    create = { id, content -> ApiResult.Success(server(id, content, version = 1)) },
                    putPhoto = { _, _, _ -> ApiResult.Success(Unit) },
                    update = { id, version, content ->
                        versions += version
                        ApiResult.Success(edits.removeFirstOrNull() ?: SubmissionEdit.Saved(server(id, content, 8)))
                    },
                ),
            history = history,
        )
    }

    @Test
    fun `us9_3 ponowna poprawka po 409`() =
        runBlocking {
            queue.saveEdit(EditedSubmission(id = ID, baseVersion = 4), precise(severity = 2), photo())
            edits.addLast(SubmissionEdit.Conflict(server(ID, precise(severity = 3), version = 7)))

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

    private fun server(id: String, content: SubmissionContent, version: Int): Submission =
        Submission(
            id = id,
            version = version,
            status = SubmissionStatus.SENT,
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            content = content,
        )

    private fun photo(): List<PendingPhotoDraft> {
        val file = tempDir.resolve("$PHOTO.jpg")
        file.writeBytes(byteArrayOf(1, 2, 3))
        return listOf(PendingPhotoDraft(photoId = PHOTO, path = file.path))
    }

    private fun precise(severity: Int): SubmissionContent =
        SubmissionContent(
            kind = SubmissionKind.PRECISE,
            location = GeoPoint(longitude = 17.06209, latitude = 51.10785),
            photoIds = listOf(PHOTO),
            barrierType = "TB-5",
            severity = severity,
        )

    private companion object {
        const val ID = "s-konflikt"
        const val PHOTO = "p-konflikt"
    }
}
