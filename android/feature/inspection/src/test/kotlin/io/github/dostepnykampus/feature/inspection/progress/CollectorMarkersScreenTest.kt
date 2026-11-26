/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.inspection.progress

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.inspection.MarkerPlace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * C5 „Obejrzane piętra” Zbierającego: piętra w kontenerach, znacznik w telefonie, „Oznacz jako obejrzane”,
 * „Cofnij znacznik” z potwierdzeniem i błąd zapisu.
 *
 * Wymagania: US15.1, US15.2, US15.3, BR-20, NFR-7
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
        show(CollectorMarkersUiState.Ready(ROWS))
        compose.onNodeWithText("Obejrzane").assertIsDisplayed()
        compose.onNodeWithText("Twój znacznik").assertIsDisplayed()
        compose.onNodeWithText("Czeka na wysłanie").assertIsDisplayed()
        compose.onNodeWithText(REVOKE).assertIsDisplayed()
    }

    /**
     * Wiersz: C5 bez internetu, znacznik w telefonie dostaje zdanie, że wyśle się po powrocie internetu.
     *
     * Wymagania: US15.3
     */
    @Test
    fun explainsPendingMarkerOffline() {
        show(CollectorMarkersUiState.Ready(ROWS, isOnline = false))
        compose.onNodeWithText(SAVED_IN_PHONE).assertIsDisplayed()
    }

    /** Wiersz: C5 z internetem, przy znaczniku w telefonie nie ma zdania o braku internetu. Wymagania: US15.3 */
    @Test
    fun omitsOfflineSentenceOnline() {
        show(CollectorMarkersUiState.Ready(ROWS))
        compose.onNodeWithText(SAVED_IN_PHONE).assertDoesNotExist()
    }

    /** Wiersz: C5, „Oznacz jako obejrzane” przy nieobejrzanym piętrze oznacza to piętro. Wymagania: US15.1 */
    @Test
    fun marksUninspectedFloor() {
        val marked = mutableListOf<MarkerPlace>()
        show(CollectorMarkersUiState.Ready(ROWS), CollectorMarkerActions(onMark = { marked += it }))
        compose.onNodeWithText("Nieobejrzane").assertIsDisplayed()
        compose.onNodeWithText("Oznacz jako obejrzane").performClick()
        assertEquals("oznaczone piętro", listOf<MarkerPlace>(MarkerPlace.Floor("p1")), marked)
    }

    /**
     * „Oznacz jako obejrzane” i „Cofnij znacznik” to czynności karty szerokie jak napis, węższe niż treść karty.
     *
     * Wymagania: NFR-17, US15.1, US15.2
     */
    @Test
    fun keepsCardActionsCompact() {
        show(CollectorMarkersUiState.Ready(ROWS))
        listOf("Oznacz jako obejrzane", REVOKE).forEach { label ->
            val bounds = compose.onNodeWithText(label).getUnclippedBoundsInRoot()
            assertTrue("„$label” ma być węższe niż treść karty: $bounds", (bounds.right - bounds.left).value < CONTENT)
        }
    }

    /** Budynek bez pięter ma jeden wiersz „Cały budynek”. Wymagania: US15.1, BR-G3 */
    @Test
    fun showsWholeBuilding() {
        show(CollectorMarkersUiState.Ready(listOf(FloorRow(MarkerPlace.WholeBuilding, label = null))))
        compose.onNodeWithText("Cały budynek").assertIsDisplayed()
    }

    /**
     * „Cofnij znacznik” przy własnym znaczniku przekazuje jego identyfikator.
     *
     * Wymagania: US15.2
     */
    @Test
    fun askRevokePassesMarkerId() {
        val asked = mutableListOf<String>()
        show(CollectorMarkersUiState.Ready(ROWS), CollectorMarkerActions(onAsk = { asked += it }))
        compose.onNodeWithText(REVOKE).performClick()
        assertEquals("pytanie o znacznik", listOf("m-1"), asked)
    }

    /**
     * Pytanie o potwierdzenie cofnięcia mówi, że postęp budynku się zmniejszy.
     *
     * Scenariusz: postep-inwentaryzacji.feature: Zbierający cofa własny znacznik po potwierdzeniu
     *
     * Wymagania: US15.2, BR-20
     */
    @Test
    fun confirmationWarnsAboutProgress() {
        show(CollectorMarkersUiState.Ready(ROWS, asking = "m-1"))
        compose.onNodeWithText("Postęp budynku się zmniejszy.", substring = true).assertIsDisplayed()
        compose.onNodeWithText(KEEP).assertIsDisplayed()
    }

    /**
     * „Zostaw znacznik” i „Cofnij znacznik” w potwierdzeniu wywołują swoje czynności.
     *
     * Scenariusz: postep-inwentaryzacji.feature: Bez potwierdzenia znacznik zostaje
     *
     * Wymagania: US15.2, BR-20
     */
    @Test
    fun confirmationButtonsCallActions() {
        val calls = mutableListOf<String>()
        val actions = CollectorMarkerActions(onKeep = { calls += KEEP }, onConfirm = { calls += REVOKE })
        show(CollectorMarkersUiState.Ready(ROWS, asking = "m-1"), actions)
        compose.onNodeWithText(KEEP).performClick()
        compose.onNodeWithText(REVOKE).performClick()
        assertEquals("kolejność czynności", listOf(KEEP, REVOKE), calls)
    }

    /** Pytanie o cofnięcie znacznika całego budynku wskazuje cały budynek. Wymagania: US15.2 */
    @Test
    fun showsWholeBuildingConfirmation() {
        val whole =
            listOf(FloorRow(place = MarkerPlace.WholeBuilding, label = null, markerId = "m-1", isPending = true))
        show(CollectorMarkersUiState.Ready(whole, asking = "m-1"))
        compose.onNodeWithText("Cofnąć znacznik całego budynku? Postęp budynku się zmniejszy.").assertIsDisplayed()
    }

    /**
     * Znacznik z serwera: własny bez „Czeka na wysłanie”, cudzy jako „Znacznik innej osoby” bez „Cofnij znacznik”.
     *
     * Wymagania: US15.1, US15.2
     */
    @Test
    fun showsSyncedOwnAndOtherMarker() {
        val synced =
            listOf(
                FloorRow(place = MarkerPlace.Floor("p0"), label = "Parter", markerId = "s-1", isPending = false),
                FloorRow(
                    place = MarkerPlace.Floor("p1"),
                    label = "1 piętro",
                    markerId = "s-2",
                    isPending = false,
                    isMine = false,
                ),
            )
        show(CollectorMarkersUiState.Ready(synced))
        compose.onNodeWithText("Twój znacznik").assertIsDisplayed()
        compose.onNodeWithText("Znacznik innej osoby").assertIsDisplayed()
        compose.onNodeWithText("Czeka na wysłanie").assertDoesNotExist()
        compose.onNodeWithText(REVOKE).assertIsDisplayed()
    }

    /**
     * Błąd zapisu w telefonie mówi, co zrobić, a „Spróbuj ponownie” zapisuje znacznik jeszcze raz.
     *
     * Wymagania: NFR-7
     */
    @Test
    fun retriesUnsavedMarker() {
        val marked = mutableListOf<MarkerPlace>()
        show(
            CollectorMarkersUiState.Ready(ROWS, unsaved = MarkerPlace.Floor("p1")),
            CollectorMarkerActions(onMark = { marked += it }),
        )
        compose.onNodeWithText("Nie udało się zapisać znacznika w telefonie.", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Spróbuj ponownie").performClick()
        assertEquals("oznaczone piętro", listOf<MarkerPlace>(MarkerPlace.Floor("p1")), marked)
    }

    /** Zrzut do porównania z makietą hi-fi C5 Zbierającego, motyw jasny. */
    @Test
    fun matchesLightMockup() {
        show(CollectorMarkersUiState.Ready(ROWS))
        compose.onRoot().captureRoboImage()
    }

    /** Zrzut do porównania z makietą hi-fi C5 Zbierającego, motyw ciemny. */
    @Test
    @Config(qualifiers = "+night")
    fun matchesDarkMockup() {
        show(CollectorMarkersUiState.Ready(ROWS))
        compose.onRoot().captureRoboImage()
    }

    private fun show(state: CollectorMarkersUiState, actions: CollectorMarkerActions = CollectorMarkerActions()) {
        compose.setContent {
            DostepnyKampusTheme {
                CollectorMarkersContent(symbol = "A-1", state = state, actions = actions, onBack = {})
            }
        }
    }

    private companion object {
        const val KEEP = "Zostaw znacznik"
        const val REVOKE = "Cofnij znacznik"
        const val CONTENT = 296f
        val ROWS =
            listOf(
                FloorRow(place = MarkerPlace.Floor("p0"), label = "Parter", markerId = "m-1", isPending = true),
                FloorRow(place = MarkerPlace.Floor("p1"), label = "1 piętro"),
            )
    }
}

private const val SAVED_IN_PHONE = "Znacznik zapisany w telefonie. Wyślemy go, gdy wróci internet."
