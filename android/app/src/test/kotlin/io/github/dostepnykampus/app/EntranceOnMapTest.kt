/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
@file:OptIn(TymczasoweRole::class)

package io.github.dostepnykampus.app

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import io.github.dostepnykampus.app.navigation.AppNavigator
import io.github.dostepnykampus.core.data.account.GuestRoleProvider
import io.github.dostepnykampus.core.data.account.TymczasoweRole
import io.github.dostepnykampus.core.data.buildings.BuildingCatalogRepository
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.feature.map.BuildingCardViewModel
import io.github.dostepnykampus.feature.map.CampusMapView
import io.github.dostepnykampus.feature.map.EntrancesScreen
import io.github.dostepnykampus.feature.map.MapCamera
import io.github.dostepnykampus.feature.map.data.CampusMapData
import io.github.dostepnykampus.feature.map.data.MapSelection
import io.github.dostepnykampus.feature.map.data.SelectedEntrance
import io.github.dostepnykampus.feature.map.navigation.BuildingRoute
import io.github.dostepnykampus.feature.map.navigation.EntrancesRoute
import io.github.dostepnykampus.feature.map.navigation.MapFocus
import io.github.dostepnykampus.feature.map.navigation.MapRoute
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.coroutines.EmptyCoroutineContext

private const val BUILDING =
    """{"type":"Feature","properties":{"kind":"building","symbols":["A-1"],"display_name":"Budynek testowy",""" +
        """"osm_id":"way/1"},"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,1],[0,0]]]}}"""

private const val NAMED_ID = "node/10"
private val NAMED_AT = GeoPoint(longitude = 0.2, latitude = 0.3)
private const val NAMED = "Wejście dostosowane"
private const val SELECTED_NAMED = "Zaznaczone wejście: »$NAMED«, budynek A-1"
private const val UNNAMED = "Wejście bez nazwy w danych"
private const val SHOW_ENTRANCE = "Pokaż wejście na mapie"

private fun entrance(id: Int, at: GeoPoint, tags: String): String =
    """{"type":"Feature","properties":{"kind":"entrance","symbols":["A-1"],"osm_id":"node/$id",$tags},""" +
        """"geometry":{"type":"Point","coordinates":[${at.longitude},${at.latitude}]}}"""

private fun catalog(vararg entrances: String): String =
    """{"type":"FeatureCollection","attribution":"© OpenStreetMap contributors","features":[""" +
        """$BUILDING,${entrances.joinToString(",")}]}"""

