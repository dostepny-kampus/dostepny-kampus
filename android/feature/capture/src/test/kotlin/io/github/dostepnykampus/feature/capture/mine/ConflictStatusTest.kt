/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.mine

import io.github.dostepnykampus.core.data.submission.own.RefusalReason
import io.github.dostepnykampus.core.data.submission.own.SendState
import io.github.dostepnykampus.core.model.submission.BarrierType
import io.github.dostepnykampus.core.model.submission.SubmissionStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

/** Status karty C3 przy zgłoszeniu czekającym na wybór wersji. Wymagania: US8.1, US9.3, BR-10 */
class ConflictStatusTest {
    /** Konflikt wersji pokazuje status wersji z serwera, a nie „Nie wysłano”. Wymagania: US8.1, US9.3, BR-10 */
    @Test
    fun showsServerStatus() {
        SubmissionStatus.entries.forEach { server ->
            val shown = refused(RefusalReason.CHANGED_ON_SERVER, server).shownStatus()
            assertEquals(ShownStatus.valueOf(server.name), shown, "status z serwera: $server")
        }
    }

    /** Bez znanego statusu z serwera konflikt pokazuje „Wysłane”. Wymagania: US8.1, BR-10 */
    @Test
    fun showsSentWithoutServerStatus() {
        val shown = refused(RefusalReason.CHANGED_ON_SERVER, null).shownStatus()
        assertEquals(ShownStatus.SENT, shown, "bez statusu z serwera")
    }

    /** Inna odmowa serwera zostaje „Nie wysłano”. Wymagania: US8.1, BR-9 */
    @Test
    fun keepsRefusedForOtherRefusals() {
        RefusalReason.entries.filter { it != RefusalReason.CHANGED_ON_SERVER }.forEach { reason ->
            val shown = refused(reason, SubmissionStatus.IN_REVIEW).shownStatus()
            assertEquals(ShownStatus.REFUSED, shown, "odmowa: $reason")
        }
        assertEquals(ShownStatus.REFUSED, refused(null, null).shownStatus(), "odmowa bez powodu")
    }

    private fun refused(reason: RefusalReason?, serverStatus: SubmissionStatus?) =
        OwnSubmissionItem(
            id = "s",
            savedAt = Instant.parse("2026-11-09T12:00:00Z"),
            type = BarrierType.STAIRS_AT_ENTRANCE,
            buildingSymbol = "A-1",
            floorNumber = 2,
            state = SendState.REFUSED,
            refusal = reason,
            serverStatus = serverStatus,
        )
}
