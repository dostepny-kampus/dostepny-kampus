/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEvent
import io.github.dostepnykampus.core.data.account.GuestRoleProvider
import io.github.dostepnykampus.feature.map.navigation.MapRoute
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val MIDWAY_MILLIS = 150L

/** ViewModel V1: ten sam obiekt po geście znaczy ten sam stan mapy (decyzja 83). */
private class MapModel : ViewModel()

/** Arkusz V2 nad V1 przy przewidywanym geście „Wstecz”: gest z dyspozytora zdarzeń, zegar Compose zatrzymany. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SheetBackGestureTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val models = mutableSetOf<ViewModel>()
    private val gesture = DirectNavigationEventInput()

    @Composable
    private fun Shell() {
        AppShell(firstLaunch = false, roles = GuestRoleProvider, onFirstHelpLeft = {}) { route ->
            if (route is MapRoute) {
                models += viewModel { MapModel() }
                val map = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color.Green, Color.Blue)))
                Text(text = "A-1", modifier = map.clickable { openBuilding(symbol = "A-1") })
            } else {
                Text(text = "karta")
            }
        }
    }

    private fun openSheet(isExpanded: Boolean) {
        compose.onNodeWithText("A-1").performClick()
        if (isExpanded) compose.onNodeWithContentDescription("Rozwiń kartę budynku").performClick()
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
    }

    private fun sheetTop(moment: String): Dp {
        val handle = hasContentDescription("Rozwiń kartę budynku") or hasContentDescription("Zwiń kartę budynku")
        return compose
            .onNode(handle)
            .assertExists("arkusz V2 $moment")
            .getUnclippedBoundsInRoot()
            .top
    }

    private fun move(progress: Float) {
        val event = NavigationEvent(swipeEdge = NavigationEvent.EDGE_LEFT, progress = progress)
        compose.runOnUiThread {
            if (progress == 0f) compose.activity.navigationEventDispatcher.addInput(gesture)
            if (progress == 0f) gesture.backStarted(event) else gesture.backProgressed(event)
        }
        compose.mainClock.advanceTimeBy(MIDWAY_MILLIS * 4)
    }

    private fun finish(event: DirectNavigationEventInput.() -> Unit, midway: () -> Unit = {}) {
        compose.runOnUiThread { gesture.event() }
        compose.mainClock.advanceTimeBy(MIDWAY_MILLIS)
        midway()
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
    }

    private fun mapBand(): IntArray {
        val screen = compose.onRoot().captureToImage().asAndroidBitmap()
        val rows = with(compose.density) { 300.dp.roundToPx() }
        return IntArray(screen.width * rows).also { screen.getPixels(it, 0, screen.width, 0, 0, screen.width, rows) }
    }

    private fun checkCancel(isExpanded: Boolean) {
        compose.setContent { Shell() }
        openSheet(isExpanded)
        val before = sheetTop("przed gestem")
        move(0f)
        move(0.6f)
        assertTrue("arkusz V2 niżej przy postępie 0,6", sheetTop("przy postępie 0,6") > before)
        finish({ backCancelled() })
        assertEquals("góra arkusza po anulowaniu", before.value, sheetTop("po anulowaniu").value, 1f)
    }

    // Wiersz: V2 do połowy, gest „Wstecz” 0,6 i anulowanie → arkusz w dół, potem znów do połowy, V2 zostaje, US2.
    @Test
    fun us2_cancel_keeps_half_sheet() = checkCancel(isExpanded = false)

    // Wiersz: V2 rozwinięte, gest „Wstecz” 0,6 i anulowanie → arkusz w dół, potem znów rozwinięty, V2 zostaje, US2.
    @Test
    fun us2_cancel_keeps_full_sheet() = checkCancel(isExpanded = true)

    // Wiersz: V2, gest „Wstecz” 0,5 i zakończenie → mapa V1 nad arkuszem nie przenika i nie blaknie, US2, decyzja 48;
    // V2 zdjęte z historii, V1 z tym samym ViewModel, US1.1, decyzja 83; kolejne „Wstecz” zamyka aplikację, NFR-15.
    @Test
    fun us1_1_gesture_closes_v2() {
        compose.setContent { Shell() }
        openSheet(isExpanded = false)
        val before = mapBand()
        move(0f)
        move(0.5f)
        assertArrayEquals("mapa V1 przy postępie 0,5", before, mapBand())
        finish({ backCompleted() }) { assertArrayEquals("mapa V1 w trakcie zamykania", before, mapBand()) }
        assertArrayEquals("mapa V1 po zakończeniu gestu", before, mapBand())
        compose.onNodeWithText("karta").assertDoesNotExist()
        assertEquals("ViewModel V1 po geście", 1, models.size)
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        assertTrue("„Wstecz” na V1 zamyka aplikację", compose.activity.isFinishing)
    }

    // Wiersz: V1, dotknięcie konturu → V2, „Wstecz” przyciskiem → V1: mapa nie miga, US2, decyzja 48.
    @Test
    fun us2_open_close_map_unchanged() {
        compose.setContent { Shell() }
        val before = mapBand()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithText("A-1").performClick()
        compose.mainClock.advanceTimeBy(MIDWAY_MILLIS)
        assertArrayEquals("mapa V1 przy otwieraniu V2", before, mapBand())
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.mainClock.advanceTimeBy(MIDWAY_MILLIS)
        assertArrayEquals("mapa V1 przy zamykaniu V2", before, mapBand())
    }

    // Wiersz: V2 rozwinięte, odtworzenie aktywności → arkusz dalej rozwinięty, NFR-22.
    @Test
    fun nfr22_full_sheet_restored() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { Shell() }
        openSheet(isExpanded = true)
        compose.mainClock.autoAdvance = true
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithContentDescription("Zwiń kartę budynku").assertExists("arkusz V2 rozwinięty po odtworzeniu")
    }
}
