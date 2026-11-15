/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submission.own

import io.github.dostepnykampus.core.data.submission.sync.FakeHistory
import io.github.dostepnykampus.core.database.submission.OwnSubmissionEntity
import io.github.dostepnykampus.core.database.submission.OwnSubmissionState
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.submission.BarrierGroup
import io.github.dostepnykampus.core.model.submission.Submission
import io.github.dostepnykampus.core.model.submission.SubmissionContent
import io.github.dostepnykampus.core.model.submission.SubmissionKind
import io.github.dostepnykampus.core.model.submission.SubmissionStatus
import io.github.dostepnykampus.core.network.ApiResult
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.Instant

/**
 * Statusy listy C3 z serwera w historii telefonu.
 *
 * Wymagania: US8.2, US9.1, BR-16, US8.1, US4.4
 */
class OwnStatusesTest {
    private val history = FakeHistory()

    /** Scenariusz (częściowo): zgloszenia-zbierajacego.feature: Zgłoszenie odrzucone pokazuje powód odrzucenia. */
    @Test
    fun takesStatusFromServer() =
        runBlocking {
            history.save(OwnSubmissionEntity(id = "a", savedAtMillis = 1, kind = "PRECISE"))
            history.markSent("a")
            val rejected = server("a").copy(status = SubmissionStatus.REJECTED, rejectionReason = REASON, version = 3)

            assertEquals(
                StatusRefresh.DONE,
                refresh(ApiResult.Success(listOf(rejected))),
                "odświeżenie statusów kończy się powodzeniem",
            )

            val row = history.rows.getValue("a")
            assertEquals("REJECTED", row.serverStatus, "status z serwera")
            assertEquals(REASON, row.rejectionReason, "powód odrzucenia z serwera")
            assertEquals(3, row.serverVersion, "wersja z serwera")
            assertEquals(1, row.savedAtMillis, "czas zapisu zostaje")
        }

    @Test
    fun addsNewFromServer() =
        runBlocking {
            refresh(ApiResult.Success(listOf(server("b"))))

            val row = history.rows.getValue("b")
            assertEquals(OwnSubmissionState.SENT, row.state, "zgłoszenie jest wysłane")
            assertEquals("TB-5", row.barrierType, "typ bariery zostaje")
            assertEquals(CREATED.toEpochMilli(), row.savedAtMillis, "czas zapisu to czas utworzenia")
            assertEquals("SENT", row.serverStatus, "status z serwera")
        }

    @Test
    fun skipsQuick() =
        runBlocking {
            val quick = content().copy(kind = SubmissionKind.QUICK, barrierGroup = BarrierGroup.OTHER)

            refresh(ApiResult.Success(listOf(server("q").copy(content = quick))))

            assertNull(history.rows["q"], "zgłoszenie szybkie nie trafia do historii")
        }

    @Test
    fun handlesOfflineAndError() =
        runBlocking {
            assertEquals(
                StatusRefresh.OFFLINE,
                refresh(ApiResult.NetworkError(IOException())),
                "brak sieci to tryb offline",
            )
            assertEquals(
                StatusRefresh.FAILED,
                refresh(ApiResult.ServerError(code = 403)),
                "błąd serwera to niepowodzenie",
            )
            assertEquals(StatusRefresh.FAILED, refresh(ApiResult.NotFound), "brak zasobu to niepowodzenie")
        }

    @Test
    fun givesModelWithStatus() {
        val row = OwnSubmissionEntity(id = "a", savedAtMillis = 1, kind = "PRECISE", serverStatus = "IN_REVIEW")

        assertEquals(SubmissionStatus.IN_REVIEW, row.toOwnSubmission().serverStatus, "znany status jest czytany")
        assertNull(row.copy(serverStatus = "LOST").toOwnSubmission().serverStatus, "nieznany status znika")
        assertNull(row.copy(serverStatus = null).toOwnSubmission().serverStatus, "brak statusu zostaje pusty")
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
