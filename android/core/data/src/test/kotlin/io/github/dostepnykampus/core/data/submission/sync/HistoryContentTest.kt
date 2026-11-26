/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submission.sync

import io.github.dostepnykampus.core.data.server.ServerEdit
import io.github.dostepnykampus.core.data.submission.sync.SubmissionFixtures.photo
import io.github.dostepnykampus.core.data.submission.sync.SubmissionFixtures.precise
import io.github.dostepnykampus.core.model.submission.SubmissionStatus
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/** Wysłane zgłoszenie zostaje w historii telefonu z pełną treścią i zdjęciami do decyzji. Wymagania: US8.3, NFR-5 */
class HistoryContentTest {
    @TempDir
    lateinit var tempDir: File

    private val store = FakeStore()
    private val history = FakeHistory()
    private val queue by lazy {
        editQueue(store = store, records = SentRecords(history = history), edits = ArrayDeque<ServerEdit>())
    }

    /** Historia ma pełną treść i ścieżki zdjęć z ostatniego zapisu. Wymagania: US8.3 */
    @Test
    fun keepsFullContent() =
        runBlocking {
            val content = precise(PHOTO).copy(description = "Brak podjazdu", isManualCorrection = true)
            val photos = photo(tempDir, PHOTO)
            val saved = queue.save(content, photos)

            val copy = ownCopyOf(history.rows.getValue(saved.id).contentJson)
            assertEquals(content, copy?.content, "pełna treść w historii")
            assertEquals(photos.map { it.path }, copy?.photoPaths, "ścieżki zdjęć w historii")
        }

    /** Wysłanie nie usuwa zdjęć własnego zgłoszenia: poprawka przed decyzją ich potrzebuje. Wymagania: US8.3 */
    @Test
    fun keepsPhotosAfterSend() =
        runBlocking {
            val file = File(photo(tempDir, PHOTO).single().path)
            queue.save(precise(PHOTO), listOf(PendingPhotoDraft(photoId = PHOTO, path = file.path)))
            queue.sendAll()

            assertTrue(store.rows.isEmpty(), "kolejka pusta po wysłaniu")
            assertTrue(file.exists(), "zdjęcie zostaje do decyzji")
        }

    /** Sprzątanie oszczędza zdjęcia zgłoszenia przed decyzją i usuwa je po przyjęciu. Wymagania: NFR-5, NFR-7 */
    @Test
    fun releasesPhotosAfterDecision() =
        runBlocking {
            val file = File(photo(tempDir, PHOTO).single().path)
            val saved = queue.save(precise(PHOTO), listOf(PendingPhotoDraft(photoId = PHOTO, path = file.path)))
            queue.sendAll()
            file.setLastModified(System.currentTimeMillis() - TWO_HOURS)

            queue.deleteOrphanPhotos(dir = { tempDir }, kept = emptySet())
            assertTrue(file.exists(), "zdjęcie zgłoszenia przed decyzją zostaje")

            history.saveServerStatus(id = saved.id, status = SubmissionStatus.ACCEPTED.name, reason = null, version = 2)
            queue.deleteOrphanPhotos(dir = { tempDir }, kept = emptySet())
            assertFalse(file.exists(), "po przyjęciu zdjęcie znika")
        }

    private companion object {
        const val PHOTO = "p-historia"
        const val TWO_HOURS = 7_200_000L
    }
}
