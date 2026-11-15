/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submission.sync

import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.data.submission.sync.SubmissionFixtures.photo
import io.github.dostepnykampus.core.data.submission.sync.SubmissionFixtures.precise
import io.github.dostepnykampus.core.data.submission.sync.SubmissionFixtures.server
import io.github.dostepnykampus.core.database.submission.OwnSubmissionState
import io.github.dostepnykampus.core.model.submission.SubmissionContent
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
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
    fun sendsOnResave() =
        runBlocking {
            val queue = queue()
            queue.save(precise(FIRST, severity = 2), photo(tempDir, FIRST))
            store.createResults.addLast(ServerReply.Fault(code = BAD_REQUEST))
            queue.sendAll()
            assertEquals(OwnSubmissionState.REFUSED, history.rows.getValue(ID).state)
            queue.sendAll()
            assertEquals(1, sent.size)

            val before = scheduled
            queue.saveEdit(
                edited = EditedSubmission(id = ID, baseVersion = null),
                content = precise(SECOND, severity = 1),
                photos = photo(tempDir, SECOND),
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
}

private const val ID = "s1"
private const val FIRST = "p1"
private const val SECOND = "p2"
private const val BAD_REQUEST = 400
