/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submissions

import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.database.OwnSubmissionState
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.Submission
import io.github.dostepnykampus.core.model.SubmissionContent
import io.github.dostepnykampus.core.model.SubmissionKind
import io.github.dostepnykampus.core.model.SubmissionStatus
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.Instant
import kotlin.coroutines.EmptyCoroutineContext

/** Zgłoszenie „Nie wysłano” po odmowie 4xx wraca do wysyłki dopiero po ponownym zapisie. Wymagania: BR-26, US9.5 */
class RefusedResaveTest {
    @TempDir
    lateinit var tempDir: File

    private val store = FakeStore()
    private val history = FakeHistory()
    private val sent = mutableListOf<SubmissionContent>()
    private var scheduled = 0

    /**
     * Start od odmowy 400, potem poprawka z C3 pod tym samym identyfikatorem i kolejny przebieg wysyłki.
     * Scenariusz: synchronizacja.feature: Ponowny zapis wysyła zgłoszenie „Nie wysłano” znów
     */
    @Test
    fun `br26 ponowny zapis wysyła`() =
        runBlocking {
            val queue = queue()
            queue.save(precise(severity = 2, photo = FIRST), photo(FIRST))
            store.createResults.addLast(ServerReply.Fault(code = BAD_REQUEST))
            queue.sendAll()
            assertEquals(OwnSubmissionState.REFUSED, history.rows.getValue(ID).state)
            queue.sendAll()
            assertEquals(1, sent.size)

            val before = scheduled
            queue.saveEdit(
                edited = EditedSubmission(id = ID, baseVersion = null),
                content = precise(severity = 1, photo = SECOND),
                photos = photo(SECOND),
            )

            val local = history.rows.getValue(ID)
            assertEquals(OwnSubmissionState.LOCAL, local.state)
            assertNull(local.refusalCode)
            assertEquals(before + 1, scheduled)

            queue.sendAll()

            assertEquals(2, sent.size)
            assertEquals(1, sent.last().severity)
            assertEquals(OwnSubmissionState.SENT, history.rows.getValue(ID).state)
        }

    private fun queue(): SubmissionQueue =
        SubmissionQueue(
            store = store,
            ioContext = EmptyCoroutineContext,
            server =
                SubmissionServer(
                    create = { id, content ->
                        sent += content
                        store.createResults.removeFirstOrNull() ?: ServerReply.Ok(server(id, content))
                    },
                    putPhoto = { _, _, _ -> ServerReply.Ok(Unit) },
                    update = { _, _, _ -> ServerReply.Missing },
                ),
            stamps = SubmissionStamps(newId = { ID }),
            onSaved = { scheduled++ },
            history = history,
        )

    private fun server(id: String, content: SubmissionContent): Submission =
        Submission(
            id = id,
            version = 1,
            status = SubmissionStatus.SENT,
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            content = content,
        )

    private fun photo(id: String): List<PendingPhotoDraft> {
        val file = tempDir.resolve("$id.jpg")
        file.writeBytes(byteArrayOf(1, 2, 3))
        return listOf(PendingPhotoDraft(photoId = id, path = file.path))
    }

    private fun precise(severity: Int, photo: String): SubmissionContent =
        SubmissionContent(
            kind = SubmissionKind.PRECISE,
            location = GeoPoint(longitude = 17.06209, latitude = 51.10785),
            photoIds = listOf(photo),
            barrierType = "TB-5",
            severity = severity,
        )
}

private const val ID = "s1"
private const val FIRST = "p1"
private const val SECOND = "p2"
private const val BAD_REQUEST = 400
