/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
@file:OptIn(TymczasoweRole::class)

package io.github.dostepnykampus.app

import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.IntSize
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
import io.github.dostepnykampus.feature.map.MapCamera
import io.github.dostepnykampus.feature.map.data.CampusMapData
import io.github.dostepnykampus.feature.map.navigation.BuildingRoute
import io.github.dostepnykampus.feature.map.navigation.MapRoute
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asinh
import kotlin.math.pow
import kotlin.math.tan

private const val UNIT = 0.0001
private const val WORLD_DP_AT_ZOOM_0 = 512.0
private const val FULL_TURN_DEGREES = 360.0
private val SQUARE = arrayOf(0 to 0, 4 to 0, 4 to 4, 0 to 4)

private val U_SHAPE = arrayOf(0 to 0, 40 to 0, 40 to 20, 30 to 20, 30 to 4, 10 to 4, 10 to 20, 0 to 20)

private fun building(symbol: String, longitude: Double, latitude: Double, corners: Array<Pair<Int, Int>>): String {
    val ring =
        (corners.toList() + corners.first()).joinToString(separator = ",", prefix = "[", postfix = "]") { (x, y) ->
            "[${longitude + x * UNIT},${latitude + y * UNIT}]"
        }
    val properties = """"kind":"building","symbols":["$symbol"],"name":"Budynek testowy","osm_id":"way/$symbol""""
    return """{"type":"Feature","properties":{$properties},"geometry":{"type":"Polygon","coordinates":[$ring]}}"""
}

private val BUILDINGS =
    listOf(
        building(symbol = "A-1", longitude = 17.05, latitude = 51.11, corners = SQUARE),
        building(symbol = "T-9", longitude = 17.065, latitude = 51.11, corners = SQUARE),
        building(symbol = "C-13", longitude = 17.05, latitude = 51.125, corners = SQUARE),
        building(symbol = "U-1", longitude = 17.05, latitude = 51.10, corners = U_SHAPE),
    ).joinToString(separator = ",")

private val CATALOG =
    """{"type":"FeatureCollection","attribution":"© OpenStreetMap contributors","features":[$BUILDINGS]}"""

private fun mercatorY(latitude: Double): Double = asinh(tan(Math.toRadians(latitude)))

