/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

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
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
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
import io.github.dostepnykampus.core.data.buildings.BuildingCatalogRepository
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.feature.map.CampusMapScreen
import io.github.dostepnykampus.feature.map.CampusMapView
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
    private val repository =
        BuildingCatalogRepository(ioContext = EmptyCoroutineContext) {
            """{"type":"FeatureCollection","attribution":"© OpenStreetMap contributors","features":[""" +
                """{"type":"Feature","properties":{"kind":"building","symbols":["C-13","C-14"],""" +
                """"name":"Budynek testowy",""" +
                """"osm_id":"way/1"},""" +
                """"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}}]}"""
        }

    @Composable
    private fun Screen(fontScale: Float = 1f, focus: String? = null, sheet: Dp = 0.dp) {
        DostepnyKampusTheme {
            val density = Density(density = LocalDensity.current.density, fontScale = fontScale)
            CompositionLocalProvider(LocalDensity provides density) {
                CampusMapScreen(
                    repository = repository,
                    networkStatus = { online },
                    onBuildingClick = { opened += it },
                    focus = focus,
                    contentPadding = PaddingValues(bottom = sheet),
                    mapView = fakeMap,
                )
            }
        }
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
        compose.onNodeWithText("© OpenStreetMap contributors").assertExists()
        compose.runOnUiThread { listOf(listOf("C-3", "C-4"), listOf("C-13")).forEach(touch) }
        assertEquals(listOf("C-13"), opened)
    }

    @Test
    fun nfr37_short_osm_at_large_font() {
        compose.setContent { Screen(fontScale = 1.5f) }
        compose.onNodeWithText("© OpenStreetMap").assertExists()
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
        compose.onNodeWithText("Nie znaleźliśmy budynku »A-1 329«.").assertExists()
        compose.onNodeWithContentDescription("Wyczyść pole").performClick()
        compose.onNodeWithText("Nie znaleźliśmy", substring = true).assertDoesNotExist()
        assertEquals(emptyList<String>(), opened)
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
