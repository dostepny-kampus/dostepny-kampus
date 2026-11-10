/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import io.github.dostepnykampus.core.data.submissions.PendingPhotoDraft
import io.github.dostepnykampus.core.model.BarrierGroup
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.feature.capture.navigation.ReportRoute
import io.github.dostepnykampus.feature.capture.navigation.ReportStep
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Krok 3 V4 bierze zgłoszenie z wpisu historii tylko wtedy, gdy kroki 1 i 2 są wypełnione (US4.8). */
class QuickReportRouteTest {
    private val filled =
        ReportRoute(
            step = ReportStep.SEND,
            photoId = "zdjecie-1",
            photoPath = "/zdjecia/zdjecie-1.jpg",
            group = BarrierGroup.TOILETS,
            floorNumber = 2,
            isFloorAsked = true,
        )

    @Test
    fun `pełne kroki dają zgłoszenie`() {
        val expected =
            QuickReport(
                photo = PendingPhotoDraft(photoId = "zdjecie-1", path = "/zdjecia/zdjecie-1.jpg"),
                group = BarrierGroup.TOILETS,
                floorNumber = 2,
            )
        assertEquals(expected, filled.quickReport())
    }

    @Test
    fun `us4_6 wejście z trasy w treści`() {
        val point = GeoPoint(longitude = 17.06, latitude = 51.1)
        val position = QuickPosition(point = point, accuracyMeters = 6.0, isInBuilding = true)
        val content = filled.copy(entranceId = "A-1-w1").quickReport()?.content(position)
        assertEquals("A-1-w1", content?.entranceId)
        assertNull(filled.quickReport()?.content(position)?.entranceId)
    }

    @Test
    fun `bez zdjęcia albo grupy null`() {
        assertNull(filled.copy(photoId = null).quickReport())
        assertNull(filled.copy(photoPath = null).quickReport())
        assertNull(filled.copy(group = null).quickReport())
    }
}
