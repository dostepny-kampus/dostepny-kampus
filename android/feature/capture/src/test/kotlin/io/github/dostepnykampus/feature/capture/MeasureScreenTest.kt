/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.location.GnssSystem
import io.github.dostepnykampus.core.location.PositionAverage
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.feature.capture.navigation.ReportFormRoute
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Napisy C1 z makiety „zespół w terenie” dla każdego stanu pomiaru (US6.1, NFR-2, NFR-18, NFR-23). */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class MeasureScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val average = PositionAverage(GeoPoint(longitude = 17.06, latitude = 51.11), 0.8, fixCount = 30)
    private val satellites = mapOf(GnssSystem.GPS to 7, GnssSystem.GALILEO to 5, GnssSystem.GLONASS to 4)

    private val next = mutableListOf<ReportFormRoute>()

    private fun show(state: MeasureUiState, onRepeat: () -> Unit = {}) {
        compose.setContent {
            DostepnyKampusTheme {
                MeasureContent(state = state, onRepeat = onRepeat, onBack = {}, onNext = { next.add(it) })
            }
        }
    }

    /** Wiersz: C1, szukanie satelitów → komunikat i „Powtórz pomiar” jako czynność główna, NFR-1, PW-9. */
    @Test
    fun us6_1_searching_message() {
        var repeats = 0
        show(MeasureUiState.Searching, onRepeat = { repeats++ })
        compose.onNodeWithText("Szukamy satelitów", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Powtórz pomiar").performClick()
        assertEquals(1, repeats)
    }

    /**
     * Wiersz: C1, pomiar trwa → „Pomiar: zostało 12 s”, „Dokładność: 0,8 m (szacunek)”, satelity tekstem, US6.1, NFR-2.
     */
    @Test
    fun us6_1_measuring_texts() {
        show(MeasureUiState.Measuring(remainingSeconds = 12, average = average, satellites = satellites))
        compose
            .onNodeWithText("Pomiar: zostało 12 s")
            .assertIsDisplayed()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        compose.onNodeWithText("Dokładność: 0,8 m (szacunek)").assertIsDisplayed()
        compose.onNodeWithText("Satelity: GPS 7, Galileo 5, GLONASS 4").assertIsDisplayed()
    }

    /** Wiersz: C1, po pomiarze → „Pomiar zakończony” z dokładnością i satelitami z końca pomiaru, US6.1. */
    @Test
    fun us6_1_finished_texts() {
        show(MeasureUiState.Finished(average = average, satellites = satellites))
        compose
            .onNodeWithText("Pomiar zakończony")
            .assertIsDisplayed()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        compose.onNodeWithText("Dokładność: 0,8 m (szacunek)").assertIsDisplayed()
        compose.onNodeWithText("Powtórz pomiar").assertIsDisplayed()
    }

    /** Wiersz: C1, „Dalej” → C2, US6: po decyzji „poza budynkiem” formularz dostaje punkt bez budynku (BR-G4). */
    @Test
    fun us6_next_after_place_decision() {
        show(MeasureUiState.Finished(average = average, satellites = satellites, place = PlaceChoice.Outside))
        compose.onNodeWithText("Dalej").performClick()
        val route = ReportFormRoute(longitude = 17.06, latitude = 51.11, accuracyMeters = 0.8, buildingSymbol = null)
        assertEquals(listOf(route), next)
    }

    /** Wiersz: C1, „Dalej” dopiero po decyzji o budynku, US6.2. */
    @Test
    fun us6_2_no_next_while_locating() {
        show(MeasureUiState.Finished(average = average, satellites = satellites))
        compose.onNodeWithText("Dalej").assertDoesNotExist()
    }

    @Test
    fun us6_1_no_satellites_in_fix() {
        show(MeasureUiState.Measuring(remainingSeconds = 30, average = average, satellites = emptyMap()))
        compose.onNodeWithText("Satelity: brak w pozycji").assertIsDisplayed()
    }

    /** Wiersz: C1, błąd odbiornika → komunikat, co zrobić, i „Powtórz pomiar” bez „Dalej”, NFR-50. */
    @Test
    fun nfr50_source_error_message() {
        var repeats = 0
        show(MeasureUiState.Failed, onRepeat = { repeats++ })
        compose.onNodeWithText("Nie odczytaliśmy pozycji", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Dalej").assertDoesNotExist()
        compose.onNodeWithText("Powtórz pomiar").performClick()
        assertEquals(1, repeats)
    }

    /** Wiersz: C1, lokalizacja wyłączona → komunikat i „Otwórz ustawienia” lokalizacji, bez „Dalej”, UC7 4c. */
    @Test
    fun uc7_4c_location_off_settings() {
        show(MeasureUiState.LocationOff)
        compose.onNodeWithText("Lokalizacja w telefonie jest wyłączona", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Dalej").assertDoesNotExist()
        compose.onNodeWithText("Otwórz ustawienia").performClick()
        val opened = shadowOf(compose.activity).nextStartedActivity
        assertEquals(Settings.ACTION_LOCATION_SOURCE_SETTINGS, opened.action)
    }

    /** Wiersz: C1, powrót po przerwie → „Pomiar był przerwany, więc zaczęliśmy go od nowa.” przy szukaniu, NFR-27. */
    @Test
    fun nfr27_restart_message() {
        compose.setContent {
            DostepnyKampusTheme {
                MeasureContent(state = MeasureUiState.Searching, onRepeat = {}, onBack = {}, isRestarted = true)
            }
        }
        compose.onNodeWithText("Pomiar był przerwany, więc zaczęliśmy go od nowa.").assertIsDisplayed()
        compose.onNodeWithText("Szukamy satelitów", substring = true).assertIsDisplayed()
    }
}
