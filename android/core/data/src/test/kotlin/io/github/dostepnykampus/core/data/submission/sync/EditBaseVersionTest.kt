/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submission.sync

import io.github.dostepnykampus.core.data.server.ServerEdit
import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.data.submission.sync.SubmissionFixtures.photo
import io.github.dostepnykampus.core.data.submission.sync.SubmissionFixtures.precise
import io.github.dostepnykampus.core.data.submission.sync.SubmissionFixtures.server
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
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
    fun keepsServerContent() =
        runBlocking {
            val target = queue(history)
            target.save(precise(PHOTO), photo(tempDir, PHOTO))
            store.photoResults.addLast(ServerReply.Offline)
            target.sendAll()

            target.saveEdit(
                EditedSubmission(id = ID, baseVersion = null),
                precise(PHOTO, severity = 1),
                photo(tempDir, PHOTO),
            )
            target.sendAll()

            assertEquals(listOf(ID), creates, "zgłoszenie utworzone raz")
            assertEquals(listOf(ID to SERVER_VERSION), edits, "edycja idzie od wersji z serwera")
        }

    @Test
    fun editsSent() =
        runBlocking {
            val target = queue(history)
            target.save(precise(PHOTO), photo(tempDir, PHOTO))
            target.sendAll()

            target.saveEdit(
                EditedSubmission(id = ID, baseVersion = null),
                precise(PHOTO, severity = 1),
                photo(tempDir, PHOTO),
            )
            target.sendAll()

            assertEquals(listOf(ID), creates, "wysłane zgłoszenie powstało na serwerze raz")
            assertEquals(listOf(ID to SERVER_VERSION), edits, "poprawka wysłanego zgłoszenia idzie jako edycja")
        }

    @Test
    fun rejectsWithoutVersion() =
        runBlocking {
            val target = queue(history = null)
            target.save(precise(PHOTO), photo(tempDir, PHOTO))
            store.photoResults.addLast(ServerReply.Offline)
            target.sendAll()

            val refused =
                assertThrows<SaveRefusedException> {
                    runBlocking {
                        target.saveEdit(EditedSubmission(ID, baseVersion = null), precise(PHOTO), photo(tempDir, PHOTO))
                    }
                }
            assertEquals(SaveRefusal.UNKNOWN_VERSION, refused.reason, "zapis bez wersji jest odrzucony")
            target.sendAll()

            assertEquals(listOf(ID), creates, "zgłoszenie utworzone raz")
            assertEquals(emptyList<Pair<String, Int>>(), edits, "edycja nie idzie na serwer")
        }

    @Test
    fun keepsPhoneOnly() =
        runBlocking {
            val target = queue(history)
            target.save(precise(PHOTO), photo(tempDir, PHOTO))
            target.saveEdit(
                EditedSubmission(id = ID, baseVersion = null),
                precise(PHOTO, severity = 1),
                photo(tempDir, PHOTO),
            )

            target.sendAll()

            assertEquals(listOf(ID), creates, "zgłoszenie utworzone raz")
            assertEquals(emptyList<Pair<String, Int>>(), edits, "edycja nie idzie na serwer")
        }

    private fun queue(history: FakeHistory?): SubmissionQueue =
        SubmissionQueue(
            store = store,
            ioContext = EmptyCoroutineContext,
            server =
                submissionServer(
                    create = { id, content ->
                        creates.add(id)
                        ServerReply.Ok(server(id, content, SERVER_VERSION))
                    },
                    putPhoto = { _, _, _ -> store.photoResults.removeFirstOrNull() ?: ServerReply.Ok(Unit) },
                    update = { id, version, content ->
                        edits.add(id to version)
                        ServerReply.Ok(ServerEdit.Saved(server(id, content, SERVER_VERSION)))
                    },
                ),
            stamps = SubmissionStamps(newId = { ID }),
            records = SentRecords(history = history),
        )

    private companion object {
        const val ID = "s-poprawka"
        const val PHOTO = "p-poprawka"
        const val SERVER_VERSION = 2
    }
}
