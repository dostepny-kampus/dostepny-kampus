/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submission.conflict

import io.github.dostepnykampus.core.data.cache.FakeServerCopyDao
import io.github.dostepnykampus.core.data.server.ServerEdit
import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.data.submission.sync.EditedSubmission
import io.github.dostepnykampus.core.data.submission.sync.FakeHistory
import io.github.dostepnykampus.core.data.submission.sync.FakeStore
import io.github.dostepnykampus.core.data.submission.sync.SubmissionFixtures.photo
import io.github.dostepnykampus.core.data.submission.sync.SubmissionFixtures.precise
import io.github.dostepnykampus.core.data.submission.sync.SubmissionFixtures.server
import io.github.dostepnykampus.core.data.submission.sync.SubmissionQueue
import io.github.dostepnykampus.core.data.submission.sync.SubmissionStore
import io.github.dostepnykampus.core.data.submission.sync.submissionServer
import io.github.dostepnykampus.core.database.submission.OwnSubmissionState
import io.github.dostepnykampus.core.database.submission.PendingEditDao
import io.github.dostepnykampus.core.database.submission.PendingSubmissionWithPhotos
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Po 409 telefon ma obie wersje zgłoszenia, a wybór zostawia jedną z nich.
 *
 * Wymagania: US9.3, BR-10
 */
class VersionConflictsTest {
    @TempDir
    lateinit var tempDir: File

    private val pending = FakeStore()
    private val history = FakeHistory()
    private val copies = FakeServerCopyDao()
    private val versions = mutableListOf<Int>()
    private val severities = mutableListOf<Int?>()
    private val edits = ArrayDeque<ServerEdit>()
    private var scheduled = 0
    private val store by lazy {
        SubmissionStore(
            pending = pending,
            history = history,
            edits =
                object : PendingEditDao {
                    override suspend fun find(id: String): PendingSubmissionWithPhotos? =
                        pending.rows.find { it.submission.id == id }
                },
            conflicts = copies,
        )
    }
    private val queue by lazy {
        SubmissionQueue(
            store = store,
            ioContext = EmptyCoroutineContext,
            server =
                submissionServer(
                    create = { id, content -> ServerReply.Ok(server(id, content, version = 1)) },
                    putPhoto = { _, _, _ -> ServerReply.Ok(Unit) },
                    update = { id, version, content ->
                        versions += version
                        severities += content.severity
                        ServerReply.Ok(edits.removeFirstOrNull() ?: ServerEdit.Saved(server(id, content, 8)))
                    },
                ),
        )
    }
    private val conflicts by lazy {
        StoredVersionConflicts(
            store = store,
            ioContext = EmptyCoroutineContext,
            subject = { OWNER },
            onSaved = { scheduled++ },
        )
    }

    /**
     * Scenariusz (częściowo): synchronizacja.feature: Przy konflikcie wersji aplikacja pokazuje obie wersje.
     */
    @Test
    fun showsBothVersions() =
        runBlocking {
            conflictOnSeverity()

            val conflict = checkNotNull(conflicts.find(ID)) { "konflikt po 409" }

            assertEquals(2, conflict.own.severity, "wersja własna ma uciążliwość 2")
            assertEquals(3, conflict.server.content.severity, "wersja z serwera ma uciążliwość 3")
            assertEquals(7, conflict.server.version, "wersja z serwera ma jej numer")
            assertEquals(listOf(ConflictField.SEVERITY), conflict.differences, "różni się tylko uciążliwość")
            assertEquals(listOf(4), versions, "serwer ma nadal swoją wersję")
        }

    /**
     * Scenariusz (przykład: własną): synchronizacja.feature: Przy konflikcie wersji Zbierający wybiera wersję.
     */
    @Test
    fun keepMineSendsAgain() =
        runBlocking {
            conflictOnSeverity()

            conflicts.keepMine(ID)

            assertNull(conflicts.find(ID), "po wyborze nie ma konfliktu")
            assertTrue(copies.rows.isEmpty(), "kopia wersji z serwera znika")
            assertEquals(1, scheduled, "wysyłka jest zaplanowana")
            assertEquals(OwnSubmissionState.LOCAL, history.rows.getValue(ID).state, "C3 pokazuje „W telefonie”")
            queue.sendAll()
            assertEquals(listOf(4, 7), versions, "moja wersja idzie jako edycja wersji z serwera")
            assertEquals(2, severities.last(), "serwer dostaje uciążliwość 2")
            assertEquals(OwnSubmissionState.SENT, history.rows.getValue(ID).state, "moja wersja jest wysłana")
        }

    /**
     * Scenariusz (przykład: z serwera): synchronizacja.feature: Przy konflikcie wersji Zbierający wybiera wersję.
     */
    @Test
    fun takeServerDropsMine() =
        runBlocking {
            val files = conflictOnSeverity()

            conflicts.takeServer(ID)

            assertNull(conflicts.find(ID), "po wyborze nie ma konfliktu")
            assertTrue(pending.rows.isEmpty(), "moja zmiana znika z kolejki")
            assertTrue(copies.rows.isEmpty(), "kopia wersji z serwera znika")
            assertFalse(files.single().exists(), "plik zdjęcia mojej zmiany znika")
            val row = history.rows.getValue(ID)
            assertEquals(OwnSubmissionState.SENT, row.state, "C3 pokazuje zgłoszenie z serwera")
            assertEquals(3, row.severity, "C3 ma uciążliwość z serwera")
            assertEquals(7, row.serverVersion, "C3 ma wersję z serwera")
            assertEquals(listOf(4), versions, "nic nie idzie na serwer")
        }

    /**
     * Scenariusz: synchronizacja.feature: Odłożony wybór wersji zostawia zmianę w telefonie.
     */
    @Test
    fun laterChangesNothing() =
        runBlocking {
            conflictOnSeverity()
            val before = conflicts.find(ID)
            val rows = pending.rows.toList()

            queue.sendAll()

            assertEquals(before, conflicts.find(ID), "konflikt czeka na wybór")
            assertEquals(rows, pending.rows, "zmiana zostaje w telefonie")
            assertEquals(listOf(4), versions, "odmowa nie wraca na serwer sama")
        }

    @Test
    fun otherAccountSeesNone() =
        runBlocking {
            conflictOnSeverity()
            val other =
                StoredVersionConflicts(
                    store = store,
                    ioContext = EmptyCoroutineContext,
                    subject = { "inne-konto" },
                    onSaved = { scheduled++ },
                )

            assertNull(other.find(ID), "konflikt innego konta jest niewidoczny")
            other.takeServer(ID)
            assertEquals(1, pending.rows.size, "inne konto nie usuwa cudzej zmiany")
        }

    private suspend fun conflictOnSeverity(): List<File> {
        val photos = photo(tempDir, PHOTO)
        queue.saveEdit(EditedSubmission(id = ID, baseVersion = 4), precise(PHOTO, severity = 2), photos)
        edits.addLast(ServerEdit.Conflict(server(ID, precise(PHOTO, severity = 3), version = 7)))
        queue.sendAll()
        return photos.map { File(it.path) }
    }

    private companion object {
        const val ID = "s-dwie-wersje"
        const val PHOTO = "p-dwie-wersje"
        const val OWNER = "konto-testowe"
    }
}
