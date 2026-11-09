/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
@file:OptIn(TymczasoweRole::class)

package io.github.dostepnykampus.app

import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import io.github.dostepnykampus.core.data.account.GuestRoleProvider
import io.github.dostepnykampus.core.data.account.TymczasoweRole
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

/** Budynek z listy V1, z wyszukiwania albo z mapy: tryb karty V2 przy TalkBack i bez niego (NFR-19, decyzja 118). */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class ListEntryTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val choice = TestMapListChoice().apply { savedChoice.value = true }
    private var touch: (List<String>) -> Unit = {}
    private val fakeMap = CampusMapView { _, _, _, onBuildingsTouch, _ -> touch = onBuildingsTouch }
    private lateinit var appViewModel: CampusMapViewModel

    @Before
    fun talkBackOn() {
        shadowOf(compose.activity.getSystemService(AccessibilityManager::class.java)).setTouchExplorationEnabled(true)
        appViewModel = compose.activity.appMapViewModel(choice)
    }

    @Composable
    private fun Shell() {
        AppShell(firstLaunch = false, roles = GuestRoleProvider, onFirstHelpLeft = {}) {
            TestAppScreen(it, appViewModel, fakeMap)
        }
    }

    private fun showApp() {
        compose.setContent { Shell() }
    }

    private fun pickA1() {
        compose.waitUntil { compose.onAllNodes(ListedA1).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(ListedA1).performClick()
        compose.waitForIdle()
    }

    private fun checkCompact(moment: String) {
        compose.onNode(sheetHandle(isExpanded = false)).assertExists("mała karta: $moment")
        compose.onNodeWithText(BUILDING_ON_MAP).assertDoesNotExist()
    }

    /** Wiersz: V1, dotknięcie konturu budynku → V2, mała karta nad mapą także przy TalkBack, US2.8, decyzja 115. */
    @Test
    fun us2_8_contour_talkback_compact() {
        choice.savedChoice.value = false
        showApp()
        compose.waitUntil { compose.onAllNodesWithText(OSM).fetchSemanticsNodes().isNotEmpty() }
        compose.runOnUiThread { touch(listOf("A-1")) }
        compose.waitForIdle()
        checkCompact("kontur przy TalkBack")
    }

    /**
     * Wiersze: V1 bez TalkBack, dotknięcie budynku na liście przy zwykłej czcionce → V1 w widoku mapy z małą kartą
     * V2; „Wstecz” → lista, zapamiętany wybór „Lista budynków” bez zmian, także po odtworzeniu aktywności, US2.8,
     * NFR-19, NFR-22, decyzja 118.
     */
    @Test
    fun nfr19_list_compact_to_list() {
        shadowOf(compose.activity.getSystemService(AccessibilityManager::class.java)).setTouchExplorationEnabled(false)
        val restoration = StateRestorationTester(compose)
        restoration.setContent { Shell() }
        pickA1()
        restoration.emulateSavedInstanceStateRestore()
        checkCompact("budynek z listy bez TalkBack, po odtworzeniu")
        compose.onNodeWithText(OSM).assertExists("mapa pod małą kartą")
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        compose.onNode(hasText("Lista budynków") and isSelectable()).assertIsSelected()
        assertEquals(true, choice.savedChoice.value)
    }

    /**
     * Wiersz: V1, wybór wyniku w polu „Wpisz symbol budynku, np. C-13” → V2, mała karta także przy TalkBack, US1.1,
     * US2.8.
     */
    @Test
    fun us1_1_search_talkback_compact() {
        choice.savedChoice.value = false
        showApp()
        compose.onNode(hasSetTextAction()).performTextInput("A-1")
        pickA1()
        checkCompact("wynik wyszukiwania przy TalkBack")
    }
}
