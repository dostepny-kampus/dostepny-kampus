/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.IntSize
import androidx.navigation3.runtime.NavKey
import io.github.dostepnykampus.app.navigation.AppNavigator
import io.github.dostepnykampus.app.navigation.AppScreen
import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.feature.map.camera.CampusMapView
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.campus.CampusBounds
import io.github.dostepnykampus.feature.map.campus.CampusMapData
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
    val properties =
        """"kind":"building","symbols":["$symbol"],"display_name":"Budynek testowy",""" +
            """"osm_id":"way/$symbol""""
    return """{"type":"Feature","properties":{$properties},"geometry":{"type":"Polygon","coordinates":[$ring]}}"""
}

private val BUILDINGS =
    listOf(
        building(symbol = "A-1", longitude = 17.05, latitude = 51.11, corners = SQUARE),
        building(symbol = "A-2", longitude = 17.0506, latitude = 51.11, corners = SQUARE),
        building(symbol = "T-9", longitude = 17.065, latitude = 51.11, corners = SQUARE),
        building(symbol = "C-13", longitude = 17.05, latitude = 51.125, corners = SQUARE),
        building(symbol = "U-1", longitude = 17.05, latitude = 51.10, corners = U_SHAPE),
    ).joinToString(separator = ",")

private val CATALOG =
    """{"type":"FeatureCollection","attribution":"© OpenStreetMap contributors","features":[$BUILDINGS]}"""

private fun mercatorY(latitude: Double): Double = asinh(tan(Math.toRadians(latitude)))

private fun centerNearestInside(ideal: Double, half: Double, area: CampusBounds): Double =
    ideal.coerceIn(minimumValue = mercatorY(area.south) + half, maximumValue = mercatorY(area.north) - half)