private val OTHERS =
    arrayOf(
        entrance(12, GeoPoint(longitude = 0.8, latitude = 0.2), """"entrance":"yes","wheelchair":"no""""),
        entrance(13, GeoPoint(longitude = 0.6, latitude = 0.9), """"entrance":"yes""""),
    )

/**
 * „Pokaż wejście na mapie” na V3 przez prawdziwy ekran V1 do tego, co dostaje widok mapy (podróbka): zaznaczone
 * wejście, pinezka z podpisem, kamera i nazwa dla TalkBack. Katalog
 * „Budynek testowy” jest syntetyczny (nie dane PWr).
 *
 * `showEntranceOnMap`: Od dotknięcia budynku na V1 przez V2 i V3 do „Pokaż wejście na mapie”; zwraca to, co dostała
 * mapa po tym.
 *
 * Wymagania: US3.3, US2.1, NFR-18
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class EntranceOnMapTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val shown = mutableListOf<Pair<CampusMapData, MapCamera>>()
    private val selections = mutableListOf<MapSelection>()
    private var touch: (List<String>) -> Unit = {}
    private val choice = TestMapListChoice()
    private val fakeMap =
        CampusMapView { data: CampusMapData, selection, camera, _, onBuildingsTouch, _, _: Modifier ->
            shown += data to camera
            selections += selection
            touch = onBuildingsTouch
        }

    @Composable
    private fun AppNavigator.Screen(route: NavKey, json: String) {
        val repository = remember { BuildingCatalogRepository(ioContext = EmptyCoroutineContext) { json } }
        when (route) {
            is MapRoute -> {
                val model = remember { inlineMapViewModel(repository, { flowOf(true) }, choice) }
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
                val card = remember { BuildingCardViewModel(route.symbol, repository, { flowOf(true) }) }
                val state by card.uiState.collectAsStateWithLifecycle()
                BuildingCard(symbol = route.symbol, state = state, isOnline = true, onRetry = card::retry)
            }

            is EntrancesRoute -> {
                EntrancesScreen(
                    symbol = route.symbol,
                    onShowEntrance = { showOnMap(MapFocus.Entrance(id = it, buildingSymbol = route.symbol)) },
                    onBack = { back() },
                    viewModel = remember { BuildingCardViewModel(route.symbol, repository, { flowOf(true) }) },
                )
            }

            else -> {
                AppScreen(route)
            }
        }
    }

    private fun show(json: String) {
        compose.setContent {
            DostepnyKampusTheme {
                AppShell(
                    firstLaunch = false,
                    roles = GuestRoleProvider,
                    onFirstHelpLeft = {},
                    onChooseMap = { choice.savedChoice.value = false },
                ) {
                    Screen(it, json)
                }
            }
        }
    }

    private fun read(text: String) = hasContentDescription(text) or hasText(text)

    private fun click(text: String) {
        compose.waitUntil { compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasText(text)).performClick()
        compose.waitForIdle()
    }

    private fun showEntranceOnMap(json: String): Pair<CampusMapData, MapCamera> {
        show(json)
        compose.waitUntil { shown.isNotEmpty() }
        compose.runOnUiThread { touch(listOf("A-1")) }
        click("Sprawdź, którędy wejść")
        click(SHOW_ENTRANCE)
        val v3 = hasText("Którędy wejść", substring = true)
        compose.waitUntil { compose.onAllNodes(v3).fetchSemanticsNodes().isEmpty() }
        return shown.last()
    }

    private fun namedCatalog(): String =
        catalog(entrance(10, NAMED_AT, """"entrance":"yes","ref":"$NAMED","wheelchair":"yes""""), *OTHERS)

    /** Wiersz E2 (O1, O2, O4): V3 → V1; mapa dostaje wybrane wejście z punktem i podpisem z nazwą. */
    @Test
    fun us3_3_map_gets_entrance() {
        showEntranceOnMap(namedCatalog())
        val expected = SelectedEntrance(id = NAMED_ID, location = NAMED_AT, label = NAMED, buildingSymbol = "A-1")
        assertEquals(expected, selections.last().entrance)
    }

    /** Wiersz E2, E4: wejście bez nazwy w danych dostaje podpis zastępczy, także dla TalkBack. */
    @Test
    fun us3_3_unnamed_gets_fallback() {
        val json = catalog(entrance(10, NAMED_AT, """"entrance":"yes","wheelchair":"yes""""), *OTHERS)
        showEntranceOnMap(json)
        val selected = selections.last().entrance
        assertEquals(UNNAMED, selected?.label)
        assertEquals(NAMED_AT, selected?.location)
        compose.onNode(read("Zaznaczone wejście: »$UNNAMED«, budynek A-1")).assertExists()
    }

    /** Wiersz E2 (O5): pozostałe wejścia, także niedostępne i bez danych, zostają w danych mapy. */
    @Test
    fun us3_3_other_entrances_stay() {
        val (data, _) = showEntranceOnMap(namedCatalog())
        assertEquals(NAMED_ID, selections.last().entrance?.id)
        assertEquals(3, Regex("\"marking\"").findAll(data.entrances.toString()).count())
    }

    /** Wiersz E3 (O3): kamera na wejściu, nie na budynku (środek budynku to 0,5; 0,5). */
    @Test
    fun us3_3_camera_on_entrance() {
        val (_, camera) = showEntranceOnMap(namedCatalog())
        assertEquals(NAMED_AT, camera.center)
    }

    /** Wiersz E4: TalkBack czyta nazwę zaznaczonego wejścia i budynek. */
    @Test
    fun nfr18_entrance_read_aloud() {
        showEntranceOnMap(namedCatalog())
        compose.onNode(read(SELECTED_NAMED)).assertExists()
    }

    /**
     * Wiersz E5 (O6): „Pokaż budynek na mapie” zaznacza budynek jak dziś, bez pinezki wejścia i bez zdania o wejściu.
     */
    @Test
    @Config(fontScale = 2f)
    fun nfr15_building_has_no_pin() {
        show(namedCatalog())
        compose.waitUntil { compose.onAllNodes(ListedA1).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(ListedA1).performClick()
        click(BUILDING_ON_MAP)
        compose.waitUntil { shown.isNotEmpty() }
        assertEquals("A-1", selections.last().building)
        assertNull(selections.last().entrance)
        compose.onNode(hasText("Zaznaczone wejście", substring = true)).assertDoesNotExist()
        compose.onNode(hasContentDescription("Zaznaczone wejście", substring = true)).assertDoesNotExist()
    }
}
