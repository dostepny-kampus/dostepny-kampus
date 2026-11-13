/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import io.github.dostepnykampus.core.data.submissions.PendingPhotoDraft
import io.github.dostepnykampus.core.model.BarrierType
import io.github.dostepnykampus.core.model.Severity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Zmiany pól C2: zdjęcia w kolejności, piętro przy zmianie miejsca, braki na bieżąco (US7.1, US7.4). */
class ReportEditTest {
    private val start = ReportUiState(form = ReportForm(place = A1))

    @Test
    fun `każde pole w formularzu`() {
        val state =
            start
                .edited(ReportEdit.Floor(ReportFloor.Numbered(number = 2, isMezzanine = false)))
                .edited(ReportEdit.Type(BarrierType.HEAVY_DOOR))
                .edited(ReportEdit.Rating(Severity.HINDRANCE))
                .edited(ReportEdit.Description("Drzwi bez samozamykacza"))

        assertEquals(
            ReportForm(
                place = A1,
                floor = ReportFloor.Numbered(number = 2, isMezzanine = false),
                barrierType = BarrierType.HEAVY_DOOR,
                severity = Severity.HINDRANCE,
                description = "Drzwi bez samozamykacza",
            ),
            state.form,
        )
    }

    @Test
    fun `zdjęcia w kolejności`() {
        val state = start.edited(ReportEdit.Photo(photo("p1"))).edited(ReportEdit.Photo(photo("p2")))

        assertEquals(listOf("p1", "p2"), state.form.photoIds)
        assertEquals(listOf(photo("p1"), photo("p2")), state.photos)
    }

    @Test
    fun `ten sam budynek, to piętro`() {
        val state =
            start
                .edited(ReportEdit.Floor(ReportFloor.Planned("A-1-0")))
                .edited(ReportEdit.Type(BarrierType.THRESHOLD))
                .edited(ReportEdit.Place(A1.copy(accuracyMeters = 0.5)))

        assertEquals(ReportFloor.Planned("A-1-0"), state.form.floor)
        assertEquals(BarrierType.THRESHOLD, state.form.barrierType)
        assertEquals(0.5, state.form.place.accuracyMeters)
    }

    /**
     * Zmiana miejsca do innego budynku albo na ulicę kasuje piętro, a wybrany typ zostaje (US7.4). Brak pola piętra na
     * ulicy sprawdza `ReportScreenTest`.
     * Scenariusz (częściowo): pomiar-precyzyjny.feature: Piętro zostaje po zmianie miejsca tylko w tym samym budynku
     */
    @Test
    fun `inny budynek kasuje piętro`() {
        val state =
            start
                .edited(ReportEdit.Floor(ReportFloor.Planned("A-1-0")))
                .edited(ReportEdit.Type(BarrierType.STAIRS_AT_ENTRANCE))
                .edited(ReportEdit.Place(A1.copy(buildingSymbol = "C-13")))

        assertNull(state.form.floor)
        assertEquals("C-13", state.form.place.buildingSymbol)
        assertEquals(BarrierType.STAIRS_AT_ENTRANCE, state.form.barrierType)
    }

    @Test
    fun `poza budynkiem bez piętra`() {
        val state =
            start
                .edited(ReportEdit.Floor(ReportFloor.Planned("A-1-0")))
                .edited(ReportEdit.Type(BarrierType.STAIRS_AT_ENTRANCE))
                .edited(ReportEdit.Place(A1.copy(buildingSymbol = null)))

        assertNull(state.form.floor)
        assertEquals(BarrierType.STAIRS_AT_ENTRANCE, state.form.barrierType)
    }

    @Test
    fun `bez zapisz braków nie ma`() {
        assertEquals(emptyList<ReportFormError>(), start.edited(ReportEdit.Description("x")).errors)
    }

    @Test
    fun `brak znika po uzupełnieniu`() {
        val shown = start.copy(errors = start.form.errors(), isSaveAttempted = true)
        val state =
            shown
                .edited(ReportEdit.Floor(ReportFloor.Planned("A-1-0")))
                .edited(ReportEdit.Type(BarrierType.OTHER_BARRIER))
                .edited(ReportEdit.Rating(Severity.BLOCKING))

        assertEquals(listOf(ReportFormError.MISSING_PHOTO, ReportFormError.MISSING_DESCRIPTION), state.errors)
        assertEquals(
            listOf(ReportFormError.MISSING_DESCRIPTION),
            state.edited(ReportEdit.Photo(photo("p1"))).errors,
        )
    }

    @Test
    fun `po zapisz nowy brak od razu`() {
        val complete =
            start
                .copy(errors = start.form.errors(), isSaveAttempted = true)
                .edited(ReportEdit.Floor(ReportFloor.Planned("A-1-0")))
                .edited(ReportEdit.Type(BarrierType.THRESHOLD))
                .edited(ReportEdit.Rating(Severity.BLOCKING))
                .edited(ReportEdit.Photo(photo("p1")))
        assertEquals(emptyList<ReportFormError>(), complete.errors)

        val other = complete.edited(ReportEdit.Type(BarrierType.OTHER_BARRIER))

        assertEquals(listOf(ReportFormError.MISSING_DESCRIPTION), other.errors)
    }

    @Test
    fun `bez budynku piętro pominięte`() {
        val outside = ReportUiState(form = ReportForm(place = A1.copy(buildingSymbol = null)))

        assertNull(outside.edited(ReportEdit.Floor(ReportFloor.Planned("A-1-0"))).form.floor)
    }

    private fun photo(id: String) = PendingPhotoDraft(photoId = id, path = "/data/$id.jpg")
}
