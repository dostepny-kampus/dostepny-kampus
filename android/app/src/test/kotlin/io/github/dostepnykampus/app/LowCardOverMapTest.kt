/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import io.github.dostepnykampus.app.navigation.AppNavigator
import io.github.dostepnykampus.app.navigation.AppScreen
import io.github.dostepnykampus.app.navigation.BuildingCard
import io.github.dostepnykampus.app.navigation.MapScreen
import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
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
import io.github.dostepnykampus.core.model.room.Room
import io.github.dostepnykampus.core.model.room.RoomCategory
import io.github.dostepnykampus.core.model.room.RoomList
import io.github.dostepnykampus.feature.map.building.BuildingCardViewModel
import io.github.dostepnykampus.feature.map.camera.CampusMapView
import io.github.dostepnykampus.feature.map.navigation.BuildingRoute
import io.github.dostepnykampus.feature.map.navigation.MapRoute
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Niska karta budynku nad mapą V1 z wierszem o wejściach, nagłówkiem „Zamknij” i wystającym nagłówkiem „Wejścia”
 * oraz jej rozwinięcie do pełnego ekranu, na syntetycznym katalogu (nie dane PWr).
 *
 * Wymagania: US2.8, NFR-53
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class LowCardOverMapTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val choice = TestMapListChoice()
    private var touch: (List<String>) -> Unit = {}
    private val fakeMap =
        CampusMapView { _, _, _, _, _, onBuildingsTouch, _, modifier ->
            touch = onBuildingsTouch
            Box(modifier.testTag(MAP_TAG))
        }

    /**
     * Budynek A-1 z wejściem dostępnym: niska karta z „Wejście dostępne”, zalecanym i „Pokaż wejście na mapie”.
     * Scenariusz (przykład: A-1): przegladanie-mapy.feature: Niska karta nad mapą mówi słowami o wejściach
     *
     * Wymagania: US2.8
     */
    @Test
    fun showsLowCardForAccessible() {
        checkLowCard(
            symbol = "A-1",
            summary = "Wejście dostępne",
            sentence = "Zalecane: „Wejście główne”.",
            hasButton = true,
        )
    }

    /**
     * Budynek B-8 bez wejść dostępnych: niska karta z „Brak wejścia dostępnego”, bez przycisku wypełnionego.
     * Scenariusz (przykład: B-8): przegladanie-mapy.feature: Niska karta nad mapą mówi słowami o wejściach
     *
     * Wymagania: US2.8
     */
    @Test
    fun showsLowCardWithoutAccess() {
        checkLowCard(
            symbol = "B-8",
            summary = "Brak wejścia dostępnego",
            sentence = "Żadne z 2 wejść nie jest dostępne.",
            hasButton = false,
        )
    }

    /**
     * Budynek C-13 z wejściami bez danych: niska karta z „Brak danych o wejściach”, bez zdania i przycisku.
     * Scenariusz (przykład: C-13): przegladanie-mapy.feature: Niska karta nad mapą mówi słowami o wejściach
     *
     * Wymagania: US2.8
     */
    @Test
    fun showsLowCardWithoutData() {
        checkLowCard(symbol = "C-13", summary = "Brak danych o wejściach", sentence = null, hasButton = false)
    }

    /**
     * Budynek A-2 bez wejść w danych: niska karta z „Brak danych o wejściach”, bez zdania i przycisku.
     * Scenariusz (przykład: A-2): przegladanie-mapy.feature: Niska karta nad mapą mówi słowami o wejściach
     *
     * Wymagania: US2.8
     */
    @Test
    fun showsLowCardWithoutEntrances() {
        checkLowCard(symbol = "A-2", summary = "Brak danych o wejściach", sentence = null, hasButton = false)
    }

    /**
     * Dotknięcie „Wejścia” na niskiej karcie A-1 z wejściami i planami pięter rozwija opis na cały ekran.
     * Scenariusz (przykład: A-1): przegladanie-mapy.feature: Wystający nagłówek „Wejścia” od razu rozwija kartę
     *
     * Wymagania: US2.8, NFR-53
     */
    @Test
    fun expandsDetailedCardFromPeek() {
        expandFromPeek(symbol = "A-1", hasPlans = true)
        compose.onNodeWithText("Wejście główne").assertIsDisplayed()
        compose.onNodeWithText("Piętra").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Parter").performScrollTo().assertIsDisplayed()
    }

    /**
     * Dotknięcie „Wejścia” na niskiej karcie A-2 bez wejść i planów rozwija kartę na cały ekran z pustym miejscem.
     * Scenariusz (przykład: A-2): przegladanie-mapy.feature: Wystający nagłówek „Wejścia” od razu rozwija kartę
     *
     * Wymagania: US2.8, NFR-53
     */
    @Test
    fun expandsShortCardFromPeek() {
        val cardBottom = expandFromPeek(symbol = "A-2", hasPlans = false)
        compose.onNodeWithText(NO_ENTRANCES_TEXT).assertIsDisplayed()
        val lastNode = compose.onNodeWithText(NO_PLAN_TEXT).assertIsDisplayed()
        val lastContent = lastNode.getUnclippedBoundsInRoot()
        assertTrue(
            "pod krótką treścią A-2 zostaje puste miejsce: do ${lastContent.bottom}, karta do $cardBottom",
            cardBottom - lastContent.bottom > EMPTY_SPACE_MIN_DP.dp,
        )
    }

    @Composable
    private fun AppNavigator.TestScreen(route: NavKey, plans: FloorPlans) {
        when (route) {
            is MapRoute -> {
                val model =
                    remember {
                        inlineMapViewModel(repository = repository, networkStatus = { flowOf(true) }, choice = choice)
                    }
                val state by model.uiState.collectAsStateWithLifecycle()
                MapScreen(
                    focus = route.focus,
                    state = state,
                    onListChange = model::chooseList,
                    onSearch = model::search,
                    mapView = fakeMap,
                )
            }

            is BuildingRoute -> {
                val card =
                    remember(route.symbol) {
                        BuildingCardViewModel(
                            symbol = route.symbol,
                            repository = repository,
                            networkStatus = { flowOf(true) },
                            plans = plans,
                        )
                    }
                val state by card.uiState.collectAsStateWithLifecycle()
                Box(Modifier.testTag(CARD_TAG)) {
                    BuildingCard(symbol = route.symbol, state = state, isOnline = true, onRetry = card::retry)
                }
            }

            else -> AppScreen(route)
        }
    }

    private fun openCompactCard(symbol: String, plans: FloorPlans = FakePlans(hasPlans = false)) {
        compose.setContent {
            DostepnyKampusTheme {
                AppShell(firstLaunch = false, roles = TestRoles(), onFirstHelpLeft = {}) { route ->
                    TestScreen(route = route, plans = plans)
                }
            }
        }
        compose.waitUntil(WAIT_MS) { compose.onAllNodesWithText(OSM).fetchSemanticsNodes().isNotEmpty() }
        compose.runOnUiThread { touch(listOf(symbol)) }
        compose.waitUntil(WAIT_MS) {
            compose.onAllNodes(hasContentDescription(PEEK) and hasClickAction()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitForIdle()
    }

    private fun checkLowCard(symbol: String, summary: String, sentence: String?, hasButton: Boolean) {
        openCompactCard(symbol = symbol)
        val map = compose.onNodeWithTag(MAP_TAG).getUnclippedBoundsInRoot()
        val handle = compose.onNode(sheetHandle(isExpanded = false)).assertIsDisplayed().getUnclippedBoundsInRoot()
        val osm = compose.onNodeWithText(OSM).assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue(
            "niska karta $symbol nad mapą: uchwyt ${handle.top}, mapa $map",
            handle.top > LOW_CARD_MIN_TOP_DP.dp && handle.top < map.bottom,
        )
        assertTrue("podpis mapy nad niską kartą: osm ${osm.bottom}, uchwyt ${handle.top}", osm.bottom <= handle.top)
        compose.onNodeWithText(symbol, useUnmergedTree = true).assertIsDisplayed()
        val summaryNode = compose.onNodeWithText(summary, useUnmergedTree = true).assertIsDisplayed()
        val summaryBounds = summaryNode.getUnclippedBoundsInRoot()
        if (sentence != null) {
            compose.onNode(hasText(summary) and hasText(sentence)).assertIsDisplayed()
            val sentenceNode = compose.onNodeWithText(sentence, useUnmergedTree = true).assertIsDisplayed()
            val sentenceBounds = sentenceNode.getUnclippedBoundsInRoot()
            assertTrue("zdanie pod napisem „$summary”", sentenceBounds.top >= summaryBounds.bottom)
        } else {
            val merged = compose.onNodeWithText(summary).assertIsDisplayed().fetchSemanticsNode()
            val rawTexts = merged.config[SemanticsProperties.Text]
            assertEquals("wiersz ma tylko napis „$summary” bez zdania", listOf(summary), rawTexts.map { it.text })
        }
        val cardButtons =
            compose.onAllNodes(hasClickAction() and hasAnyAncestor(hasTestTag(CARD_TAG))).fetchSemanticsNodes()
        if (hasButton) {
            val button = compose.onNode(hasText(SHOW_ENTRANCE) and hasClickAction()).assertIsDisplayed()
            val buttonTop = button.getUnclippedBoundsInRoot().top
            assertTrue("„$SHOW_ENTRANCE” pod wierszem o wejściach", buttonTop >= summaryBounds.bottom)
            assertEquals("czynności na karcie $symbol", ACTIONS_WITH_RECOMMENDED, cardButtons.size)
        } else {
            compose.onNodeWithText(SHOW_ENTRANCE).assertDoesNotExist()
            assertEquals("brak przycisku wypełnionego na karcie $symbol", ACTIONS_NO_BUTTON, cardButtons.size)
        }
        val close = compose.onNode(hasText(CLOSE) and hasClickAction()).assertIsDisplayed().getUnclippedBoundsInRoot()
        val symbolBounds = compose.onNodeWithText(symbol, useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue(
            "„$CLOSE” w nagłówku obok symbolu: $close, symbol $symbolBounds",
            close.top < symbolBounds.bottom && close.left > symbolBounds.right,
        )
        val line = compose.onNodeWithTag(DESCRIPTION_LINE).getUnclippedBoundsInRoot()
        val peekNode = compose.onNode(hasContentDescription(PEEK) and hasClickAction()).assertIsDisplayed()
        val peek = peekNode.getUnclippedBoundsInRoot()
        assertTrue("kreska pod „$CLOSE”: $line, $close", line.top >= close.bottom)
        assertTrue("„$PEEK” pod kreską: $peek, kreska $line", peek.top >= line.bottom)
        compose.onNodeWithText(NO_ENTRANCES_TEXT).assertDoesNotExist()
        compose.onNodeWithText(SHOW_ON_MAP).assertDoesNotExist()
    }

    private fun expandFromPeek(symbol: String, hasPlans: Boolean): Dp {
        openCompactCard(symbol = symbol, plans = FakePlans(hasPlans = hasPlans))
        compose.onNode(hasContentDescription(PEEK) and hasClickAction()).performClick()
        compose.waitForIdle()
        if (hasPlans) {
            compose.waitUntil(WAIT_MS) { compose.onAllNodesWithText("Parter").fetchSemanticsNodes().isNotEmpty() }
        }
        val map = compose.onNodeWithTag(MAP_TAG).getUnclippedBoundsInRoot()
        val handle = compose.onNode(sheetHandle(isExpanded = true)).assertIsDisplayed().getUnclippedBoundsInRoot()
        assertEquals("uchwyt na górze ekranu po rozwinięciu $symbol", 0f, handle.top.value, BOUNDS_TOLERANCE)
        val card = compose.onNodeWithTag(CARD_TAG).getUnclippedBoundsInRoot()
        assertEquals("karta $symbol wypełnia cały ekran", map.bottom.value, card.bottom.value, BOUNDS_TOLERANCE)
        compose.onNode(hasContentDescription(PEEK) and hasClickAction()).assertDoesNotExist()
        compose.onNodeWithText(symbol, useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText(PEEK).assertIsDisplayed()
        return card.bottom
    }

    private class FakePlans(private val hasPlans: Boolean) : FloorPlans {
        override suspend fun download(symbol: String): Boolean = hasPlans && symbol == "A-1"

        override suspend fun floors(symbol: String): FloorList? =
            FloorList(source = SOURCE, floors = listOf(Floor(id = "P0", label = "Parter", order = 0)))
                .takeIf { hasPlans && symbol == "A-1" }

        override suspend fun rooms(symbol: String, floor: String): RoomList? =
            RoomList(source = SOURCE, floorOutline = NO_AREA, rooms = listOf(ROOM))
                .takeIf { hasPlans && symbol == "A-1" }
    }

    private companion object {
        const val WAIT_MS = 5_000L
        const val LOW_CARD_MIN_TOP_DP = 300
        const val EMPTY_SPACE_MIN_DP = 100
        const val BOUNDS_TOLERANCE = 1f
        const val ACTIONS_WITH_RECOMMENDED = 3
        const val ACTIONS_NO_BUTTON = 2
        const val MAP_TAG = "mapa-v1"
        const val CARD_TAG = "karta-v2"
        const val DESCRIPTION_LINE = "kreska-opisu"
        const val OSM = "© OpenStreetMap"
        const val PEEK = "Wejścia"
        const val CLOSE = "Zamknij"
        const val SHOW_ENTRANCE = "Pokaż wejście na mapie"
        const val SHOW_ON_MAP = "Pokaż budynek na mapie"
        const val NO_ENTRANCES_TEXT = "Nie mamy jeszcze danych o wejściach do tego budynku."
        const val NO_PLAN_TEXT = "Nie mamy planu pięter tego budynku."
        const val SOURCE = "Politechnika Wrocławska"
        val NO_AREA = GeoMultiPolygon(emptyList())
        val ROOM =
            Room(
                id = "r1",
                number = "101",
                function = "Sala dydaktyczna",
                category = RoomCategory.CLASSROOM,
                geometry = NO_AREA,
                labelPoint = GeoPoint(longitude = 0.0005, latitude = 0.0005),
                isLabelled = true,
            )
        val BUILDINGS = listOf("A-1" to 0.0, "B-8" to 0.002, "C-13" to 0.004, "A-2" to 0.006)
        val ENTRANCES =
            listOf(
                "A-1" to """"entrance":"main","wheelchair":"yes","ref":"Wejście główne"""",
                "B-8" to """"entrance":"main","wheelchair":"no","ref":"Główne"""",
                "B-8" to """"entrance":"yes","wheelchair":"no","ref":"Boczne"""",
                "C-13" to """"entrance":"main","ref":"Główne"""",
                "C-13" to """"entrance":"yes","ref":"Boczne"""",
            )
        val SERVER_BUILDINGS =
            ServerBuildings(
                source = SOURCE,
                buildings = BUILDINGS.map { (symbol, west) -> building(symbol = symbol, west = west) },
                detailed = setOf("A-1", "C-13"),
            )
        val GEOJSON_FEATURES =
            BUILDINGS.map { (symbol, west) -> bldgJson(symbol = symbol, west = west) } +
                ENTRANCES.mapIndexed { id, (symbol, tags) -> entrJson(symbol = symbol, id = id, tags = tags) }
        val GEOJSON =
            GEOJSON_FEATURES.joinToString(
                separator = ",",
                prefix = """{"type":"FeatureCollection","attribution":"© OpenStreetMap contributors","features":[""",
                postfix = "]}",
            )
        val repository =
            BuildingCatalogRepository(
                ioContext = EmptyCoroutineContext,
                listBuildings = { ServerReply.Ok(SERVER_BUILDINGS) },
            ) { GEOJSON }

        fun building(symbol: String, west: Double): Building {
            val east = west + 0.001
            val ring = listOf(west to 0.0, east to 0.0, east to 0.001, west to 0.001, west to 0.0)
            return Building(
                symbol = symbol,
                name = "Budynek $symbol",
                outline = GeoPolygon(listOf(ring.map { (lon, lat) -> GeoPoint(longitude = lon, latitude = lat) })),
            )
        }

        fun bldgJson(symbol: String, west: Double): String {
            val east = west + 0.001
            val ring = "[[$west,0.0],[$east,0.0],[$east,0.001],[$west,0.001],[$west,0.0]]"
            return """{"type":"Feature","properties":{"kind":"building","symbols":["$symbol"],""" +
                """"display_name":"Budynek $symbol","osm_id":"way/$symbol"},""" +
                """"geometry":{"type":"Polygon","coordinates":[$ring]}}"""
        }

        fun entrJson(symbol: String, id: Int, tags: String): String =
            """{"type":"Feature","properties":{"kind":"entrance","symbols":["$symbol"],"osm_id":"node/$id",$tags},""" +
                """"geometry":{"type":"Point","coordinates":[0.0005,0.0005]}}"""
    }
}
