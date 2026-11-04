/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.core.data.account.GuestRoleProvider
import io.github.dostepnykampus.feature.map.CampusMapScreen
import io.github.dostepnykampus.feature.map.CampusMapView
import io.github.dostepnykampus.feature.map.CampusMapViewModel
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Przycisk pełnego ekranu V2 otwartego od razu (US2.9). */
internal const val BUILDING_ON_MAP = "Pokaż budynek na mapie"

/** Budynek A-1 na liście V1 z katalogu w zasobach aplikacji (NFR-19). */
internal val ListedA1 = hasContentDescription("A-1, ", substring = true)

private const val OSM = "© OpenStreetMap contributors"

private val ListToggle = hasText("Lista budynków") and isSelectable()

/** Budynek wybrany z listy V1 albo dotknięty na mapie przy TalkBack: tryb karty V2 i powrót (NFR-19, decyzja 118). */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class ListEntryTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val choice = TestMapListChoice().apply { savedChoice.value = true }
    private val fromList = mutableListOf<String>()
    private val fromMap = mutableListOf<String>()
    private var touch: (List<String>) -> Unit = {}
    private val fakeMap = CampusMapView { _, _, _, onBuildingsTouch, _ -> touch = onBuildingsTouch }
    private lateinit var appViewModel: CampusMapViewModel

    @Before
    fun talkBackOn() {
        shadowOf(compose.activity.getSystemService(AccessibilityManager::class.java)).setTouchExplorationEnabled(true)
        appViewModel = compose.activity.appMapViewModel(choice)
    }

    private fun showApp() {
        compose.setContent {
            AppShell(firstLaunch = false, roles = GuestRoleProvider, onFirstHelpLeft = {}) {
                TestAppScreen(it, appViewModel, fakeMap)
            }
        }
    }

    // Wiersz: V1, dotknięcie konturu budynku → V2, mała karta nad mapą także przy TalkBack, US2.8, decyzja 115.
    @Test
    fun us2_8_contour_talkback_compact() {
        choice.savedChoice.value = false
        showApp()
        compose.waitUntil { compose.onAllNodesWithText(OSM).fetchSemanticsNodes().isNotEmpty() }
        compose.runOnUiThread { touch(listOf("A-1")) }
        compose.waitForIdle()
        compose.onNode(sheetHandle(isExpanded = false)).assertExists("mała karta po dotknięciu konturu przy TalkBack")
        compose.onNodeWithText(BUILDING_ON_MAP).assertDoesNotExist()
    }

    // Wiersze: V1, dotknięcie budynku na liście przy zwykłej czcionce → V1 w widoku mapy z małą kartą V2; zamknięcie
    // małej karty → lista bez zaznaczenia, zapamiętany wybór „Lista budynków” bez zmian, także po odtworzeniu
    // aktywności; dotknięcie konturu nie jest wyborem z listy, US2.8, NFR-19, NFR-22, decyzja 118.
    @Test
    fun nfr19_list_card_closes_to_list() {
        var sheet by mutableStateOf(0.dp)
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            CampusMapScreen(
                onBuildingClick = { fromMap += it },
                contentPadding = PaddingValues(bottom = sheet),
                onListBuildingClick = { fromList += it },
                mapView = fakeMap,
                viewModel = appViewModel,
            )
        }
        compose.waitUntil { compose.onAllNodes(ListedA1).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(ListedA1).performClick()
        assertEquals(listOf("A-1"), fromList)
        sheet = 400.dp
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText(OSM).assertExists("mapa pod małą kartą po odtworzeniu")
        compose.runOnUiThread { touch(listOf("A-1")) }
        sheet = 0.dp
        compose.onNode(ListToggle).assertIsSelected()
        assertEquals(listOf(true, listOf("A-1"), listOf("A-1")), listOf(choice.savedChoice.value, fromMap, fromList))
    }
}
