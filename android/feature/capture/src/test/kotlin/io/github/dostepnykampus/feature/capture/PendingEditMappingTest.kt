/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import io.github.dostepnykampus.core.data.submissions.EditedSubmission
import io.github.dostepnykampus.core.data.submissions.PendingEdit
import io.github.dostepnykampus.core.data.submissions.PendingPhotoDraft
import io.github.dostepnykampus.core.model.BarrierType
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.Severity
import io.github.dostepnykampus.core.model.SubmissionContent
import io.github.dostepnykampus.core.model.SubmissionFloor
import io.github.dostepnykampus.core.model.SubmissionKind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Pola C2 z treści zapisanej w kolejce telefonu. Wymagania: US8.3, BR-G4 */
class PendingEditMappingTest {
    private val photo = PendingPhotoDraft(photoId = "p1", path = "/data/p1.jpg")

    @Test
    fun `us8_3 pola z treści`() {
        val state = edit(floor = SubmissionFloor(number = -1, isMezzanine = true)).toReportUiState()

        assertEquals("A-1", state.form.place.buildingSymbol)
        assertEquals(ReportFloor.Numbered(number = -1, isMezzanine = true), state.form.floor)
        assertEquals(BarrierType.STAIRS_AT_ENTRANCE, state.form.barrierType)
        assertEquals(Severity.BLOCKING, state.form.severity)
        assertEquals(listOf(photo), state.photos)
        assertEquals(EditedSubmission("s1", baseVersion = null), state.form.editedSubmission)
    }

    @Test
    fun `us7_2 piętro z planu`() {
        val state = edit(floor = SubmissionFloor(floorId = "A-1-0")).toReportUiState()

        assertEquals(ReportFloor.Planned("A-1-0"), state.form.floor)
    }

    @Test
    fun `brg4 poza budynkiem bez piętra`() {
        val state = edit(floor = SubmissionFloor(number = 1), building = null).toReportUiState()

        assertNull(state.form.floor)
        assertNull(state.form.place.buildingSymbol)
    }

    @Test
    fun `nieznane kody puste`() {
        val state = edit(floor = SubmissionFloor(), type = "TB-99", severity = null).toReportUiState()

        assertNull(state.form.floor)
        assertNull(state.form.barrierType)
        assertNull(state.form.severity)
        assertEquals("", state.form.description)
    }

    private fun edit(
        floor: SubmissionFloor,
        building: String? = "A-1",
        type: String = "TB-1",
        severity: Int? = 3,
    ): PendingEdit =
        PendingEdit(
            edited = EditedSubmission("s1", baseVersion = null),
            content =
                SubmissionContent(
                    kind = SubmissionKind.PRECISE,
                    location = GeoPoint(longitude = 17.06209, latitude = 51.10785),
                    photoIds = listOf(photo.photoId),
                    buildingSymbol = building,
                    floor = floor,
                    barrierType = type,
                    severity = severity,
                ),
            photos = listOf(photo),
        )
}
