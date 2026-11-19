/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.quick

import io.github.dostepnykampus.core.data.submission.sync.PendingPhotoDraft
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.core.model.room.RoomCategory
import io.github.dostepnykampus.core.model.submission.BarrierGroup
import io.github.dostepnykampus.core.model.submission.SubmissionFloor
import io.github.dostepnykampus.core.model.submission.SubmissionKind
import io.github.dostepnykampus.feature.capture.navigation.ReportPlace
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Zgłoszenie szybkie V4: pytanie o piętro i treść z bieżącą pozycją. Wymagania: US4.1, US4.3, US4.5, BR-14 */
class QuickReportTest {
    private val point = GeoPoint(longitude = 17.06209, latitude = 51.10785)
    private val photo = PendingPhotoDraft(photoId = "zdjecie-1", path = "/zdjecia/zdjecie-1.jpg")

    @Test
    fun asksFloorInBuilding() {
        val withFloor = BarrierGroup.entries - BarrierGroup.OTHER
        withFloor.forEach { assertTrue(it.asksFloor(isInBuilding = true)) }
        assertFalse(BarrierGroup.OTHER.asksFloor(isInBuilding = true))
    }

    @Test
    fun skipsFloorOutsideBuilding() {
        assertFalse(BarrierGroup.TOILETS.asksFloor(isInBuilding = false))
    }

    @Test
    fun asksFloorAtUnknownPosition() {
        assertTrue(BarrierGroup.VERTICAL_CIRCULATION.asksFloor(isInBuilding = null))
        assertFalse(BarrierGroup.OTHER.asksFloor(isInBuilding = null))
    }

    @Test
    fun buildsBodyWithGroupAndFloor() {
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
    fun dropsFloorOutsideContour() {
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
    fun usesDetailedFloorId() {
        val report =
            QuickReport(photo = photo, group = BarrierGroup.TOILETS, floorId = "A-1/p-1", floorLabel = "Piwnica")
        val content = report.content(QuickPosition(point = point, accuracyMeters = 6.0, isInBuilding = true))
        assertEquals(SubmissionFloor(floorId = "A-1/p-1"), content.floor)
    }

    @Test
    fun leavesFloorEmptyOnUnknown() {
        val report = QuickReport(photo = photo, group = BarrierGroup.TOILETS)
        val content = report.content(QuickPosition(point = point, accuracyMeters = 6.0, isInBuilding = true))
        assertNull(content.floor)
    }

    /**
     * Zgłoszenie przy wejściu wybranym w arkuszu wejścia ma punkt, budynek i piętro wejścia, bez dokładności
     * pozycji; piętro z kroku 2 nie zastępuje piętra wejścia.
     * Scenariusz (częściowo): zgloszenie-szybkie.feature: Zgłoszenie przy wejściu ma miejsce w punkcie wybranego
     *   wejścia
     *
     * Wymagania: US4.6, BR-8
     */
    @Test
    fun takesPlaceFromEntrance() {
        val entrance =
            Entrance(
                id = "node/6",
                buildingSymbol = "A-1",
                name = null,
                isMain = true,
                type = "main",
                location = point,
                marking = EntranceMarking.ACCESSIBLE,
                floorId = "A-1/p0",
            )
        val report = QuickReport(photo = photo, group = BarrierGroup.ENTRANCE_AND_DOORS, floorNumber = 2)
        val content = report.content(entrancePosition(entrance))
        assertEquals(point, content.location)
        assertNull(content.accuracyMeters)
        assertEquals("node/6", content.entranceId)
        assertEquals(SubmissionFloor(floorId = "A-1/p0"), content.floor)
    }

    /**
     * Zgłoszenie przy obiekcie planu ma punkt obiektu, jego budynek i piętro oglądanego planu, bez dokładności
     * pozycji i bez wejścia.
     * Scenariusz (częściowo, przykład: arkusz windy): zgloszenie-szybkie.feature: Zgłoszenie przy obiekcie
     *   planu ma miejsce w punkcie obiektu i piętro planu
     *
     * Wymagania: US4.6, BR-8, BR-14
     */
    @Test
    fun takesPlaceFromPlanObject() {
        val place = ReportPlace(RoomCategory.ELEVATOR, "A-1", "A-1/p2", "2 piętro", point.latitude, point.longitude)
        val report = QuickReport(photo = photo, group = BarrierGroup.VERTICAL_CIRCULATION, floorNumber = 1)
        val content = report.content(placePosition(place))
        assertEquals(point, content.location)
        assertNull(content.accuracyMeters)
        assertNull(content.entranceId)
        assertEquals(SubmissionFloor(floorId = "A-1/p2"), content.floor)
    }
}
