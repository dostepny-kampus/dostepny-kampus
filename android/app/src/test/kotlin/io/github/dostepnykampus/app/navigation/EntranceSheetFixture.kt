/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.navigation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.dostepnykampus.app.AppShell
import io.github.dostepnykampus.app.EXPAND_HANDLE
import io.github.dostepnykampus.app.MapOrBuildingScreen
import io.github.dostepnykampus.app.TestMapListChoice
import io.github.dostepnykampus.app.TestRoles
import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.feature.map.camera.CampusMapView
import io.github.dostepnykampus.feature.map.camera.TouchSign
import io.github.dostepnykampus.feature.map.campus.CampusMapData
import io.github.dostepnykampus.feature.map.campus.MapSelection
import io.github.dostepnykampus.feature.map.campus.SelectedEntrance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import kotlin.coroutines.EmptyCoroutineContext

private const val BUILDING = "A-1"
private const val BUILDING_ROW = "Budynek A-1"

internal val ACCESSIBLE_PIN =
    SelectedEntrance("node/2", GeoPoint(longitude = 0.2, latitude = 0.3), "Wejście dostosowane", BUILDING)

internal val MAIN_PIN =
    SelectedEntrance("node/1", GeoPoint(longitude = 0.5, latitude = 0.0), "Wejście główne", BUILDING)

private val CATALOG_JSON =
    buildList {
        val poly = "[[[0,0],[1,0],[1,1],[0,1],[0,0]]]"
        val b = """"kind":"building","symbols":["$BUILDING"],"display_name":"Budynek główny","osm_id":"way/1""""
        add("""{"type":"Feature","properties":{$b},"geometry":{"type":"Polygon","coordinates":$poly}}""")
        val pts = listOf(MAIN_PIN.location, ACCESSIBLE_PIN.location)
        listOf("Wejście główne", "Wejście dostosowane", "Boczne", "Dziedziniec", "Wschodnie", "Zachodnie")
            .forEachIndexed { idx, name ->
                val pt = pts.getOrElse(idx) { GeoPoint(0.1 * idx, 0.5) }
                val kind = if (idx == 0) "main" else "yes"
                val wc = if (idx == 1) "yes" else "no"
                val p =
                    """"kind":"entrance","symbols":["$BUILDING"],"osm_id":"node/${idx + 1}",""" +
                        """"entrance":"$kind","wheelchair":"$wc","ref":"$name""""
                val g = """"type":"Point","coordinates":[${pt.longitude},${pt.latitude}]"""
                add("""{"type":"Feature","properties":{$p},"geometry":{$g}}""")
            }
    }.joinToString(",", """{"type":"FeatureCollection","attribution":"© OpenStreetMap","features":[""", "]}")

/**
 * Powłoka aplikacji z mapą V1, kartą budynku A-1 i arkuszem wejścia nad mapą zastępczą.
 *
 * Wymagania: US2.8, US3.3, NFR-15, NFR-18, NFR-53
 */
open class EntranceSheetFixture {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    protected val selections = mutableListOf<MapSelection>()
    protected var touchBuildings: (List<String>) -> Unit = {}
    protected var touchSign: (TouchSign, String) -> Unit = { _, _ -> }
    protected val cardHeader = hasContentDescription("$BUILDING_ROW, ", substring = true)
    protected val cardScroll = hasScrollAction() and hasAnyDescendant(hasText("Wejścia"))
    protected val sheetBuildingRow = hasContentDescription(BUILDING_ROW)
    protected val recommendedRow = hasClickAction() and hasText(ACCESSIBLE_PIN.label)
    protected val mainEntranceRow = hasClickAction() and hasText(MAIN_PIN.label)

    private val choice = TestMapListChoice()
    private val repository = BuildingCatalogRepository(ioContext = EmptyCoroutineContext) { CATALOG_JSON }
    private val fakeMap =
        CampusMapView { _: CampusMapData, selection, _, _, _, onBuildingsTouch, onSignTouch, modifier: Modifier ->
            selections += selection
            touchBuildings = onBuildingsTouch
            touchSign = onSignTouch
            Box(modifier.testTag("fake-map"))
        }

    protected fun start(withLowCard: Boolean = false) {
        compose.setContent {
            DostepnyKampusTheme {
                AppShell(false, TestRoles(), {}, onChooseMap = { choice.savedChoice.value = false }) { route ->
                    MapOrBuildingScreen(route, repository, choice, fakeMap) { AppScreen(it) }
                }
            }
        }
        compose.waitUntil { selections.isNotEmpty() }
        compose.waitForIdle()
        assertNull("bez pinezki wejścia na starcie", selections.last().entrance)
        if (withLowCard) {
            compose.runOnUiThread { touchBuildings(listOf(BUILDING)) }
            waitForCard()
        }
    }

    protected fun waitForCard() {
        compose.waitUntil { compose.onAllNodes(cardHeader).fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
    }

    protected fun waitForSheet() {
        compose.waitUntil { compose.onAllNodes(sheetBuildingRow).fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
    }

    protected fun openSheetByRecommended() {
        compose.onNodeWithText("Pokaż wejście na mapie").performClick()
        waitForSheet()
    }

    protected fun openSheetBySign(id: String = ACCESSIBLE_PIN.id) {
        compose.runOnUiThread { touchSign(TouchSign.ENTRANCE, id) }
        waitForSheet()
    }

    protected fun closeSheet() {
        compose.onNodeWithText("Zamknij").performClick()
        compose.waitForIdle()
    }

    protected fun pressBack() {
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    protected fun assertPinned(expected: SelectedEntrance) {
        compose.onNode(isHeading() and hasText("Wejście")).assertExists("nagłówek arkusza wejścia")
        compose.onNode(sheetBuildingRow).assertExists("wiersz budynku w arkuszu wejścia")
        compose.onNodeWithText(expected.label).assertExists("nazwa wejścia w arkuszu: ${expected.label}")
        assertEquals("pinezka i podpis wejścia na mapie", expected, selections.last().entrance)
        val spoken = "Zaznaczone wejście: »${expected.label}«, budynek $BUILDING"
        compose.onNode(hasContentDescription(spoken)).assertExists("opis zaznaczonego wejścia dla TalkBack")
    }

    protected fun assertUnpinned() {
        compose.onNode(sheetBuildingRow).assertDoesNotExist()
        compose.onNode(hasContentDescription("Zgłoś barierę przy wejściu:", substring = true)).assertDoesNotExist()
        assertNull("bez pinezki i podpisu na mapie", selections.last().entrance)
        compose.onNode(hasContentDescription("Zaznaczone wejście", substring = true)).assertDoesNotExist()
    }

    protected fun assertMapWithoutCard() {
        compose.onNode(cardHeader).assertDoesNotExist()
        compose.onNodeWithContentDescription(EXPAND_HANDLE).assertDoesNotExist()
        compose.onNodeWithText("Szukaj budynku, np. C-13").assertExists("pole wyszukiwania na mapie")
        compose.onNodeWithContentDescription("Powiększ").assertExists("przyciski mapy po zamknięciu arkusza")
        assertNull("bez zaznaczenia budynku na mapie", selections.last().building)
    }
}
