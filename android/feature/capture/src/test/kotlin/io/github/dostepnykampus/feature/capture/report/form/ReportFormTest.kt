/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.report.form

import io.github.dostepnykampus.core.model.submission.BarrierGroup
import io.github.dostepnykampus.core.model.submission.BarrierType
import io.github.dostepnykampus.core.model.submission.Severity
import io.github.dostepnykampus.core.model.submission.SubmissionContent
import io.github.dostepnykampus.core.model.submission.SubmissionFloor
import io.github.dostepnykampus.core.model.submission.SubmissionKind
import io.github.dostepnykampus.feature.capture.report.A1
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Formularz C2: bez typu, uciążliwości, zdjęcia i piętra w budynku nie ma zapisu; TB-29 wymaga opisu.
 *
 * Wymagania: BR-7
 */
class ReportFormTest {
    @Test
    fun listsAllMissingWhenEmpty() {
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
    fun missesOnlyType() {
        assertEquals(listOf(ReportFormError.MISSING_BARRIER_TYPE), complete().copy(barrierType = null).errors())
    }

    @Test
    fun missesOnlyPhoto() {
        assertEquals(listOf(ReportFormError.MISSING_PHOTO), complete().copy(photoIds = emptyList()).errors())
    }

    @Test
    fun missesFloorInBuilding() {
        val noFloor = complete().copy(floor = null)

        assertEquals(ReportCheck.Incomplete(listOf(ReportFormError.MISSING_FLOOR)), noFloor.check())
    }

    @Test
    fun requiresDescriptionForOther() {
        val other = complete().copy(barrierType = BarrierType.OTHER_BARRIER)

        assertEquals(listOf(ReportFormError.MISSING_DESCRIPTION), other.copy(description = "").errors())
        assertEquals(listOf(ReportFormError.MISSING_DESCRIPTION), other.copy(description = "  \n").errors())
        assertEquals(emptyList<ReportFormError>(), other.copy(description = "Brak poręczy").errors())
    }

    @Test
    fun buildsMeasurementWhenComplete() {
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
    fun turnsEmptyDescriptionToNull() {
        val check = complete().check() as ReportCheck.Complete

        assertNull(check.content.description)
    }

    /**
     * W budynku podstawowym B-8 wpis tekstu daje piętro w treści zgłoszenia; „1,5” nie jest liczbą całkowitą, więc
     * piętra brak i formularz się nie zapisuje.
     * Scenariusz: budynki-bez-szczegolow.feature: Piętro w budynku podstawowym wpisuje się jako liczbę całkowitą
     *
     * Wymagania: US7.2
     */
    @Test
    fun acceptsBasicBuildingFloor() {
        val examples =
            listOf(
                Triple("0", false, SubmissionFloor(number = 0, isMezzanine = false)),
                Triple("2", false, SubmissionFloor(number = 2, isMezzanine = false)),
                Triple("-1", false, SubmissionFloor(number = -1, isMezzanine = false)),
                Triple("1", true, SubmissionFloor(number = 1, isMezzanine = true)),
            )
        examples.forEach { (typed, isMezzanine, expected) ->
            val check = basicBuilding(typed, isMezzanine).check() as ReportCheck.Complete
            assertEquals(expected, check.content.floor, typed)
        }
        assertEquals(ReportCheck.Incomplete(listOf(ReportFormError.MISSING_FLOOR)), basicBuilding("1,5").check())
    }

    @Test
    fun acceptsTypedFloor() {
        val form = complete().copy(floor = ReportFloor.Numbered(number = -1, isMezzanine = true))
        val check = form.check() as ReportCheck.Complete

        assertEquals(SubmissionFloor(number = -1, isMezzanine = true), check.content.floor)
    }

    /**
     * Budynek bez planów pięter przyjmuje zgłoszenie z piętrem wpisanym liczbą.
     * Scenariusz: budynki-bez-szczegolow.feature: Zgłoszenie w budynku po nieudanym imporcie planów
     *
     * Wymagania: BR-G3
     */
    @Test
    fun acceptsFloorWithoutPlans() {
        val form =
            complete().copy(
                place = A1.copy(buildingSymbol = "B-1"),
                floor = ReportFloor.Numbered(number = 3, isMezzanine = false),
            )
        val check = form.check() as ReportCheck.Complete

        assertEquals("B-1", check.content.buildingSymbol)
        assertEquals(SubmissionFloor(number = 3, isMezzanine = false), check.content.floor)
    }

    @Test
    fun dropsFloorOutside() {
        val outside = A1.copy(buildingSymbol = null)
        val check = complete().copy(place = outside, floor = null).check() as ReportCheck.Complete

        assertNull(check.content.buildingSymbol)
        assertNull(check.content.floor)
        assertThrows<IllegalArgumentException> { ReportForm(place = outside, floor = ReportFloor.Planned("A-1-0")) }
    }

    @Test
    fun marksAcceptedChange() {
        val check = complete().copy(replacesSubmissionId = "s-old").check() as ReportCheck.Complete

        assertEquals("s-old", check.content.replacesSubmissionId)
    }

    @Test
    fun showsFieldChange() {
        val start = ReportForm(place = A1)

        assertEquals(start, start.copy())
        assertEquals(false, start == start.copy(description = "x"))
    }

    @Test
    fun parsesFloorNumber() {
        assertEquals(0, parseFloorNumber("0"))
        assertEquals(-2, parseFloorNumber(" -2 "))
        assertEquals(12, parseFloorNumber("12"))
        assertNull(parseFloorNumber(""))
        assertNull(parseFloorNumber("1,5"))
        assertNull(parseFloorNumber("parter"))
    }

    private fun basicBuilding(typed: String, isMezzanine: Boolean = false): ReportForm =
        complete().copy(
            place = A1.copy(buildingSymbol = "B-8"),
            floor = parseFloorNumber(typed)?.let { ReportFloor.Numbered(number = it, isMezzanine = isMezzanine) },
        )

    private fun complete(): ReportForm =
        ReportForm(
            place = A1,
            floor = ReportFloor.Planned(floorId = "A-1-0"),
            barrierType = BarrierType.THRESHOLD,
            severity = Severity.ONLY_WITH_HELP,
            photoIds = listOf("p1"),
        )
}
