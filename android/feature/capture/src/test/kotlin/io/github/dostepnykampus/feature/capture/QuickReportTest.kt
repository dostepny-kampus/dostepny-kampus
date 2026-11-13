/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import io.github.dostepnykampus.core.data.reports.QuickPosition
import io.github.dostepnykampus.core.data.reports.QuickReport
import io.github.dostepnykampus.core.data.reports.content
import io.github.dostepnykampus.core.data.submissions.PendingPhotoDraft
import io.github.dostepnykampus.core.model.BarrierGroup
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.SubmissionFloor
import io.github.dostepnykampus.core.model.SubmissionKind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Zgłoszenie szybkie V4: pytanie o piętro według BR-14 i treść z bieżącą pozycją. Wymagania: US4.1, US4.3, US4.5 */
class QuickReportTest {
    private val point = GeoPoint(longitude = 17.06209, latitude = 51.10785)
    private val photo = PendingPhotoDraft(photoId = "zdjecie-1", path = "/zdjecia/zdjecie-1.jpg")

    @Test
    fun `w budynku pytanie o piętro`() {
        val withFloor = BarrierGroup.entries - BarrierGroup.OTHER
        withFloor.forEach { assertTrue(it.asksFloor(isInBuilding = true)) }
        assertFalse(BarrierGroup.OTHER.asksFloor(isInBuilding = true))
    }

    @Test
    fun `poza budynkiem bez pytania`() {
        assertFalse(BarrierGroup.TOILETS.asksFloor(isInBuilding = false))
    }

    @Test
    fun `nieznana pozycja z pytaniem`() {
        assertTrue(BarrierGroup.VERTICAL_CIRCULATION.asksFloor(isInBuilding = null))
        assertFalse(BarrierGroup.OTHER.asksFloor(isInBuilding = null))
    }

    @Test
    fun `treść z grupą i piętrem`() {
        val report = QuickReport(photo = photo, group = BarrierGroup.TOILETS, floorNumber = 2)
        val content = report.content(QuickPosition(point = point, accuracyMeters = 6.0, isInBuilding = true))
        assertEquals(SubmissionKind.QUICK, content.kind)
        assertEquals(point, content.location)
        assertEquals(listOf("zdjecie-1"), content.photoIds)
        assertEquals(6.0, content.accuracyMeters)
        assertEquals(BarrierGroup.TOILETS, content.barrierGroup)
        assertEquals(SubmissionFloor(number = 2), content.floor)
        assertNull(content.buildingSymbol)
    }

    @Test
    fun `poza konturem bez piętra`() {
        val report = QuickReport(photo = photo, group = BarrierGroup.TOILETS, floorNumber = 2)
        val content = report.content(QuickPosition(point = point, accuracyMeters = 6.0, isInBuilding = false))
        assertNull(content.floor)
    }

    /**
     * Wiersz: V4 krok 2 w budynku szczegółowym, piętro z danych PWr → zgłoszenie z identyfikatorem piętra.
     *
     * Wymagania: US4.5
     */
    @Test
    fun us4_5_detailed_floor_id() {
        val report =
            QuickReport(photo = photo, group = BarrierGroup.TOILETS, floorId = "A-1/p-1", floorLabel = "Piwnica")
        val content = report.content(QuickPosition(point = point, accuracyMeters = 6.0, isInBuilding = true))
        assertEquals(SubmissionFloor(floorId = "A-1/p-1"), content.floor)
    }

    @Test
    fun `nie wiem zostawia piętro puste`() {
        val report = QuickReport(photo = photo, group = BarrierGroup.TOILETS)
        val content = report.content(QuickPosition(point = point, accuracyMeters = 6.0, isInBuilding = true))
        assertNull(content.floor)
    }
}
