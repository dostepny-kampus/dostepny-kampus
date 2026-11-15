/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.map.MapListChoice
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.feature.map.CampusMapScreen
import io.github.dostepnykampus.feature.map.camera.CameraMotion
import io.github.dostepnykampus.feature.map.camera.CameraTransition
import io.github.dostepnykampus.feature.map.camera.CampusMapView
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.camera.MapMotion
import io.github.dostepnykampus.feature.map.campus.CampusMapData
import io.github.dostepnykampus.feature.map.navigation.MapFocus
import io.github.dostepnykampus.feature.map.overlay.MAP_STATUS_BAR_TAG
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.coroutines.EmptyCoroutineContext

/** Wybór w przełączniku „Mapa” i „Lista budynków” w pamięci testu; na start osoba nic nie wybrała. */
internal class TestMapListChoice : MapListChoice {
    override val savedChoice = MutableStateFlow<Boolean?>(null)

    override suspend fun choose(isList: Boolean) {
        savedChoice.value = isList
    }
}

/** Kontroler klawiatury, który liczy wywołania [hide]. */
private class HidingKeyboard : SoftwareKeyboardController {
    var hidden = 0

    override fun hide() {
        hidden++
    }

    override fun show() = Unit
}

private const val KEYBOARD_HEIGHT = 600

