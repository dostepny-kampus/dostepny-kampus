/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import io.github.dostepnykampus.core.data.map.MapListChoice
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.feature.map.camera.CameraMotion
import io.github.dostepnykampus.feature.map.camera.CameraTransition
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.camera.MapMotion
import io.github.dostepnykampus.feature.map.overlay.MAP_STATUS_BAR_TAG
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asinh
import kotlin.math.pow
import kotlin.math.tan

/** Widok mapy albo listy wybrany przez osobę, w pamięci testu; na start osoba nic nie wybrała. */
internal class TestMapListChoice : MapListChoice {
    override val savedChoice = MutableStateFlow<Boolean?>(null)

    override suspend fun choose(isList: Boolean) {
        savedChoice.value = isList
    }
}

internal const val KEYBOARD_HEIGHT = 600

/** Podaje ekranowi [activity] klawiaturę otwartą na [height] pikseli albo zamkniętą, jak system: w odstępach okna. */
internal fun dispatchKeyboard(activity: ComponentActivity, isOpen: Boolean, height: Int = KEYBOARD_HEIGHT) {
    val ime = WindowInsetsCompat.Type.ime()
    val insets =
        WindowInsetsCompat
            .Builder()
            .setInsets(ime, Insets.of(0, 0, 0, if (isOpen) height else 0))
            .setVisible(ime, isOpen)
            .build()
    val content = activity.findViewById<ViewGroup>(android.R.id.content)
    ViewCompat.dispatchApplyWindowInsets(content.getChildAt(0), insets)
}

private const val STATUS_BAR_HEIGHT = 96

/** Podaje ekranowi [activity] systemowy pasek stanu o wysokości [STATUS_BAR_HEIGHT] pikseli w odstępach okna. */
internal fun dispatchStatusBar(activity: ComponentActivity) {
    val bars = WindowInsetsCompat.Type.statusBars()
    val insets = WindowInsetsCompat.Builder().setInsets(bars, Insets.of(0, STATUS_BAR_HEIGHT, 0, 0)).build()
    val content = activity.findViewById<ViewGroup>(android.R.id.content)
    ViewCompat.dispatchApplyWindowInsets(content.getChildAt(0), insets)
}

private const val WORLD_DP_AT_ZOOM_0 = 512.0
private const val FULL_TURN_DEGREES = 360.0

private val SMALL_OUTLINE_CORNERS =
    listOf(
        GeoPoint(longitude = 0.0, latitude = 0.0),
        GeoPoint(longitude = 0.001, latitude = 0.0),
        GeoPoint(longitude = 0.001, latitude = 0.001),
        GeoPoint(longitude = 0.0, latitude = 0.001),
    )

private const val C14_ENTRANCE_ON_EAST_EDGE =
    """,{"type":"Feature","properties":{"kind":"entrance","symbols":["C-14"],"osm_id":"node/5","entrance":"yes"},""" +
        """"geometry":{"type":"Point","coordinates":[0.001,0.0005]}}"""

private fun squareBuilding(symbol: String, osmId: Int, from: Double, to: Double): String {
    val ring = "[[$from,$from],[$to,$from],[$to,$to],[$from,$to],[$from,$from]]"
    return """,{"type":"Feature","properties":{"kind":"building","symbols":["$symbol"],"osm_id":"way/$osmId"},""" +
        """"geometry":{"type":"Polygon","coordinates":[$ring]}}"""
}

private val FAR_BUILDINGS_AROUND_C14 =
    squareBuilding(symbol = "X-1", osmId = 2, from = -0.004, to = -0.0035) +
        squareBuilding(symbol = "X-2", osmId = 3, from = 0.0045, to = 0.005)

private val MAP_BUTTONS_ABOVE_CARD =
    listOf(
        "Powiększ",
        "Pomniejsz",
        "Przesuń mapę w górę",
        "Przesuń mapę w dół",
        "Przesuń mapę w lewo",
        "Przesuń mapę w prawo",
    )

private fun mercator(latitude: Double): Double = asinh(tan(Math.toRadians(latitude)))

