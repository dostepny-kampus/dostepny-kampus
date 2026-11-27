/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.navigation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
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
import io.github.dostepnykampus.core.data.barrier.AcceptedBarriers
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
        CampusMapView { _, selection, move, inset, _, idle, touch, sign, modifier: Modifier ->
            moves += move to inset
            selections += selection
            onIdle = idle
            onBuildings = touch
            onSign = sign
            Box(modifier)
        }

    private val repo =
        BuildingCatalogRepository(
            ioContext = EmptyCoroutineContext,
            listBuildings = { ServerReply.Ok(SERVER_BUILDINGS) },
            readGeoJson = { GEOJSON },
        )
    private val server =
        MapServer(
            barriers = { listOf(BARRIER) },
            rooms = { ServerReply.Ok(listOf(FOUND_329)) },
            paths = { emptyList() },
        )

    /**
     * Scenariusz (przykład: dotknie znaku wejścia A-1 na mapie): przegladanie-mapy.feature:
     *   Wiersz budynku w arkuszu otwiera kartę budynku bez przelotu mapy.
     */
    @Test
    fun opensCardFromEntrance() {
        openSign(sign = TouchSign.ENTRANCE, id = "node/1", header = "Wejście")
    }

    /**
     * Scenariusz (przykład: dotknie znaku toalety dostosowanej na planie A-1): przegladanie-mapy.feature:
     *   Wiersz budynku w arkuszu otwiera kartę budynku bez przelotu mapy.
     */
    @Test
    fun opensCardFromToilet() {
        openSign(sign = TouchSign.PLAN, id = "wc", header = "Toaleta dostosowana")
    }

    /**
     * Scenariusz (przykład: dotknie schodów na planie A-1): przegladanie-mapy.feature:
     *   Wiersz budynku w arkuszu otwiera kartę budynku bez przelotu mapy.
     */
    @Test
    fun opensCardFromStairs() {
        openSign(sign = TouchSign.PLAN, id = "schody", header = "Schody")
    }

    /**
     * Scenariusz (przykład: dotknie znaku bariery przy budynku A-1): przegladanie-mapy.feature:
     *   Wiersz budynku w arkuszu otwiera kartę budynku bez przelotu mapy.
     */
    @Test
    fun opensCardFromBarrier() {
        openSign(sign = TouchSign.BARRIER, id = BARRIER.id, header = "Próg")
    }

    /**
     * Scenariusz (przykład: wyszuka „A-1 329”): przegladanie-mapy.feature:
     *   Wiersz budynku w arkuszu otwiera kartę budynku bez przelotu mapy.
     */
    @Test
    fun opensCardFromRoomSearch() {
        startApp()
        moveTo(OFF_CENTER)
        compose.onNode(hasSetTextAction()).performTextInput("A-1 329")
        val result = hasContentDescription("Sala 329, ", substring = true)
        compose.waitUntil("wynik sali 329", timeoutMillis = WAIT_MS) {
            compose.onAllNodes(result).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(result).performClick()
        compose.waitUntil("kamera i arkusz sali 329", timeoutMillis = WAIT_MS) {
            val rowNodes = compose.onAllNodes(hasContentDescription(BUILDING_ROW)).fetchSemanticsNodes()
            val shown = moves.last().first
            rowNodes.isNotEmpty() && shown.camera.center == ROOM_329.labelPoint
        }
        moveTo(moves.last().run { first.camera })
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

    private fun openSign(sign: TouchSign, id: String, header: String) {
        startApp()
        moveTo(OFF_CENTER)
        if (sign == TouchSign.PLAN) {
            compose.waitUntil("plan A-1 przed dotknięciem $header", timeoutMillis = WAIT_MS) {
                selections.lastOrNull()?.plan?.building == "A-1"
            }
        }
        compose.runOnUiThread { onSign(sign, id) }
        compose.waitForIdle()
        compose.onNodeWithText(header).assertExists("arkusz $header przed wyborem budynku")
        assertCardWithoutFlight(sheetHeader = header)
    }

    private fun startApp() {
        val plans = FakePlans()
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
            val entrances = remember { ServerEntrances { emptyList() } }
            val barriers = remember { AcceptedBarriers { emptyList() } }
            val planModel =
                remember {
                    MapPlanViewModel(repository = repo, plans = plans, entrances = entrances, barriers = barriers)
                }
            val entrModel = remember { BuildingEntrancesViewModel(entrances) }
            val liftModel = remember { EntranceLiftViewModel(plans) }
            val roomModel = remember { RoomCardViewModel(plans = plans, entrances = entrances, barriers = barriers) }
            DostepnyKampusTheme {
                AppShell(firstLaunch = false, roles = TestRoles(), onFirstHelpLeft = {}) { route ->
                    if (route is MapRoute) {
                        HiltMapScreen(
                            focus = route.focus,
                            viewModel = mapModel,
                            planViewModel = planModel,
                            entrancesViewModel = entrModel,
                            liftViewModel = liftModel,
                            mapView = fakeMap,
                            roomViewModel = roomModel,
                        )
                    } else {
                        OtherScreen(route = route, repo = repo)
                    }
                }
            }
        }
        compose.waitUntil("widok mapy i bariery gotowe", timeoutMillis = WAIT_MS) {
            val state = mapModel.uiState.value
            onIdle != null && moves.isNotEmpty() && state.barriers.isNotEmpty()
        }
        compose.waitForIdle()
    }

    private fun moveTo(camera: MapCamera) {
        compose.runOnUiThread { onIdle?.invoke(camera) }
        compose.waitForIdle()
    }

    private fun assertCardWithoutFlight(sheetHeader: String) {
        val before = moves.size
        val beforeMove = moves.last().first
        val beforeCamera = beforeMove.camera
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

        val afterMoves =
            moves
                .subList(fromIndex = before, toIndex = moves.size)
                .asSequence()
                .map { it.first }
                .filter { it != beforeMove }
                .distinct()
                .toList()
        assertTrue("brak nowego ruchu po wyborze wiersza budynku", afterMoves.isNotEmpty())
        assertTrue("mapa bez przelotu: $afterMoves", afterMoves.none { it.motion == CameraMotion.FLIGHT })
        assertTrue("ruch jak przy dotknięciu konturu: $afterMoves", afterMoves.all { it.motion == CameraMotion.NONE })
        val finalCamera = moves.last().run { first.camera }
        assertEquals("powiększenie zostaje jak przed wyborem", beforeCamera.zoom, finalCamera.zoom, 0.0)
        assertEquals("długość bez przelotu", beforeCamera.center.longitude, finalCamera.center.longitude, 1e-9)
    }

    private class FakePlans : FloorPlans {
        override suspend fun download(symbol: String): Boolean = symbol == "A-1"

        override suspend fun floors(symbol: String): FloorList? =
            FloorList(source = SOURCE, floors = listOf(FLOOR_0, FLOOR_2)).takeIf { symbol == "A-1" }

        override suspend fun rooms(symbol: String, floor: String): RoomList? =
            when {
                symbol != "A-1" -> null
                floor == FLOOR_0.id -> RoomList(source = SOURCE, floorOutline = EMPTY_POLY, rooms = listOf(WC, STAIRS))
                floor == FLOOR_2.id -> RoomList(source = SOURCE, floorOutline = EMPTY_POLY, rooms = listOf(ROOM_329))
                else -> null
            }
    }

    private companion object {
        const val WAIT_MS = 5_000L
        const val SOURCE = "Politechnika Wrocławska"
        const val BUILDING_ROW = "Budynek A-1"
        val EMPTY_POLY = GeoMultiPolygon(emptyList())
        val OFF_CENTER = MapCamera(center = GeoPoint(longitude = 17.0621, latitude = 51.1075), zoom = 18.5)
        val FLOOR_0 = Floor(id = "f0", label = "Parter", order = 0)
        val FLOOR_2 = Floor(id = "f2", label = "2 piętro", order = 2)
        val ROOM_329 = room(id = "329", kind = RoomCategory.LECTURE_HALL, lon = 17.0621, lat = 51.1076, num = "329")
        val FOUND_329 = FoundRoom(building = "A-1", floor = FLOOR_2, room = ROOM_329)
        val WC = room(id = "wc", kind = RoomCategory.ACCESSIBLE_TOILET, lon = 17.0617, lat = 51.1075)
        val STAIRS = room(id = "schody", kind = RoomCategory.STAIRCASE, lon = 17.0618, lat = 51.1075)
        val BARRIER =
            AcceptedBarrier(
                id = "b1",
                location = GeoPoint(longitude = 17.0619, latitude = 51.1073),
                severity = Severity.HINDRANCE,
                type = BarrierType.THRESHOLD,
                buildingSymbol = "A-1",
                verifiedAt = null,
            )
        val A1 = building(symbol = "A-1", west = 17.0615, east = 17.0623)
        val A4 = building(symbol = "A-4", west = 17.0625, east = 17.0629)
        val SERVER_BUILDINGS = ServerBuildings(source = SOURCE, buildings = listOf(A1, A4), detailed = setOf("A-1"))
        const val GEOJSON =
            """{"type":"FeatureCollection","attribution":"© OSM","features":[""" +
                """{"type":"Feature","properties":{"kind":"building","symbols":["A-1"],"osm_id":"way/a1"},""" +
                """"geometry":{"type":"Polygon","coordinates":[[[17.0615,51.1073],[17.0623,51.1073],""" +
                """[17.0623,51.1077],[17.0615,51.1077],[17.0615,51.1073]]]}},""" +
                """{"type":"Feature","properties":{"kind":"building","symbols":["A-4"],"osm_id":"way/a4"},""" +
                """"geometry":{"type":"Polygon","coordinates":[[[17.0625,51.1073],[17.0629,51.1073],""" +
                """[17.0629,51.1077],[17.0625,51.1077],[17.0625,51.1073]]]}},""" +
                """{"type":"Feature","properties":{"kind":"entrance","symbols":["A-1"],"osm_id":"node/1",""" +
                """"entrance":"main","wheelchair":"yes"},"geometry":{"type":"Point",""" +
                """"coordinates":[17.0619,51.1073]}}]}"""

        fun building(symbol: String, west: Double, east: Double): Building {
            val pts = listOf(west to 51.1073, east to 51.1073, east to 51.1077, west to 51.1077, west to 51.1073)
            val outline = GeoPolygon(listOf(pts.map { (lon, lat) -> GeoPoint(longitude = lon, latitude = lat) }))
            return Building(symbol = symbol, name = null, outline = outline)
        }

        fun room(id: String, kind: RoomCategory, lon: Double, lat: Double, num: String? = null): Room =
            Room(
                id = id,
                number = num,
                function = kind.name,
                category = kind,
                geometry = EMPTY_POLY,
                labelPoint = GeoPoint(longitude = lon, latitude = lat),
                isLabelled = num != null,
            )
    }
}

@Composable
private fun AppNavigator.OtherScreen(route: NavKey, repo: BuildingCatalogRepository) {
    if (route is BuildingRoute) {
        val card = remember(route.symbol) { BuildingCardViewModel(route.symbol, repo, { flowOf(true) }) }
        val state by card.uiState.collectAsStateWithLifecycle()
        val isOnline by card.online.collectAsStateWithLifecycle()
        BuildingCard(symbol = route.symbol, state = state, isOnline = isOnline, onRetry = card::retry)
    } else {
        AppScreen(route)
    }
}
