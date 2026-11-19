/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.quick

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.github.dostepnykampus.core.data.submission.sync.PendingPhotoDraft
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.core.model.submission.BarrierGroup
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Krok 3 V4 i potwierdzenie: podsumowanie, zdanie o miejscu i podziękowanie. Wymagania: US4.1, US4.4, US4.5 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class QuickSendStepTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val photo = PendingPhotoDraft(photoId = "zdjecie-1", path = "/brak/zdjecie-1.jpg")

    private val point = GeoPoint(longitude = 17.06, latitude = 51.11)
    private val here = QuickLookup.Found(QuickPosition(point = point, accuracyMeters = 6.0, isInBuilding = true))

    private fun show(report: QuickReport, isFloorAsked: Boolean, lookup: QuickLookup) {
        compose.setContent {
            DostepnyKampusTheme { QuickSendSummary(report = report, isFloorAsked = isFloorAsked, lookup = lookup) }
        }
    }

    /** Wiersz: V4 krok 3 → grupa, piętro i „Dołączymy miejsce, w którym teraz stoisz.”. Wymagania: US4.1, US4.5 */
    @Test
    fun showsSummaryWithFloor() {
        val report = QuickReport(photo = photo, group = BarrierGroup.TOILETS, floorNumber = 2)
        show(report = report, isFloorAsked = true, lookup = here)
        compose.onNodeWithText("Toalety").assertIsDisplayed()
        compose.onNodeWithText("Piętro 2").assertIsDisplayed()
        compose.onNodeWithText("Dołączymy miejsce, w którym teraz stoisz.").assertIsDisplayed()
    }

    /**
     * Wiersz: V4 krok 3 przy wejściu bez nazwy w danych → napis z nazwą z importu i opisem położenia.
     * Scenariusz: zgloszenie-szybkie.feature: Napis wejścia bez nazwy w danych ma opis położenia
     *
     * Wymagania: US4.6, BR-27
     */
    @Test
    fun showsPlaceOfUnnamedEntrance() {
        val entrance =
            Entrance(
                id = "node/3",
                buildingSymbol = "E-3",
                name = "Wejście",
                isMain = false,
                type = null,
                location = point,
                marking = EntranceMarking.NO_DATA,
                isNameGenerated = true,
                placeDescription = "Od strony E-3, od południa.",
            )
        val report = QuickReport(photo = photo, group = BarrierGroup.ENTRANCE_AND_DOORS)
        show(report = report, isFloorAsked = false, lookup = QuickLookup.Found(entrancePosition(entrance)))
        compose.onNodeWithText("Wejście: Wejście, od strony E-3, od południa").assertIsDisplayed()
    }

    /**
     * Wiersz: V4 krok 3 przy wejściu głównym z nazwą nadaną przez import → napis bez cudzysłowu.
     *
     * Wymagania: US4.6, BR-27
     */
    @Test
    fun showsMainWithoutQuotes() {
        val entrance =
            Entrance(
                id = "node/1",
                buildingSymbol = "A-1",
                name = "Wejście główne",
                isMain = true,
                type = null,
                location = point,
                marking = EntranceMarking.ACCESSIBLE,
                isNameGenerated = true,
            )
        val report = QuickReport(photo = photo, group = BarrierGroup.ENTRANCE_AND_DOORS)
        show(report = report, isFloorAsked = false, lookup = QuickLookup.Found(entrancePosition(entrance)))
        compose.onNodeWithText("Wejście: Wejście główne").assertIsDisplayed()
    }

    /** Wiersz: V4 krok 3, „Nie wiem” → podsumowanie z „Nie wiem”. Wymagania: US4.5 */
    @Test
    fun showsUnknownFloor() {
        show(QuickReport(photo = photo, group = BarrierGroup.TOILETS), isFloorAsked = true, lookup = here)
        compose.onNodeWithText("Nie wiem").assertIsDisplayed()
    }

    /** Wiersz: V4 krok 3, pozycja jeszcze nieznana → „Ustalamy, gdzie jesteś…”. Wymagania: NFR-23 */
    @Test
    fun showsPositionPending() {
        val report = QuickReport(photo = photo, group = BarrierGroup.OTHER)
        show(report = report, isFloorAsked = false, lookup = QuickLookup.Locating)
        compose.onNodeWithText("Ustalamy, gdzie jesteś…").assertIsDisplayed()
        compose.onNodeWithText("Nie wiem").assertDoesNotExist()
    }

    /** Wiersz: V4 brak pozycji → komunikat, co zrobić, zamiast „Ustalamy…”; grupa zostaje. Wymagania: UC7 */
    @Test
    fun showsPositionLost() {
        show(QuickReport(photo = photo, group = BarrierGroup.TOILETS), isFloorAsked = false, lookup = QuickLookup.Lost)
        compose.onNodeWithText("Telefon nie ustalił, gdzie jesteś.", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Toalety").assertIsDisplayed()
        compose.onNodeWithText("Ustalamy, gdzie jesteś…").assertDoesNotExist()
    }

    /**
     * Wiersz: V4 stan „Poza terenem PWr” → komunikat zamiast zdania o miejscu; grupa zostaje.
     * Scenariusz (częściowo): zgloszenie-szybkie.feature: Zgłoszenie szybkie tylko na terenie PWr
     *
     * Wymagania: US4.9, BR-G5
     */
    @Test
    fun showsOutsideGrounds() {
        val report = QuickReport(photo = photo, group = BarrierGroup.TOILETS)
        show(report = report, isFloorAsked = false, lookup = QuickLookup.Outside)
        compose.onNodeWithText("Jesteś poza terenem PWr.", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Toalety").assertIsDisplayed()
        compose.onNodeWithText("Ustalamy, gdzie jesteś…").assertDoesNotExist()
    }

    /** Wiersz: V4 potwierdzenie → zgłoszenie trafi do weryfikacji. Wymagania: US4.4 */
    @Test
    fun showsThankYou() {
        compose.setContent { DostepnyKampusTheme { QuickSentMessage() } }
        compose.onNodeWithText("Dziękujemy", substring = true).assertIsDisplayed()
    }
}