/**
 * Ekran V1: mapa, karta, przyciski i pasek nad mapą; przygotowanie w [CampusMapScreenFixture].
 *
 * Wymagania: US2, US1
 */
@RunWith(RobolectricTestRunner::class)
class CampusMapScreenTest : CampusMapScreenFixture() {
    /** Plakietka jest niska jak w hi-fi, ale odnośnik ma cel dotyku 48 dp. Wymagania: NFR-17 */
    @Test
    fun showsButtonsAndOsm() {
        compose.setContent { Screen() }
        compose.onNodeWithContentDescription("Powiększ").performClick()
        compose.onNodeWithContentDescription("Przesuń mapę w górę").performClick()
        compose.waitForIdle()
        val (start, zoomed, moved) = cameras.distinct()
        assertEquals(listOf(start.zoom + 1, start.center), listOf(zoomed.zoom, zoomed.center))
        assertEquals(true, moved.center.latitude > zoomed.center.latitude)
        val names = listOf("Pomniejsz", "Przesuń mapę w dół", "Przesuń mapę w lewo", "Przesuń mapę w prawo")
        names.forEach { compose.onNodeWithContentDescription(it).assertExists() }
        compose.onNodeWithText("© OpenStreetMap contributors").assertHasClickAction().assertHeightIsAtLeast(48.dp)
        compose.runOnUiThread { listOf(listOf("C-3", "C-4"), listOf("C-13")).forEach(touch) }
        assertEquals(listOf("C-13"), opened)
    }

    /**
     * Wiersz: V1, strzałki przesuwania zatrzymują się bez komunikatu, gdy brzeg widoku dojdzie do brzegu prostokąta
     * terenu, czyli konturów (teren spoza pliku granic) powiększonych o 100 m: środek zostaje wewnątrz prostokąta,
     * a następna strzałka już nie przesuwa mapy.
     *
     * Wymagania: NFR-39, NFR-53
     */
    @Test
    fun stopsArrowsAtEdge() {
        compose.setContent { Screen() }
        repeat(times = 5) { compose.onNodeWithContentDescription("Pomniejsz").performClick() }
        repeat(times = 40) { compose.onNodeWithContentDescription("Przesuń mapę w prawo").performClick() }
        compose.waitForIdle()
        val edge = cameras.last()
        assertTrue("środek ${edge.center} w prostokącie", edge.center.longitude in 0.5..1.0 + 100.0 / 111_195.0)
        compose.onNodeWithContentDescription("Przesuń mapę w prawo").performClick()
        compose.waitForIdle()
        assertEquals("następna strzałka nie przesuwa mapy", edge, cameras.last())
    }

    /**
     * Wiersz: V1, najmniejsze powiększenie pokazuje cały obszar kampusu; bardziej oddalić mapy się nie da, więc
     * „Pomniejsz” przy małym konturze zatrzymuje się wyżej niż dawne powiększenie 12.
     *
     * Wymagania: NFR-39, NFR-53
     */
    @Test
    fun stopsZoomOutAtCampus() {
        ring = "[[0,0],[0.001,0],[0.001,0.001],[0,0]]"
        compose.setContent { Screen() }
        repeat(times = 20) { compose.onNodeWithContentDescription("Pomniejsz").performClick() }
        compose.waitForIdle()
        val farthest = cameras.last()
        assertTrue("powiększenie ${farthest.zoom}", farthest.zoom > 12.0)
        compose.onNodeWithContentDescription("Pomniejsz").performClick()
        compose.waitForIdle()
        assertEquals(farthest, cameras.last())
    }

    /**
     * Wiersz: czcionka 1,5 → start na liście z „Pokaż mapę” pod tytułem; wybór mapy zostaje zapisany, a mapa ma
     * krótki podpis „© OpenStreetMap”.
     *
     * Wymagania: NFR-19, NFR-20, NFR-37
     */
    @Test
    fun shortensOsmOnList() {
        compose.setContent { Screen(fontScale = 1.5f) }
        compose.onNode(showMap).performClick()
        compose.onNode(showList).assertExists()
        compose.onNodeWithText("© OpenStreetMap").assertExists()
        assertEquals(false, choice.savedChoice.value)
    }

