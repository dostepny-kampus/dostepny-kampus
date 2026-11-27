/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import android.net.ConnectivityManager
import android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
import android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.feature.map.camera.CameraMotion
import io.github.dostepnykampus.feature.map.camera.CameraTransition
import io.github.dostepnykampus.feature.map.camera.CampusMapView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNetworkCapabilities

/**
 * Jedna czynność osoby przez prawdziwą oboczkę aplikacji (V1 z arkuszem V2) do podróbki widoku mapy, która zapisuje
 * każde polecenie ruchu podane mapie. Dane PWr z zasobów aplikacji. Liczy się nowy numer czynności polecenia: jedna
 * czynność daje najwyżej jeden. Pierwsze polecenie mapy, która dopiero powstała, to jej widok początkowy, a nie ruch.
 *
 * Wymagania: US2.8, NFR-56
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class OneMovePerActionTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val choice = TestMapListChoice()
    private val shown = mutableListOf<CameraTransition>()
    private var touch: (List<String>) -> Unit = {}
    private val fakeMap =
        CampusMapView { _, _, move, _, _, onBuildingsTouch, _, modifier: Modifier ->
            shown += move
            touch = onBuildingsTouch
            Box(modifier)
        }

    /** Wiersz: V1, dotknięcie konturu → mała karta V2 i najwyżej jeden ruch mapy. Wymagania: US2.8, NFR-56 */
    @Test
    fun tapMovesAtMostOnce() {
        val moves = movesAfter { compose.runOnUiThread { touch(listOf("A-1")) } }
        assertTrue("Najwyżej jeden ruch: $moves", moves.size <= 1)
    }

    /** Wiersz: V1, wynik wyszukiwania budynku obok → jeden ruch mapy nad kartę. Wymagania: US1.1, US2.8, NFR-56 */
    @Test
    fun searchMovesOnce() {
        val moves = movesAfter { search("A-2") }
        assertEquals("Jeden ruch: $moves", 1, moves.size)
    }

    /** Wiersz: V1, wynik wyszukiwania z innego terenu → jeden przelot. Wymagania: US1.8, NFR-56 */
    @Test
    fun otherAreaFliesOnce() {
        val moves = movesAfter { search("T-16") }
        assertEquals("Jeden przelot: $moves", listOf(CameraMotion.FLIGHT), moves.map { it.motion })
    }

    /** Wiersz: V1, budynek z listy budynków → mapa od razu na budynku, bez ruchu na ekranie. Wymagania: US2.8 */
    @Test
    fun listPickStandsStill() {
        choice.savedChoice.value = true
        val moves = movesAfter { click(building("A-5")) }
        assertTrue("Bez ruchu na ekranie: $moves", moves.all { it.motion == CameraMotion.NONE })
    }

    /**
     * Wiersz: V1, wybór terenu w panelu po zamknięciu karty V2 → ruch według drogi na teren, nie przelot przycisków
     * 300 ms. Wymagania: US1.7, NFR-56
     */
    @Test
    fun areaAfterCloseFollowsRoute() {
        startApp()
        compose.runOnUiThread { touch(listOf("A-1")) }
        click(hasText("Zamknij") and hasClickAction())
        compose.waitForIdle()
        val moves = movesAfter(isStarted = true) { chooseArea("Wittigowo") }
        assertEquals("Jeden przelot: $moves", listOf(CameraMotion.FLIGHT), moves.map { it.motion })
        assertNotEquals("Według drogi: $moves", CONTROL_FLIGHT_MILLIS, moves.single().durationMillis)
    }

    private fun movesAfter(isStarted: Boolean = false, action: () -> Unit): List<CameraTransition> {
        if (!isStarted) startApp()
        val before = shown.size
        val last = shown.lastOrNull()
        action()
        compose.waitForIdle()
        val after = shown.subList(fromIndex = before, toIndex = shown.size)
        val changes =
            after.fold(listOfNotNull(last)) { kept, move ->
                if (kept.lastOrNull()?.action == move.action) kept else kept + move
            }
        return changes.drop(1)
    }

    private fun chooseArea(name: String) {
        click(hasContentDescription("Zmień teren", substring = true) and hasClickAction())
        click(hasText(name) and isSelectable())
    }

    private fun search(symbol: String) {
        compose.onNode(hasSetTextAction()).performTextInput(symbol)
        click(building(symbol))
    }

    private fun building(symbol: String): SemanticsMatcher =
        hasClickAction() and (hasContentDescription(symbol) or hasContentDescription("$symbol, ", substring = true))

    private fun click(matcher: SemanticsMatcher) {
        compose.waitUntil(timeoutMillis = 5_000) { compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(matcher).performClick()
    }

    private fun startApp() {
        val connectivity = compose.activity.getSystemService(ConnectivityManager::class.java)
        val capabilities = ShadowNetworkCapabilities.newInstance()
        listOf(NET_CAPABILITY_INTERNET, NET_CAPABILITY_VALIDATED).forEach(shadowOf(capabilities)::addCapability)
        shadowOf(connectivity).setNetworkCapabilities(connectivity.activeNetwork, capabilities)
        val model = compose.activity.appMapViewModel(choice)
        compose.setContent {
            DostepnyKampusTheme {
                AppShell(firstLaunch = false, roles = TestRoles(), onFirstHelpLeft = {}) {
                    TestAppScreen(route = it, mapViewModel = model, mapView = fakeMap)
                }
            }
        }
        compose.waitUntil { choice.savedChoice.value == true || shown.isNotEmpty() }
        compose.waitForIdle()
    }

    private companion object {
        const val CONTROL_FLIGHT_MILLIS = 300L
    }
}
