/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submission.pending

import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.data.submission.sync.EditedSubmission
import io.github.dostepnykampus.core.data.submission.sync.FakeStore
import io.github.dostepnykampus.core.data.submission.sync.PendingPhotoDraft
import io.github.dostepnykampus.core.data.submission.sync.SubmissionFixtures
import io.github.dostepnykampus.core.data.submission.sync.SubmissionQueue
import io.github.dostepnykampus.core.data.submission.sync.SubmissionStamps
import io.github.dostepnykampus.core.database.submission.PendingEditDao
import io.github.dostepnykampus.core.database.submission.PendingSubmissionWithPhotos
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.submission.SubmissionContent
import io.github.dostepnykampus.core.model.submission.SubmissionFloor
import io.github.dostepnykampus.core.model.submission.SubmissionKind
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import kotlin.coroutines.EmptyCoroutineContext

/** Poprawka z C3 wczytuje z kolejki to, co zapisano, z wersją bazową; wysłanego już nie ma. Wymagania: US8.3, BR-26 */
class PendingEditsTest {
    private val store = FakeStore()
    private val edits =
        pendingEditsOf(
            object : PendingEditDao {
                override suspend fun find(id: String): PendingSubmissionWithPhotos? =
                    store.rows.find { it.submission.id == id }
            },
            subject = { "konto-testowe" },
        )
    private val queue =
        SubmissionQueue(
            store = store,
            ioContext = EmptyCoroutineContext,
            server = SubmissionFixtures.acceptingServer { _, _ -> ServerReply.Offline },
            stamps = SubmissionStamps(newId = { ID }),
        )

    @Test
    fun returnsToC2OnSave() =
        runBlocking {
            queue.save(precise(), photos())

            val edit = edits.load(ID)

            assertEquals(
                PendingEdit(EditedSubmission(ID, baseVersion = null), precise(), photos()),
                edit,
                "poprawka wczytana z kolejki to zapisane zgłoszenie",
            )
        }

    @Test
    fun keepsBaseVersion() =
        runBlocking {
            queue.saveEdit(EditedSubmission(ID, baseVersion = 4), precise(), photos())

            assertEquals(
                EditedSubmission(ID, baseVersion = 4),
                checkNotNull(edits.load(ID)).edited,
                "wersja bazowa zostaje z zapisu",
            )
        }

    @Test
    fun editsRejectedToo() =
        runBlocking {
            queue.save(precise(), photos())
            store.markRefused(ID)

            assertEquals(precise(), edits.load(ID)?.content, "odrzucone zgłoszenie można poprawić")
        }

    @Test
    fun givesNullWhenNotOnPhone() =
        runBlocking {
            assertNull(edits.load(ID), "nie ma poprawki dla zgłoszenia spoza telefonu")
        }

    private fun photos(): List<PendingPhotoDraft> =
        listOf("p1", "p2").map { PendingPhotoDraft(photoId = it, path = "zdjecia/$it.jpg") }

    private fun precise(): SubmissionContent =
        SubmissionContent(
            kind = SubmissionKind.PRECISE,
            location = GeoPoint(longitude = 17.06209, latitude = 51.10785),
            photoIds = listOf("p1", "p2"),
            accuracyMeters = 3.5,
            isManualCorrection = false,
            buildingSymbol = "A-1",
            floor = SubmissionFloor(number = 2, isMezzanine = false),
            barrierType = "TB-5",
            severity = 2,
            description = "Wysoki próg",
        )
}

private const val ID = "s1"
