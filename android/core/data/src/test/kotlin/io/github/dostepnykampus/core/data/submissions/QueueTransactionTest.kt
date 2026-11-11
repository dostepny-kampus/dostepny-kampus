/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submissions

import io.github.dostepnykampus.core.database.OwnSubmissionDao
import io.github.dostepnykampus.core.database.OwnSubmissionEntity
import io.github.dostepnykampus.core.database.OwnSubmissionState
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.Submission
import io.github.dostepnykampus.core.model.SubmissionContent
import io.github.dostepnykampus.core.model.SubmissionKind
import io.github.dostepnykampus.core.model.SubmissionStatus
import io.github.dostepnykampus.core.network.ApiResult
import io.github.dostepnykampus.core.network.SubmissionEdit
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.Instant
import kotlin.coroutines.EmptyCoroutineContext

/** Błąd zapisu historii cofa zapis kolejki z tej samej pary, więc tabele nie zostają w sprzeczności (#859). */
class QueueTransactionTest {
    @TempDir
    lateinit var tempDir: File

    private val history = FakeHistory()
    private val store = FakeStore().also { it.history = history }
    private var failing = ""

    @Test
    fun `nfr5 zapis bez historii`() =
        runBlocking {
            failing = "save"

            assertThrows<IllegalStateException> { runBlocking { queue().save(precise(), photo()) } }

            assertEquals(emptyList<Any>(), store.rows)
        }

    @Test
    fun `br10 utworzenie bez wersji`() =
        runBlocking {
            queue().save(precise(), photo())
            failing = "saveVersion"

            assertThrows<IllegalStateException> { runBlocking { queue().sendAll() } }

            val row = store.rows.single()
            assertFalse(row.submission.isCreatedOnServer)
        }

    @Test
    fun `us8_1 usunięcie bez wysłania`() =
        runBlocking {
            queue().save(precise(), photo())
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
                    create = { id, content -> ApiResult.Success(server(id, content)) },
                    putPhoto = { _, _, _ -> ApiResult.Success(Unit) },
                    update = { id, _, content -> ApiResult.Success(SubmissionEdit.Saved(server(id, content))) },
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

    private fun server(id: String, content: SubmissionContent): Submission =
        Submission(
            id = id,
            version = 1,
            status = SubmissionStatus.SENT,
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            content = content,
        )

    private fun photo(): List<PendingPhotoDraft> {
        val file = tempDir.resolve("$PHOTO.jpg")
        file.writeBytes(byteArrayOf(1, 2, 3))
        return listOf(PendingPhotoDraft(photoId = PHOTO, path = file.path))
    }

    private fun precise(): SubmissionContent =
        SubmissionContent(
            kind = SubmissionKind.PRECISE,
            location = GeoPoint(longitude = 17.06209, latitude = 51.10785),
            photoIds = listOf(PHOTO),
            barrierType = "TB-5",
            severity = 2,
        )

    private companion object {
        const val ID = "s-transakcja"
        const val PHOTO = "p-transakcja"
    }
}