/**
 * „Pokaż budynek na mapie” z V2 przez prawdziwe ekrany aplikacji do kamery podróbki widoku mapy: budynek jest widoczny,
 * a dalsze przesuwanie i obrót zostają przy osobie. Katalog jest syntetyczny: A-1 w środku pierwszego widoku, T-9 i
 * C-13 ponad kilometr od niego, U-1 w kształcie litery U (`U_SHAPE`), którego środek obrysu leży poza obszarem podpisu.
 * A-2 stoi obok A-1 w tym samym terenie, więc prostokąt pierwszego terenu jest szerszy niż widok startowy.
 * `shows` sprawdza, czy punkt podpisu (`labelPointOf`) leży w niezasłoniętej części mapy (Merkator), a `showOnMap`
 * idzie od listy V1 przez V2 otwarte od razu (TalkBack).
 *
 * Wymagania: US2.9, NFR-22, NFR-53, US2.8
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
    private var bottomInset = 0.0
    private var topInset = 0.0
    private var density = 1f
    private var isIdleReported = false
    private val choice = TestMapListChoice()
    private val repository = BuildingCatalogRepository(ioContext = EmptyCoroutineContext) { CATALOG }
    private val fakeMap =
        CampusMapView { data: CampusMapData, _, move, inset, onCameraIdle, onBuildingsTouch, _, modifier: Modifier ->
            shown += data to move.camera
            bottomInset = inset.bottom
            topInset = inset.top
            touch = onBuildingsTouch
            density = LocalDensity.current.density
            SideEffect { if (isIdleReported) onCameraIdle(move.camera) }
            Box(modifier.onSizeChanged { mapSize = it })
        }

    @Before
    fun startOnMapWithTalkBack() {
        shadowOf(compose.activity.getSystemService(AccessibilityManager::class.java)).setTouchExplorationEnabled(true)
        restoration.setContent {
            DostepnyKampusTheme {
                AppShell(
                    firstLaunch = false,
                    roles = TestRoles(),
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
        MapOrBuildingScreen(
            route = route,
            repository = repository,
            choice = choice,
            mapView = fakeMap,
        ) { AppScreen(it) }
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
        click(hasText("Pokaż listę"))
        click(hasContentDescription("$symbol, ", substring = true) and hasClickAction())
        click(hasText(BUILDING_ON_MAP))
        compose.waitUntil { shown.size > count }
    }

    private fun assertShown(symbol: String) {
        val (data, camera) = shown.last()
        assertTrue("$symbol poza widokiem, kamera: $camera", camera.shows(data.labelPointOf(symbol)))
        assertTrue("powiększenie mniejsze niż startowe: $camera", camera.zoom >= shown.first().second.zoom)
    }

    /**
     * Wiersz: K1, V2 „Pokaż budynek na mapie” → kamera mapy widzi budynek daleko od startu.
     *
     * Wymagania: US2.9, NFR-19, NFR-20
     */
    @Test
    fun bringsFarBuildingIntoView() {
        showOnMap("T-9")
        assertShown("T-9")
    }

    /**
     * Wiersz: K2, mapa przesunięta strzałkami → „Pokaż budynek na mapie” dla A-1 wraca do budynku.
     *
     * Wymagania: US2.9, NFR-53
     */
    @Test
    fun showsA1AfterArrows() {
        pan("prawo", times = 4)
        showOnMap("A-1")
        assertShown("A-1")
    }

    /** Wiersz: K1, budynek w kształcie litery U → kamera widzi punkt podpisu, nie środek obrysu. Wymagania: US2.9 */
    @Test
    fun usesUShapeLabelPoint() {
        showOnMap("U-1")
        assertShown("U-1")
    }

    /** Wiersz: K3 (strażnik), mała karta V2 i „Wstecz” → mapa stoi na ekranie. Wymagania: US2.8, NFR-56 */
    @Test
    fun keepsViewOnBack() {
        pan("prawo", times = 2)
        compose.runOnUiThread { touch(listOf("A-1")) }
        compose.waitForIdle()
        val above = shown.last().second
        val under = bottomInset
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        assertStays(above = above, under = under)
    }

    private fun assertStays(above: MapCamera, under: Double) {
        val after = shown.last().second
        val perRadian = WORLD_DP_AT_ZOOM_0 * 2.0.pow(above.zoom) / (2 * PI) * density
        val shift = (mercatorY(above.center.latitude) - mercatorY(after.center.latitude)) * perRadian
        assertEquals("bez arkusza", 0.0, bottomInset, 0.0)
        assertEquals("powiększenie zostaje", above.zoom, after.zoom, 0.0)
        assertEquals("bez ruchu w poziomie", above.center.longitude, after.center.longitude, 1e-9)
        assertEquals("ekran stoi: środek niżej o pół paska i karty", (topInset + under) / 2, shift, 1.0)
    }

    /** Wiersz: K4, po „Pokaż budynek na mapie” strzałka przesuwa mapę i fokus jej nie cofa. Wymagania: US2.9, NFR-53 */
    @Test
    fun pansAfterShow() {
        showOnMap("T-9")
        val before = shown.last().second
        pan("prawo", times = 1)
        val after = shown.last().second
        assertTrue("strzałka nie przesunęła mapy: $before → $after", after.center.longitude > before.center.longitude)
        assertEquals(before.zoom, after.zoom, 0.0)
    }

    /** Wiersz: K5, po „Pokaż budynek na mapie” obrót ekranu → ten sam obszar, nie start. Wymagania: NFR-22 */
    @Test
    fun keepsShowOnRotation() {
        showOnMap("T-9")
        val before = shown.last().second
        restoration.emulateSavedInstanceStateRestore()
        compose.waitForIdle()
        assertShown("T-9")
        assertEquals(before, shown.last().second)
    }

    /** Wiersz: K6, drugie „Pokaż budynek na mapie” po przesunięciu mapy → kamera na drugim budynku. Wymagania: US2.9 */
    @Test
    fun movesOnSecondShow() {
        showOnMap("T-9")
        pan("prawo", times = 1)
        showOnMap("C-13")
        assertShown("C-13")
    }

    /**
     * Wiersz: K6, to samo „Pokaż budynek na mapie” po przesunięciu mapy strzałkami → kamera znów na budynku. Teren T-9
     * jest mały, więc mapa najpierw się przybliża, żeby strzałki miały dokąd ją przesunąć.
     *
     * Wymagania: US2.9
     */
    @Test
    fun showsSameAgain() {
        showOnMap("T-9")
        repeat(2) {
            compose.onNodeWithContentDescription("Powiększ").performClick()
            compose.waitForIdle()
        }
        pan("prawo", times = 4)
        showOnMap("T-9")
        assertShown("T-9")
    }

    /**
     * Wiersz: K3, po „Pokaż” i geście osoby na V1 mała karta budynku i „Wstecz” → mapa nie wraca, budynek
     * dalej widać, powiększenie zostaje. Widok mapy zgłasza kamerę tylko po geście osoby, więc przy otwartej karcie
     * atrapa nic nie zgłasza.
     *
     * Wymagania: US2.8, NFR-56
     */
    @Test
    fun goesBackAfterShow() {
        isIdleReported = true
        showOnMap("T-9")
        compose.waitForIdle()
        isIdleReported = false
        compose.runOnUiThread { touch(listOf("T-9")) }
        compose.waitForIdle()
        val above = shown.last().second
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        assertShown("T-9")
        assertEquals("powiększenie zostaje", above.zoom, shown.last().second.zoom, 0.0)
    }

    /**
     * Wiersz: K3, po „Pokaż” mała karta T-9 i „Wstecz” na małym terenie: widoczna część mapy pod paskiem wraca do
     * prostokąta terenu najmniejszym przesunięciem w pionie, powiększenie zostaje. Na terenie, który mieści widok,
     * mapa stoi (`keepsViewOnBack`).
     * Scenariusz: przegladanie-mapy.feature: Na małym terenie zamknięcie karty przesuwa mapę tylko do brzegu terenu
     *
     * Wymagania: US2.8, NFR-39, NFR-56
     */
    @Test
    fun keepsSmallAreaViewInBounds() {
        isIdleReported = true
        showOnMap("T-9")
        compose.waitForIdle()
        isIdleReported = false
        compose.runOnUiThread { touch(listOf("T-9")) }
        compose.waitForIdle()
        val above = shown.last().second
        val under = bottomInset
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        val (data, after) = shown.last()
        val area = data.areas.first { "T-9" in it.symbols }.bounds
        val perRadian = WORLD_DP_AT_ZOOM_0 * 2.0.pow(above.zoom) / (2 * PI) * density
        val ideal = mercatorY(above.center.latitude) - (topInset + under) / 2 / perRadian
        val half = (mapSize.height - topInset) / 2 / perRadian
        val expected = centerNearestInside(ideal = ideal, half = half, area = area)
        assertTrue("teren T-9 mieści widok, test nie sprawdza małego terenu", abs(expected - ideal) * perRadian > 1.0)
        assertEquals("bez arkusza", 0.0, bottomInset, 0.0)
        assertEquals("powiększenie zostaje", above.zoom, after.zoom, 0.0)
        assertEquals("bez ruchu w poziomie", above.center.longitude, after.center.longitude, 1e-9)
        assertEquals(
            "widok w granicy terenu, przesunięty najmniej",
            expected * perRadian,
            mercatorY(after.center.latitude) * perRadian,
            1.0,
        )
    }
}
