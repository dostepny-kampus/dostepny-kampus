/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.github.dostepnykampus.core.data.barriers.AcceptedBarrier
import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.core.model.Severity
import io.github.dostepnykampus.core.model.SubmissionFloor
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Miejsce bariery słowami do arkusza bariery na V1. Wymagania: US1.5 */
@RunWith(RobolectricTestRunner::class)
class BarrierPlaceTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** Wiersz: arkusz bariery, miejsce to budynek i wejście albo piętro. Wymagania: US1.5 */
    @Test
    fun us1_5_building_entrance_floor() {
        val barrier = barrier(symbol = "A-1", floor = SubmissionFloor(number = 0))
        val main = building(symbol = "A-1", name = "Gmach Główny")
        show(ChosenBarrier(barrier = barrier, building = main, entrance = ENTRANCE))

        compose.onNodeWithText("A-1 Gmach Główny · Wejście Cesarskie, parter").assertExists()
    }

    /** Wiersz: budynek bez nazwy to „Budynek C-13”, piętro z dopiskiem „półpiętro”. Wymagania: US1.5 */
    @Test
    fun us1_5_no_name_mezzanine() {
        val barrier = barrier(symbol = "C-13", floor = SubmissionFloor(number = 2, isMezzanine = true))
        show(ChosenBarrier(barrier = barrier, building = building(symbol = "C-13", name = null), entrance = null))

        compose.onNodeWithText("Budynek C-13 · piętro 2, półpiętro").assertExists()
    }

    /** Wiersz: bariera poza budynkiem ma „Poza budynkiem”. Wymagania: US1.5, BR-G2 */
    @Test
    fun us1_5_outside_building() {
        show(ChosenBarrier(barrier = barrier(symbol = null, floor = null), building = null, entrance = null))

        compose.onNodeWithText("Poza budynkiem").assertExists()
    }

    private fun show(chosen: ChosenBarrier) {
        compose.setContent { Text(barrierPlace(chosen)) }
    }

    private fun building(symbol: String, name: String?): Building {
        val corners = listOf(0.0 to 0.0, 1.0 to 0.0, 1.0 to 1.0, 0.0 to 0.0)
        val outline = GeoPolygon(listOf(corners.map { (lon, lat) -> GeoPoint(longitude = lon, latitude = lat) }))
        return Building(symbol = symbol, name = name, outline = outline)
    }

    private fun barrier(symbol: String?, floor: SubmissionFloor?) =
        AcceptedBarrier(
            id = "z1",
            location = GeoPoint(longitude = 17.06, latitude = 51.107),
            severity = Severity.BLOCKING,
            type = null,
            buildingSymbol = symbol,
            verifiedAt = null,
            floor = floor,
        )

    private companion object {
        const val ENTRANCE = "Wejście Cesarskie"
    }
}