    /**
     * Wiersz: zapisany widok listy → V1 startuje na liście także przy zwykłej czcionce.
     *
     * Wymagania: NFR-19
     */
    @Test
    fun keepsListChoice() {
        choice.savedChoice.value = true
        compose.setContent { Screen() }
        compose.onNode(showMap).assertExists()
        compose.onNodeWithContentDescription("Powiększ").assertDoesNotExist()
    }

    /** Wiersz: kontur wspólny „C-13/C-14” → arkusz „Który budynek?”, przycisk budynku → V2. */
    @Test
    fun opensCardFromChoice() {
        compose.setContent { Screen() }
        compose.waitForIdle()
        compose.runOnUiThread { touch(listOf("C-13", "C-14")) }
        compose.onNodeWithText("Który budynek?").assertExists()
        val action =
            SemanticsMatcher("otwiera opis budynku") {
                it.config.getOrNull(SemanticsActions.OnClick)?.label == "otwiera opis budynku"
            }
        compose.onNodeWithContentDescription("C-14, Budynek testowy").assert(action).performClick()
        compose.onNodeWithText("Który budynek?").assertDoesNotExist()
        assertEquals(listOf("C-14"), opened)
    }

    /** Wiersz: „Zamknij” w arkuszu „Który budynek?” → V1 bez arkusza. */
    @Test
    fun closesBuildingChoice() {
        compose.setContent { Screen() }
        compose.waitForIdle()
        compose.runOnUiThread { touch(listOf("C-13", "C-14")) }
        compose.onNodeWithText("Budynek C-13", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Zamknij").performClick()
        compose.onNodeWithText("Który budynek?").assertDoesNotExist()
        assertEquals(emptyList<String>(), opened)
    }

    /**
     * Wiersz: arkusz „Który budynek?” zamyka tylko przycisk budynku albo „Zamknij”; dotknięcie pustej mapy go
     * zostawia.
     */
    @Test
    fun closesChoiceOnEmptyTap() {
        compose.setContent { Screen() }
        compose.waitForIdle()
        compose.runOnUiThread { touch(listOf("C-13", "C-14")) }
        compose.runOnUiThread { touch(emptyList()) }
        compose.onNodeWithText("Który budynek?").assertExists("arkusz po dotknięciu pustej mapy")
    }

    /**
     * Wiersz: otwarta klawiatura chowa przyciski mapy, podpis OSM zostaje; po zamknięciu przyciski wracają.
     *
     * Wymagania: NFR-53
     */
    @Test
    fun hidesButtonsForKeyboard() {
        compose.setContent { Screen() }
        keyboard(isOpen = true)
        compose.onNodeWithContentDescription("Powiększ").assertDoesNotExist()
        compose.onNodeWithContentDescription("Przesuń mapę w górę").assertDoesNotExist()
        compose.onNodeWithText("© OpenStreetMap contributors").assertExists()
        keyboard(isOpen = false)
        compose.onNodeWithContentDescription("Powiększ").assertExists()
        compose.onNodeWithContentDescription("Przesuń mapę w górę").assertExists()
    }

    /**
     * Scenariusz „Przy karcie budynek jest cały widoczny, a mapą sterują przyciski”: podpis OSM i przyciski
     * mapy stoją nad arkuszem V2, a nagłówka, „Legendy” i „ⓘ” nad nim nie ma.
     *
     * Wymagania: US2.8, NFR-37, NFR-53
     */
    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun keepsOsmAboveSheet() {
        compose.setContent { Screen(sheet = 400.dp) }
        val bottom = compose.onRoot().getBoundsInRoot().bottom
        val osm = compose.onNodeWithText("© OpenStreetMap contributors").getBoundsInRoot()
        assertEquals(true, osm.bottom <= bottom - 400.dp)
        val down = compose.onNodeWithContentDescription("Pomniejsz").getBoundsInRoot()
        assertEquals(true, down.bottom <= bottom - 400.dp)
        compose.onNodeWithContentDescription("Przesuń mapę w górę").assertExists()
        compose.onNodeWithText("Mapa kampusu").assertDoesNotExist()
        compose.onNodeWithContentDescription("Legenda i źródła mapy").assertDoesNotExist()
    }

    /**
     * Mała karta V2 nad V1 przy zwykłej czcionce: cały kontur budynku i jego wejście mieszczą się w części mapy nad
     * kartą, nad kartą są „Powiększ”, „Pomniejsz” i strzałki przesuwania, „Pomniejsz” oddala mapę bez zamykania
     * karty, a pola symbolu i „Pokaż listę” nad kartą nie ma.
     * Scenariusz: przegladanie-mapy.feature: Przy karcie budynek jest cały widoczny, a mapą sterują przyciski
     *
     * Wymagania: US2.8, US1.4, NFR-53
     */
    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun showsWholeBuildingAboveCard() {
        ring = "[[0,0],[0.001,0],[0.001,0.001],[0,0.001],[0,0]]"
        entrances = C14_ENTRANCE_ON_EAST_EDGE + FAR_BUILDINGS_AROUND_C14
        compose.setContent { Screen(focus = "C-14", sheet = 400.dp) }
        compose.waitForIdle()
        val root = compose.onRoot().getBoundsInRoot()
        val cardTop = root.bottom - 400.dp
        val above = cameras.last()
        val covered = with(compose.density) { (insets.last() + tops.last()).toFloat().toDp() }
        val visible = (root.bottom - root.top - covered).value
        val points = SMALL_OUTLINE_CORNERS + GeoPoint(longitude = 0.001, latitude = 0.0005)
        points.forEach { point ->
            val isShown = above.shows(point = point, widthDp = root.right.value, heightDp = visible)
            assertTrue("$point nad kartą, kamera $above", isShown)
        }
        MAP_BUTTONS_ABOVE_CARD.forEach { name ->
            val button = compose.onNodeWithContentDescription(name).getBoundsInRoot()
            assertTrue("$name nad kartą", button.bottom <= cardTop)
        }
        compose.onNodeWithContentDescription("Pomniejsz").performClick()
        compose.waitForIdle()
        assertTrue("mapa się oddala: ${cameras.last()}", cameras.last().zoom < above.zoom)
        assertEquals("mała karta zostaje otwarta", "C-14" to 0, focuses.last() to emptyTaps)
        compose.onNodeWithText("Wpisz symbol budynku, np. C-13").assertDoesNotExist()
        compose.onNode(showList).assertDoesNotExist()
    }

    private fun MapCamera.shows(point: GeoPoint, widthDp: Float, heightDp: Float): Boolean {
        val world = WORLD_DP_AT_ZOOM_0 * 2.0.pow(zoom)
        val dx = (point.longitude - center.longitude) / FULL_TURN_DEGREES * world
        val dy = (mercator(point.latitude) - mercator(center.latitude)) / (2 * PI) * world
        return abs(dx) <= widthDp / 2 && abs(dy) <= heightDp / 2
    }

    /**
     * Scenariusz „Przy niskiej mapie nad kartą nie ma przycisków mapy”: poniżej 300 dp wolnej mapy przycisków
     * nie ma, a po zamknięciu karty wracają.
     *
     * Wymagania: US2.8, NFR-53
     */
    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun hidesButtonsOnLowMap() {
        var sheet by mutableStateOf(560.dp)
        compose.setContent { Screen(focus = "C-13", sheet = sheet) }
        compose.onNodeWithContentDescription("Pomniejsz").assertDoesNotExist()
        compose.onNodeWithContentDescription("Przesuń mapę w górę").assertDoesNotExist()
        compose.onNodeWithText("© OpenStreetMap contributors").assertExists()
        sheet = 0.dp
        compose.onNodeWithContentDescription("Pomniejsz").assertExists()
    }

    /**
     * Mapa pod arkuszem nie ma nagłówka i sięga pod systemowy pasek stanu: pasek dostaje tło o swojej wysokości,
     * więc zegar i ikony nie leżą na podkładzie mapy. Z nagłówkiem V1 tła nie ma.
     *
     * Wymagania: NFR-16
     */
    @Test
    fun paintsStatusBar() {
        var sheet by mutableStateOf(200.dp)
        compose.setContent { Screen(focus = "C-13", sheet = sheet) }
        compose.runOnUiThread { dispatchStatusBar(compose.activity) }
        compose.waitForIdle()
        val bar = compose.onNodeWithTag(MAP_STATUS_BAR_TAG).getBoundsInRoot()
        val expected = with(compose.density) { STATUS_BAR_HEIGHT.toDp() }
        assertEquals(0.dp to expected, bar.top to bar.bottom)
        sheet = 0.dp
        compose.onNodeWithTag(MAP_STATUS_BAR_TAG).assertDoesNotExist()
    }

    /**
     * Scenariusz „Przy karcie budynek jest cały widoczny, a mapą sterują przyciski”: mapa pod arkuszem sięga
     * pod pasek stanu, ale przyciski mapy stoją pod jego tłem.
     *
     * Wymagania: US2.8, NFR-16, NFR-53
     */
    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun placesButtonsBelowStatusBar() {
        compose.setContent { Screen(focus = "C-13", sheet = 400.dp) }
        compose.runOnUiThread { dispatchStatusBar(compose.activity) }
        compose.waitForIdle()
        val bar = compose.onNodeWithTag(MAP_STATUS_BAR_TAG).getBoundsInRoot()
        val up = compose.onNodeWithContentDescription("Przesuń mapę w górę").getBoundsInRoot()
        assertEquals(true, bar.bottom > 0.dp && up.top >= bar.bottom)
    }

    /**
     * Wiersz: mała karta V2 nad V1, dotknięcie pustej mapy → zamknięcie karty; dotknięcie konturu, także wspólnego,
     * zamknięciem nie jest.
     *
     * Wymagania: US2.8, NFR-17
     */
    @Test
    fun handlesEmptyMapTap() {
        compose.setContent { Screen(focus = "C-13", sheet = 200.dp) }
        compose.waitForIdle()
        compose.runOnUiThread { listOf(listOf("C-13"), listOf("C-13", "C-14")).forEach(touch) }
        assertEquals("dotknięcia konturów", 0 to listOf("C-13"), emptyTaps to opened)
        compose.runOnUiThread { touch(emptyList()) }
        assertEquals("dotknięcie pustej mapy", 1, emptyTaps)
    }

    /**
     * Scenariusz „Przy karcie budynek jest cały widoczny, a mapą sterują przyciski”: V1 → V2 z budynkiem
     * nad arkuszem, „Wstecz” bez ruszania mapy → widok sprzed otwarcia V2; po „Powiększ” nad kartą zostaje
     * widok osoby (151 i 153).
     *
     * Wymagania: US2.8, NFR-53
     */
    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun keepsFocusAboveSheet() {
        var sheet by mutableStateOf(0.dp)
        compose.setContent { Screen(focus = "C-14", sheet = sheet) }
        compose.waitForIdle()
        val start = cameras.last()
        sheet = 400.dp
        compose.waitForIdle()
        val aboveSheet = cameras.last()
        assertEquals(true, insets.last() > 0)
        assertEquals(true, aboveSheet != start)
        sheet = 0.dp
        compose.waitForIdle()
        assertEquals(start to 0.0, cameras.last() to insets.last())
        sheet = 400.dp
        compose.onNodeWithContentDescription("Powiększ").performClick()
        compose.waitForIdle()
        val closer = aboveSheet.copy(zoom = aboveSheet.zoom + 1)
        assertEquals(CameraTransition(closer, CameraMotion.FLIGHT, durationMillis = 300), moves.last())
        sheet = 0.dp
        compose.waitForIdle()
        assertEquals(closer to 0.0, cameras.last() to insets.last())
    }

    /**
     * Wiersz: nad małą kartą V2 osoba ruszyła mapę, potem arkusz zmienia wysokość → ruch kamery ten sam, zmienia
     * się tylko zasłonięty dół mapy, więc mapa nie dostaje nowego celu.
     *
     * Wymagania: US2.8, NFR-56
     */
    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun movesOnlyInsetWithSheetHeight() {
        var sheet by mutableStateOf(400.dp)
        compose.setContent { Screen(focus = "C-14", sheet = sheet) }
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Pomniejsz").performClick()
        compose.waitForIdle()
        val moved = moves.last()
        val shownInsets =
            listOf(300.dp, 350.dp, 320.dp).map { height ->
                sheet = height
                compose.waitForIdle()
                assertEquals(moved, moves.last())
                insets.last()
            }
        assertEquals(3, shownInsets.distinct().size)
    }

    /**
     * Wiersz: górny pasek leży nad mapą i odjeżdża w górę razem z ruchem mapy do budynku nad małą kartą: w połowie
     * ruchu jest w drodze, po ruchu go nie ma. Bez karty kamera ma zasłoniętą górę pod paskiem, z kartą nie.
     *
     * Wymagania: US2.8, NFR-56
     */
    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun slidesBarWithMapMove() {
        var sheet by mutableStateOf(0.dp)
        val motion = MapMotion()
        compose.setContent { Screen(focus = "C-14", sheet = sheet, motion = motion) }
        compose.waitForIdle()
        assertTrue("kamera bez karty pod paskiem: ${tops.last()}", tops.last() > 0.0)
        val rest = barTop()
        compose.mainClock.autoAdvance = false
        sheet = 400.dp
        Snapshot.sendApplyNotifications()
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeByFrame()
        val pick = moves.last()
        compose.mainClock.advanceTimeBy(pick.durationMillis / 2)
        val midway = barTop()
        assertTrue("w połowie ruchu pasek odjeżdża: $midway < $rest", midway < rest)
        compose.mainClock.advanceTimeBy(pick.durationMillis)
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        compose.onNodeWithText("Mapa kampusu").assertDoesNotExist()
        assertEquals("z kartą góra mapy odkryta", 0.0, tops.last(), 0.0)
    }

    /**
     * Wiersz: karta V2 zasłoniła mapę, a mapa jeszcze nie ruszyła: górny pasek stoi na miejscu i znika dopiero po
     * odjeździe w całości.
     *
     * Wymagania: US2.8, NFR-56
     */
    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun keepsBarUntilItLeaves() {
        var sheet by mutableStateOf(0.dp)
        compose.setContent { Screen(sheet = sheet, motion = MapMotion()) }
        compose.waitForIdle()
        val rest = barTop()
        compose.mainClock.autoAdvance = false
        sheet = 400.dp
        Snapshot.sendApplyNotifications()
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeByFrame()
        assertEquals("pasek czeka na ruch mapy", rest, barTop(), 0.5f)
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        compose.onNodeWithText("Mapa kampusu").assertDoesNotExist()
    }

    /**
     * Wiersz: klawiatura otwiera się albo zamyka w animacji; kolejne wysokości otwartej klawiatury nie przeliczają
     * mapy.
     *
     * Wymagania: NFR-56
     */
    @Test
    fun keepsMapWhileKeyboardMoves() {
        compose.setContent { Screen() }
        keyboard(isOpen = true)
        val shown = moves.size
        listOf(500, 400, 300).forEach { height ->
            compose.runOnUiThread { dispatchKeyboard(compose.activity, isOpen = true, height = height) }
            compose.waitForIdle()
        }
        assertEquals("mapa bez przeliczenia w trakcie animacji klawiatury", shown, moves.size)
    }

    /**
     * Wiersz: „Zamknij” na karcie V2 nad mapą daje jeden cel ruchu mapy, od razu pod górnym paskiem.
     *
     * Wymagania: US2.8, NFR-56
     */
    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun closesCardWithOneMove() {
        var sheet by mutableStateOf(0.dp)
        var focus by mutableStateOf<String?>("C-14")
        compose.setContent { Screen(focus = focus, sheet = sheet, motion = MapMotion()) }
        compose.waitForIdle()
        val bar = tops.last()
        sheet = 400.dp
        compose.waitForIdle()
        val before = moves.size
        focus = null
        sheet = 0.dp
        compose.waitForIdle()
        val targets = moves.subList(fromIndex = before, toIndex = moves.size).map { it.camera }.toSet()
        val shownTops = tops.subList(fromIndex = before, toIndex = tops.size).toSet()
        assertEquals("jeden cel po zamknięciu: $targets, góra $shownTops, pasek $bar", 1, targets.size)
        assertEquals("góra pod paskiem od pierwszej klatki: $shownTops", setOf(bar), shownTops)
    }

    private fun barTop(): Float {
        val bar = compose.onNodeWithText("Mapa kampusu").getUnclippedBoundsInRoot()
        return bar.top.value
    }

    /**
     * Wiersz: wybór budynku nad małą kartą V2 daje jeden ruch mapy; osiadanie arkusza (trzy wysokości) nie zmienia
     * celu, rodzaju ani czasu ruchu, tylko zasłonięty dół mapy.
     *
     * Wymagania: US2.8, NFR-56
     */
    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun movesOncePerPick() {
        var sheet by mutableStateOf(0.dp)
        val motion = MapMotion()
        compose.setContent { Screen(focus = "C-14", sheet = sheet, motion = motion) }
        compose.waitForIdle()
        sheet = 400.dp
        compose.waitForIdle()
        val pick = moves.last()
        listOf(300.dp, 350.dp, 320.dp).forEach { height ->
            sheet = height
            compose.waitForIdle()
            assertEquals(pick, moves.last())
        }
        assertEquals(true, pick.motion != CameraMotion.NONE)
        assertEquals("jeden zapis ruchu dla karty", 1, motion.moves)
        assertEquals("karta dostaje czas ruchu mapy", pick.durationMillis, motion.durationMillis)
    }

    /**
     * Wiersz: dotknięcie konturu na mapie → mała karta, a mapa nie przybliża się do budynku; rusza się najwyżej tyle,
     * żeby zasłonięty kontur był widoczny.
     *
     * Wymagania: US2.8, US3.2, NFR-56
     */
    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun tapsWithoutZoom() {
        ring = "[[0,0],[0.0003,0],[0.0003,0.0003],[0,0.0003],[0,0]]"
        entrances = FAR_BUILDINGS_AROUND_C14
        var sheet by mutableStateOf(0.dp)
        var focus by mutableStateOf<String?>(null)
        compose.setContent { Screen(focus = focus, sheet = sheet, motion = MapMotion()) }
        compose.waitForIdle()
        val start = cameras.last()
        compose.runOnUiThread { touch(listOf("C-14")) }
        focus = "C-14"
        sheet = 400.dp
        compose.waitForIdle()
        assertEquals("dotknięty budynek otwiera kartę", listOf("C-14"), opened)
        assertEquals("powiększenie bez zmian: $moves", start.zoom, moves.last().camera.zoom, 0.0)
    }

    /**
     * Wiersz: V1, dotknięty budynek cały widoczny nad kartą → mapa stoi na ekranie, choć pasek nad mapą chowa się
     * razem z otwarciem karty: środek widoku idzie wyżej o połowę paska i karty.
     *
     * Wymagania: US2.8, NFR-56
     */
    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun keepsTappedBuildingOnScreen() {
        ring = "[[0,0],[0.0003,0],[0.0003,0.0003],[0,0.0003],[0,0]]"
        entrances = FAR_BUILDINGS_AROUND_C14 + squareBuilding(symbol = "X-3", osmId = 4, from = 0.0025, to = 0.003)
        var sheet by mutableStateOf(0.dp)
        var focus by mutableStateOf<String?>(null)
        compose.setContent { Screen(focus = focus, sheet = sheet, motion = MapMotion()) }
        compose.waitForIdle()
        val start = cameras.last()
        val bar = tops.last()
        compose.runOnUiThread { touch(listOf("C-14")) }
        focus = "C-14"
        compose.waitForIdle()
        sheet = 200.dp
        compose.waitForIdle()
        val end = moves.last()
        val pixelsPerRadian = WORLD_DP_AT_ZOOM_0 * 2.0.pow(start.zoom) / (2 * PI) * compose.density.density
        val shift = (mercator(end.camera.center.latitude) - mercator(start.center.latitude)) * pixelsPerRadian
        assertTrue("pasek nad mapą przed dotknięciem: $tops", bar > 0)
        assertEquals("widoczny budynek: mapa bez ruchu, $moves", CameraMotion.NONE, end.motion)
        assertEquals("ekran stoi: środek wyżej o pół paska i karty", (bar + insets.last()) / 2, shift, 1.0)
    }

    /**
     * Wiersz: budynek wybrany na liście budynków → mapa powstaje razem z małą kartą i od razu stoi na budynku, bez
     * ruchu kamery i bez awarii przed pomiarem mapy; karta wjeżdża w 300 ms.
     *
     * Wymagania: US2.8, NFR-19, NFR-56
     */
    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun movesToListPick() {
        choice.savedChoice.value = true
        var sheet by mutableStateOf(0.dp)
        var focus by mutableStateOf<String?>(null)
        val motion = MapMotion()
        compose.setContent { Screen(focus = focus, sheet = sheet, motion = motion) }
        compose.waitForIdle()
        focus = "C-14"
        sheet = 400.dp
        compose.waitForIdle()
        assertEquals("jeden zapis widoku budynku z listy", 1, motion.moves)
        assertEquals("z listy bez ruchu kamery: $moves", CameraMotion.NONE, moves.last().motion)
        assertEquals("karta wjeżdża w 300 ms", 300L, motion.durationMillis)
    }

    /**
     * Wiersz: V1, „Pokaż listę” → lista z katalogu bez mapy; budynek z listy → V2 jako arkusz na mapie,
     * a po „Wstecz” znów lista.
     *
     * Wymagania: NFR-19, US2.5
     */
    @Test
    fun opensCardFromList() {
        var sheet by mutableStateOf(0.dp)
        compose.setContent { Screen(sheet = sheet) }
        compose.onNode(showList).performClick()
        compose.onNode(showMap).assertExists()
        compose.onNodeWithContentDescription("Powiększ").assertDoesNotExist()
        compose.onNodeWithContentDescription("C-13, Budynek testowy").assertExists()
        compose.onNodeWithContentDescription("C-14, Budynek testowy").performClick()
        assertEquals(listOf("C-14"), opened)
        sheet = 400.dp
        compose.onNodeWithText("© OpenStreetMap contributors").assertExists()
        compose.onNode(showMap).assertDoesNotExist()
        sheet = 0.dp
        compose.onNode(showMap).assertExists()
        compose.onNodeWithText("© OpenStreetMap contributors").assertDoesNotExist()
    }

    /**
     * Wiersz: V1 bez internetu → linia „Bez internetu” bez daty pod polem, czytana jako komunikat stanu, a po powrocie
     * internetu znika (WCAG 4.1.3).
     *
     * Wymagania: US9.1, NFR-10
     */
    @Test
    fun showsOfflineLine() {
        compose.setContent { Screen() }
        compose.onNodeWithText("Bez internetu", substring = true).assertDoesNotExist()
        online.value = false
        val status = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)
        compose.onNode(status and hasAnyDescendant(hasText("Bez internetu"))).assertExists()
        val field = compose.onNode(hasSetTextAction()).getBoundsInRoot()
        assertEquals(true, compose.onNodeWithText("Bez internetu").getBoundsInRoot().top >= field.bottom)
        online.value = true
        compose.onNodeWithText("Bez internetu").assertDoesNotExist()
    }

    /** Wiersz: V2 otwarte od razu, „Pokaż budynek na mapie” → V1 z zaznaczonym budynkiem. Wymagania: US2.9 */
    @Test
    fun movesFocusToMap() {
        compose.setContent { Screen(focus = "C-14") }
        compose.waitForIdle()
        assertEquals("C-14", focuses.last())
    }

    /** Wiersz: V2, „Wstecz” → V1 w poprzednim stanie z tym samym widokiem kamery. */
    @Test
    fun keepsCameraAfterRestore() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { Screen() }
        compose.onNodeWithContentDescription("Powiększ").performClick()
        compose.waitForIdle()
        val zoomed = cameras.last()
        restoration.emulateSavedInstanceStateRestore()
        compose.waitForIdle()
        assertEquals(zoomed, cameras.last())
    }
}
