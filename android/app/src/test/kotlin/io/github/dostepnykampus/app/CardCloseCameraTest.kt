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
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.feature.map.CampusMapViewModel
import io.github.dostepnykampus.feature.map.camera.CameraMotion
import io.github.dostepnykampus.feature.map.camera.CameraTransition
import io.github.dostepnykampus.feature.map.camera.CampusMapView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNetworkCapabilities

/**
 * Zamknięcie małej karty V2 przez prawdziwą oboczkę aplikacji do podróbki widoku mapy, która zapisuje każdy ruch
 * razem z zasłoniętą górą mapy. Arkusz karty znika z opóźnieniem, a mapa od pierwszej klatki po „Zamknij” liczy cel
 * pod górnym paskiem, więc kamera dostaje jeden ruch zamiast przelotu i drugiego ruchu po pasku.
 *
 * Wymagania: US2.8, NFR-56
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class CardCloseCameraTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val shown = mutableListOf<Pair<CameraTransition, Double>>()
    private var touch: (List<String>) -> Unit = {}
    private lateinit var mapViewModel: CampusMapViewModel
    private val fakeMap =
        CampusMapView { _, _, move, inset, _, onBuildingsTouch, _, modifier: Modifier ->
            shown += move to inset.top
            touch = onBuildingsTouch
            Box(modifier)
        }

    /** Telefon z internetem: w Robolectric sieć nie jest sprawdzona, więc V1 pokazałoby „Bez internetu”. */
    @Before
    fun startOnMap() {
        val connectivity = compose.activity.getSystemService(ConnectivityManager::class.java)
        val capabilities = ShadowNetworkCapabilities.newInstance()
        listOf(NET_CAPABILITY_INTERNET, NET_CAPABILITY_VALIDATED).forEach(shadowOf(capabilities)::addCapability)
        shadowOf(connectivity).setNetworkCapabilities(connectivity.activeNetwork, capabilities)
        mapViewModel = compose.activity.appMapViewModel()
        compose.setContent {
            DostepnyKampusTheme {
                AppShell(firstLaunch = false, roles = TestRoles(), onFirstHelpLeft = {}) {
                    TestAppScreen(route = it, mapViewModel = mapViewModel, mapView = fakeMap)
                }
            }
        }
        compose.waitUntil { shown.isNotEmpty() }
    }

    /**
     * Wiersz: dotknięcie budynku na V1 → mała karta V2, „Zamknij” → V1: każda klatka z celem po zamknięciu ma górę
     * mapy pod paskiem, bez klatki z górą zero.
     *
     * Wymagania: US2.8, NFR-56
     */
    @Test
    fun closesWithBarInset() {
        compose.runOnUiThread { touch(listOf("A-1")) }
        compose.waitForIdle()
        val before = shown.size
        compose.onNodeWithText("Zamknij").performClick()
        compose.waitForIdle()
        val (move, top) = shown.last()
        val tops = shown.subList(fromIndex = before, toIndex = shown.size).filter { it.first == move }.map { it.second }
        assertTrue("po zamknięciu mapa ma górę pod paskiem: $top", top > 0.0)
        assertEquals("góra przy celu po zamknięciu: $tops", setOf(top), tops.toSet())
    }

    /**
     * Wiersz: mała karta V2, „Zamknij” → V1: mapa dostaje cel i dopełnienie w jednym skoku bez ruchu na ekranie, bez
     * przelotu ani przesunięcia w czasie zjazdu karty.
     *
     * Wymagania: US2.8, NFR-56
     */
    @Test
    fun closesWithoutFlight() {
        val moves = movesAfterClose { compose.onNodeWithText("Zamknij").performClick() }
        assertTrue("po zamknięciu mapa dostaje widok bez arkusza: $moves", moves.isNotEmpty())
        assertEquals("po zamknięciu tylko skok: $moves", setOf(CameraMotion.NONE), moves.map { it.motion }.toSet())
    }

    /**
     * Wiersz: mała karta V2, „Wstecz” → V1: tak samo jak „Zamknij”, tylko skok bez ruchu na ekranie.
     *
     * Wymagania: US2.8, NFR-56
     */
    @Test
    fun backClosesWithoutFlight() {
        val back = compose.activity.onBackPressedDispatcher
        val moves = movesAfterClose { compose.runOnUiThread { back.onBackPressed() } }
        assertTrue("po „Wstecz” mapa dostaje widok bez arkusza: $moves", moves.isNotEmpty())
        assertEquals("po „Wstecz” tylko skok: $moves", setOf(CameraMotion.NONE), moves.map { it.motion }.toSet())
    }

    private fun movesAfterClose(close: () -> Unit): List<CameraTransition> {
        compose.runOnUiThread { touch(listOf("A-1")) }
        compose.waitForIdle()
        val card = shown.last().first
        val before = shown.size
        close()
        compose.waitForIdle()
        return shown.subList(fromIndex = before, toIndex = shown.size).map { it.first }.filter { it != card }.distinct()
    }
}
