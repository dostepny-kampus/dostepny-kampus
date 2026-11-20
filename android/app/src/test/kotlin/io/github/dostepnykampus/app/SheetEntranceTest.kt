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
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import io.github.dostepnykampus.app.navigation.SheetSceneStrategy
import io.github.dostepnykampus.app.navigation.sceneStrategies
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.feature.map.camera.CameraMotion
import io.github.dostepnykampus.feature.map.camera.CameraTransition
import io.github.dostepnykampus.feature.map.camera.MapCamera
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

/**
 * Wjazd małej karty V2 nad mapą: w czasie i po krzywej ruchu mapy do budynku, bez ponownego wjazdu otwartej karty.
 *
 * Wymagania: NFR-56, US2.8
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class SheetEntranceTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /**
     * Wiersz: V2 mała karta nad mapą — karta wjeżdża dokładnie w czasie ruchu mapy (400 ms): w połowie jest jeszcze
     * w drodze, tuż przed końcem też, a po nim stoi.
     */
    @Test
    fun slidesInWithMapMove() {
        val motion = MapMotion()
        val stack = mutableStateListOf<NavKey>(MapRoute())
        show(stack, motion)
        compose.mainClock.autoAdvance = false
        compose.runOnUiThread {
            stack.add(BuildingRoute(symbol = "A-1"))
            Snapshot.sendApplyNotifications()
        }
        advanceUntilCardComposed()
        compose.runOnUiThread {
            motion.record(move(durationMillis = MOVE_MILLIS))
            Snapshot.sendApplyNotifications()
        }
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeBy(MOVE_MILLIS / 2)
        val midway = cardTop()
        compose.mainClock.advanceTimeBy(MOVE_MILLIS / 2 - FRAME_MARGIN_MILLIS)
        val almost = cardTop()
        compose.mainClock.advanceTimeBy(2 * FRAME_MARGIN_MILLIS)
        val rest = cardTop()
        assertTrue("w połowie ruchu karta jeszcze wjeżdża: $midway > $almost", midway > almost)
        assertTrue("tuż przed końcem ruchu karta jeszcze wjeżdża: $almost > $rest", almost > rest)
        compose.mainClock.advanceTimeBy(MOVE_MILLIS)
        assertEquals("po ruchu karta stoi", rest.value, cardTop().value, 0.5f)
    }

    /** Wiersz: V2 otwarta karta, wybór innego budynku — karta zmienia treść i nie wjeżdża od nowa. */
    @Test
    fun keepsOpenCardInPlace() {
        val motion = MapMotion()
        val stack = mutableStateListOf<NavKey>(MapRoute(), BuildingRoute(symbol = "A-1"))
        show(stack, motion)
        val rest = cardTop()
        compose.mainClock.autoAdvance = false
        compose.runOnUiThread {
            stack[1] = BuildingRoute(symbol = "A-2")
            Snapshot.sendApplyNotifications()
        }
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeByFrame()
        assertEquals("nowa treść od razu na miejscu", rest.value, cardTop(symbol = "A-2").value, 0.5f)
    }

    /** Wiersz: systemowe „Usuń animacje” — ruch mapy ma czas 0, więc karta od razu stoi na miejscu. */
    @Test
    fun showsAtOnceWithoutMotion() {
        val motion = MapMotion()
        val stack = mutableStateListOf<NavKey>(MapRoute())
        show(stack, motion)
        compose.runOnUiThread {
            stack.add(BuildingRoute(symbol = "A-1"))
            Snapshot.sendApplyNotifications()
        }
        compose.waitForIdle()
        val rest = cardTop()
        compose.runOnUiThread {
            stack.removeAt(1)
            Snapshot.sendApplyNotifications()
        }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        compose.runOnUiThread {
            stack.add(BuildingRoute(symbol = "A-1"))
            Snapshot.sendApplyNotifications()
        }
        advanceUntilCardComposed()
        compose.runOnUiThread {
            motion.record(move(durationMillis = 0L))
            Snapshot.sendApplyNotifications()
        }
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeByFrame()
        assertEquals("bez animacji karta od razu na miejscu", rest.value, cardTop().value, 0.5f)
    }

    private fun show(stack: List<NavKey>, motion: MapMotion) {
        val sheet = SheetSceneStrategy(motion)
        compose.setContent {
            NavDisplay(backStack = stack, onBack = {}, sceneStrategies = listOf(sheet)) { route: NavKey ->
                NavEntry(key = route, metadata = SheetSceneStrategy.metadata(route)) { Text(text = "$route") }
            }
        }
        compose.waitForIdle()
    }

    private fun advanceUntilCardComposed() {
        repeat(MAX_FRAMES) {
            compose.mainClock.advanceTimeByFrame()
            if (compose.onAllNodesWithText("${BuildingRoute(symbol = "A-1")}").fetchSemanticsNodes().isNotEmpty()) {
                return
            }
        }
        fail("karta nie pojawiła się w $MAX_FRAMES klatkach")
    }

    private fun cardTop(symbol: String = "A-1"): Dp {
        val root = compose.onRoot().getUnclippedBoundsInRoot().bottom
        val top = compose.onNodeWithText("${BuildingRoute(symbol = symbol)}").getUnclippedBoundsInRoot().top
        return top.coerceAtMost(root)
    }

    private fun move(durationMillis: Long): CameraTransition {
        val camera = MapCamera(center = GeoPoint(longitude = 17.06, latitude = 51.11), zoom = 17.0)
        return CameraTransition(camera = camera, motion = CameraMotion.EASE, durationMillis = durationMillis)
    }

    private companion object {
        const val MOVE_MILLIS = 400L
        const val FRAME_MARGIN_MILLIS = 64L
        const val MAX_FRAMES = 5
    }
}
