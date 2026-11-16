/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import io.github.dostepnykampus.feature.map.camera.MapMotion
import io.github.dostepnykampus.feature.map.navigation.BuildingRoute
import io.github.dostepnykampus.feature.map.navigation.MapRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Zjazd zamkniętej małej karty V2 w dół zamiast zniknięcia w jednej klatce. Wymagania: NFR-56, US2.8 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class SheetExitTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /**
     * Wiersz: V1, „Zamknij” na małej karcie — karta zjeżdża w dół z miejsca, w którym stała, w czasie `medium2`
     * (300 ms): w połowie jest niżej niż przed zamknięciem, a po końcu jej nie ma.
     */
    @Test
    fun slidesDownOnClose() {
        val stack = mutableStateListOf<NavKey>(MapRoute(), BuildingRoute(symbol = "A-1"))
        show(stack)
        val open = compose.onNodeWithText("${BuildingRoute(symbol = "A-1")}").getUnclippedBoundsInRoot().top
        compose.mainClock.autoAdvance = false
        compose.runOnUiThread {
            stack.removeAt(1)
            Snapshot.sendApplyNotifications()
        }
        advanceUntilClosing()
        val start = closingTop()
        compose.mainClock.advanceTimeBy(SLIDE_MILLIS / 2)
        val midway = closingTop()
        assertEquals("zjazd zaczyna się w miejscu karty", open.value, start.value, FRAME_SHIFT_DP)
        assertTrue("w połowie karta zjeżdża: $midway > $start", midway > start)
        compose.mainClock.advanceTimeBy(SLIDE_MILLIS)
        val left = compose.onAllNodesWithTag(CLOSING_SHEET_TAG).fetchSemanticsNodes()
        assertTrue("po zjeździe karty nie ma", left.isEmpty())
    }

    private fun show(stack: List<NavKey>) {
        val sheet = SheetSceneStrategy(MapMotion())
        compose.setContent {
            NavDisplay(backStack = stack, onBack = {}, sceneStrategies = listOf(sheet)) { route: NavKey ->
                NavEntry(key = route, metadata = SheetSceneStrategy.metadata(route)) { Text(text = "$route") }
            }
        }
        compose.waitForIdle()
    }

    private fun advanceUntilClosing() {
        repeat(MAX_FRAMES) {
            compose.mainClock.advanceTimeByFrame()
            if (compose.onAllNodesWithTag(CLOSING_SHEET_TAG).fetchSemanticsNodes().isNotEmpty()) return
        }
        fail("zamknięta karta nie zjeżdża po $MAX_FRAMES klatkach")
    }

    private fun closingTop(): Dp = compose.onNodeWithTag(CLOSING_SHEET_TAG).getUnclippedBoundsInRoot().top

    private companion object {
        const val SLIDE_MILLIS = 300L
        const val FRAME_SHIFT_DP = 60f
        const val MAX_FRAMES = 5
    }
}
