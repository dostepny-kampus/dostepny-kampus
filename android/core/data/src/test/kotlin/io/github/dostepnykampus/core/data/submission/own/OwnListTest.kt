/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submission.own

import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.data.submission.sync.EditedSubmission
import io.github.dostepnykampus.core.data.submission.sync.FakeHistory
import io.github.dostepnykampus.core.data.submission.sync.FakeStore
import io.github.dostepnykampus.core.data.submission.sync.SubmissionFixtures.photo
import io.github.dostepnykampus.core.data.submission.sync.SubmissionFixtures.server
import io.github.dostepnykampus.core.data.submission.sync.SubmissionQueue
import io.github.dostepnykampus.core.data.submission.sync.SubmissionServer
import io.github.dostepnykampus.core.data.submission.sync.SubmissionStamps
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.submission.BarrierGroup
import io.github.dostepnykampus.core.model.submission.SubmissionContent
import io.github.dostepnykampus.core.model.submission.SubmissionKind
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.coroutines.EmptyCoroutineContext

/** Lista C3 czytana tak jak w aplikacji: z historii zapisanej przez kolejkę. Wymagania: US8.1, US4.4 */
class OwnListTest {
    @TempDir
    lateinit var tempDir: File

    private val store = FakeStore()
    private val history = FakeHistory()
    private val ids = ArrayDeque(listOf(QUICK_ID, PRECISE_ID))

    /**
     * Zgłoszenie szybkie wysłane, zgłoszenie z pomiaru TB-13 w C-13 zapisane; lista ma tylko to drugie.
     * Scenariusz: zgloszenia-zbierajacego.feature: Lista nie pokazuje własnych zgłoszeń szybkich
     */
    @Test
    fun listsWithoutQuick() =
        runBlocking {
            val queue = queue()
            queue.save(quick(), photo(tempDir, "p1"))
            queue.sendAll()
            queue.save(precise(), photo(tempDir, "p2"))

            val list = ownSubmissionsOf(history, store).observe().first()

            val item = list.single()
            assertEquals(PRECISE_ID, item.id)
            assertEquals("TB-13", item.barrierType)
            assertEquals("C-13", item.buildingSymbol)
            assertEquals(listOf(QUICK_ID), store.created)
        }

    /**
     * Zgłoszenie wysłane, potem jego poprawka bez sieci: lista pokazuje stan „Wysłane” i czekającą zmianę.
     * Scenariusz (częściowo): zgloszenia-zbierajacego.feature: Zgłoszenie ze zmianą czekającą w telefonie ma
     *   napis przy statusie
     */
    @Test
    fun marksChangeWaiting() =
        runBlocking {
            val queue = queue()
            val saved = queue.save(precise(), photo(tempDir, "p2"))
            queue.sendAll()
            queue.saveEdit(EditedSubmission(id = saved.id, baseVersion = 1), precise(), photo(tempDir, "p2"))

            val item = ownSubmissionsOf(history, store).observe().first().single()

            assertEquals(SendState.SENT, item.state)
            assertTrue(item.isChangeWaiting)
        }

    @Test
    fun skipsWaitingWithoutChange() =
        runBlocking {
            val queue = queue()
            queue.save(precise(), photo(tempDir, "p2"))
            queue.sendAll()

            val listed = ownSubmissionsOf(history, store).observe().first()
            assertFalse(listed.single().isChangeWaiting)
        }

    private fun queue(): SubmissionQueue =
        SubmissionQueue(
            store = store,
            ioContext = EmptyCoroutineContext,
            server =
                SubmissionServer(
                    create = { id, content -> ServerReply.Ok(server(id, content)) },
                    putPhoto = { _, _, _ -> ServerReply.Ok(Unit) },
                    update = { _, _, _ -> ServerReply.Missing },
                ),
            stamps = SubmissionStamps(newId = { ids.removeFirst() }),
            history = history,
        )

    private fun quick(): SubmissionContent =
        SubmissionContent(
            kind = SubmissionKind.QUICK,
            location = PLACE,
            photoIds = listOf("p1"),
            barrierGroup = BarrierGroup.VERTICAL_CIRCULATION,
        )

    private fun precise(): SubmissionContent =
        SubmissionContent(
            kind = SubmissionKind.PRECISE,
            location = PLACE,
            photoIds = listOf("p2"),
            buildingSymbol = "C-13",
            barrierType = "TB-13",
            severity = 2,
        )
}

private const val QUICK_ID = "s-quick"
private const val PRECISE_ID = "s-precise"
private val PLACE = GeoPoint(longitude = 17.06209, latitude = 51.10785)
