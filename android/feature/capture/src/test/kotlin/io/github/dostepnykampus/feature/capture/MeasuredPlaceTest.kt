/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import io.github.dostepnykampus.core.geo.BuildingHint
import io.github.dostepnykampus.core.location.PositionAverage
import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.feature.capture.navigation.ReportFormRoute
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** „Dalej” z C1 do C2: miejsce z pomiaru i budynek po decyzji Zbierającego (US6, US6.2, BR-G4). */
class MeasuredPlaceTest {
    private val point = GeoPoint(longitude = 17.06209, latitude = 51.10785)
    private val average = PositionAverage(point, accuracyMeters = 0.8, fixCount = 30)
    private val a1 = Building("A-1", "Gmach Główny", GeoPolygon(listOf(List(4) { point })))

    private fun finished(place: PlaceChoice) = MeasureUiState.Finished(average, emptyMap(), place = place)

    // Wiersz: C1, „Dalej” → C2, US6: budynek potwierdzony z nazwą idzie do formularza.
    @Test
    fun us6_chosen_building_to_c2() {
        val route = finished(PlaceChoice.Chosen(a1)).reportFormRoute()
        assertEquals(ReportFormRoute(17.06209, 51.10785, 0.8, "A-1", "Gmach Główny"), route)
    }

    // Wiersz: C1, „Poza budynkiem” → C2 bez budynku i piętra, BR-G4.
    @Test
    fun brg4_outside_to_c2() {
        val route = finished(PlaceChoice.Outside).reportFormRoute()
        assertEquals(ReportFormRoute(17.06209, 51.10785, 0.8, buildingSymbol = null), route)
    }

    // Wiersz: C1, „Czy to budynek A-1?” → „Dalej” czeka na decyzję, US6.2.
    @Test
    fun us6_2_no_next_before_decision() {
        assertNull(finished(PlaceChoice.Locating).reportFormRoute())
        assertNull(finished(PlaceChoice.Suggested(BuildingHint(a1, 3.0, isInside = false))).reportFormRoute())
        assertNull(MeasureUiState.Searching.reportFormRoute())
    }

    @Test
    fun us7_4_place_from_route() {
        val place = ReportFormRoute(17.06209, 51.10785, 0.8, "A-1", "Gmach Główny").place()
        assertEquals(A1.copy(buildingName = "Gmach Główny"), place)
    }
}