/**
 * „Pokaż budynek na mapie” na V2 przez prawdziwe ekrany aplikacji do kamery, którą dostaje widok mapy (podróbka):
 * budynek jest widoczny, a dalsze przesuwanie i obrót zostają przy osobie (US2.9, NFR-22, NFR-53). Katalog
 * „Budynek testowy” jest syntetyczny (nie dane PWr).
 *
 * `U_SHAPE`: Litera U o szerokości 400 m: grube ramiona i cienka podstawa, więc środek obrysu leży poza obszarem
 * podpisu.
 *
 * `BUILDINGS`: A-1 w środku pierwszego widoku, T-9 i C-13 po ponad kilometrze od niego, U-1 w kształcie litery U.
 *
 * Jak natywna mapa po ruchu: bezczynność z otrzymaną kamerą, gdy test o to prosi.
 *
 * `labelPointOf`: Punkt podpisu budynku z danych, które dostała mapa (jeden `labelPoint` na kontur).
 *
 * `shows`: Czy [point] leży w widocznej części mapy: środek kamery stoi w środku części niezasłoniętej (Merkator).
 *
 * `showOnMap`: Od listy V1 przez V2 otwarte od razu (TalkBack) do „Pokaż budynek na mapie”; przed poleceniem budynku
 * nie widać.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class BuildingOnMapCameraTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val restoration = StateRestorationTester(compose)
    private val shown = mutableListOf<Pair<CampusMapData, MapCamera>>()
    private var touch: (List<String>) -> Unit = {}
    private var mapSize = IntSize.Zero
    private var density = 1f
    private var isIdleReported = false
    private val choice = TestMapListChoice()
    private val repository = BuildingCatalogRepository(ioContext = EmptyCoroutineContext) { CATALOG }
    private val fakeMap =
        CampusMapView { data: CampusMapData, _, camera, onCameraIdle, onBuildingsTouch, modifier: Modifier ->
            shown += data to camera
            touch = onBuildingsTouch
            density = LocalDensity.current.density
            SideEffect { if (isIdleReported) onCameraIdle(camera) }
            Box(modifier.onSizeChanged { mapSize = it })
        }

    @Before
    fun startOnMapWithTalkBack() {
        shadowOf(compose.activity.getSystemService(AccessibilityManager::class.java)).setTouchExplorationEnabled(true)
        restoration.setContent {
            DostepnyKampusTheme {
                AppShell(
                    firstLaunch = false,
                    roles = GuestRoleProvider,
                    onFirstHelpLeft = {},
                    onChooseMap = { choice.savedChoice.value = false },
                ) {
                    Screen(it)
                }
            }
        }
        compose.waitUntil { shown.isNotEmpty() && mapSize != IntSize.Zero }
    }

    @Composable
    private fun AppNavigator.Screen(route: NavKey) {
        when (route) {
            is MapRoute -> {
                val model = remember { inlineMapViewModel(repository, { flowOf(true) }, choice) }
                val state by model.uiState.collectAsStateWithLifecycle()
                MapScreen(focus = route.focus, state = state, onListChange = model::chooseList, mapView = fakeMap)
            }

            is BuildingRoute -> {
                val card = remember { BuildingCardViewModel(route.symbol, repository, { flowOf(true) }) }
                val state by card.uiState.collectAsStateWithLifecycle()
                BuildingCard(symbol = route.symbol, state = state, isOnline = true, onRetry = card::retry)
            }

            else -> {
                AppScreen(route)
            }
        }
    }

    private fun click(matcher: SemanticsMatcher) {
        compose.waitUntil { compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(matcher).performClick()
        compose.waitForIdle()
    }

    private fun pan(direction: String, times: Int) =
        repeat(times) {
            compose.onNodeWithContentDescription("Przesuń mapę w $direction").performClick()
            compose.waitForIdle()
        }

    private fun CampusMapData.labelPointOf(symbol: String): GeoPoint {
        val feature = labelPoints.toString().split("""{"type":"Feature"""").first { """"label":"$symbol"""" in it }
        val (longitude, latitude) = Regex("""\[(-?[\d.E-]+),(-?[\d.E-]+)""").find(feature)!!.destructured
        return GeoPoint(longitude = longitude.toDouble(), latitude = latitude.toDouble())
    }

    private fun MapCamera.shows(point: GeoPoint): Boolean {
        val world = WORLD_DP_AT_ZOOM_0 * 2.0.pow(zoom)
        val dx = (point.longitude - center.longitude) / FULL_TURN_DEGREES * world
        val dy = (mercatorY(point.latitude) - mercatorY(center.latitude)) / (2 * PI) * world
        return abs(dx) <= mapSize.width / density / 2 && abs(dy) <= (mapSize.height - bottomInset) / density / 2
    }

    private fun showOnMap(symbol: String) {
        val (data, before) = shown.last()
        assertFalse("$symbol widać już przed poleceniem", before.shows(data.labelPointOf(symbol)))
        val count = shown.size
        click(hasText("Lista budynków") and isSelectable())
        click(hasContentDescription("$symbol, ", substring = true))
        click(hasText(BUILDING_ON_MAP))
        compose.waitUntil { shown.size > count }
    }

    private fun assertShown(symbol: String) {
        val (data, camera) = shown.last()
        assertTrue("$symbol poza widokiem, kamera: $camera", camera.shows(data.labelPointOf(symbol)))
        assertTrue("powiększenie mniejsze niż startowe: $camera", camera.zoom >= shown.first().second.zoom)
    }

    /** Wiersz: K1, V2 „Pokaż budynek na mapie” → kamera mapy widzi budynek daleko od startu, US2.9, NFR-19, NFR-20. */
    @Test
    fun us2_9_far_building_in_view() {
        showOnMap("T-9")
        assertShown("T-9")
    }

    /** Wiersz: K2, mapa przesunięta strzałkami → „Pokaż budynek na mapie” dla A-1 wraca do budynku, US2.9, NFR-53. */
    @Test
    fun us2_9_a1_after_arrows() {
        pan("prawo", times = 4)
        showOnMap("A-1")
        assertShown("A-1")
    }

    /** Wiersz: K1, budynek w kształcie litery U → kamera widzi punkt podpisu, nie środek obrysu, US2.9. */
    @Test
    fun us2_9_u_shape_label_point() {
        showOnMap("U-1")
        assertShown("U-1")
    }

    /** Wiersz: K3 (strażnik), mała karta V2 i „Wstecz” → kamera sprzed karty, decyzja 83. */
    @Test
    fun us2_8_back_keeps_view() {
        pan("prawo", times = 2)
        val before = shown.last().second
        compose.runOnUiThread { touch(listOf("A-1")) }
        compose.waitForIdle()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        assertEquals(before, shown.last().second)
    }

    /** Wiersz: K4, po „Pokaż budynek na mapie” strzałka przesuwa mapę i fokus jej nie cofa, US2.9, NFR-53. */
    @Test
    fun us2_9_pan_after_show() {
        showOnMap("T-9")
        val before = shown.last().second
        pan("prawo", times = 1)
        val after = shown.last().second
        assertTrue("strzałka nie przesunęła mapy: $before → $after", after.center.longitude > before.center.longitude)
        assertEquals(before.zoom, after.zoom, 0.0)
    }

    /** Wiersz: K5, po „Pokaż budynek na mapie” obrót ekranu → ten sam obszar, nie start, NFR-22. */
    @Test
    fun nfr22_show_survives_rotation() {
        showOnMap("T-9")
        val before = shown.last().second
        restoration.emulateSavedInstanceStateRestore()
        compose.waitForIdle()
        assertShown("T-9")
        assertEquals(before, shown.last().second)
    }

    /** Wiersz: K6, drugie „Pokaż budynek na mapie” po przesunięciu mapy → kamera na drugim budynku, US2.9. */
    @Test
    fun us2_9_second_show_moves() {
        showOnMap("T-9")
        pan("prawo", times = 1)
        showOnMap("C-13")
        assertShown("C-13")
    }

    /** Wiersz: K6, to samo „Pokaż budynek na mapie” po przesunięciu mapy strzałkami → kamera znów na budynku, US2.9. */
    @Test
    fun us2_9_same_show_again() {
        showOnMap("T-9")
        pan("prawo", times = 4)
        showOnMap("T-9")
        assertShown("T-9")
    }

    /**
     * Wiersz: K3, po „Pokaż” i bezczynności kamery mała karta innego budynku i „Wstecz”
     * → widok jak przed kartą, decyzja 83.
     */
    @Test
    fun us2_8_back_after_show() {
        isIdleReported = true
        showOnMap("T-9")
        compose.waitForIdle()
        val before = shown.last().second
        compose.runOnUiThread { touch(listOf("A-1")) }
        compose.waitForIdle()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        assertShown("T-9")
        assertEquals(before, shown.last().second)
    }
}
