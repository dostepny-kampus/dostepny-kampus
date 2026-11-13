/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submissions

import io.github.dostepnykampus.core.database.OwnSubmissionEntity
import io.github.dostepnykampus.core.database.OwnSubmissionState
import io.github.dostepnykampus.core.model.BarrierGroup
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.Submission
import io.github.dostepnykampus.core.model.SubmissionContent
import io.github.dostepnykampus.core.model.SubmissionKind
import io.github.dostepnykampus.core.model.SubmissionStatus
import io.github.dostepnykampus.core.network.ApiResult
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.Instant

/** Statusy listy C3 z serwera w historii telefonu. Wymagania: US8.2, US9.1, BR-16 */
class OwnStatusesTest {
    private val history = FakeHistory()

    @Test
    fun `us8_2 status i powód z serwera`() =
        runBlocking {
            history.save(OwnSubmissionEntity(id = "a", savedAtMillis = 1, kind = "PRECISE"))
            history.markSent("a")
            val rejected = server("a").copy(status = SubmissionStatus.REJECTED, rejectionReason = REASON, version = 3)

            assertEquals(StatusRefresh.DONE, refresh(ApiResult.Success(listOf(rejected))))

            val row = history.rows.getValue("a")
            assertEquals("REJECTED", row.serverStatus)
            assertEquals(REASON, row.rejectionReason)
            assertEquals(3, row.serverVersion)
            assertEquals(1, row.savedAtMillis)
        }

    @Test
    fun `us8_1 nowe z serwera`() =
        runBlocking {
            refresh(ApiResult.Success(listOf(server("b"))))

            val row = history.rows.getValue("b")
            assertEquals(OwnSubmissionState.SENT, row.state)
            assertEquals("TB-5", row.barrierType)
            assertEquals(CREATED.toEpochMilli(), row.savedAtMillis)
            assertEquals("SENT", row.serverStatus)
        }

    @Test
    fun `us4_4 szybkie pominięte`() =
        runBlocking {
            val quick = content().copy(kind = SubmissionKind.QUICK, barrierGroup = BarrierGroup.OTHER)

            refresh(ApiResult.Success(listOf(server("q").copy(content = quick))))

            assertNull(history.rows["q"])
        }

    @Test
    fun `us9_1 brak sieci i błąd`() =
        runBlocking {
            assertEquals(StatusRefresh.OFFLINE, refresh(ApiResult.NetworkError(IOException())))
            assertEquals(StatusRefresh.FAILED, refresh(ApiResult.ServerError(code = 403)))
            assertEquals(StatusRefresh.FAILED, refresh(ApiResult.NotFound))
        }

    @Test
    fun `us8_2 model ze statusem`() {
        val row = OwnSubmissionEntity(id = "a", savedAtMillis = 1, kind = "PRECISE", serverStatus = "IN_REVIEW")

        assertEquals(SubmissionStatus.IN_REVIEW, row.toOwnSubmission().serverStatus)
        assertNull(row.copy(serverStatus = "LOST").toOwnSubmission().serverStatus)
        assertNull(row.copy(serverStatus = null).toOwnSubmission().serverStatus)
    }

    private suspend fun refresh(result: ApiResult<List<Submission>>): StatusRefresh =
        ownStatusesOf(mine = { result }, history = history).refresh()

    private fun server(id: String): Submission =
        Submission(id = id, version = 1, status = SubmissionStatus.SENT, createdAt = CREATED, content = content())

    private fun content(): SubmissionContent =
        SubmissionContent(
            kind = SubmissionKind.PRECISE,
            location = GeoPoint(longitude = 17.06209, latitude = 51.10785),
            photoIds = listOf("p1"),
            barrierType = "TB-5",
            severity = 2,
        )
}

private val CREATED = Instant.parse("2026-11-10T12:00:00Z")
private const val REASON = "Nieostre zdjęcie"
