/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.github.dostepnykampus.app.navigation.isExpanded
import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.feature.map.camera.CampusMapView
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.coroutines.EmptyCoroutineContext

private const val OSM_START = "© OpenStreetMap"

private const val ONE_BUILDING_CATALOG =
    """{"type":"FeatureCollection","attribution":"© OpenStreetMap contributors","features":[""" +
        """{"type":"Feature","properties":{"kind":"building","symbols":["A-1"],""" +
        """"display_name":"Budynek testowy","osm_id":"way/1"},""" +
        """"geometry":{"type":"Polygon","coordinates":[[[0,0],[0.001,0],[0.001,0.001],[0,0.001],[0,0]]]}}]}"""

/**
 * Lista budynków V1 przy czcionce 200 % przez prawdziwe ekrany V1 i V2 do podróbki widoku mapy, na jednym
 * syntetycznym budynku A-1 (nie dane PWr).
 *
 * Wymagania: US2.9, NFR-19, NFR-20
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp", fontScale = 2f)
class ListAtLargeFontTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val choice = TestMapListChoice().apply { savedChoice.value = true }
    private val repository = BuildingCatalogRepository(ioContext = EmptyCoroutineContext) { ONE_BUILDING_CATALOG }
    private val selected = mutableListOf<String?>()
    private val fakeMap = CampusMapView { _, selection, _, _, _, _, _, _, _ -> selected += selection.building }

    /**
     * Budynek z listy otwiera od razu pełny opis bez mapy; „Pokaż budynek na mapie” prowadzi do mapy z zaznaczonym
     * budynkiem, a nie do listy, a aplikacja zapamiętuje widok mapy z „Pokaż listę” w wierszu tytułu.
     * Scenariusz: przegladanie-mapy.feature: Z listy przy dużej czcionce opis budynku prowadzi do mapy
     *
     * Wymagania: US2.9, NFR-19, NFR-20
     */
    @Test
    fun opensMapFromListAtLargeFont() {
        compose.setContent {
            DostepnyKampusTheme {
                AppShell(
                    firstLaunch = false,
                    roles = TestRoles(),
                    onFirstHelpLeft = {},
                    onChooseMap = { choice.savedChoice.value = false },
                ) { route ->
                    MapOrBuildingScreen(route = route, repository = repository, choice = choice, mapView = fakeMap) {}
                }
            }
        }
        compose.waitUntil(timeoutMillis = 5_000) { compose.onAllNodes(ListedA1).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(ListedA1).performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText(BUILDING_ON_MAP).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(sheetHandle(isExpanded = false)).assertDoesNotExist()
        compose.onNodeWithText(OSM_START, substring = true).assertDoesNotExist()
        compose.onNodeWithText(BUILDING_ON_MAP).performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals("zaznaczony budynek na mapie", "A-1", selected.lastOrNull())
        compose.onNodeWithText(OSM_START, substring = true).assertExists("mapa po „Pokaż budynek na mapie”")
        compose.onNode(ListedA1).assertDoesNotExist()
        assertEquals("zapamiętany widok mapy", false, choice.savedChoice.value)
        compose.onNode(hasText("Pokaż listę") and hasClickAction()).assertExists()
    }
}
