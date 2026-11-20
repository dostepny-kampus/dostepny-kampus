/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation3.runtime.NavKey
import io.github.dostepnykampus.app.navigation.HiltMapScreen
import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.building.ServerEntrances
import io.github.dostepnykampus.core.data.room.FloorPlans
import io.github.dostepnykampus.core.data.server.ServerBuildings
import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.GeoMultiPolygon
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.core.model.building.Building
import io.github.dostepnykampus.core.model.building.Floor
import io.github.dostepnykampus.core.model.building.FloorList
import io.github.dostepnykampus.core.model.room.RoomList
import io.github.dostepnykampus.feature.map.camera.CampusMapView
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.camera.TouchSign
import io.github.dostepnykampus.feature.map.entrance.BuildingEntrancesViewModel
import io.github.dostepnykampus.feature.map.entrance.EntranceLiftViewModel
import io.github.dostepnykampus.feature.map.navigation.BuildingRoute
import io.github.dostepnykampus.feature.map.navigation.MapRoute
import io.github.dostepnykampus.feature.map.plan.MapPlanViewModel
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Ścieżka nawigacji od dotknięcia budynku i znaku wejścia do wyboru planu piętra w [MapPlanViewModel].
 *
 * Wymagania: US16.1, US16.2, US2.8, NFR-56
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class MapPlanSelectionTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var shownPlan: String? = null
    private var onIdle: ((MapCamera) -> Unit)? = null
    private var onBuildings: (List<String>) -> Unit = {}
    private var onSign: (TouchSign, String) -> Unit = { _, _ -> }
    private val fakeMap =
        CampusMapView { _, selection, _, _, idle, touch, sign, modifier: Modifier ->
            shownPlan = selection.plan?.building
            onIdle = idle
            onBuildings = touch
            onSign = sign
            Box(modifier)
        }

    /** Scenariusz: przegladanie-mapy.feature: Otwarta karta budynku albo sali trzyma plan tego budynku. */
    @Test
    fun tracksPlanWithCardAndEntrance() {
        val repo =
            BuildingCatalogRepository(
                ioContext = EmptyCoroutineContext,
                listBuildings = { ServerReply.Ok(SERVER_BUILDINGS) },
            ) { GEOJSON }
        val plans = FakePlans()
        val entrances = ServerEntrances { emptyList() }
        compose.setContent {
            val mapModel = remember { inlineMapViewModel(repo, { flowOf(true) }, TestMapListChoice()) }
            val planModel = remember { MapPlanViewModel(repo, plans, entrances) }
            val entrModel = remember { BuildingEntrancesViewModel(entrances) }
            val liftModel = remember { EntranceLiftViewModel(plans) }
            DostepnyKampusTheme {
                AppShell(firstLaunch = false, roles = TestRoles(), onFirstHelpLeft = {}) { route: NavKey ->
                    when (route) {
                        is MapRoute ->
                            HiltMapScreen(
                                focus = route.focus,
                                viewModel = mapModel,
                                planViewModel = planModel,
                                entrancesViewModel = entrModel,
                                liftViewModel = liftModel,
                                mapView = fakeMap,
                            )

                        is BuildingRoute -> Text("Karta ${route.symbol}")

                        else -> Unit
                    }
                }
            }
        }
        compose.waitUntil { onIdle != null }
        move(INSIDE_A1, "A-1")
        tap(listOf("B-1"), "B-1")
        compose.onNodeWithText("Karta B-1").assertIsDisplayed()
        move(FAR_WEST, null)
        compose.onNodeWithText("Karta B-1").assertIsDisplayed()
        move(INSIDE_A1, "B-1")
        tap(listOf("B-8"), null)
        compose.onNodeWithText("Karta B-8").assertIsDisplayed()
        tap(emptyList(), "A-1")
        compose.waitForIdle()
        compose.onNodeWithText("Karta B-8").assertDoesNotExist()
        compose.runOnUiThread { onSign(TouchSign.ENTRANCE, "node/b8") }
        compose.waitUntil { shownPlan == null }
        tap(emptyList(), "A-1")
        assertEquals("plan po zamknięciu arkusza wejścia wraca do A-1", "A-1", shownPlan)
    }

    private fun move(center: GeoPoint, expected: String?) {
        compose.runOnUiThread { onIdle?.invoke(MapCamera(center = center, zoom = 18.0)) }
        compose.waitUntil { shownPlan == expected }
    }

    private fun tap(symbols: List<String>, expected: String?) {
        compose.runOnUiThread { onBuildings(symbols) }
        compose.waitUntil { shownPlan == expected }
    }

    private class FakePlans : FloorPlans {
        override suspend fun download(symbol: String): Boolean = true

        override suspend fun floors(symbol: String): FloorList? =
            FloorList(SOURCE, listOf(Floor("P0", "Parter", 0))).takeIf { symbol in setOf("A-1", "B-1") }

        override suspend fun rooms(symbol: String, floor: String): RoomList? =
            RoomList(SOURCE, GeoMultiPolygon(emptyList()), emptyList()).takeIf { symbol in setOf("A-1", "B-1") }
    }

    private companion object {
        const val SOURCE = "Politechnika Wrocławska"
        val INSIDE_A1 = GeoPoint(longitude = 17.0502, latitude = 51.1102)
        val FAR_WEST = GeoPoint(longitude = 17.048, latitude = 51.1102)
        val SERVER_BUILDINGS =
            ServerBuildings(
                source = SOURCE,
                buildings = listOf(square("A-1", 17.0500), square("B-1", 17.0505), square("B-8", 17.0510)),
                detailed = setOf("A-1", "B-1"),
            )
        val GEOJSON =
            """{"type":"FeatureCollection","attribution":"© OSM","features":[""" +
                """{"type":"Feature","properties":{"kind":"building","symbols":["B-8"],"osm_id":"way/b8"},""" +
                """"geometry":{"type":"Polygon","coordinates":[[[17.051,51.11],[17.0514,51.11],""" +
                """[17.0514,51.1104],[17.051,51.1104],[17.051,51.11]]]}},""" +
                """{"type":"Feature","properties":{"kind":"entrance","symbols":["B-8"],"osm_id":"node/b8",""" +
                """"entrance":"yes","wheelchair":"yes"},"geometry":{"type":"Point",""" +
                """"coordinates":[17.0512,51.1102]}}]}"""

        fun square(symbol: String, west: Double): Building {
            val east = west + 0.0004
            val pts = listOf(west to 51.11, east to 51.11) + listOf(east to 51.1104, west to 51.1104, west to 51.11)
            return Building(symbol, null, GeoPolygon(listOf(pts.map { (lon, lat) -> GeoPoint(lon, lat) })))
        }
    }
}
