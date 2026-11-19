/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation3.runtime.NavKey
import io.github.dostepnykampus.app.navigation.AppNavigator
import io.github.dostepnykampus.app.navigation.AppScreen
import io.github.dostepnykampus.app.navigation.SignInRoute
import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.feature.map.camera.CampusMapView
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.campus.CampusMapData
import io.github.dostepnykampus.feature.map.campus.MapSelection
import io.github.dostepnykampus.feature.map.campus.SelectedEntrance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.coroutines.EmptyCoroutineContext

private const val BUILDING =
    """{"type":"Feature","properties":{"kind":"building","symbols":["A-1"],"display_name":"Budynek testowy",""" +
        """"osm_id":"way/1"},"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,1],[0,0]]]}}"""

private const val NAMED_ID = "node/10"
private val NAMED_AT = GeoPoint(longitude = 0.2, latitude = 0.3)
private const val NAMED = "Wejście dostosowane"
private const val SELECTED_NAMED = "Zaznaczone wejście: »$NAMED«, budynek A-1"
private const val UNNAMED = "Wejście"
private const val SHOW_ENTRANCE = "Pokaż wejście na mapie"
private const val SIDE = "Wejście boczne"
private val SIDE_AT = GeoPoint(longitude = 0.9, latitude = 0.5)
private const val SHOW_OTHER = "Pokaż to wejście na mapie"

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
 * „Pokaż wejście na mapie” na karcie V2 przez prawdziwy ekran V1 do tego, co dostaje widok mapy (podróbka):
 * zaznaczone wejście, pinezka z podpisem, kamera, arkusz wejścia i nazwa dla TalkBack. Katalog
 * „Budynek testowy” jest syntetyczny (nie dane PWr).
 *
 * `showEntranceOnMap`: Od dotknięcia budynku na V1 przez V2 do „Pokaż wejście na mapie”; zwraca to, co dostała
 * mapa po tym.
 *
 * Wymagania: US3.3, US2.1, US4.6, US14.2, NFR-18, NFR-15
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp")
class EntranceOnMapTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val shown = mutableListOf<Pair<CampusMapData, MapCamera>>()
    private val selections = mutableListOf<MapSelection>()
    private var touch: (List<String>) -> Unit = {}
    private val choice = TestMapListChoice()
    private lateinit var navigator: AppNavigator
    private val fakeMap =
        CampusMapView { data: CampusMapData, selection, move, _, _, onBuildingsTouch, _, _: Modifier ->
            shown += data to move.camera
            selections += selection
            touch = onBuildingsTouch
        }

    @Composable
    private fun AppNavigator.Screen(route: NavKey, json: String) {
        navigator = this
        val repository = remember { BuildingCatalogRepository(ioContext = EmptyCoroutineContext) { json } }
        MapOrBuildingScreen(
            route = route,
            repository = repository,
            choice = choice,
            mapView = fakeMap,
        ) { other ->
            if (other !is SignInRoute) {
                AppScreen(other)
            }
        }
    }

    private fun show(json: String) {
        compose.setContent {
            DostepnyKampusTheme {
                AppShell(
                    firstLaunch = false,
                    roles = TestRoles(),
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
        click(SHOW_ENTRANCE)
        return shown.last()
    }

    private fun namedCatalog(): String =
        catalog(entrance(10, NAMED_AT, """"entrance":"yes","ref":"$NAMED","wheelchair":"yes""""), *OTHERS)

    /** Wiersz E2 (O1, O2, O4): V2 → V1; mapa dostaje wybrane wejście z punktem i podpisem z nazwą. */
    @Test
    fun passesEntranceToMap() {
        showEntranceOnMap(namedCatalog())
        val expected = SelectedEntrance(id = NAMED_ID, location = NAMED_AT, label = NAMED, buildingSymbol = "A-1")
        assertEquals(expected, selections.last().entrance)
    }

    /**
     * Wiersz: V1, dotknięcie budynku → niska karta V2 z „Wejście dostępne” i zdaniem o wejściu zalecanym, czytanymi
     *   razem; to 1 dotknięcie od mapy.
     * Scenariusz: przegladanie-mapy.feature: Wejście dostępne najwyżej 3 dotknięcia od mapy
     *
     * Wymagania: US3.2, US3.1, BR-25
     */
    @Test
    fun answersAfterOneTap() {
        show(namedCatalog())
        compose.waitUntil { shown.isNotEmpty() }
        compose.runOnUiThread { touch(listOf("A-1")) }
        val answer = hasText("Wejście dostępne") and hasText("Zalecane: „$NAMED”.")
        compose.waitUntil { compose.onAllNodes(answer).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasText(SHOW_ENTRANCE)).assertExists("„Pokaż wejście na mapie” pod zdaniem")
    }

    /**
     * Wiersz: V1, dotknięcie budynku bez wejścia dostępnego → karta z „Brak wejścia dostępnego” i zdaniem, ile wejść
     *   nie jest dostępnych; bez „Pokaż wejście na mapie”.
     * Scenariusz: przegladanie-mapy.feature: Brak wejścia dostępnego jest powiedziany wprost
     *
     * Wymagania: US3.1, BR-25
     */
    @Test
    fun saysNoAccessibleAfterTap() {
        show(catalog(entrance(10, NAMED_AT, """"entrance":"yes","ref":"$NAMED","wheelchair":"no""""), OTHERS[0]))
        compose.waitUntil { shown.isNotEmpty() }
        compose.runOnUiThread { touch(listOf("A-1")) }
        val answer = hasText("Brak wejścia dostępnego") and hasText("Żadne z 2 wejść nie jest dostępne.")
        compose.waitUntil { compose.onAllNodes(answer).fetchSemanticsNodes().isNotEmpty() }
        val buttons = compose.onAllNodes(hasText(SHOW_ENTRANCE)).fetchSemanticsNodes()
        assertTrue("bez „Pokaż wejście na mapie”, a jest ${buttons.size}", buttons.isEmpty())
    }

    /** Wiersz: „Pokaż wejście na mapie” na V2 → V1 z otwartym arkuszem tego wejścia. Wymagania: US3.3, US3.2 */
    @Test
    fun opensEntranceSheet() {
        showEntranceOnMap(namedCatalog())
        val sheet = hasText("Wejście")
        compose.waitUntil { compose.onAllNodes(sheet).fetchSemanticsNodes().isNotEmpty() }
    }

    /**
     * Wiersz: arkusz wejścia zalecanego nie pokazuje innego wejścia dostępnego ani „Pokaż to wejście na mapie”;
     *   inne wejścia są na liście „Wejścia” karty.
     *
     * Wymagania: US3.3
     */
    @Test
    fun hidesOtherEntranceInSheet() {
        val main = entrance(10, NAMED_AT, """"entrance":"main","ref":"$NAMED","wheelchair":"yes"""")
        val side = entrance(11, SIDE_AT, """"entrance":"yes","ref":"$SIDE","wheelchair":"yes"""")
        showEntranceOnMap(catalog(main, side))
        compose.waitUntil { compose.onAllNodes(hasText("Wejście")).fetchSemanticsNodes().isNotEmpty() }

        assertEquals("node/10", selections.last().entrance?.id)
        compose.onNode(hasText(SIDE)).assertDoesNotExist()
        compose.onNode(hasContentDescription(SHOW_OTHER, substring = true)).assertDoesNotExist()
    }

    /**
     * Wiersz: arkusz wejścia, Gość → „Zgłoś barierę przy wejściu” prowadzi do L1, który pamięta wejście.
     * Scenariusz (częściowo): zgloszenie-szybkie.feature: Zgłoszenie przy wejściu ma miejsce w punkcie wybranego
     *   wejścia
     *
     * Wymagania: US4.6, US14.2
     */
    @Test
    fun sendsGuestFromSheetToSignIn() {
        showEntranceOnMap(namedCatalog())
        val report = hasContentDescription("Zgłoś barierę przy wejściu: »$NAMED«")
        compose.waitUntil { compose.onAllNodes(report).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(report).performScrollTo().performClick()
        compose.waitForIdle()

        assertEquals(SignInRoute(isForReport = true, entranceId = NAMED_ID), navigator.current)
    }

    /** Wiersz E2, E4: wejście bez nazwy w danych dostaje podpis zastępczy, także dla TalkBack. */
    @Test
    fun namesUnnamedEntrance() {
        val json = catalog(entrance(10, NAMED_AT, """"entrance":"yes","wheelchair":"yes""""), *OTHERS)
        showEntranceOnMap(json)
        val selected = selections.last().entrance
        assertEquals(UNNAMED, selected?.label)
        assertEquals(NAMED_AT, selected?.location)
        compose.onNode(read("Zaznaczone wejście: $UNNAMED, budynek A-1")).assertExists()
    }

    /** Wiersz E2 (O5): pozostałe wejścia, także niedostępne i bez danych, zostają w danych mapy. */
    @Test
    fun keepsOtherEntrances() {
        val (data, _) = showEntranceOnMap(namedCatalog())
        assertEquals(NAMED_ID, selections.last().entrance?.id)
        assertEquals(3, Regex("\"marking\"").findAll(data.entrances.toString()).count())
    }

    /** Wiersz E3 (O3): kamera na wejściu, nie na budynku (środek budynku to 0,5; 0,5). */
    @Test
    fun centersCameraOnEntrance() {
        val (_, camera) = showEntranceOnMap(namedCatalog())
        assertEquals(NAMED_AT, camera.center)
    }

    /** Wiersz E4: TalkBack czyta nazwę zaznaczonego wejścia i budynek. */
    @Test
    fun readsEntranceAloud() {
        showEntranceOnMap(namedCatalog())
        compose.onNode(read(SELECTED_NAMED)).assertExists()
    }

    /**
     * Wiersz E5 (O6): „Pokaż budynek na mapie” zaznacza budynek jak dziś, bez pinezki wejścia i bez zdania o wejściu.
     */
    @Test
    @Config(fontScale = 2f)
    fun drawsNoPinForBuilding() {
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
