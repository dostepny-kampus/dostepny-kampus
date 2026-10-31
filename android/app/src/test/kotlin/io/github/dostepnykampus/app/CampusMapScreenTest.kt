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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
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
import io.github.dostepnykampus.core.data.buildings.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.map.MapListChoice
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.feature.map.CampusMapScreen
import io.github.dostepnykampus.feature.map.CampusMapView
import io.github.dostepnykampus.feature.map.CampusMapViewModel
import io.github.dostepnykampus.feature.map.MapCamera
import io.github.dostepnykampus.feature.map.data.CampusMapData
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
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

// Wysokość klawiatury w pikselach: przy gęstości zrzutów 2× to 300 dp, jak na telefonie.
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

/** Ekran V1 z widokiem zastępczym zamiast MapLibre i katalogiem „Budynek testowy” (nie dane PWr). */
@RunWith(RobolectricTestRunner::class)
class CampusMapScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val cameras = mutableListOf<MapCamera>()
    private val focuses = mutableListOf<String?>()
    private val opened = mutableListOf<String>()
    private var touch: (List<String>) -> Unit = {}
    private val fakeMap =
        CampusMapView { data: CampusMapData, camera, _, onBuildingsTouch, _: Modifier ->
            cameras += camera
            focuses += data.focus
            touch = onBuildingsTouch
        }
    private val online = MutableStateFlow(true)
    private val choice = TestMapListChoice()
    private val listToggle = hasText("Lista budynków") and isSelectable()
    private val repository =
        BuildingCatalogRepository(ioContext = EmptyCoroutineContext) {
            """{"type":"FeatureCollection","attribution":"© OpenStreetMap contributors","features":[""" +
                """{"type":"Feature","properties":{"kind":"building","symbols":["C-13","C-14"],""" +
                """"name":"Budynek testowy",""" +
                """"osm_id":"way/1"},""" +
                """"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}}]}"""
        }

    private val viewModel by lazy { CampusMapViewModel(repository, { online }, choice) }

    @Composable
    private fun Screen(fontScale: Float = 1f, focus: String? = null, sheet: Dp = 0.dp) {
        DostepnyKampusTheme {
            val density = Density(density = LocalDensity.current.density, fontScale = fontScale)
            CompositionLocalProvider(LocalDensity provides density) {
                CampusMapScreen(
                    onBuildingClick = { opened += it },
                    focus = focus,
                    contentPadding = PaddingValues(bottom = sheet),
                    mapView = fakeMap,
                    viewModel = viewModel,
                )
            }
        }
    }

    private fun keyboard(isOpen: Boolean) {
        compose.runOnUiThread { dispatchKeyboard(compose.activity, isOpen) }
        compose.waitForIdle()
    }

    @Test
    fun nfr53_buttons_nfr37_osm_us2() {
        compose.setContent { Screen() }
        compose.onNodeWithContentDescription("Powiększ").performClick()
        compose.onNodeWithContentDescription("Przesuń mapę w górę").performClick()
        compose.waitForIdle()
        val (start, zoomed, moved) = cameras.distinct()
        assertEquals(listOf(start.zoom + 1, start.center), listOf(zoomed.zoom, zoomed.center))
        assertEquals(true, moved.center.latitude > zoomed.center.latitude)
        val names = listOf("Pomniejsz", "Przesuń mapę w dół", "Przesuń mapę w lewo", "Przesuń mapę w prawo")
        names.forEach { compose.onNodeWithContentDescription(it).assertExists() }
        // Plakietka jest niska jak w hi-fi, ale odnośnik ma cel dotyku 48 dp (NFR-17).
        compose.onNodeWithText("© OpenStreetMap contributors").assertHasClickAction().assertHeightIsAtLeast(48.dp)
        compose.runOnUiThread { listOf(listOf("C-3", "C-4"), listOf("C-13")).forEach(touch) }
        assertEquals(listOf("C-13"), opened)
    }

    // Wiersz: czcionka 1,5 → start na liście, „Lista” czytane jako „Lista budynków” (NFR-19, NFR-20); wybór „Mapa”
    // zostaje zapisany, a mapa ma krótki podpis „© OpenStreetMap” (NFR-37).
    @Test
    fun nfr20_list_start_short_osm() {
        compose.setContent { Screen(fontScale = 1.5f) }
        compose.onNode(hasText("Lista") and hasContentDescription("Lista budynków")).assertIsSelected()
        compose.onNode(hasText("Mapa") and isSelectable()).performClick()
        compose.onNodeWithText("© OpenStreetMap").assertExists()
        assertEquals(false, choice.savedChoice.value)
    }

    // Wiersz: zapisany wybór „Lista budynków” → V1 startuje na liście także przy zwykłej czcionce (NFR-19).
    @Test
    fun nfr19_saved_list_choice() {
        choice.savedChoice.value = true
        compose.setContent { Screen() }
        compose.onNode(listToggle).assertIsSelected()
        compose.onNodeWithContentDescription("Powiększ").assertDoesNotExist()
    }

    // Wiersz: kontur wspólny „C-13/C-14” → arkusz „Który budynek?”, przycisk budynku → V2, decyzja 85.
    @Test
    fun us2_which_building_opens_v2() {
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

    // Wiersz: „Zamknij” w arkuszu „Który budynek?” → V1 bez arkusza.
    @Test
    fun us2_which_building_closes() {
        compose.setContent { Screen() }
        compose.waitForIdle()
        compose.runOnUiThread { touch(listOf("C-13", "C-14")) }
        compose.onNodeWithText("Budynek C-13", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Zamknij").performClick()
        compose.onNodeWithText("Który budynek?").assertDoesNotExist()
        assertEquals(emptyList<String>(), opened)
    }

    // Wiersz: otwarta klawiatura chowa przyciski mapy, podpis OSM zostaje; po zamknięciu przyciski wracają
    // (NFR-53, decyzja 91).
    @Test
    fun nfr53_keyboard_hides_buttons() {
        compose.setContent { Screen() }
        keyboard(isOpen = true)
        compose.onNodeWithContentDescription("Powiększ").assertDoesNotExist()
        compose.onNodeWithContentDescription("Przesuń mapę w górę").assertDoesNotExist()
        compose.onNodeWithText("© OpenStreetMap contributors").assertExists()
        keyboard(isOpen = false)
        compose.onNodeWithContentDescription("Powiększ").assertExists()
        compose.onNodeWithContentDescription("Przesuń mapę w górę").assertExists()
    }

    // Wiersz: V2 jako arkusz nad V1, podpis OSM i przyciski nad arkuszem, decyzja 87.
    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun nfr37_osm_above_sheet() {
        compose.setContent { Screen(sheet = 400.dp) }
        val bottom = compose.onRoot().getBoundsInRoot().bottom
        val osm = compose.onNodeWithText("© OpenStreetMap contributors").getBoundsInRoot()
        assertEquals(true, osm.bottom <= bottom - 400.dp)
        compose.onNodeWithContentDescription("Pomniejsz").assertDoesNotExist()
        compose.onNodeWithText("Mapa kampusu").assertDoesNotExist()
        compose.onNodeWithContentDescription("Przesuń mapę w górę").assertDoesNotExist()
    }

    // Wiersz: V1 → V2 z budynkiem nad arkuszem, „Wstecz” → widok sprzed otwarcia V2, decyzja 83.
    @Test
    fun us1_1_focus_above_sheet() {
        var sheet by mutableStateOf(0.dp)
        compose.setContent { Screen(focus = "C-14", sheet = sheet) }
        compose.waitForIdle()
        val start = cameras.last()
        sheet = 400.dp
        compose.waitForIdle()
        val aboveSheet = cameras.last()
        assertEquals(listOf(start.zoom, true), listOf(aboveSheet.zoom, aboveSheet.bottomInset > 0))
        assertEquals(true, aboveSheet.center != start.center)
        sheet = 0.dp
        compose.waitForIdle()
        assertEquals(start, cameras.last())
    }

    // Wiersz: pole symbolu „c14” → wynik „Budynek C-14” → V2 z obrysem, US1.1.
    @Test
    fun us1_1_search_result_opens_v2() {
        compose.setContent { Screen() }
        compose.onNode(hasSetTextAction()).performTextInput("c14")
        compose.onNodeWithText("Budynek C-14", useUnmergedTree = true).assertExists()
        compose.onNodeWithContentDescription("C-14, Budynek testowy").performClick()
        assertEquals(listOf("C-14"), opened)
        compose.onNodeWithContentDescription("Wyczyść pole").assertDoesNotExist()
    }

    // Wiersz: „A-1 329” → stan „brak wyniku”, decyzja 82; „×” „Wyczyść pole” → pole puste.
    @Test
    fun us1_1_no_result_and_clear() {
        compose.setContent { Screen() }
        compose.onNode(hasSetTextAction()).performTextInput("A-1 329")
        compose.onNodeWithText("Nie znaleźliśmy budynku „A-1 329”.").assertExists()
        compose.onNodeWithContentDescription("Wyczyść pole").performClick()
        compose.onNodeWithText("Nie znaleźliśmy", substring = true).assertDoesNotExist()
        assertEquals(emptyList<String>(), opened)
    }

    // Wiersz: V1, „Lista budynków” → lista z katalogu bez mapy; budynek z listy → V2 jako arkusz na mapie,
    // a po „Wstecz” znów lista (NFR-19, US2.5, decyzja 48).
    @Test
    fun nfr19_list_opens_v2_on_map() {
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

    // Wiersz: V1, lista i pole symbolu → lista zawężona do wpisanego budynku (NFR-19, US1.1).
    @Test
    fun nfr19_symbol_narrows_list() {
        compose.setContent { Screen() }
        compose.onNode(listToggle).performClick()
        compose.onNode(hasSetTextAction()).performTextInput("c14")
        compose.onNodeWithContentDescription("C-14, Budynek testowy").assertExists()
        compose.onNodeWithContentDescription("C-13, Budynek testowy").assertDoesNotExist()
    }

    // Wiersz: V1, brak wyniku → „Lista budynków” w karcie czyści pole i pokazuje całą listę (UC1 3a, PW-9).
    @Test
    fun uc1_3a_no_result_opens_list() {
        compose.setContent { Screen() }
        compose.onNode(hasSetTextAction()).performTextInput("C-31")
        val inCard = hasAnyAncestor(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion))
        compose.onNode(hasText("Lista budynków") and inCard).performClick()
        compose.onNode(listToggle).assertIsSelected()
        compose.onNodeWithText("Nie znaleźliśmy", substring = true).assertDoesNotExist()
        compose.onNodeWithContentDescription("C-13, Budynek testowy").assertExists()
    }

    // Wiersz: V1 bez internetu → linia „Bez internetu” bez daty pod polem, czytana jako komunikat stanu, a po powrocie
    // internetu znika (US9.1, NFR-10, decyzja 84, WCAG 4.1.3).
    @Test
    fun us9_1_offline_line() {
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

    // Wiersz: V2, „Wstecz” → V1 z zaznaczonym budynkiem (US1.1).
    @Test
    fun us1_1_focus_reaches_map() {
        compose.setContent { Screen(focus = "C-14") }
        compose.waitForIdle()
        assertEquals("C-14", focuses.last())
    }

    // Wiersz: V2, „Wstecz” → V1 w poprzednim stanie z tym samym widokiem kamery, decyzja 83.
    @Test
    fun us1_camera_survives_restore() {
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
