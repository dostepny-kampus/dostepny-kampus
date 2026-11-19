/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.plan

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.room.RoomCategory
import io.github.dostepnykampus.feature.map.overlay.SheetExits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Arkusz toalety dostosowanej i schodów po dotknięciu znaku planu. Wymagania: US16.1, NFR-18, NFR-53 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class PlanSignSheetTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val clicks = mutableListOf<String>()

    /**
     * Wiersz: toaleta dostosowana na planie ma nagłówek z fokusem, „Zamknij”, wiersz budynku i piętro z danych.
     * Scenariusz (częściowo, przykład: dotknie znaku toalety dostosowanej na planie A-1): przegladanie-mapy.feature:
     *   Wiersz budynku w arkuszu otwiera kartę budynku bez przelotu mapy
     *
     * Wymagania: US16.1, US2.8, NFR-18
     */
    @Test
    fun showsToilet() {
        show("wc")

        compose.onNodeWithText("Toaleta dostosowana").assertIsFocused()
        compose.onNodeWithText("Parter").assertExists()
        compose.onNodeWithText("Budynek A-1, parter").assertDoesNotExist()
        compose.onNodeWithText("Zamknij").assertHeightIsAtLeast(48.dp).performClick()
        compose.onNodeWithContentDescription("Budynek A-1").assertHeightIsAtLeast(48.dp).performClick()
        assertEquals("„Zamknij”, potem wiersz budynku", listOf("zamknij", "budynek A-1"), clicks)
    }

    /** Wiersz: schody na planie mają nagłówek „Schody”, wiersz budynku i piętro z danych PWr. Wymagania: US16.1 */
    @Test
    fun showsStairs() {
        show("schody")

        compose.onNodeWithText("Schody").assertExists()
        compose.onNodeWithContentDescription("Budynek A-1").assertExists()
        compose.onNodeWithText("1 piętro").assertExists()
    }

    /** Pomieszczenie spoza planu nie ma arkusza. Wymagania: US16.1 */
    @Test
    fun skipsUnknownRoom() {
        assertNull("pomieszczenie spoza planu", SIGN_PLAN.signOf("brak"))
    }

    /** Wiersz: przy czcionce 200% arkusz mieści nagłówek i miejsce. Wymagania: NFR-20 */
    @Test
    @Config(fontScale = 2f)
    fun matchesLargeFontMockup() {
        show("wc")
        compose.onRoot().captureRoboImage()
    }

    /** Zrzut arkusza toalety dostosowanej, motyw jasny. */
    @Test
    fun matchesLightMockup() {
        show("wc")
        compose.onRoot().captureRoboImage()
    }

    /** Zrzut arkusza toalety dostosowanej, motyw ciemny. */
    @Test
    @Config(qualifiers = "+night")
    fun matchesDarkMockup() {
        show("wc")
        compose.onRoot().captureRoboImage()
    }

    /**
     * Wiersz: arkusz schodów → „Zgłoś barierę przy schodach” na końcu, TalkBack z budynkiem i piętrem; dotknięcie
     *   przekazuje schody z punktem znaku i piętrem oglądanego planu.
     * Scenariusz (częściowo, przykład: arkusz schodów): zgloszenie-szybkie.feature: Zgłoszenie przy obiekcie
     *   planu ma miejsce w punkcie obiektu i piętro planu
     *
     * Wymagania: US4.6, BR-8, BR-14
     */
    @Test
    fun reportsAtStairs() {
        val reports = mutableListOf<PlanReport>()
        compose.showPlanSheet(id = "schody", onReport = { reports += it })
        compose.onNodeWithContentDescription("Zgłoś barierę przy schodach: Budynek A-1, 1 piętro").performClick()
        val point = GeoPoint(longitude = 0.5, latitude = 0.5)
        assertEquals(
            "Przycisk w liście schodów nie zgłasza tych schodów",
            listOf(PlanReport(RoomCategory.STAIRCASE, "A-1", "f1", "1 piętro", point)),
            reports,
        )
    }

    private fun show(id: String) =
        compose.showPlanSheet(
            id = id,
            exits = SheetExits(onClose = { clicks += "zamknij" }, onBuilding = { clicks += "budynek $it" }),
        )
}
