/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submissions

import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.model.BarrierGroup
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.Submission
import io.github.dostepnykampus.core.model.SubmissionContent
import io.github.dostepnykampus.core.model.SubmissionKind
import io.github.dostepnykampus.core.model.SubmissionStatus
import io.github.dostepnykampus.core.network.ApiResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.Instant
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
    fun `us4_4 lista bez szybkich`() =
        runBlocking {
            val queue = queue()
            queue.save(quick(), photo("p1"))
            queue.sendAll()
            queue.save(precise(), photo("p2"))

            val list = ownSubmissionsOf(history).observe().first()

            val item = list.single()
            assertEquals(PRECISE_ID, item.id)
            assertEquals("TB-13", item.barrierType)
            assertEquals("C-13", item.buildingSymbol)
            assertEquals(listOf(QUICK_ID), store.created)
        }

    private fun queue(): SubmissionQueue =
        SubmissionQueue(
            store = store,
            ioContext = EmptyCoroutineContext,
            server =
                SubmissionServer(
                    create = { id, content -> ServerReply.Ok(server(id, content)) },
                    putPhoto = { _, _, _ -> ServerReply.Ok(Unit) },
                    update = { _, _, _ -> ApiResult.NotFound },
                ),
            stamps = SubmissionStamps(newId = { ids.removeFirst() }),
            history = history,
        )

    private fun server(id: String, content: SubmissionContent): Submission =
        Submission(
            id = id,
            version = 1,
            status = SubmissionStatus.SENT,
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            content = content,
        )

    private fun photo(id: String): List<PendingPhotoDraft> {
        val file = tempDir.resolve("$id.jpg")
        file.writeBytes(byteArrayOf(1, 2, 3))
        return listOf(PendingPhotoDraft(photoId = id, path = file.path))
    }

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
