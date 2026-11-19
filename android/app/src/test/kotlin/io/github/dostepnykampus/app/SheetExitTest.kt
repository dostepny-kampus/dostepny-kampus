/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
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

    /**
     * Wiersz: V1, „Zamknij” na małej karcie — dół mapy zasłonięty dla jej przycisków maleje razem z zjeżdżającą
     * kartą: w połowie zjazdu jest między zasłonięciem przed zamknięciem a zerem, a po końcu zerem.
     */
    @Test
    fun mapCoverFollowsClosingCard() {
        val stack = mutableStateListOf<NavKey>(MapRoute(), BuildingRoute(symbol = "A-1"))
        var cover: () -> Int = { -1 }
        show(stack) { cover = LocalSheetPadding.current.shownCover }
        val open = compose.runOnIdle { cover() }
        compose.mainClock.autoAdvance = false
        compose.runOnUiThread {
            stack.removeAt(1)
            Snapshot.sendApplyNotifications()
        }
        advanceUntilClosing()
        compose.mainClock.advanceTimeBy(SLIDE_MILLIS / 2)
        val midway = compose.runOnUiThread<Int> { cover() }
        assertTrue("karta zasłania dół mapy: $open", open > 0)
        assertTrue("w połowie zjazdu zasłonięcie maleje razem z kartą: $midway z $open", midway in 1..<open)
        compose.mainClock.advanceTimeBy(SLIDE_MILLIS)
        assertEquals("po zjeździe mapa odsłonięta", 0, compose.runOnUiThread<Int> { cover() })
    }

    /**
     * Wiersz: V1, „Zamknij” na małej karcie — w każdej klatce od zamknięcia do końca zjazdu zasłonięcie dołu mapy
     * maleje albo stoi: zaczyna od zasłonięcia otwartej karty, nie spada do zera przed zjazdem i kończy na zerze.
     */
    @Test
    fun coverNeverJumpsOnClose() {
        val stack = mutableStateListOf<NavKey>(MapRoute(), BuildingRoute(symbol = "A-1"))
        val covers = mutableListOf<Int>()
        show(stack) { CoverProbe(covers) }
        val open = compose.runOnIdle { covers.last() }
        compose.mainClock.autoAdvance = false
        compose.runOnUiThread {
            covers.clear()
            stack.removeAt(1)
            Snapshot.sendApplyNotifications()
        }
        repeat(SLIDE_FRAMES) { compose.mainClock.advanceTimeByFrame() }
        val seen = compose.runOnUiThread<List<Int>> { covers.toList() }
        assertTrue("karta zasłania dół mapy: $open", open > 0)
        assertEquals("pierwsza klatka po zamknięciu: $seen", open, seen.first())
        assertTrue("zasłonięcie tylko maleje: $seen", seen.zipWithNext().all { (before, after) -> after <= before })
        assertEquals("po zjeździe mapa odsłonięta: $seen", 0, seen.last())
    }

    private fun show(stack: List<NavKey>, onMap: @Composable () -> Unit = {}) {
        val sheet = SheetSceneStrategy(MapMotion())
        compose.setContent {
            NavDisplay(backStack = stack, onBack = {}, sceneStrategies = listOf(sheet)) { route: NavKey ->
                NavEntry(key = route, metadata = SheetSceneStrategy.metadata(route)) {
                    if (route is MapRoute) onMap()
                    Text(text = "$route")
                }
            }
        }
        compose.waitForIdle()
    }

    @Composable
    private fun CoverProbe(covers: MutableList<Int>) {
        val sheet = LocalSheetPadding.current
        Box(
            modifier =
                Modifier.layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    covers += sheet.shownCover()
                    layout(width = placeable.width, height = placeable.height) { placeable.place(x = 0, y = 0) }
                },
        )
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
        const val SLIDE_FRAMES = 30
    }
}
