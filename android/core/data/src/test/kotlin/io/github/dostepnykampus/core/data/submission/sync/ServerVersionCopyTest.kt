/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submission.sync

import io.github.dostepnykampus.core.data.cache.FakeServerCopyDao
import io.github.dostepnykampus.core.data.server.ServerEdit
import io.github.dostepnykampus.core.data.submission.sync.SubmissionFixtures.photo
import io.github.dostepnykampus.core.data.submission.sync.SubmissionFixtures.precise
import io.github.dostepnykampus.core.data.submission.sync.SubmissionFixtures.server
import io.github.dostepnykampus.core.database.cache.ServerCopyEntity
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.coroutines.EmptyCoroutineContext

/** Po 409 telefon ma całą wersję zgłoszenia z serwera obok własnej. Wymagania: US9.3, BR-10 */
class ServerVersionCopyTest {
    @TempDir
    lateinit var tempDir: File

    private val store = FakeStore()
    private val history = FakeHistory()
    private val copies = FakeServerCopyDao()
    private val edits = ArrayDeque<ServerEdit>()
    private val queue by lazy {
        editQueue(store = store, records = SentRecords(history = history, conflicts = copies), edits = edits)
    }

    @Test
    fun keepsServerVersionOn409() =
        runBlocking {
            val theirs = server(ID, precise(PHOTO, severity = 3), version = 7)
            val edited = EditedSubmission(id = ID, baseVersion = 4)
            queue.saveEdit(edited, precise(PHOTO, severity = 2), photo(tempDir, PHOTO))
            edits.addLast(ServerEdit.Conflict(theirs))

            queue.sendAll()

            val copy = serverVersionCopy(copies, ID, EmptyCoroutineContext).restore()
            assertEquals(theirs, copy?.value, "kopia ma całą wersję z serwera")
            assertEquals(2, store.rows.single().submission.severity, "wersja własna zostaje w kolejce")
        }

    @Test
    fun savedEditKeepsNoCopy() =
        runBlocking {
            queue.saveEdit(EditedSubmission(id = ID, baseVersion = 4), precise(PHOTO), photo(tempDir, PHOTO))

            queue.sendAll()

            assertTrue(copies.rows.isEmpty(), "bez konfliktu nie ma kopii wersji z serwera")
        }

    @Test
    fun brokenCopyReadsAsNone() =
        runBlocking {
            copies.save(ServerCopyEntity(key = serverVersionKey(ID), documentJson = "{", syncedAt = 0))

            assertNull(serverVersionCopy(copies, ID, EmptyCoroutineContext).restore(), "zepsuta kopia to brak kopii")
        }

    private companion object {
        const val ID = "s-kopia-serwera"
        const val PHOTO = "p-kopia-serwera"
    }
}
