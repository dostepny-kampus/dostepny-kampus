/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.activity.ComponentActivity
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.core.model.BuildingCatalog
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.feature.map.data.CampusMapData
import io.github.dostepnykampus.feature.map.data.MapSelection
import io.github.dostepnykampus.feature.map.data.campusMapData
import io.github.dostepnykampus.feature.map.data.selection
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Styl mapy V1 w kompozycji: buduje go zmiana danych, zaznaczenia albo barw, a nie ruch kamery. */
@RunWith(RobolectricTestRunner::class)
class MapStyleRememberTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val a = GeoPoint(longitude = 0.0, latitude = 0.0)
    private val outline = GeoPolygon(listOf(listOf(a, a.copy(longitude = 1.0), a.copy(latitude = 1.0), a)))
    private val building = Building(symbol = "C-13", name = "Budynek testowy", outline = outline)
    private val other = building.copy(symbol = "C-14")
    private val day =
        MapStyleColors(land = "#E8EDEE", building = "#BFE0EC", buildingOutline = "#004E5E", selectedOutline = "#001F27")

    private var data by mutableStateOf(dataOf(building))
    private var selection by mutableStateOf(MapSelection())
    private var colors by mutableStateOf(day)
    private var camera by mutableStateOf(MapCamera(center = a, zoom = 16.0))
    private val cameras = mutableListOf<MapCamera>()
    private val builds = mutableListOf<String>()
    private val styles = mutableListOf<String>()

    /** Sam ruch kamery rekomponuje widok, ale stylu nie buduje drugi raz. */
    @Test
    fun camera_move_keeps_style() {
        show()
        compose.runOnIdle { camera = camera.copy(zoom = 17.0) }
        compose.runOnIdle {
            assertEquals(listOf(16.0, 17.0), cameras.map { it.zoom })
            assertEquals(1, builds.size)
            assertEquals(styles.first(), styles.last())
        }
    }

    /** Zmiana danych, zaznaczenia i motywu daje za każdym razem nowy styl. */
    @Test
    fun change_rebuilds_style() {
        show()
        compose.runOnIdle { data = dataOf(building, other) }
        compose.runOnIdle { selection = data.selection(building = "C-14") }
        compose.runOnIdle { colors = day.copy(isNight = true) }
        compose.runOnIdle {
            assertEquals(4, builds.size)
            assertEquals(4, builds.toSet().size)
            assertEquals(builds.last(), styles.last())
        }
    }

    private fun show() {
        compose.setContent {
            val seen = camera
            val style =
                rememberMapStyle(data = data, selection = selection, colors = colors) { mapData, chosen, palette ->
                    campusMapStyle(data = mapData, selection = chosen, colors = palette).also(builds::add)
                }
            SideEffect {
                cameras += seen
                styles += style
            }
        }
    }

    private fun dataOf(vararg buildings: Building): CampusMapData =
        campusMapData(BuildingCatalog(buildings = buildings.toList(), entrances = emptyList(), attribution = ""))
}
