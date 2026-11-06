/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import io.github.dostepnykampus.core.model.BarrierGroup
import io.github.dostepnykampus.core.model.BarrierType
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.Severity
import io.github.dostepnykampus.core.model.SubmissionContent
import io.github.dostepnykampus.core.model.SubmissionFloor
import io.github.dostepnykampus.core.model.SubmissionKind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/** Formularz C2: bez typu, uciążliwości, zdjęcia i piętra w budynku nie ma zapisu; TB-29 wymaga opisu (BR-7). */
class ReportFormTest {
    @Test
    fun `pusty ma wszystkie braki`() {
        assertEquals(
            ReportCheck.Incomplete(
                listOf(
                    ReportFormError.MISSING_FLOOR,
                    ReportFormError.MISSING_BARRIER_TYPE,
                    ReportFormError.MISSING_SEVERITY,
                    ReportFormError.MISSING_PHOTO,
                ),
            ),
            ReportForm(place = A1).check(),
        )
    }

    @Test
    fun `brak tylko typu bariery`() {
        assertEquals(listOf(ReportFormError.MISSING_BARRIER_TYPE), complete().copy(barrierType = null).errors())
    }

    @Test
    fun `brak tylko zdjęcia`() {
        assertEquals(listOf(ReportFormError.MISSING_PHOTO), complete().copy(photoIds = emptyList()).errors())
    }

    @Test
    fun `w budynku brak piętra`() {
        val noFloor = complete().copy(floor = null)

        assertEquals(ReportCheck.Incomplete(listOf(ReportFormError.MISSING_FLOOR)), noFloor.check())
    }

    @Test
    fun `TB-29 wymaga opisu`() {
        val other = complete().copy(barrierType = BarrierType.OTHER_BARRIER)

        assertEquals(listOf(ReportFormError.MISSING_DESCRIPTION), other.copy(description = "").errors())
        assertEquals(listOf(ReportFormError.MISSING_DESCRIPTION), other.copy(description = "  \n").errors())
        assertEquals(emptyList<ReportFormError>(), other.copy(description = "Brak poręczy").errors())
    }

    @Test
    fun `kompletny daje pomiar`() {
        assertEquals(
            ReportCheck.Complete(
                SubmissionContent(
                    kind = SubmissionKind.PRECISE,
                    location = A1.location,
                    photoIds = listOf("p1"),
                    accuracyMeters = 0.8,
                    isManualCorrection = false,
                    buildingSymbol = "A-1",
                    floor = SubmissionFloor(floorId = "A-1-0"),
                    barrierGroup = BarrierGroup.HORIZONTAL_CIRCULATION,
                    barrierType = "TB-13",
                    severity = 2,
                    description = "Próg przy sekretariacie",
                ),
            ),
            complete().copy(description = "  Próg przy sekretariacie ").check(),
        )
    }

    @Test
    fun `pusty opis to null`() {
        val check = complete().check() as ReportCheck.Complete

        assertNull(check.content.description)
    }

    @Test
    fun `piętro wpisane liczbą`() {
        val form = complete().copy(floor = ReportFloor.Numbered(number = -1, isMezzanine = true))
        val check = form.check() as ReportCheck.Complete

        assertEquals(SubmissionFloor(number = -1, isMezzanine = true), check.content.floor)
    }

    @Test
    fun `poza budynkiem bez piętra`() {
        val outside = A1.copy(buildingSymbol = null)
        val check = complete().copy(place = outside, floor = null).check() as ReportCheck.Complete

        assertNull(check.content.buildingSymbol)
        assertNull(check.content.floor)
        assertThrows<IllegalArgumentException> { ReportForm(place = outside, floor = ReportFloor.Planned("A-1-0")) }
    }

    @Test
    fun `zmiana przyjętego`() {
        val check = complete().copy(replacesSubmissionId = "s-old").check() as ReportCheck.Complete

        assertEquals("s-old", check.content.replacesSubmissionId)
    }

    @Test
    fun `zmiana pola widoczna`() {
        val start = ReportForm(place = A1)

        assertEquals(start, start.copy())
        assertEquals(false, start == start.copy(description = "x"))
    }

    @Test
    fun `numer piętra z tekstu`() {
        assertEquals(0, parseFloorNumber("0"))
        assertEquals(-2, parseFloorNumber(" -2 "))
        assertEquals(12, parseFloorNumber("12"))
        assertNull(parseFloorNumber(""))
        assertNull(parseFloorNumber("1,5"))
        assertNull(parseFloorNumber("parter"))
    }

    private fun complete(): ReportForm =
        ReportForm(
            place = A1,
            floor = ReportFloor.Planned(floorId = "A-1-0"),
            barrierType = BarrierType.THRESHOLD,
            severity = Severity.ONLY_WITH_HELP,
            photoIds = listOf("p1"),
        )
}

private val A1 =
    ReportPlace(
        location = GeoPoint(longitude = 17.06209, latitude = 51.10785),
        accuracyMeters = 0.8,
        buildingSymbol = "A-1",
        isManualCorrection = false,
    )
