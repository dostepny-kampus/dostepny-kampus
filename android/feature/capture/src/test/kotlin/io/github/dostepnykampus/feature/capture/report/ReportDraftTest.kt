/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Yevhenii Lukianenko, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.report

import io.github.dostepnykampus.core.data.submission.sync.EditedSubmission
import io.github.dostepnykampus.core.data.submission.sync.PendingPhotoDraft
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.submission.BarrierType
import io.github.dostepnykampus.core.model.submission.Severity
import io.github.dostepnykampus.feature.capture.report.form.ReportEdit
import io.github.dostepnykampus.feature.capture.report.form.ReportFloor
import io.github.dostepnykampus.feature.capture.report.form.ReportForm
import io.github.dostepnykampus.feature.capture.report.form.ReportPlace
import io.github.dostepnykampus.feature.capture.report.form.ReportStage
import io.github.dostepnykampus.feature.capture.report.form.ReportUiState
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** „Zmień miejsce” na C2: powrót do C1 z nowym miejscem zostawia wpisane pola. Wymagania: US7.4, NFR-5, NFR-7 */
class ReportDraftTest {
    private val draft = ReportDraft()
    private val floor = ReportFloor.Planned("A-1-0")
    private val photo = PendingPhotoDraft(photoId = "p1", path = "/data/p1.jpg")
    private val edited = EditedSubmission(id = "s1", baseVersion = 2)
    private val moved = A1.copy(location = GeoPoint(longitude = 17.0622, latitude = 51.1079))

    /**
     * Wiersz: C2 „Zmień miejsce” → C1 → „Dalej”, typ, uciążliwość i zdjęcie zostają.
     * Scenariusz: pomiar-precyzyjny.feature: Powrót do pomiaru zmienia miejsce, a wpisane pola zostają
     *
     * Wymagania: US7.4
     */
    @Test
    fun keepsFormOnPlaceChange() {
        val first = filled()
        first.changePlace()
        assertEquals(ReportStage.PLACE_CHANGED, first.uiState.value?.stage)

        val form = reopened(moved, draft = restored(draft))
        assertEquals(moved, form.form.place)
        assertEquals(floor, form.form.floor)
        assertEquals(BarrierType.THRESHOLD, form.form.barrierType)
        assertEquals(Severity.ONLY_WITH_HELP, form.form.severity)
        assertEquals(listOf(photo), form.photos)
        assertEquals(listOf(photo.photoId), form.form.photoIds)
        assertEquals("Próg przy drzwiach", form.form.description)
        assertEquals(edited, form.form.editedSubmission)
        assertEquals(ReportStage.EDITING, form.stage)
    }

    /** Wiersz: C2 „Zmień miejsce” do innego budynku albo na ulicę → piętro znika. Wymagania: US7.4 */
    @Test
    fun keepsFloorInSameBuilding() {
        filled().changePlace()
        assertNull(reopened(A1.copy(buildingSymbol = "C-13")).form.floor)
        filled().changePlace()
        assertNull(reopened(A1.copy(buildingSymbol = null)).form.floor)
    }

    @Test
    fun restoresEditing() {
        val viewModel = filled()
        val recreated = restored(draft)
        assertEquals(viewModel.uiState.value, reopened(A1, draft = recreated))
        viewModel.open(ReportForm(place = A1), recreated)
        viewModel.edit(ReportEdit.Description("Nowy opis"))
        assertEquals("Nowy opis", reopened(A1, draft = restored(recreated)).form.description)
    }

    @Test
    fun clearsDraftOnDiscard() {
        filled().confirmExit()
        assertEquals(ReportForm(place = A1), reopened(A1).form)
    }

    /** Wiersz: proces zabity na C2 → sprzątanie przy starcie zna plik zdjęcia odtworzonego szkicu. */
    @Test
    fun restoresDraftPhoto() {
        filled()
        assertEquals(listOf(photo.path), restored(draft).photoPaths())
        draft.clear()
        assertEquals(emptyList<String>(), draft.photoPaths())
    }

    private fun restored(draft: ReportDraft): ReportDraft = Json.decodeFromString(Json.encodeToString(draft))

    private fun filled(): ReportFormViewModel =
        ReportFormViewModel { _, _, _ -> }.apply {
            open(ReportForm(place = A1, editedSubmission = edited), draft)
            edit(ReportEdit.Floor(floor))
            edit(ReportEdit.Type(BarrierType.THRESHOLD))
            edit(ReportEdit.Rating(Severity.ONLY_WITH_HELP))
            edit(ReportEdit.Photo(photo))
            edit(ReportEdit.Description("Próg przy drzwiach"))
        }

    private fun reopened(place: ReportPlace, draft: ReportDraft = this.draft): ReportUiState {
        val viewModel = ReportFormViewModel { _, _, _ -> }
        viewModel.open(ReportForm(place = place), draft)
        return checkNotNull(viewModel.uiState.value)
    }
}
