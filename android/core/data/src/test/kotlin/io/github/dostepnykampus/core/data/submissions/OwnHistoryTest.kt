/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submissions

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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException
import java.time.Instant
import kotlin.coroutines.EmptyCoroutineContext

/** Kolejka zapisuje stan wysyłki własnych zgłoszeń dla listy C3 (US8, NFR-5). */
class OwnHistoryTest {
    @TempDir
    lateinit var tempDir: File

    private val store = FakeStore()
    private val history = FakeHistory()

    @Test
    fun `us8 zapis to W telefonie`() =
        runBlocking {
            queue().save(precise(), photo())

            val row = history.rows.getValue(ID)
            assertEquals(OwnSubmissionState.LOCAL, row.state)
            assertEquals("TB-5", row.barrierType)
        }

    @Test
    fun `us4_4 szybkie poza listą`() =
        runBlocking {
            queue().save(precise().copy(kind = SubmissionKind.QUICK), photo())

            assertTrue(history.rows.isEmpty())
        }

    @Test
    fun `nfr5 wysłane z wersją`() =
        runBlocking {
            val target = queue()
            target.save(precise(), photo())
            target.sendAll()

            val row = history.rows.getValue(ID)
            assertEquals(OwnSubmissionState.SENT, row.state)
            assertEquals(3, row.serverVersion)
        }

    @Test
    fun `nfr5 bez sieci zostaje lokalne`() =
        runBlocking {
            val target = queue()
            target.save(precise(), photo())
            store.createResults.addLast(ApiResult.NetworkError(IOException("sieć")))
            target.sendAll()

            assertEquals(OwnSubmissionState.LOCAL, history.rows.getValue(ID).state)
        }

    @Test
    fun `nfr5 odmowa z kodem`() =
        runBlocking {
            val target = queue()
            target.save(precise(), photo())
            store.createResults.addLast(ApiResult.ServerError(code = 422))
            target.sendAll()

            val row = history.rows.getValue(ID)
            assertEquals(OwnSubmissionState.REFUSED, row.state)
            assertEquals(422, row.refusalCode)
        }

    @Test
    fun `us8_3 poprawka znów lokalna`() =
        runBlocking {
            val target = queue()
            target.save(precise(), photo())
            target.sendAll()
            target.saveEdit(EditedSubmission(id = ID, baseVersion = 3), precise().copy(severity = 1), photo())

            val row = history.rows.getValue(ID)
            assertEquals(OwnSubmissionState.LOCAL, row.state)
            assertEquals(1, row.severity)
            assertNull(row.serverVersion)
        }

    private fun queue(): SubmissionQueue =
        SubmissionQueue(
            store = store,
            ioContext = EmptyCoroutineContext,
            server =
                SubmissionServer(
                    create = { id, content -> store.createResults.removeFirstOrNull() ?: accepted(id, content) },
                    putPhoto = { _, _, _ -> ApiResult.Success(Unit) },
                    update = { id, _, content -> ApiResult.Success(SubmissionEdit.Saved(server(id, content))) },
                ),
            newId = { ID },
            history = history,
        )

    private fun accepted(id: String, content: SubmissionContent): ApiResult<Submission> =
        ApiResult.Success(server(id, content))

    private fun server(id: String, content: SubmissionContent): Submission =
        Submission(
            id = id,
            version = 3,
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
}

private const val ID = "s1"
private const val PHOTO = "p1"
