/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.inspection.progress

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.inspection.MarkerPlace
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * C5 „Obejrzane piętra” Zbierającego: piętra w kontenerach, znacznik w telefonie, „Oznacz jako obejrzane”
 * i błąd zapisu.
 *
 * Wymagania: US15.1, US15.3, NFR-7
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class CollectorMarkersScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /**
     * Wiersz: C5, znacznik w telefonie to „Obejrzane”, „Twój znacznik” i „Czeka na wysłanie”.
     *
     * Wymagania: US15.3
     */
    @Test
    fun showsPendingMarker() {
        show(CollectorMarkersUiState.Ready(ROWS)) {}
        compose.onNodeWithText("Obejrzane").assertIsDisplayed()
        compose.onNodeWithText("Twój znacznik").assertIsDisplayed()
        compose.onNodeWithText("Czeka na wysłanie").assertIsDisplayed()
    }

    /** Wiersz: C5, „Oznacz jako obejrzane” przy nieobejrzanym piętrze oznacza to piętro. Wymagania: US15.1 */
    @Test
    fun marksUninspectedFloor() {
        val marked = mutableListOf<MarkerPlace>()
        show(CollectorMarkersUiState.Ready(ROWS)) { marked += it }
        compose.onNodeWithText("Nieobejrzane").assertIsDisplayed()
        compose.onNodeWithText("Oznacz jako obejrzane").performClick()
        assertEquals("oznaczone piętro", listOf<MarkerPlace>(MarkerPlace.Floor("p1")), marked)
    }

    /** Budynek bez pięter ma jeden wiersz „Cały budynek”. Wymagania: US15.1, BR-G3 */
    @Test
    fun showsWholeBuilding() {
        show(CollectorMarkersUiState.Ready(listOf(FloorRow(MarkerPlace.WholeBuilding, label = null)))) {}
        compose.onNodeWithText("Cały budynek").assertIsDisplayed()
    }

    /**
     * Błąd zapisu w telefonie mówi, co zrobić, a „Spróbuj ponownie” zapisuje znacznik jeszcze raz.
     *
     * Wymagania: NFR-7
     */
    @Test
    fun retriesUnsavedMarker() {
        val marked = mutableListOf<MarkerPlace>()
        show(CollectorMarkersUiState.Ready(ROWS, unsaved = MarkerPlace.Floor("p1"))) { marked += it }
        compose.onNodeWithText("Nie udało się zapisać znacznika w telefonie.", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Spróbuj ponownie").performClick()
        assertEquals("oznaczone piętro", listOf<MarkerPlace>(MarkerPlace.Floor("p1")), marked)
    }

    /** Zrzut do porównania z makietą hi-fi C5 Zbierającego, motyw jasny. */
    @Test
    fun matchesLightMockup() {
        show(CollectorMarkersUiState.Ready(ROWS)) {}
        compose.onRoot().captureRoboImage()
    }

    /** Zrzut do porównania z makietą hi-fi C5 Zbierającego, motyw ciemny. */
    @Test
    @Config(qualifiers = "+night")
    fun matchesDarkMockup() {
        show(CollectorMarkersUiState.Ready(ROWS)) {}
        compose.onRoot().captureRoboImage()
    }

    private fun show(state: CollectorMarkersUiState, onMark: (MarkerPlace) -> Unit) {
        compose.setContent {
            DostepnyKampusTheme {
                CollectorMarkersContent(symbol = "A-1", state = state, onMark = onMark, onBack = {})
            }
        }
    }

    private companion object {
        val ROWS =
            listOf(
                FloorRow(place = MarkerPlace.Floor("p0"), label = "Parter", markerId = "m-1", isPending = true),
                FloorRow(MarkerPlace.Floor("p1"), label = "1 piętro"),
            )
    }
}
