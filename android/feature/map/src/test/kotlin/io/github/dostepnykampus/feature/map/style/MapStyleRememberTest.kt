/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.style

import androidx.activity.ComponentActivity
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.core.model.building.Building
import io.github.dostepnykampus.core.model.building.BuildingCatalog
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.campus.CampusBounds
import io.github.dostepnykampus.feature.map.campus.CampusMapData
import io.github.dostepnykampus.feature.map.campus.MapSelection
import io.github.dostepnykampus.feature.map.campus.SelectedEntrance
import io.github.dostepnykampus.feature.map.campus.campusMapData
import io.github.dostepnykampus.feature.map.rememberMapStyle
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Styl mapy V1 w kompozycji: buduje go zmiana danych, terenu albo barw, a nie zaznaczenie ani ruch kamery. */
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
    private val styles = mutableListOf<String?>()
    private var basemap by mutableStateOf<Basemap?>(null)

    /** Sam ruch kamery rekomponuje widok, ale stylu nie buduje drugi raz. */
    @Test
    fun keepsStyleOnCameraMove() {
        show()
        compose.runOnIdle { camera = camera.copy(zoom = 17.0) }
        compose.runOnIdle {
            assertEquals(listOf(16.0, 17.0), cameras.map { it.zoom })
            assertEquals(1, builds.size)
            assertEquals(styles.first(), styles.last())
        }
    }

    /** Zmiana zaznaczonego budynku, wejścia i bariery nie buduje stylu drugi raz. Wymagania: US1.1, US1.5, US3.3 */
    @Test
    fun keepsStyleOnSelection() {
        show()
        val entrance = SelectedEntrance(id = "w1", location = a, label = "Główne", buildingSymbol = "C-13")
        compose.runOnIdle { selection = selection.copy(building = "C-13") }
        compose.runOnIdle { selection = selection.copy(entrance = entrance) }
        compose.runOnIdle { selection = selection.copy(barrier = "b1") }
        compose.runOnIdle {
            assertEquals(1, builds.size)
            assertEquals(styles.first(), styles.last())
        }
    }

    /**
     * Zmiana danych, prostokąta i granicy terenu, motywu oraz podkładu daje za każdym razem nowy styl.
     * Wymagania: BR-G5, NFR-39
     */
    @Test
    fun rebuildsStyleOnChange() {
        show()
        val area = CampusBounds(west = 0.0, south = 0.0, east = 1.0, north = 1.0)
        compose.runOnIdle { data = dataOf(building, other) }
        compose.runOnIdle {
            selection = MapSelection(bounds = area)
            basemap = Basemap(bounds = area)
        }
        compose.runOnIdle { selection = selection.copy(boundary = outline) }
        compose.runOnIdle { colors = day.copy(isNight = true) }
        compose.runOnIdle { basemap = Basemap(bounds = area, minZoom = 12.0) }
        compose.runOnIdle {
            assertEquals(6, builds.size)
            assertEquals(6, builds.toSet().size)
            assertEquals(builds.last(), styles.last())
        }
    }

    /** Z terenem styl powstaje dopiero z prostokątem podkładu, czyli po pomiarze mapy. Wymagania: NFR-39 */
    @Test
    fun waitsForMeasuredMap() {
        selection = MapSelection(bounds = CampusBounds(west = 0.0, south = 0.0, east = 1.0, north = 1.0))
        show()
        compose.runOnIdle {
            assertEquals("bez rozmiaru mapy styl nie powstaje", 0, builds.size)
            assertEquals("bez rozmiaru mapy brak stylu", null, styles.last())
        }
        compose.runOnIdle { basemap = selection.bounds?.let { Basemap(bounds = it) } }
        compose.runOnIdle {
            assertEquals("po pomiarze jeden styl", 1, builds.size)
            assertEquals("po pomiarze styl gotowy", builds.single(), styles.last())
        }
    }

    private fun show() {
        compose.setContent {
            val seen = camera
            val style =
                rememberMapStyle(
                    data = data,
                    selection = selection,
                    colors = colors,
                    basemap = basemap,
                ) { mapData, area, palette, box ->
                    campusMapStyle(data = mapData, selection = area, colors = palette, basemap = box).also(builds::add)
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
