/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
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
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.CampusBounds
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.feature.map.area.AreaShift
import io.github.dostepnykampus.feature.map.area.CompassDirection
import io.github.dostepnykampus.feature.map.area.shiftTo
import io.github.dostepnykampus.feature.map.camera.CampusMapView
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.campus.MapArea
import io.github.dostepnykampus.feature.map.campus.MapSelection
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
    private val fakeMap =
        CampusMapView { shownData, shownSelection, move, _, _, _, _, _, _ ->
            areas = shownData.areas
            outlines = shownData.outlinesBySymbol
            selection = shownSelection
            camera = move.camera
        }

    private fun showApp(isFirstLaunch: Boolean = false, waitForMap: Boolean = true) {
        val mapViewModel = compose.activity.appMapViewModel(choice)
        compose.setContent {
            DostepnyKampusTheme {
                AppShell(isFirstLaunch, TestRoles(), onFirstHelpLeft = {}) {
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
    fun startsInPwrArea() {
        showApp(isFirstLaunch = true)
        assertShowsArea(name = "Kampus główny", symbol = "A-1")
    }

    /**
     * Wiersz: V1, strzałka przesuwania na terenie startowym, którego osoba nie wybierała, dochodzi do brzegu
     * prostokąta terenu → komunikat TalkBack z nazwą „Kampus główny”.
     * Scenariusz: przegladanie-mapy.feature: Przy granicy terenu TalkBack mówi, jak zmienić teren.
     *
     * Wymagania: US1.7, NFR-53
     */
    @Test
    fun announcesEndOfStartArea() {
        showApp()
        val notice = hasContentDescription(EDGE_NOTICE)
        repeat(MAX_PANS) {
            if (compose.onAllNodes(notice).fetchSemanticsNodes().isEmpty()) {
                compose.onNodeWithContentDescription("Przesuń mapę w prawo").performClick()
                compose.waitForIdle()
            }
        }
        compose.onNode(notice).assertExists("komunikat o końcu terenu Kampus główny przy brzegu")
    }

    /**
     * Wiersz: V1, panel „Teren na mapie” ma pięć terenów PWr, „Kampus główny” wybrany, przy innych liczba
     * budynków i odległość.
     * Scenariusz: przegladanie-mapy.feature: Lista terenów podaje nazwę, liczbę budynków i odległość.
     *
     * Wymagania: US1.7, BR-G5
     */
    @Test
    fun listsPwrAreas() {
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
    fun choosesPwrArea() {
        showApp()
        openAreaList()
        compose.onNode(hasText("Wittigowo") and isSelectable()).performClick()
        compose.waitForIdle()
        assertShowsArea(name = "Wittigowo", symbol = "T-15")
    }

    /**
     * Wiersz: V1, wyszukanie „T-15” i wybór wyniku zaznacza T-15, przenosi granicę mapy na teren „Wittigowo”
     * i nad małą kartą pokazuje napis „Pokazuję teren: Wittigowo · 2 km na południowy wschód”.
     * Scenariusz: przegladanie-mapy.feature: Wyszukanie budynku z innego terenu zmienia teren
     *
     * Wymagania: US1.8, BR-G5, NFR-56
     */
    @Test
    fun searchesOtherPwrArea() {
        showApp()
        compose.onNode(hasSetTextAction()).performTextInput("T-15")
        compose.onNodeWithContentDescription("T-15, ", substring = true).performClick()
        compose.waitForIdle()
        assertEquals("T-15", selection.building)
        assertShowsArea(name = "Wittigowo", symbol = "T-15", hasSwitch = false)
        compose.onNodeWithText("Pokazuję teren: Wittigowo · 2 km na południowy wschód").assertIsDisplayed()
    }

    /**
     * Wiersz: V1, po przejściu z „Kampusu głównego” na „Wittigowo” napis podaje odległość i kierunek z danych PWr:
     * „Pokazuję teren: Wittigowo · 2 km na południowy wschód”.
     * Scenariusz (częściowo): przegladanie-mapy.feature: Wyszukanie budynku z innego terenu zmienia teren
     *
     * Wymagania: US1.8, NFR-56
     */
    @Test
    fun shiftsToWittigowoSouthEast() {
        showApp()
        assertEquals(
            "zmiana z Kampusu głównego na Wittigowo",
            AreaShift(name = "Wittigowo", kilometers = 2, direction = CompassDirection.SouthEast),
            area("Kampus główny").shiftTo(area("Wittigowo")),
        )
    }

    private fun searchT15AndGoBack() {
        compose.onNode(hasSetTextAction()).performTextInput("T-15")
        compose.onNodeWithContentDescription("T-15, ", substring = true).performClick()
        compose.waitForIdle()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    /**
     * Wiersz: V1, wyszukanie „T-15”, wybór wyniku i „Wstecz” zdejmują zaznaczenie, a mapa zostaje na terenie
     * „Wittigowo”.
     * Scenariusz: przegladanie-mapy.feature: Zamknięcie karty budynku z innego terenu zostawia ten teren
     *
     * Wymagania: US1.8, BR-G5
     */
    @Test
    fun keepsAreaOnBack() {
        showApp()
        searchT15AndGoBack()
        assertEquals(null, selection.building)
        assertEquals(area("Wittigowo").bounds, selection.bounds)
        compose.onNodeWithContentDescription("Teren: Wittigowo. Zmień teren").assertExists()
    }

    /**
     * Wiersz: V1, po „Wstecz” z karty „T-15” wybranie terenu „Kampus główny” pokazuje ten teren.
     * Scenariusz: przegladanie-mapy.feature: Teren wybrany przez osobę zostaje po zamknięciu karty
     *
     * Wymagania: US1.7, US1.8, BR-G5
     */
    @Test
    fun keepsOwnArea() {
        showApp()
        searchT15AndGoBack()
        compose.onNodeWithContentDescription("Teren: Wittigowo. Zmień teren").performClick()
        compose.onNode(hasText("Kampus główny") and isSelectable()).performClick()
        compose.waitForIdle()
        assertEquals(area("Kampus główny").bounds, selection.bounds)
        compose.onNodeWithContentDescription("Teren: Kampus główny. Zmień teren").assertExists()
    }

    /**
     * Wiersz: V1, lista budynków ma nagłówki terenów z liczbą budynków, a T-15 stoi pod nagłówkiem
     * „Wittigowo”.
     * Scenariusz: przegladanie-mapy.feature: Lista budynków jest podzielona na tereny.
     *
     * Wymagania: US1.8, NFR-19
     */
    @Test
    fun showsPwrListHeadings() {
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
        const val MAX_PANS = 60
        const val EDGE_NOTICE =
            "Tu kończy się teren Kampus główny. Inny teren wybierzesz przyciskiem «Zmień teren» u góry mapy."
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
