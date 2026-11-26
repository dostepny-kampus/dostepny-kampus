/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.navigation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import io.github.dostepnykampus.app.AppShell
import io.github.dostepnykampus.app.EXPAND_HANDLE
import io.github.dostepnykampus.app.TestMapListChoice
import io.github.dostepnykampus.app.TestRoles
import io.github.dostepnykampus.app.sheetHandle
import io.github.dostepnykampus.core.data.barrier.AcceptedBarrier
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
import io.github.dostepnykampus.core.model.room.FoundRoom
import io.github.dostepnykampus.core.model.room.Room
import io.github.dostepnykampus.core.model.room.RoomCategory
import io.github.dostepnykampus.core.model.room.RoomList
import io.github.dostepnykampus.core.model.submission.BarrierType
import io.github.dostepnykampus.core.model.submission.Severity
import io.github.dostepnykampus.feature.map.CampusMapViewModel
import io.github.dostepnykampus.feature.map.building.BuildingCardViewModel
import io.github.dostepnykampus.feature.map.camera.CameraMotion
import io.github.dostepnykampus.feature.map.camera.CameraTransition
import io.github.dostepnykampus.feature.map.camera.CampusMapView
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.camera.MapInset
import io.github.dostepnykampus.feature.map.camera.TouchSign
import io.github.dostepnykampus.feature.map.campus.MapSelection
import io.github.dostepnykampus.feature.map.campus.MapServer
import io.github.dostepnykampus.feature.map.campus.campusMapData
import io.github.dostepnykampus.feature.map.entrance.BuildingEntrancesViewModel
import io.github.dostepnykampus.feature.map.entrance.EntranceLiftViewModel
import io.github.dostepnykampus.feature.map.navigation.BuildingRoute
import io.github.dostepnykampus.feature.map.navigation.MapRoute
import io.github.dostepnykampus.feature.map.plan.MapPlanViewModel
import io.github.dostepnykampus.feature.map.room.RoomCardViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Wiersz budynku w arkuszu znaku, wyboru budynku albo sali nad mapą V1 otwiera niską kartę V2 bez przelotu mapy.
 *
 * Wymagania: US2, US2.8, NFR-18, NFR-53
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp")
class BuildingRowNoFlightTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val moves = mutableListOf<Pair<CameraTransition, MapInset>>()
    private val selections = mutableListOf<MapSelection>()
    private var onIdle: ((MapCamera) -> Unit)? = null
    private var onBuildings: (List<String>) -> Unit = {}
    private var onSign: (TouchSign, String) -> Unit = { _, _ -> }
    private lateinit var mapModel: CampusMapViewModel
    private val fakeMap =
        CampusMapView { _, selection, move, inset, idle, touch, sign, modifier: Modifier ->
            moves += move to inset
            selections += selection
            onIdle = idle
            onBuildings = touch
            onSign = sign
            Box(modifier)
        }

    /**
     * Scenariusz (przykład: dotknie znaku wejścia A-1 na mapie): przegladanie-mapy.feature:
     *   Wiersz budynku w arkuszu otwiera kartę budynku bez przelotu mapy.
     */
    @Test
    fun opensCardFromEntrance() {
        startApp()
        moveTo(OFF_CENTER)
        compose.runOnUiThread { onSign(TouchSign.ENTRANCE, ENTRANCE_ID) }
        compose.waitForIdle()
        compose.onNodeWithText("Wejście").assertExists("arkusz wejścia przed wyborem budynku")
        assertCardWithoutFlight(sheetHeader = "Wejście")
    }

    /**
     * Scenariusz (przykład: dotknie znaku toalety dostosowanej na planie A-1): przegladanie-mapy.feature:
     *   Wiersz budynku w arkuszu otwiera kartę budynku bez przelotu mapy.
     */
    @Test
    fun opensCardFromToilet() =
        openPlanSign(id = WC_ID, header = "Toaleta dostosowana")

    /**
     * Scenariusz (przykład: dotknie schodów na planie A-1): przegladanie-mapy.feature:
     *   Wiersz budynku w arkuszu otwiera kartę budynku bez przelotu mapy.
     */
    @Test
    fun opensCardFromStairs() =
        openPlanSign(id = STAIRS_ID, header = "Schody")

    /**
     * Scenariusz (przykład: dotknie znaku bariery przy budynku A-1): przegladanie-mapy.feature:
     *   Wiersz budynku w arkuszu otwiera kartę budynku bez przelotu mapy.
     */
    @Test
    fun opensCardFromBarrier() {
        startApp()
        moveTo(OFF_CENTER)
        compose.runOnUiThread { onSign(TouchSign.BARRIER, BARRIER.id) }
        compose.waitForIdle()
        compose.onNodeWithText("Próg").assertExists("arkusz bariery przed wyborem budynku")
        assertCardWithoutFlight(sheetHeader = "Próg")
    }

    /**
     * Scenariusz (przykład: wyszuka „A-1 329”): przegladanie-mapy.feature:
     *   Wiersz budynku w arkuszu otwiera kartę budynku bez przelotu mapy.
     */
    @Test
    fun opensCardFromRoomSearch() {
        startApp()
        compose.onNode(hasSetTextAction()).performTextInput("A-1 329")
        val result = hasContentDescription("Sala 329, ", substring = true)
        compose.waitUntil("wynik sali 329", timeoutMillis = WAIT_MS) {
            compose.onAllNodes(result).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(result).performClick()
        compose.waitUntil("kamera i arkusz sali 329", timeoutMillis = WAIT_MS) {
            compose.onAllNodes(hasContentDescription(BUILDING_ROW)).fetchSemanticsNodes().isNotEmpty() &&
                moves.last().first.camera.center == ROOM_POINT
        }
        moveTo(moves.last().first.camera)
        assertCardWithoutFlight(sheetHeader = "Sala 329")
    }

    /**
     * Scenariusz (przykład: dotknie mapy obok konturów A-1 i A-4, poza nimi): przegladanie-mapy.feature:
     *   Wiersz budynku w arkuszu otwiera kartę budynku bez przelotu mapy.
     */
    @Test
    fun opensCardFromChoiceSheet() {
        startApp()
        moveTo(OFF_CENTER)
        compose.runOnUiThread { onBuildings(listOf("A-1", "A-4")) }
        compose.waitForIdle()
        compose.onNodeWithText("Który budynek?").assertExists("arkusz wyboru budynku")
        compose.onNodeWithContentDescription("Budynek A-4").assertExists("wiersz budynku A-4 w arkuszu")
        assertCardWithoutFlight(sheetHeader = "Który budynek?")
    }

    private fun openPlanSign(id: String, header: String) {
        startApp()
        moveTo(OFF_CENTER)
        compose.waitUntil("plan A-1 przed dotknięciem $header", timeoutMillis = WAIT_MS) {
            selections.lastOrNull()?.plan?.building == "A-1"
        }
        compose.runOnUiThread { onSign(TouchSign.PLAN, id) }
        compose.waitForIdle()
        compose.onNodeWithText(header).assertExists("arkusz $header przed wyborem budynku")
        assertCardWithoutFlight(sheetHeader = header)
    }

    private fun startApp() {
        val repo =
            BuildingCatalogRepository(EmptyCoroutineContext, { ServerReply.Ok(SERVER_BUILDINGS) }) { GEOJSON }
        val plans = FakePlans()
        val entrances = ServerEntrances { emptyList() }
        val server = MapServer({ listOf(BARRIER) }, { ServerReply.Ok(listOf(FOUND_329)) }, { emptyList() })
        compose.setContent {
            mapModel =
                remember {
                    CampusMapViewModel(
                        repository = repo,
                        networkStatus = { flowOf(true) },
                        mapListChoice = TestMapListChoice(),
                        computeContext = UnconfinedTestDispatcher(),
                        geometry = ::campusMapData,
                        server = server,
                    )
                }
            val planModel = remember { MapPlanViewModel(repo, plans, entrances, { listOf(BARRIER) }) }
            val entrModel = remember { BuildingEntrancesViewModel(entrances) }
            val liftModel = remember { EntranceLiftViewModel(plans) }
            val roomModel = remember { RoomCardViewModel(plans, entrances, { listOf(BARRIER) }) }
            DostepnyKampusTheme {
                AppShell(firstLaunch = false, roles = TestRoles(), onFirstHelpLeft = {}) { route: NavKey ->
                    when (route) {
                        is MapRoute -> {
                            HiltMapScreen(
                                focus = route.focus,
                                viewModel = mapModel,
                                planViewModel = planModel,
                                entrancesViewModel = entrModel,
                                liftViewModel = liftModel,
                                mapView = fakeMap,
                                roomViewModel = roomModel,
                            )
                        }

                        is BuildingRoute -> {
                            val card =
                                remember(route.symbol) {
                                    BuildingCardViewModel(
                                        route.symbol,
                                        repo,
                                        { flowOf(true) },
                                        entrances,
                                        { listOf(BARRIER) },
                                        plans,
                                    )
                                }
                            val state by card.uiState.collectAsStateWithLifecycle()
                            val isOnline by card.online.collectAsStateWithLifecycle()
                            BuildingCard(route.symbol, state, isOnline, card::retry)
                        }

                        else -> {
                            AppScreen(route)
                        }
                    }
                }
            }
        }
        compose.waitUntil("widok mapy i bariery gotowe", timeoutMillis = WAIT_MS) {
            onIdle != null && moves.isNotEmpty() && mapModel.uiState.value.barriers.isNotEmpty()
        }
        compose.waitForIdle()
    }

    private fun moveTo(camera: MapCamera) {
        compose.runOnUiThread { onIdle?.invoke(camera) }
        compose.waitForIdle()
    }

    private fun assertCardWithoutFlight(sheetHeader: String) {
        val before = moves.size
        val beforeCamera = moves.last().first.camera
        compose.onNodeWithContentDescription(BUILDING_ROW).performClick()
        compose.waitUntil("niska karta budynku A-1", timeoutMillis = WAIT_MS) {
            compose.onAllNodes(sheetHandle(isExpanded = false)).fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitForIdle()

        compose.onNodeWithText(sheetHeader).assertDoesNotExist()
        compose.onNode(hasContentDescription(BUILDING_ROW)).assertExists("nagłówek karty budynku A-1")
        compose.onNode(sheetHandle(isExpanded = false)).assertExists("karta budynku A-1 na niskiej wysokości")
        compose.onNodeWithText("Zamknij").assertExists("przycisk „Zamknij” na karcie budynku")
        compose.onNodeWithText("Pokaż budynek na mapie").assertDoesNotExist()
        val handleTop = compose.onNodeWithContentDescription(EXPAND_HANDLE).getUnclippedBoundsInRoot().top
        assertTrue("niska karta zostawia mapę nad sobą: $handleTop", handleTop > 0.dp)
        assertEquals("zaznaczony budynek A-1 na mapie", "A-1", selections.last().building)

        val afterMoves = moves.subList(fromIndex = before, toIndex = moves.size).map { it.first }.distinct()
        assertTrue("mapa bez przelotu: $afterMoves", afterMoves.none { it.motion == CameraMotion.FLIGHT })
        assertTrue("ruch jak przy dotknięciu konturu: $afterMoves", afterMoves.all { it.motion == CameraMotion.NONE })
        val finalCamera = moves.last().first.camera
        assertEquals("powiększenie zostaje jak przed wyborem", beforeCamera.zoom, finalCamera.zoom, 0.0)
        assertEquals("długość bez przelotu", beforeCamera.center.longitude, finalCamera.center.longitude, 1e-9)
    }

    private class FakePlans : FloorPlans {
        override suspend fun download(symbol: String): Boolean = symbol == "A-1"

        override suspend fun floors(symbol: String): FloorList? =
            FloorList(SOURCE, listOf(FLOOR_0, FLOOR_2)).takeIf { symbol == "A-1" }

        override suspend fun rooms(symbol: String, floor: String): RoomList? =
            when {
                symbol != "A-1" -> null
                floor == FLOOR_0.id -> RoomList(SOURCE, AREA_A1, listOf(WC, STAIRS))
                floor == FLOOR_2.id -> RoomList(SOURCE, AREA_A1, listOf(ROOM_329))
                else -> null
            }
    }

    private companion object {
        const val WAIT_MS = 5_000L
        const val SOURCE = "Politechnika Wrocławska"
        const val BUILDING_ROW = "Budynek A-1"
        const val ENTRANCE_ID = "node/1"
        const val WC_ID = "wc"
        const val STAIRS_ID = "schody"
        val OFF_CENTER = MapCamera(center = GeoPoint(longitude = 17.0506, latitude = 51.1102), zoom = 18.5)
        val ROOM_POINT = GeoPoint(longitude = 17.0506, latitude = 51.1103)
        val FLOOR_0 = Floor(id = "f0", label = "Parter", order = 0)
        val FLOOR_2 = Floor(id = "f2", label = "2 piętro", order = 2)
        val A1_POINTS =
            listOf(17.0500 to 51.1100, 17.0508 to 51.1100, 17.0508 to 51.1104, 17.0500 to 51.1104, 17.0500 to 51.1100)
                .map { (lon, lat) -> GeoPoint(longitude = lon, latitude = lat) }
        val AREA_A1 = GeoPolygon(listOf(A1_POINTS))
        val ROOM_329 = room("329", RoomCategory.LECTURE_HALL, "Sala wykładowa", ROOM_POINT, number = "329")
        val FOUND_329 = FoundRoom(building = "A-1", floor = FLOOR_2, room = ROOM_329)
        val WC = room(WC_ID, RoomCategory.ACCESSIBLE_TOILET, "Toaleta", GeoPoint(17.0502, 51.1102))
        val STAIRS = room(STAIRS_ID, RoomCategory.STAIRCASE, "Schody", GeoPoint(17.0503, 51.1102))
        val BARRIER =
            AcceptedBarrier(
                id = "b1",
                location = GeoPoint(longitude = 17.0504, latitude = 51.1100),
                severity = Severity.HINDRANCE,
                type = BarrierType.THRESHOLD,
                buildingSymbol = "A-1",
                verifiedAt = null,
            )
        val SERVER_BUILDINGS =
            ServerBuildings(
                source = SOURCE,
                buildings = listOf(building("A-1", 17.0500, 17.0508), building("A-4", 17.0510, 17.0514)),
                detailed = setOf("A-1"),
            )
        const val GEOJSON =
            """{"type":"FeatureCollection","attribution":"© OSM","features":[""" +
                """{"type":"Feature","properties":{"kind":"building","symbols":["A-1"],"osm_id":"way/a1"},""" +
                """"geometry":{"type":"Polygon","coordinates":[[[17.0500,51.1100],[17.0508,51.1100],""" +
                """[17.0508,51.1104],[17.0500,51.1104],[17.0500,51.1100]]]}},""" +
                """{"type":"Feature","properties":{"kind":"building","symbols":["A-4"],"osm_id":"way/a4"},""" +
                """"geometry":{"type":"Polygon","coordinates":[[[17.0510,51.1100],[17.0514,51.1100],""" +
                """[17.0514,51.1104],[17.0510,51.1104],[17.0510,51.1100]]]}},""" +
                """{"type":"Feature","properties":{"kind":"entrance","symbols":["A-1"],"osm_id":"node/1",""" +
                """"entrance":"main","wheelchair":"yes"},"geometry":{"type":"Point",""" +
                """"coordinates":[17.0504,51.1100]}}]}"""

        fun building(symbol: String, west: Double, east: Double): Building {
            val pts = listOf(west to 51.1100, east to 51.1100, east to 51.1104, west to 51.1104, west to 51.1100)
            val outline = GeoPolygon(listOf(pts.map { (lon, lat) -> GeoPoint(longitude = lon, latitude = lat) }))
            return Building(symbol = symbol, name = null, outline = outline)
        }

        fun room(
            id: String,
            category: RoomCategory,
            function: String,
            point: GeoPoint,
            number: String? = null,
        ): Room =
            Room(
                id = id,
                number = number,
                function = function,
                category = category,
                geometry = GeoMultiPolygon(emptyList()),
                labelPoint = point,
                isLabelled = number != null,
            )
    }
}
