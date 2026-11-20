/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import io.github.dostepnykampus.app.navigation.LocalSheetPadding
import io.github.dostepnykampus.app.navigation.SheetSceneStrategy
import io.github.dostepnykampus.app.navigation.sceneStrategies
import io.github.dostepnykampus.app.navigation.shownFocus
import io.github.dostepnykampus.feature.map.camera.MapMotion
import io.github.dostepnykampus.feature.map.navigation.BuildingRoute
import io.github.dostepnykampus.feature.map.navigation.MapFocus
import io.github.dostepnykampus.feature.map.navigation.MapRoute
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Część mapy zasłonięta kartą V2: karta ma wysokość treści od pierwszej klatki, więc mapa nie widzi stanu „bez karty”.
 *
 * Wymagania: NFR-56, US2.8
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class SheetCoverTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /**
     * Wiersz: V2 mała karta, „wysokość gotowej treści od pierwszej klatki” — mapa, która już wie o wyborze budynku,
     * od pierwszej kompozycji dostaje zasłoniętą część większą od zera, więc pasek nie wraca, a ruch do zapisanego
     * widoku nie startuje.
     */
    @Test
    fun coversMapFromFirstFrame() {
        val covers = mutableListOf<Dp>()
        val stack = mutableStateListOf<NavKey>(MapRoute())
        show(stack, covers)
        compose.mainClock.autoAdvance = false
        covers.clear()
        compose.runOnUiThread {
            stack[0] = MapRoute(MapFocus.Building("A-1"))
            stack.add(BuildingRoute(symbol = "A-1"))
            Snapshot.sendApplyNotifications()
        }
        repeat(FRAMES) { compose.mainClock.advanceTimeByFrame() }
        assertTrue("mapa przeliczona po wyborze budynku", covers.isNotEmpty())
        assertTrue("zasłonięta część od pierwszej klatki: $covers", covers.all { it > 0.dp })
    }

    /** Wiersz: V2 otwarta karta, wybór innego budynku — mapa z nowym zaznaczeniem nie widzi stanu „bez karty”. */
    @Test
    fun keepsCoverWhenCardChanges() {
        val covers = mutableListOf<Dp>()
        val stack = mutableStateListOf<NavKey>(MapRoute(MapFocus.Building("A-1")), BuildingRoute(symbol = "A-1"))
        show(stack, covers)
        compose.mainClock.autoAdvance = false
        covers.clear()
        compose.runOnUiThread {
            stack[0] = MapRoute(MapFocus.Building("A-2"))
            stack[1] = BuildingRoute(symbol = "A-2")
            Snapshot.sendApplyNotifications()
        }
        repeat(FRAMES) { compose.mainClock.advanceTimeByFrame() }
        assertTrue("mapa przeliczona po zmianie budynku", covers.isNotEmpty())
        assertTrue("zasłonięta część bez przerwy: $covers", covers.all { it > 0.dp })
    }

    private fun show(stack: List<NavKey>, covers: MutableList<Dp>) {
        val sheet = SheetSceneStrategy(MapMotion())
        compose.setContent {
            NavDisplay(backStack = stack, onBack = {}, sceneStrategies = listOf(sheet)) { route: NavKey ->
                val key = if (route is MapRoute) MAP_CONTENT_KEY else "$route"
                NavEntry(key = route, contentKey = key, metadata = SheetSceneStrategy.metadata(route)) {
                    if (route is MapRoute) {
                        val focus = shownFocus(route.focus, isSheetAbove = SheetSceneStrategy.isSheet(stack.last()))
                        val cover = LocalSheetPadding.current.cover
                        SideEffect { if (focus != null) covers += cover }
                    }
                    Text(text = "$route")
                }
            }
        }
        compose.waitForIdle()
    }

    private companion object {
        const val FRAMES = 5
        const val MAP_CONTENT_KEY = "mapa"
    }
}
