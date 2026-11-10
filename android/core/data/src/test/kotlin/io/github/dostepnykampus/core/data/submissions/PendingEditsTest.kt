/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submissions

import io.github.dostepnykampus.core.database.PendingEditDao
import io.github.dostepnykampus.core.database.PendingSubmissionWithPhotos
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.SubmissionContent
import io.github.dostepnykampus.core.model.SubmissionFloor
import io.github.dostepnykampus.core.model.SubmissionKind
import io.github.dostepnykampus.core.network.ApiResult
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.io.IOException
import kotlin.coroutines.EmptyCoroutineContext

/** Poprawka z C3 wczytuje z kolejki to, co zapisano, z wersją bazową; wysłanego już nie ma (US8.3, BR-26). */
class PendingEditsTest {
    private val store = FakeStore()
    private val edits =
        pendingEditsOf(
            object : PendingEditDao {
                override suspend fun find(id: String): PendingSubmissionWithPhotos? =
                    store.rows.find { it.submission.id == id }
            },
        )
    private val queue =
        SubmissionQueue(
            store = store,
            ioContext = EmptyCoroutineContext,
            create = { _, _ -> ApiResult.NetworkError(IOException()) },
            putPhoto = { _, _, _ -> ApiResult.Success(Unit) },
            newId = { ID },
        )

    @Test
    fun `us8_3 zapis wraca do C2`() =
        runBlocking {
            queue.save(precise(), photos())

            val edit = edits.load(ID)

            assertEquals(PendingEdit(EditedSubmission(ID, baseVersion = null), precise(), photos()), edit)
        }

    @Test
    fun `us8_3 z wersją bazową`() =
        runBlocking {
            queue.saveEdit(EditedSubmission(ID, baseVersion = 4), precise(), photos())

            assertEquals(EditedSubmission(ID, baseVersion = 4), checkNotNull(edits.load(ID)).edited)
        }

    @Test
    fun `br26 odrzucone też do poprawki`() =
        runBlocking {
            queue.save(precise(), photos())
            store.markRefused(ID)

            assertEquals(precise(), edits.load(ID)?.content)
        }

    @Test
    fun `us8_3 brak w telefonie to null`() =
        runBlocking {
            assertNull(edits.load(ID))
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
