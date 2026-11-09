/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import io.github.dostepnykampus.core.data.submissions.PendingPhotoDraft
import io.github.dostepnykampus.core.model.BarrierType
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.Severity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** „Zmień miejsce” na C2: powrót do C1 z nowym miejscem zostawia wpisane pola (US7.4). */
class ReportDraftTest {
    private val draft = ReportDraft()
    private val floor = ReportFloor.Planned("A-1-0")
    private val photo = PendingPhotoDraft(photoId = "p1", path = "/data/p1.jpg")
    private val moved = A1.copy(location = GeoPoint(longitude = 17.0622, latitude = 51.1079))

    // Wiersz: C2 „Zmień miejsce” → C1 → „Dalej”, typ, uciążliwość i zdjęcie zostają, US7.4.
    @Test
    fun us7_4_change_place_keeps_form() {
        val first = filled()
        first.changePlace()
        assertEquals(ReportStage.LEFT, first.uiState.value?.stage)

        val form = reopened(moved)
        assertEquals(moved, form.form.place)
        assertEquals(floor, form.form.floor)
        assertEquals(BarrierType.THRESHOLD, form.form.barrierType)
        assertEquals(Severity.ONLY_WITH_HELP, form.form.severity)
        assertEquals(listOf(photo), form.photos)
        assertEquals(ReportStage.EDITING, form.stage)
    }

    // Wiersz: C2 „Zmień miejsce” do innego budynku albo na ulicę → piętro znika, US7.4.
    @Test
    fun us7_4_floor_same_building() {
        filled().changePlace()
        assertNull(reopened(A1.copy(buildingSymbol = "C-13")).form.floor)
        filled().changePlace()
        assertNull(reopened(A1.copy(buildingSymbol = null)).form.floor)
    }

    @Test
    fun `pola wracają tylko raz`() {
        filled().changePlace()
        reopened(moved)
        assertEquals(ReportForm(place = A1), reopened(A1).form)
        filled().changePlace()
        draft.clear()
        assertEquals(ReportForm(place = A1), reopened(A1).form)
    }

    private fun filled(): ReportFormViewModel =
        ReportFormViewModel(draft = draft) { _, _, _ -> }.apply {
            open(ReportForm(place = A1))
            edit(ReportEdit.Floor(floor))
            edit(ReportEdit.Type(BarrierType.THRESHOLD))
            edit(ReportEdit.Rating(Severity.ONLY_WITH_HELP))
            edit(ReportEdit.Photo(photo))
        }

    private fun reopened(place: ReportPlace): ReportUiState {
        val viewModel = ReportFormViewModel(draft = draft) { _, _, _ -> }
        viewModel.open(ReportForm(place = place))
        return checkNotNull(viewModel.uiState.value)
    }
}
