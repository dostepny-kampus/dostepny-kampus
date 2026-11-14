/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submissions

import io.github.dostepnykampus.core.data.server.ServerEdit
import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.Submission
import io.github.dostepnykampus.core.model.SubmissionContent
import io.github.dostepnykampus.core.model.SubmissionKind
import io.github.dostepnykampus.core.model.SubmissionStatus
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.Instant
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Poprawka zgłoszenia, które serwer już ma, nie idzie jako drugie utworzenie z tym samym identyfikatorem: idzie
 * jako edycja z wersją z kolejki albo historii, a bez znanej wersji zapis jest odrzucony.
 *
 * Wymagania: US8.3, BR-9, BR-10
 */
class EditBaseVersionTest {
    @TempDir
    lateinit var tempDir: File

    private val store = FakeStore()
    private val history = FakeHistory()
    private val creates = mutableListOf<String>()
    private val edits = mutableListOf<Pair<String, Int>>()

    @Test
    fun `us8_3 treść na serwerze`() =
        runBlocking {
            val target = queue(history)
            target.save(precise(), photo())
            store.photoResults.addLast(ServerReply.Offline)
            target.sendAll()

            target.saveEdit(EditedSubmission(id = ID, baseVersion = null), precise().copy(severity = 1), photo())
            target.sendAll()

            assertEquals(listOf(ID), creates)
            assertEquals(listOf(ID to SERVER_VERSION), edits)
        }

    @Test
    fun `us8_3 wysłane to edycja`() =
        runBlocking {
            val target = queue(history)
            target.save(precise(), photo())
            target.sendAll()

            target.saveEdit(EditedSubmission(id = ID, baseVersion = null), precise().copy(severity = 1), photo())
            target.sendAll()

            assertEquals(listOf(ID), creates)
            assertEquals(listOf(ID to SERVER_VERSION), edits)
        }

    @Test
    fun `br10 bez wersji odrzucone`() =
        runBlocking {
            val target = queue(history = null)
            target.save(precise(), photo())
            store.photoResults.addLast(ServerReply.Offline)
            target.sendAll()

            val refused =
                assertThrows<SaveRefusedException> {
                    runBlocking { target.saveEdit(EditedSubmission(ID, baseVersion = null), precise(), photo()) }
                }
            assertEquals(SaveRefusal.UNKNOWN_VERSION, refused.reason)
            target.sendAll()

            assertEquals(listOf(ID), creates)
            assertEquals(emptyList<Pair<String, Int>>(), edits)
        }

    @Test
    fun `br9 tylko w telefonie`() =
        runBlocking {
            val target = queue(history)
            target.save(precise(), photo())
            target.saveEdit(EditedSubmission(id = ID, baseVersion = null), precise().copy(severity = 1), photo())

            target.sendAll()

            assertEquals(listOf(ID), creates)
            assertEquals(emptyList<Pair<String, Int>>(), edits)
        }

    private fun queue(history: FakeHistory?): SubmissionQueue =
        SubmissionQueue(
            store = store,
            ioContext = EmptyCoroutineContext,
            server =
                SubmissionServer(
                    create = { id, content ->
                        creates.add(id)
                        ServerReply.Ok(server(id, content))
                    },
                    putPhoto = { _, _, _ -> store.photoResults.removeFirstOrNull() ?: ServerReply.Ok(Unit) },
                    update = { id, version, content ->
                        edits.add(id to version)
                        ServerReply.Ok(ServerEdit.Saved(server(id, content)))
                    },
                ),
            stamps = SubmissionStamps(newId = { ID }),
            history = history,
        )

    private fun server(id: String, content: SubmissionContent): Submission =
        Submission(
            id = id,
            version = SERVER_VERSION,
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
        const val ID = "s-poprawka"
        const val PHOTO = "p-poprawka"
        const val SERVER_VERSION = 2
    }
}