/** Podaje ekranowi [activity] klawiaturę otwartą albo zamkniętą tak, jak system: w odstępach okna. */
internal fun dispatchKeyboard(activity: ComponentActivity, isOpen: Boolean) {
    val ime = WindowInsetsCompat.Type.ime()
    val insets =
        WindowInsetsCompat
            .Builder()
            .setInsets(ime, Insets.of(0, 0, 0, if (isOpen) KEYBOARD_HEIGHT else 0))
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

/**
 * Ekran V1 z widokiem zastępczym zamiast MapLibre i katalogiem „Budynek testowy” (nie dane PWr).
 *
 * `KEYBOARD_HEIGHT`: Wysokość klawiatury w pikselach: przy gęstości zrzutów 2× to 300 dp, jak na telefonie.
 *
 * Wymagania: US2, US1
 */
@RunWith(RobolectricTestRunner::class)
class CampusMapScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val cameras = mutableListOf<MapCamera>()
    private val moves = mutableListOf<CameraTransition>()
    private val insets = mutableListOf<Double>()
    private val focuses = mutableListOf<String?>()
    private val opened = mutableListOf<String>()
    private var emptyTaps = 0
    private var touch: (List<String>) -> Unit = {}
    private val fakeMap =
        CampusMapView { _: CampusMapData, selection, move, inset, _, onBuildingsTouch, _, _: Modifier ->
            cameras += move.camera
            moves += move
            insets += inset.bottom
            focuses += selection.building
            touch = onBuildingsTouch
        }
    private val online = MutableStateFlow(true)
    private val choice = TestMapListChoice()
    private val listToggle = hasText("Lista budynków") and isSelectable()
    private var ring = "[[0,0],[1,0],[1,1],[0,0]]"
    private val repository =
        BuildingCatalogRepository(ioContext = EmptyCoroutineContext) {
            """{"type":"FeatureCollection","attribution":"© OpenStreetMap contributors","features":[""" +
                """{"type":"Feature","properties":{"kind":"building","symbols":["C-13","C-14"],""" +
                """"display_name":"Budynek testowy",""" +
                """"osm_id":"way/1"},""" +
                """"geometry":{"type":"Polygon","coordinates":[$ring]}}]}"""
        }

    private val viewModel by lazy { inlineMapViewModel(repository, { online }, choice) }

    @Composable
    private fun Screen(fontScale: Float = 1f, focus: String? = null, sheet: Dp = 0.dp, motion: MapMotion? = null) {
        DostepnyKampusTheme {
            val density = Density(density = LocalDensity.current.density, fontScale = fontScale)
            CompositionLocalProvider(LocalDensity provides density) {
                CampusMapScreen(
                    onBuildingClick = { opened += it },
                    focus = focus?.let { MapFocus.Building(it) },
                    contentPadding = PaddingValues(bottom = sheet),
                    onEmptyMapClick = { emptyTaps++ },
                    mapView = fakeMap,
                    motion = motion,
                    viewModel = viewModel,
                )
            }
        }
    }

    private fun enterTouchMode() {
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val root = view.javaClass.getMethod("getViewRootImpl").invoke(view)
            root?.javaClass?.getDeclaredMethod("ensureTouchMode", Boolean::class.javaPrimitiveType)?.run {
                isAccessible = true
                invoke(root, true)
            }
        }
    }

    private fun keyboard(isOpen: Boolean) {
        compose.runOnUiThread { dispatchKeyboard(compose.activity, isOpen) }
        compose.waitForIdle()
    }

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
     * Wiersz: V1, strzałki przesuwania zatrzymują się na brzegu prostokąta terenu, czyli konturów (teren spoza
     * pliku granic) powiększonych o 50 m z każdej strony, bez komunikatu.
     *
     * Wymagania: NFR-39, NFR-53
     */
    @Test
    fun stopsArrowsAtEdge() {
        compose.setContent { Screen() }
        repeat(times = 5) { compose.onNodeWithContentDescription("Pomniejsz").performClick() }
        repeat(times = 40) { compose.onNodeWithContentDescription("Przesuń mapę w prawo").performClick() }
        compose.waitForIdle()
        assertEquals(1.0 + 50.0 / 111_195.0, cameras.last().center.longitude, 5e-5)
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
     * Wiersz: czcionka 1,5 → start na liście, „Lista” czytane jako „Lista budynków”; wybór „Mapa”
     * zostaje zapisany, a mapa ma krótki podpis „© OpenStreetMap”.
     *
     * Wymagania: NFR-19, NFR-20, NFR-37
     */
    @Test
    fun shortensOsmOnList() {
        compose.setContent { Screen(fontScale = 1.5f) }
        compose.onNode(hasText("Lista") and hasContentDescription("Lista budynków")).assertIsSelected()
        compose.onNode(hasText("Mapa") and isSelectable()).performClick()
        compose.onNodeWithText("© OpenStreetMap").assertExists()
        assertEquals(false, choice.savedChoice.value)
    }

    /**
     * Wiersz: zapisany wybór „Lista budynków” → V1 startuje na liście także przy zwykłej czcionce.
     *
     * Wymagania: NFR-19
     */
    @Test
    fun keepsListChoice() {
        choice.savedChoice.value = true
        compose.setContent { Screen() }
        compose.onNode(listToggle).assertIsSelected()
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
     * Scenariusz „Przy małej karcie budynek jest cały widoczny, a mapą sterują przyciski”: podpis OSM i przyciski
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
        compose.onNodeWithText("Legenda").assertDoesNotExist()
        compose.onNodeWithContentDescription("O danych mapy").assertDoesNotExist()
    }

    /**
     * Scenariusz „Przy niskiej mapie nad małą kartą nie ma przycisków mapy”: poniżej 300 dp wolnej mapy przycisków
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
     * Scenariusz „Przy małej karcie budynek jest cały widoczny, a mapą sterują przyciski”: mapa pod arkuszem sięga
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
     * Scenariusz „Przy małej karcie budynek jest cały widoczny, a mapą sterują przyciski”: V1 → V2 z budynkiem
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
     * Zapis „C13” w polu symbolu daje wynik „Budynek C-13”.
     * Scenariusz (przykład: C13): przegladanie-mapy.feature: Wyszukanie budynku po symbolu w różnym zapisie
     *
     * Wymagania: US1.1
     */
    @Test
    fun findsC13WithoutDash() {
        assertFindsC13("C13")
    }

    /**
     * Zapis „c-13” w polu symbolu daje wynik „Budynek C-13”.
     * Scenariusz (przykład: c-13): przegladanie-mapy.feature: Wyszukanie budynku po symbolu w różnym zapisie
     *
     * Wymagania: US1.1
     */
    @Test
    fun findsC13InLowercase() {
        assertFindsC13("c-13")
    }

    /**
     * Zapis „C-13” w polu symbolu daje wynik „Budynek C-13”.
     * Scenariusz (przykład: C-13): przegladanie-mapy.feature: Wyszukanie budynku po symbolu w różnym zapisie
     *
     * Wymagania: US1.1
     */
    @Test
    fun findsC13Exactly() {
        assertFindsC13("C-13")
    }

    private fun assertFindsC13(query: String) {
        compose.setContent { Screen() }
        compose.onNode(hasSetTextAction()).performTextInput(query)
        compose.onNodeWithText("Budynek C-13", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Budynek C-14", useUnmergedTree = true).assertDoesNotExist()
    }

    /** Wiersz: pole symbolu „c14” → wynik „Budynek C-14” → V2 z obrysem. Wymagania: US1.1 */
    @Test
    fun opensCardFromResult() {
        compose.setContent { Screen() }
        compose.onNode(hasSetTextAction()).performTextInput("c14")
        compose.onNodeWithText("Budynek C-14", useUnmergedTree = true).assertExists()
        compose.onNodeWithContentDescription("C-14, Budynek testowy").performClick()
        assertEquals(listOf("C-14"), opened)
        compose.onNodeWithContentDescription("Wyczyść pole").assertDoesNotExist()
    }

    /**
     * Wiersz: po wyborze wyniku klawiatura ekranowa się chowa, więc pierwsze „Wstecz” na V2 zamyka V2, a nie tylko
     * klawiaturę. Klawiaturę podaje atrapa kontrolera, bo Robolectric nie ma okna z klawiaturą,
     * a o skutku decyduje fokus pola: po wyborze pole go nie ma. Okno jest w trybie dotyku jak na telefonie; poza nim
     * Robolectric po `clearFocus` oddaje fokus pierwszemu polu, więc fokus nigdy by nie zszedł.
     *
     * Wymagania: US1.1, NFR-15
     */
    @Test
    fun hidesKeyboardOnResult() {
        val keyboard = HidingKeyboard()
        compose.setContent { CompositionLocalProvider(LocalSoftwareKeyboardController provides keyboard) { Screen() } }
        enterTouchMode()
        compose.onNode(hasSetTextAction()).performTextInput("c14")
        compose.onNode(hasSetTextAction()).assertIsFocused()
        compose.onNodeWithContentDescription("C-14, Budynek testowy").performClick()
        assertEquals(1, keyboard.hidden)
        compose.onNode(hasSetTextAction()).assertIsNotFocused()
    }

    /** Wiersz: „A-1 329” → stan „brak wyniku”; „×” „Wyczyść pole” → pole puste. */
    @Test
    fun clearsNoResult() {
        compose.setContent { Screen() }
        compose.onNode(hasSetTextAction()).performTextInput("A-1 329")
        compose.onNodeWithText("Nie znaleźliśmy budynku „A-1 329”.").assertExists()
        compose.onNodeWithContentDescription("Wyczyść pole").performClick()
        compose.onNodeWithText("Nie znaleźliśmy", substring = true).assertDoesNotExist()
        assertEquals(emptyList<String>(), opened)
    }

    /**
     * Wiersz: V1, „Lista budynków” → lista z katalogu bez mapy; budynek z listy → V2 jako arkusz na mapie,
     * a po „Wstecz” znów lista.
     *
     * Wymagania: NFR-19, US2.5
     */
    @Test
    fun opensCardFromList() {
        var sheet by mutableStateOf(0.dp)
        compose.setContent { Screen(sheet = sheet) }
        compose.onNode(listToggle).performClick().assertIsSelected()
        compose.onNodeWithContentDescription("Powiększ").assertDoesNotExist()
        compose.onNodeWithContentDescription("C-13, Budynek testowy").assertExists()
        compose.onNodeWithContentDescription("C-14, Budynek testowy").performClick()
        assertEquals(listOf("C-14"), opened)
        sheet = 400.dp
        compose.onNodeWithText("© OpenStreetMap contributors").assertExists()
        compose.onNode(listToggle).assertDoesNotExist()
        sheet = 0.dp
        compose.onNode(listToggle).assertIsSelected()
        compose.onNodeWithText("© OpenStreetMap contributors").assertDoesNotExist()
    }

    /** Wiersz: V1, lista i pole symbolu → lista zawężona do wpisanego budynku. Wymagania: NFR-19, US1.1 */
    @Test
    fun narrowsListBySymbol() {
        compose.setContent { Screen() }
        compose.onNode(listToggle).performClick()
        compose.onNode(hasSetTextAction()).performTextInput("c14")
        compose.onNodeWithContentDescription("C-14, Budynek testowy").assertExists()
        compose.onNodeWithContentDescription("C-13, Budynek testowy").assertDoesNotExist()
    }

    /** Wiersz: V1, brak wyniku → „Lista budynków” w karcie czyści pole i pokazuje całą listę (PW-9). Wymagania: UC1 */
    @Test
    fun opensListOnNoResult() {
        compose.setContent { Screen() }
        compose.onNode(hasSetTextAction()).performTextInput("C-31")
        val inCard = hasAnyAncestor(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion))
        compose.onNode(hasText("Lista budynków") and inCard).performClick()
        compose.onNode(listToggle).assertIsSelected()
        compose.onNodeWithText("Nie znaleźliśmy", substring = true).assertDoesNotExist()
        compose.onNodeWithContentDescription("C-13, Budynek testowy").assertExists()
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
