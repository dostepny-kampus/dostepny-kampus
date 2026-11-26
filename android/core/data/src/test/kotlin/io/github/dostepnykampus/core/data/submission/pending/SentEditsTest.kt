/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submission.pending

import io.github.dostepnykampus.core.data.submission.sync.EditedSubmission
import io.github.dostepnykampus.core.data.submission.sync.FakeHistory
import io.github.dostepnykampus.core.data.submission.sync.OwnCopy
import io.github.dostepnykampus.core.data.submission.sync.PendingPhotoDraft
import io.github.dostepnykampus.core.data.submission.sync.ownCopyJson
import io.github.dostepnykampus.core.database.submission.OwnSubmissionEntity
import io.github.dostepnykampus.core.database.submission.OwnSubmissionState
import io.github.dostepnykampus.core.database.submission.PendingEditDao
import io.github.dostepnykampus.core.database.submission.PendingSubmissionWithPhotos
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.submission.SubmissionContent
import io.github.dostepnykampus.core.model.submission.SubmissionKind
import io.github.dostepnykampus.core.model.submission.SubmissionStatus
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/** Poprawka wysłanego zgłoszenia z C3 startuje z historii telefonu, bez sieci. Wymagania: US8.3, BR-10, NFR-5 */
class SentEditsTest {
    @TempDir
    lateinit var tempDir: File

    private val history = FakeHistory()
    private val queue =
        object : PendingEditDao {
            override suspend fun find(id: String): PendingSubmissionWithPhotos? = null
        }
    private val edits = pendingEditsOf(store = queue, history = history, subject = { OWNER })

    /** Wysłane zgłoszenie przed decyzją otwiera się z treścią, zdjęciami i wersją z serwera. Wymagania: US8.3, BR-10 */
    @Test
    fun loadsSentFromHistory() =
        runBlocking {
            val photo = photoFile("p1")
            sent(status = SubmissionStatus.IN_REVIEW, paths = listOf(photo.path))

            val expected =
                PendingEdit(
                    edited = EditedSubmission(id = ID, baseVersion = 3),
                    content = content(),
                    photos = listOf(PendingPhotoDraft(photoId = "p1", path = photo.path)),
                )
            assertEquals(expected, edits.load(ID), "poprawka z historii")
        }

    /** Zdjęcie, którego pliku już nie ma, wypada z poprawki, a formularz poprosi o nowe. Wymagania: BR-7 */
    @Test
    fun dropsMissingPhoto() =
        runBlocking {
            sent(status = SubmissionStatus.SENT, paths = listOf(tempDir.resolve("brak.jpg").path))

            val edit = checkNotNull(edits.load(ID))
            assertEquals(emptyList<String>(), edit.content.photoIds, "bez identyfikatora brakującego zdjęcia")
            assertEquals(emptyList<PendingPhotoDraft>(), edit.photos, "bez brakującego zdjęcia")
        }

    /** Przyjęte zgłoszenie i zgłoszenie innego konta nie otwierają poprawki. Wymagania: US8.3, NFR-33 */
    @Test
    fun skipsDecidedAndForeign() =
        runBlocking {
            sent(status = SubmissionStatus.ACCEPTED, paths = listOf(photoFile("p1").path))
            assertNull(edits.load(ID), "przyjęte zgłoszenie")

            sent(status = SubmissionStatus.SENT, paths = listOf(photoFile("p1").path), owner = "inne-konto")
            assertNull(edits.load(ID), "zgłoszenie innego konta")
        }

    private fun sent(status: SubmissionStatus, paths: List<String>, owner: String = OWNER) {
        history.rows[ID] =
            OwnSubmissionEntity(
                id = ID,
                savedAtMillis = 1,
                kind = SubmissionKind.PRECISE.name,
                state = OwnSubmissionState.SENT,
                serverVersion = 3,
                serverStatus = status.name,
                ownerSub = owner,
                contentJson = ownCopyJson(ID, OwnCopy(content = content(), photoPaths = paths)),
            )
    }

    private fun photoFile(id: String): File = tempDir.resolve("$id.jpg").apply { writeBytes(byteArrayOf(1)) }

    private fun content(): SubmissionContent =
        SubmissionContent(
            kind = SubmissionKind.PRECISE,
            location = GeoPoint(longitude = 17.06209, latitude = 51.10785),
            photoIds = listOf("p1"),
            barrierType = "TB-5",
            severity = 2,
            description = "Wysoki próg",
        )

    private companion object {
        const val ID = "s-wyslane"
        const val OWNER = "konto-testowe"
    }
}
