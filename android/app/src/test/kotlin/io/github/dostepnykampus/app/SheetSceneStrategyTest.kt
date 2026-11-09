/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategyScope
import io.github.dostepnykampus.feature.map.navigation.BuildingRoute
import io.github.dostepnykampus.feature.map.navigation.EntrancesRoute
import io.github.dostepnykampus.feature.map.navigation.MapRoute
import org.junit.Assert.assertNull
import org.junit.Test

/** Sceny z historii: układy inne niż V1 i V2 nad V1 strategia arkusza zostawia scenie z jednym ekranem. */
class SheetSceneStrategyTest {
    private fun entry(route: NavKey): NavEntry<NavKey> =
        NavEntry(key = route, metadata = SheetSceneStrategy.metadata(route)) {}

    private fun scene(vararg routes: NavKey): Scene<NavKey>? =
        with(SheetSceneStrategy) { SceneStrategyScope<NavKey>().calculateScene(routes.map(::entry)) }

    /**
     * Wiersz: V2, „Sprawdź, którędy wejść” → V3 na cały ekran, US3; inne układy historii rysuje scena z jednym ekranem.
     */
    @Test
    fun us3_v3_single_screen() {
        assertNull(scene(MapRoute(), BuildingRoute("A-1"), EntrancesRoute("A-1")))
        assertNull(scene(BuildingRoute("A-1")))
    }
}
