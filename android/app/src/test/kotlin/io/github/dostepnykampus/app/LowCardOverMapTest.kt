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
import androidx.compose.ui.semantics.getOrNull
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
 * Niska karta budynku nad mapą V1 z wierszem o wejściach, nagłówkiem „Zamknij” i wystającym nagłówkiem „Wejścia”,
 * oraz jej rozwinięcie do pełnego ekranu po dotknięciu „Wejścia”, na syntetycznym katalogu (nie dane PWr).
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
     * Budynek A-1 z wejściem dostępnym po dotknięciu na mapie: niska karta z „Wejście dostępne” i zalecanym.
     * Scenariusz (przykład: A-1): przegladanie-mapy.feature: Niska karta nad mapą mówi słowami o wejściach
     *
     * Wymagania: US2.8
     */
    @Test
    fun showsLowCardForAccessible() {
        openCompactCard(symbol = "A-1")
        assertCompactCard("A-1", "Wejście dostępne", "Zalecane: „Wejście główne”.", hasShowEntrance = true)
    }

    /**
     * Budynek B-8 bez wejść dostępnych: niska karta z „Brak wejścia dostępnego” bez przycisku wypełnionego.
     * Scenariusz (przykład: B-8): przegladanie-mapy.feature: Niska karta nad mapą mówi słowami o wejściach
     *
     * Wymagania: US2.8
     */
    @Test
    fun showsLowCardWithoutAccess() {
        openCompactCard(symbol = "B-8")
        assertCompactCard("B-8", "Brak wejścia dostępnego", "Żadne z 2 wejść nie jest dostępne.", false)
    }

    /**
     * Budynek C-13 z wejściami bez danych: niska karta z „Brak danych o wejściach”, bez zdania i przycisku.
     * Scenariusz (przykład: C-13): przegladanie-mapy.feature: Niska karta nad mapą mówi słowami o wejściach
     *
     * Wymagania: US2.8
     */
    @Test
    fun showsLowCardWithoutData() {
        openCompactCard(symbol = "C-13")
        assertCompactCard("C-13", "Brak danych o wejściach", sentence = null, hasShowEntrance = false)
    }

    /**
     * Budynek A-2 bez wejść w danych: niska karta z „Brak danych o wejściach”, bez zdania i przycisku.
     * Scenariusz (przykład: A-2): przegladanie-mapy.feature: Niska karta nad mapą mówi słowami o wejściach
     *
     * Wymagania: US2.8
     */
    @Test
    fun showsLowCardWithoutEntrances() {
        openCompactCard(symbol = "A-2")
        assertCompactCard("A-2", "Brak danych o wejściach", sentence = null, hasShowEntrance = false)
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
        val lastContent = compose.onNodeWithText(NO_PLAN_TEXT).assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue(
            "pod krótką treścią A-2 zostaje puste miejsce: do ${lastContent.bottom}, karta do $cardBottom",
            cardBottom - lastContent.bottom > EMPTY_SPACE_MIN_DP.dp,
        )
    }

    @Composable
    private fun AppNavigator.TestScreen(route: NavKey, plans: FloorPlans) {
        when (route) {
            is MapRoute -> {
                val model = remember { inlineMapViewModel(repository, { flowOf(true) }, choice) }
                val state by model.uiState.collectAsStateWithLifecycle()
                MapScreen(route.focus, state, model::chooseList, model::search, mapView = fakeMap)
            }

            is BuildingRoute -> {
                val card =
                    remember(route.symbol) {
                        BuildingCardViewModel(route.symbol, repository, { flowOf(true) }, plans = plans)
                    }
                val state by card.uiState.collectAsStateWithLifecycle()
                Box(Modifier.testTag(CARD_TAG)) {
                    BuildingCard(symbol = route.symbol, state = state, isOnline = true, onRetry = card::retry)
                }
            }

            else -> {
                AppScreen(route)
            }
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

    private fun assertCompactCard(symbol: String, summary: String, sentence: String?, hasShowEntrance: Boolean) {
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
            val sentenceBounds =
                compose.onNodeWithText(sentence, useUnmergedTree = true).assertIsDisplayed().getUnclippedBoundsInRoot()
            assertTrue("zdanie pod napisem „$summary”", sentenceBounds.top >= summaryBounds.bottom)
        } else {
            val merged = compose.onNodeWithText(summary).assertIsDisplayed().fetchSemanticsNode()
            val texts = merged.config.getOrNull(SemanticsProperties.Text)?.map { it.text }.orEmpty()
            assertEquals("wiersz ma tylko napis „$summary” bez zdania", listOf(summary), texts)
        }
        val cardButtons =
            compose.onAllNodes(hasClickAction() and hasAnyAncestor(hasTestTag(CARD_TAG))).fetchSemanticsNodes()
        if (hasShowEntrance) {
            val buttonNode = compose.onNode(hasText(SHOW_ENTRANCE) and hasClickAction()).assertIsDisplayed()
            val button = buttonNode.getUnclippedBoundsInRoot()
            assertTrue("„$SHOW_ENTRANCE” pod wierszem o wejściach", button.top >= summaryBounds.bottom)
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
            FloorList(SOURCE, listOf(Floor("P0", "Parter", 0))).takeIf { hasPlans && symbol == "A-1" }

        override suspend fun rooms(symbol: String, floor: String): RoomList? =
            RoomList(SOURCE, NO_AREA, listOf(ROOM)).takeIf { hasPlans && symbol == "A-1" }
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
            Room("r1", "101", "Sala dydaktyczna", RoomCategory.CLASSROOM, NO_AREA, GeoPoint(0.0005, 0.0005), true)

        fun building(symbol: String, name: String, west: Double): Building {
            val east = west + 0.001
            val ring = listOf(west to 0.0, east to 0.0, east to 0.001, west to 0.001, west to 0.0)
            return Building(symbol, name, GeoPolygon(listOf(ring.map { (lon, lat) -> GeoPoint(lon, lat) })))
        }

        fun bldgJson(symbol: String, name: String, id: Int, west: Double): String {
            val east = west + 0.001
            val ring = "[[$west,0.0],[$east,0.0],[$east,0.001],[$west,0.001],[$west,0.0]]"
            return """{"type":"Feature","properties":{"kind":"building","symbols":["$symbol"],""" +
                """"display_name":"$name","osm_id":"way/$id"},"geometry":{"type":"Polygon","coordinates":[$ring]}}"""
        }

        fun entrJson(symbol: String, id: Int, lon: Double, tags: String): String =
            """{"type":"Feature","properties":{"kind":"entrance","symbols":["$symbol"],"osm_id":"node/$id",$tags},""" +
                """"geometry":{"type":"Point","coordinates":[$lon,0.0005]}}"""

        val SERVER_BUILDINGS =
            ServerBuildings(
                source = SOURCE,
                buildings =
                    listOf(
                        building("A-1", "Budynek A-1", west = 0.0),
                        building("B-8", "Budynek B-8", west = 0.002),
                        building("C-13", "Budynek C-13", west = 0.004),
                        building("A-2", "Budynek A-2", west = 0.006),
                    ),
                detailed = setOf("A-1", "C-13"),
            )

        val GEOJSON =
            listOf(
                bldgJson("A-1", "Budynek A-1", id = 1, west = 0.0),
                entrJson("A-1", 1, 0.0005, """"entrance":"main","wheelchair":"yes","ref":"Wejście główne""""),
                bldgJson("B-8", "Budynek B-8", id = 2, west = 0.002),
                entrJson("B-8", 2, 0.0025, """"entrance":"main","wheelchair":"no","ref":"Główne""""),
                entrJson("B-8", 3, 0.0028, """"entrance":"yes","wheelchair":"no","ref":"Boczne""""),
                bldgJson("C-13", "Budynek C-13", id = 3, west = 0.004),
                entrJson("C-13", 4, 0.0045, """"entrance":"main","ref":"Główne""""),
                entrJson("C-13", 5, 0.0048, """"entrance":"yes","ref":"Boczne""""),
                bldgJson("A-2", "Budynek A-2", id = 4, west = 0.006),
            ).joinToString(
                separator = ",",
                prefix = """{"type":"FeatureCollection","attribution":"© OpenStreetMap contributors","features":[""",
                postfix = "]}",
            )

        val repository =
            BuildingCatalogRepository(EmptyCoroutineContext, { ServerReply.Ok(SERVER_BUILDINGS) }) { GEOJSON }
    }
}
