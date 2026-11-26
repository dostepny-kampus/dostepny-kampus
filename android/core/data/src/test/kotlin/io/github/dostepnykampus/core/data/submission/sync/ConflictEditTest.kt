/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submission.sync

import io.github.dostepnykampus.core.data.cache.FakeServerCopyDao
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.coroutines.EmptyCoroutineContext

/** Po 409 poprawka czeka na wybór wersji, a po wyborze idzie od wersji z serwera. Wymagania: US9.3, BR-10 */
class ConflictEditTest {
    @TempDir
    lateinit var tempDir: File

    private val store = FakeStore()
    private val history = FakeHistory()
    private val versions = mutableListOf<Int>()
    private val edits = ArrayDeque<ServerEdit>()
    private val copies = FakeServerCopyDao()
    private val queue by lazy {
        editQueue(store = store, records = SentRecords(history = history, conflicts = copies), edits = edits) {
            versions += it
        }
    }

    /** Poprawka zgłoszenia czekającego na wybór wersji jest odrzucona i nic nie zmienia. Wymagania: US9.3, BR-10 */
    @Test
    fun refusesEditWhileChoicePending() =
        runBlocking {
            queue.saveEdit(
                EditedSubmission(id = ID, baseVersion = 4),
                precise(PHOTO, severity = 2),
                photo(tempDir, PHOTO),
            )
            edits.addLast(ServerEdit.Conflict(server(ID, precise(PHOTO, severity = 3), version = 7)))
            queue.sendAll()

            val dao =
                object : PendingEditDao {
                    override suspend fun find(id: String): PendingSubmissionWithPhotos? =
                        store.rows.find { it.submission.id == id }
                }
            val reopened = checkNotNull(pendingEditsOf(dao) { "konto-testowe" }.load(ID))
            val refused =
                assertThrows<SaveRefusedException> {
                    runBlocking {
                        queue.saveEdit(reopened.edited, reopened.content.copy(severity = 1), reopened.photos)
                    }
                }
            queue.sendAll()

            assertEquals(SaveRefusal.VERSION_CHOICE_PENDING, refused.reason, "powód odmowy zapisu")
            val row = store.rows.single().submission
            assertEquals(2, row.severity, "w kolejce zostaje wersja sprzed odrzuconej poprawki")
            assertTrue(row.isRefused, "zgłoszenie dalej czeka na wybór wersji")
            assertEquals(listOf(4), versions, "odrzucona poprawka nie idzie na serwer")
            assertEquals(OwnSubmissionState.REFUSED, history.rows.getValue(ID).state, "historia bez zmian")
        }

    /** Po wyborze własnej wersji poprawka znów idzie od wersji z serwera. Wymagania: US9.3, BR-10 */
    @Test
    fun editsAgainAfterChoice() =
        runBlocking {
            queue.saveEdit(
                EditedSubmission(id = ID, baseVersion = 4),
                precise(PHOTO, severity = 2),
                photo(tempDir, PHOTO),
            )
            edits.addLast(ServerEdit.Conflict(server(ID, precise(PHOTO, severity = 3), version = 7)))
            queue.sendAll()
            copies.delete(serverVersionKey(ID))

            queue.saveEdit(
                EditedSubmission(id = ID, baseVersion = null),
                precise(PHOTO, severity = 1),
                photo(tempDir, PHOTO),
            )
            queue.sendAll()

            assertEquals(listOf(4, 7), versions, "wersje bazowe kolejnych edycji")
            assertEquals(OwnSubmissionState.SENT, history.rows.getValue(ID).state, "poprawka po wyborze jest wysłana")
        }

    /** Inna odmowa niż konflikt wersji nie blokuje poprawki: osoba ją poprawia i wysyła. Wymagania: US8.1, BR-26 */
    @Test
    fun editsAfterOtherRefusal() =
        runBlocking {
            val replies = ArrayDeque<ServerReply<ServerEdit>>(listOf(ServerReply.Fault(code = UNPROCESSABLE)))
            val other =
                SubmissionQueue(
                    store = store,
                    ioContext = EmptyCoroutineContext,
                    server =
                        submissionServer(
                            create = { id, content -> ServerReply.Ok(server(id, content, version = 1)) },
                            putPhoto = { _, _, _ -> ServerReply.Ok(Unit) },
                            update = { id, version, content ->
                                versions += version
                                val saved = ServerReply.Ok(ServerEdit.Saved(server(id, content, version = 5)))
                                replies.removeFirstOrNull() ?: saved
                            },
                        ),
                    records = SentRecords(history = history, conflicts = copies),
                )
            other.saveEdit(
                EditedSubmission(id = ID, baseVersion = 4),
                precise(PHOTO, severity = 2),
                photo(tempDir, PHOTO),
            )
            other.sendAll()
            assertTrue(store.rows.single().submission.isRefused, "odmowa 422 zostawia „Nie wysłano”")

            other.saveEdit(
                EditedSubmission(id = ID, baseVersion = 4),
                precise(PHOTO, severity = 1),
                photo(tempDir, PHOTO),
            )
            other.sendAll()

            assertEquals(listOf(4, 4), versions, "poprawka po innej odmowie idzie na serwer")
            assertEquals(OwnSubmissionState.SENT, history.rows.getValue(ID).state, "poprawka jest wysłana")
        }

    private companion object {
        const val UNPROCESSABLE = 422
        const val ID = "s-konflikt"
        const val PHOTO = "p-konflikt"
    }
}

internal fun editQueue(
    store: FakeStore,
    records: SentRecords,
    edits: ArrayDeque<ServerEdit>,
    onVersion: (Int) -> Unit = {},
): SubmissionQueue =
    SubmissionQueue(
        store = store,
        ioContext = EmptyCoroutineContext,
        server =
            submissionServer(
                create = { id, content -> ServerReply.Ok(server(id, content, version = 1)) },
                putPhoto = { _, _, _ -> ServerReply.Ok(Unit) },
                update = { id, version, content ->
                    onVersion(version)
                    ServerReply.Ok(edits.removeFirstOrNull() ?: ServerEdit.Saved(server(id, content, 8)))
                },
            ),
        records = records,
    )
