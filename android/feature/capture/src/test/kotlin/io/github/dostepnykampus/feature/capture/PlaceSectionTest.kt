/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.geo.BuildingHint
import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Podpowiedź budynku po pomiarze na C1 (US6.2, BR-G2, BR-G4). */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class PlaceSectionTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val a1 = building(symbol = "A-1", name = "Gmach Główny")
    private val b1 = building(symbol = "B-1", name = "Budynek B-1")
    private val actions = mutableListOf<String>()

    private fun show(place: PlaceChoice) {
        val record =
            PlaceActions(
                onConfirm = { actions += "confirm" },
                onChoose = { actions += it },
                onOutside = { actions += "outside" },
            )
        compose.setContent {
            DostepnyKampusTheme { PlaceSection(place = place, buildings = Buildings(listOf(a1, b1)), actions = record) }
        }
    }

    // Wiersz: „Czy to budynek A-1?”, pod pytaniem nazwa z danych i odległość od punktu.
    @Test
    fun us6_2_hint_name_and_distance() {
        show(PlaceChoice.Suggested(BuildingHint(a1, distanceMeters = 12.4, isInside = false)))
        compose.onNodeWithText("Czy to budynek A-1?").assertIsDisplayed()
        compose.onNodeWithText("Gmach Główny").assertIsDisplayed()
        compose.onNodeWithText("12 m od punktu").assertIsDisplayed()
        compose.onNodeWithText("Potwierdź").performClick()
        assertEquals(listOf("confirm"), actions)
    }

    @Test
    fun us6_2_choose_from_list() {
        show(PlaceChoice.Suggested(BuildingHint(a1, distanceMeters = 0.0, isInside = true)))
        compose.onNodeWithText("Punkt leży w obrysie budynku").assertIsDisplayed()
        compose.onNodeWithText("Wybierz inny").performClick()
        compose.onNodeWithText("B-1 Budynek B-1").performClick()
        assertEquals(listOf("B-1"), actions)
    }

    @Test
    fun br_g2_leave_outside() {
        show(PlaceChoice.Suggested(BuildingHint(a1, distanceMeters = 30.0, isInside = false)))
        compose.onNodeWithText("Poza budynkiem").performClick()
        assertEquals(listOf("outside"), actions)
    }

    // Daleko od budynków: „poza budynkiem” bez komunikatu o błędzie, a budynek można wybrać (BR-G4).
    @Test
    fun br_g4_outside_without_error() {
        show(PlaceChoice.Outside)
        compose.onNodeWithText("Miejsce: poza budynkiem").assertIsDisplayed()
        compose.onNodeWithText("Wybierz budynek").assertIsDisplayed()
    }

    @Test
    fun us6_2_chosen_building() {
        show(PlaceChoice.Chosen(b1))
        compose.onNodeWithText("Budynek: B-1 Budynek B-1").assertIsDisplayed()
    }

    private fun building(symbol: String, name: String): Building =
        Building(
            symbol = symbol,
            name = name,
            outline = GeoPolygon(listOf(List(4) { GeoPoint(longitude = 17.06, latitude = 51.11) })),
        )
}
