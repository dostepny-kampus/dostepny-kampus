/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.barrier

import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.core.model.submission.BarrierType
import io.github.dostepnykampus.core.model.submission.Severity
import io.github.dostepnykampus.core.model.submission.Submission
import io.github.dostepnykampus.core.model.submission.SubmissionContent
import io.github.dostepnykampus.core.model.submission.SubmissionKind
import io.github.dostepnykampus.core.model.submission.SubmissionStatus
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

/** Bariery mapy ze zgłoszeń przyjętych z serwera. Wymagania: US1.3, BR-6, NFR-11 */
class AcceptedBarriersTest {
    @Test
    fun readsThreeBarriers() =
        runBlocking {
            val submissions = Severity.entries.map { submission(id = it.name, content = precise(it.value)) }
            val barriers = ServerAcceptedBarriers { ServerReply.Ok(submissions) }.barriers()

            assertEquals(Severity.entries, barriers.map(AcceptedBarrier::severity))
            val first =
                AcceptedBarrier(
                    id = Severity.HINDRANCE.name,
                    location = PLACE,
                    severity = Severity.HINDRANCE,
                    type = BarrierType.THRESHOLD,
                    buildingSymbol = "A-1",
                    verifiedAt = VERIFIED,
                    photoIds = listOf("p1"),
                )
            assertEquals(first, barriers.first())
        }

    @Test
    fun skipsNonBarriers() =
        runBlocking {
            val marking =
                precise(severity = null).copy(
                    kind = SubmissionKind.ENTRANCE_MARKING,
                    entranceMarking = EntranceMarking.ACCESSIBLE,
                )
            val removed = precise(severity = 2).copy(isBarrierRemoved = true)
            val contents = listOf(marking, removed, precise(severity = null), precise(severity = 2))
            val submissions = contents.mapIndexed { index, content -> submission(id = "z$index", content = content) }
            val barriers = ServerAcceptedBarriers { ServerReply.Ok(submissions) }.barriers()

            assertEquals(listOf("z3"), barriers.map(AcceptedBarrier::id))
        }

    @Test
    fun givesEmptyWithoutReply() =
        runBlocking {
            val answers = listOf(ServerReply.Offline, ServerReply.Fault(code = 500))
            val lists = answers.map { answer -> ServerAcceptedBarriers { answer }.barriers() }

            assertEquals(listOf(emptyList(), emptyList<AcceptedBarrier>()), lists)
        }

    @Test
    fun dropsNotAccepted() =
        runBlocking {
            val one = listOf(submission(id = "z1", content = precise(severity = 1)))
            val answers = ArrayDeque(listOf(ServerReply.Ok(one), ServerReply.Ok(emptyList())))
            val source = ServerAcceptedBarriers { answers.removeFirst() }

            assertEquals(listOf("z1"), source.barriers().map(AcceptedBarrier::id))
            assertEquals(emptyList<AcceptedBarrier>(), source.barriers())
        }

    private fun precise(severity: Int?) =
        SubmissionContent(
            kind = SubmissionKind.PRECISE,
            location = PLACE,
            photoIds = listOf("p1"),
            buildingSymbol = "A-1",
            barrierType = BarrierType.THRESHOLD.code,
            severity = severity,
        )

    private fun submission(id: String, content: SubmissionContent) =
        Submission(
            id = id,
            version = 1,
            status = SubmissionStatus.ACCEPTED,
            createdAt = VERIFIED,
            content = content,
            verifiedAt = VERIFIED,
        )

    private companion object {
        val PLACE = GeoPoint(longitude = 17.06, latitude = 51.107)
        val VERIFIED: Instant = Instant.parse("2026-10-14T10:00:00Z")
    }
}
