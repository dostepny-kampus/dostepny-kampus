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
import io.github.dostepnykampus.core.data.submissions.PendingPhotoDraft
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.BarrierGroup
import io.github.dostepnykampus.core.model.GeoPoint
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
    fun us4_1_summary_with_floor() {
        val report = QuickReport(photo = photo, group = BarrierGroup.TOILETS, floorNumber = 2)
        show(report = report, isFloorAsked = true, lookup = here)
        compose.onNodeWithText("Toalety").assertIsDisplayed()
        compose.onNodeWithText("Piętro 2").assertIsDisplayed()
        compose.onNodeWithText("Dołączymy miejsce, w którym teraz stoisz.").assertIsDisplayed()
    }

    /** Wiersz: V4 krok 3, „Nie wiem” → podsumowanie z „Nie wiem”. Wymagania: US4.5 */
    @Test
    fun us4_5_unknown_floor() {
        show(QuickReport(photo = photo, group = BarrierGroup.TOILETS), isFloorAsked = true, lookup = here)
        compose.onNodeWithText("Nie wiem").assertIsDisplayed()
    }

    /** Wiersz: V4 krok 3, pozycja jeszcze nieznana → „Ustalamy, gdzie jesteś…”. Wymagania: NFR-23 */
    @Test
    fun nfr23_position_pending() {
        val report = QuickReport(photo = photo, group = BarrierGroup.OTHER)
        show(report = report, isFloorAsked = false, lookup = QuickLookup.Locating)
        compose.onNodeWithText("Ustalamy, gdzie jesteś…").assertIsDisplayed()
        compose.onNodeWithText("Nie wiem").assertDoesNotExist()
    }

    /** Wiersz: V4 brak pozycji → komunikat, co zrobić, zamiast „Ustalamy…”; grupa zostaje. Wymagania: UC7 */
    @Test
    fun uc7_4c_position_lost() {
        show(QuickReport(photo = photo, group = BarrierGroup.TOILETS), isFloorAsked = false, lookup = QuickLookup.Lost)
        compose.onNodeWithText("Telefon nie ustalił, gdzie jesteś.", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Toalety").assertIsDisplayed()
        compose.onNodeWithText("Ustalamy, gdzie jesteś…").assertDoesNotExist()
    }

    /** Wiersz: V4 potwierdzenie → zgłoszenie trafi do weryfikacji. Wymagania: US4.4 */
    @Test
    fun us4_4_thank_you() {
        compose.setContent { DostepnyKampusTheme { QuickSentMessage() } }
        compose.onNodeWithText("Dziękujemy", substring = true).assertIsDisplayed()
    }
}
