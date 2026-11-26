/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submission.sync

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.github.dostepnykampus.core.data.inspection.PendingMarkerChange
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.submission.SubmissionContent
import io.github.dostepnykampus.core.model.submission.SubmissionKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.Instant

/**
 * Stan kolejki wysyłki dla C4: co czeka w telefonie i kiedy kolejka doszła do końca.
 *
 * Wymagania: US9.1, BR-9, BR-10, BR-26, NFR-10
 */
class SyncStatusTest {
    @TempDir
    lateinit var directory: File

    @Test
    fun countsOnlySendable() {
        val rows =
            listOf(
                row(id = "s-1"),
                row(id = "s-odmowa").copy(isRefused = true),
                row(id = "s-inne").copy(ownerSub = "inne-konto"),
            )
        val marker = PendingMarkerChange(markerId = "m-1", buildingSymbol = "C-13", floorId = null, isRevoke = false)
        val status =
            withLog { log ->
                syncStatusOf(
                    pending = flowOf(rows),
                    markers = flowOf(listOf(marker)),
                    subjects = flowOf(OWNER),
                    log = log,
                ).status().first()
            }

        assertEquals(1, status.submissions, "tylko własne zgłoszenie bez odmowy")
        assertEquals(1, status.markers, "znacznik piętra")
        assertEquals(2, status.waiting, "razem czeka")
    }

    @Test
    fun noAccountNothingWaits() {
        val status =
            withLog { log ->
                val source =
                    syncStatusOf(
                        pending = flowOf(listOf(row("s-1"))),
                        markers = flowOf(emptyList()),
                        subjects = flowOf(null),
                        log = log,
                    )
                source.status().first()
            }

        assertEquals(0, status.waiting, "bez konta nic nie czeka")
    }

    @Test
    fun keepsLastFullRun() {
        val runs =
            withLog { log ->
                val before = log.runs().first()
                log.record(isDone = true, at = DONE_AT)
                val afterDone = log.runs().first()
                log.record(isDone = false, at = FAILED_AT)
                listOf(before, afterDone, log.runs().first())
            }

        assertEquals(SyncRun(lastSyncedAt = null, isLastRunFailed = false), runs[0], "przed pierwszą wysyłką")
        assertEquals(SyncRun(lastSyncedAt = DONE_AT, isLastRunFailed = false), runs[1], "wysyłka do końca kolejki")
        assertEquals(SyncRun(lastSyncedAt = DONE_AT, isLastRunFailed = true), runs[2], "przerwana nie zmienia chwili")
    }

    @Test
    fun runSurvivesRestart() {
        withLog { log -> log.record(isDone = true, at = DONE_AT) }

        assertEquals(DONE_AT, withLog { log -> log.runs().first().lastSyncedAt }, "chwila po ponownym starcie")
    }

    private fun <T> withLog(body: suspend (SyncLog) -> T): T =
        runBlocking {
            val job = SupervisorJob()
            val file = File(directory, "sync.preferences_pb")
            val dataStore = PreferenceDataStoreFactory.create(scope = CoroutineScope(job)) { file }
            val result = body(SyncLog(dataStore))
            job.cancelAndJoin()
            result
        }

    private fun row(id: String) =
        SubmissionContent(
            kind = SubmissionKind.PRECISE,
            location = GeoPoint(longitude = 17.06209, latitude = 51.10785),
            photoIds = listOf("p-$id"),
            severity = 2,
        ).toPendingSubmissionEntity(id = id, savedAtMillis = 1L)
            .copy(ownerSub = OWNER)

    private companion object {
        const val OWNER = "konto-testowe"
        val DONE_AT: Instant = Instant.parse("2026-11-12T09:42:00Z")
        val FAILED_AT: Instant = Instant.parse("2026-11-12T10:00:00Z")
    }
}
