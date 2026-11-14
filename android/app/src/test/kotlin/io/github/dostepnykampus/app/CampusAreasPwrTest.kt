/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
@file:OptIn(TymczasoweRole::class)

package io.github.dostepnykampus.app

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import io.github.dostepnykampus.core.data.account.GuestRoleProvider
import io.github.dostepnykampus.core.data.account.TymczasoweRole
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.feature.map.CampusBounds
import io.github.dostepnykampus.feature.map.MapArea
import io.github.dostepnykampus.feature.map.MapSelection
import io.github.dostepnykampus.feature.map.area.center
import io.github.dostepnykampus.feature.map.camera.CampusMapView
import io.github.dostepnykampus.feature.map.camera.MapCamera
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tereny mapy V1 na danych PWr z zasobów aplikacji, z widokiem zastępczym zamiast MapLibre. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class CampusAreasPwrTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val choice = TestMapListChoice()
    private var areas = emptyList<MapArea>()
    private var outlines = emptyMap<String, CampusBounds>()
    private var selection = MapSelection()
    private var camera: MapCamera? = null
    private var idle: (MapCamera) -> Unit = {}
    private val fakeMap =
        CampusMapView { shownData, shownSelection, shownCamera, onCameraIdle, _, _, _ ->
            areas = shownData.areas
            outlines = shownData.outlinesBySymbol
            selection = shownSelection
            camera = shownCamera
            idle = onCameraIdle
        }

    private fun showApp(isFirstLaunch: Boolean = false, waitForMap: Boolean = true) {
        val mapViewModel = compose.activity.appMapViewModel(choice)
        compose.setContent {
            DostepnyKampusTheme {
                AppShell(isFirstLaunch, GuestRoleProvider, onFirstHelpLeft = {}) {
                    TestAppScreen(it, mapViewModel, fakeMap)
                }
            }
        }
        if (isFirstLaunch) compose.onNodeWithText("Pomiń instrukcję").performClick()
        compose.waitForIdle()
        if (waitForMap) compose.waitUntil(timeoutMillis = DATA_TIMEOUT) { areas.isNotEmpty() }
        compose.waitForIdle()
    }

    private fun area(name: String): MapArea = areas.single { it.name == name }

    private fun assertShowsArea(name: String, symbol: String, hasSwitch: Boolean = true) {
        val bounds = area(name).bounds
        assertEquals("granica mapy", bounds, selection.bounds)
        assertTrue("$symbol na terenie $name", symbol in area(name).symbols)
        val outline = outlines.getValue(symbol)
        assertTrue("kontur $symbol w granicy terenu", bounds.contains(outline))
        val center = requireNotNull(camera).center
        assertTrue("środek widoku na terenie $name", bounds.contains(center))
        if (hasSwitch) compose.onNodeWithContentDescription("Teren: $name. Zmień teren").assertExists()
    }

    private fun headingAbove(matcher: SemanticsMatcher): SemanticsNode? {
        val top =
            compose
                .onNode(matcher)
                .fetchSemanticsNode()
                .boundsInRoot
                .top
        return compose
            .onAllNodes(AreaHeading)
            .fetchSemanticsNodes()
            .filter { it.boundsInRoot.bottom <= top }
            .maxByOrNull { it.boundsInRoot.bottom }
    }

    private fun openAreaList() {
        compose.onNodeWithContentDescription("Teren: Kampus główny. Zmień teren").performClick()
        compose.onNodeWithText("Teren na mapie").assertExists()
    }

    /**
     * Wiersz: V1 po pominięciu instrukcji pokazuje teren „Kampus główny” z budynkiem A-1 i nazwą terenu
     * na mapie.
     * Scenariusz: przegladanie-mapy.feature: Mapa otwiera się na terenie „Kampus główny”.
     *
     * Wymagania: US1.6, BR-G5
     */
    @Test
    fun us1_6_start_area_pwr() {
        showApp(isFirstLaunch = true)
        assertShowsArea(name = "Kampus główny", symbol = "A-1")
    }

    /**
     * Wiersz: V1, panel „Teren na mapie” ma pięć terenów PWr, „Kampus główny” wybrany, przy innych liczba
     * budynków i odległość.
     * Scenariusz: przegladanie-mapy.feature: Lista terenów podaje nazwę, liczbę budynków i odległość.
     *
     * Wymagania: US1.7, BR-G5
     */
    @Test
    fun us1_7_area_list_pwr() {
        showApp()
        openAreaList()
        compose.onNode(hasText("Kampus główny") and isSelectable()).assertIsSelected()
        compose.onNode(hasText("Kampus główny") and hasText("· wybrany", substring = true)).assertExists()
        OTHER_AREAS.forEach { name ->
            val row = hasText(name) and hasText(" km", substring = true) and hasText(" budyn", substring = true)
            compose.onNode(row).assertExists(name)
        }
    }

    /**
     * Wiersz: V1, wybranie „Wittigowo” z panelu pokazuje ten teren z budynkiem T-15 i jego nazwą na mapie.
     * Scenariusz: przegladanie-mapy.feature: Wybór terenu z listy pokazuje jego mapę.
     *
     * Wymagania: US1.7
     */
    @Test
    fun us1_7_choose_area_pwr() {
        showApp()
        openAreaList()
        compose.onNode(hasText("Wittigowo") and isSelectable()).performClick()
        compose.waitForIdle()
        assertShowsArea(name = "Wittigowo", symbol = "T-15")
    }

    /**
     * Wiersz: V1, widok zatrzymany gestem na granicy terenu „Kampus główny” od strony „Wittigowo” zostaje
     * w granicy przekazanej mapie, a przy krawędzi stoi przycisk „Wittigowo” z odległością.
     * Scenariusz (częściowo): przegladanie-mapy.feature: Mapa nie wychodzi poza wybrany teren
     *
     * Wymagania: US1.7, BR-G5, NFR-39
     */
    @Test
    fun us1_7_edge_button_pwr() {
        showApp()
        val main = area("Kampus główny").bounds
        val target = area("Wittigowo").bounds.center
        val edge =
            GeoPoint(
                longitude = target.longitude.coerceIn(minimumValue = main.west, maximumValue = main.east),
                latitude = target.latitude.coerceIn(minimumValue = main.south, maximumValue = main.north),
            )
        compose.runOnUiThread { idle(MapCamera(center = edge, zoom = requireNotNull(camera).zoom)) }
        compose.waitForIdle()
        assertEquals(main, selection.bounds)
        val button = hasContentDescription("Tu kończy się teren Kampus główny. Obok: Wittigowo, ", substring = true)
        compose.onNode(button and hasContentDescription(" km", substring = true)).assertExists()
    }

    /**
     * Wiersz: V1, wyszukanie „T-15” i wybór wyniku zaznacza T-15 i przenosi granicę mapy na teren
     * „Wittigowo”.
     * Scenariusz (częściowo): przegladanie-mapy.feature: Wyszukanie budynku z innego terenu zmienia teren
     *
     * Wymagania: US1.8, BR-G5
     */
    @Test
    fun us1_8_search_other_area_pwr() {
        showApp()
        compose.onNode(hasSetTextAction()).performTextInput("T-15")
        compose.onNodeWithContentDescription("T-15, ", substring = true).performClick()
        compose.waitForIdle()
        assertEquals("T-15", selection.building)
        assertShowsArea(name = "Wittigowo", symbol = "T-15", hasSwitch = false)
    }

    /**
     * Wiersz: V1, lista budynków ma nagłówki terenów z liczbą budynków, a T-15 stoi pod nagłówkiem
     * „Wittigowo”.
     * Scenariusz: przegladanie-mapy.feature: Lista budynków jest podzielona na tereny.
     *
     * Wymagania: US1.8, NFR-19
     */
    @Test
    fun us1_8_list_headings_pwr() {
        choice.savedChoice.value = true
        showApp(waitForMap = false)
        val wittigowo = hasContentDescription("Wittigowo, ", substring = true) and AreaHeading
        val t15 = hasContentDescription("T-15, ", substring = true)
        compose.waitUntil(timeoutMillis = DATA_TIMEOUT) {
            compose.onAllNodes(AreaHeading).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(hasContentDescription("Kampus główny, ", substring = true) and AreaHeading).assertExists()
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(wittigowo)
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(t15)
        var nearest = headingAbove(t15)
        repeat(times = 30) {
            if (nearest == null) {
                compose.onNode(hasScrollToNodeAction()).performSemanticsAction(SemanticsActions.ScrollBy) { scroll ->
                    scroll(0f, -SCROLL_STEP)
                }
                nearest = headingAbove(t15)
            }
        }
        val description = nearest?.run { config.getOrNull(SemanticsProperties.ContentDescription) }
        val name = description?.firstOrNull().orEmpty()
        assertTrue("T-15 pod nagłówkiem Wittigowo, a jest pod: $name", name.startsWith("Wittigowo, "))
    }

    private companion object {
        const val DATA_TIMEOUT = 10_000L
        const val SCROLL_STEP = 100f
        val AreaHeading =
            SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading) and
                hasContentDescription(" budyn", substring = true)
        val OTHER_AREAS = listOf("Wittigowo", "Bartoszowice", "Długa", "T-9 Atol")

        fun CampusBounds.contains(point: GeoPoint): Boolean =
            point.longitude in west..east && point.latitude in south..north

        fun CampusBounds.contains(other: CampusBounds): Boolean =
            other.west >= west && other.east <= east && other.south >= south && other.north <= north
    }
}
