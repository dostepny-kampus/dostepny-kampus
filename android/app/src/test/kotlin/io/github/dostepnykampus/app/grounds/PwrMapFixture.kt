/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.grounds

import android.net.ConnectivityManager
import android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
import android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import io.github.dostepnykampus.app.AppShell
import io.github.dostepnykampus.app.TestAppScreen
import io.github.dostepnykampus.app.TestRoles
import io.github.dostepnykampus.app.appMapViewModel
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.feature.map.camera.CampusMapView
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.campus.CampusMapData
import org.junit.Before
import org.junit.Rule
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowNetworkCapabilities

internal const val MAP_TAG = "fake-map"

/**
 * Powłoka aplikacji z mapą V1 na danych PWr i z widokiem zastępczym zamiast MapLibre, który zapamiętuje dane,
 * kamerę i wywołania zwrotne mapy.
 *
 * Wymagania: US1.7, US1.8
 */
open class PwrMapFixture {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    protected var data: CampusMapData? = null
    protected var camera: MapCamera? = null
    protected var touch: (List<String>) -> Unit = {}
    protected var idle: (MapCamera) -> Unit = {}
    private val fakeMap =
        CampusMapView { shown, _, move, _, onIdle, onTouch, _, modifier ->
            data = shown
            camera = move.camera
            idle = onIdle
            touch = onTouch
            Box(modifier.testTag(MAP_TAG))
        }

    @Before
    fun goOnline() {
        val connectivity = compose.activity.getSystemService(ConnectivityManager::class.java)
        val capabilities = ShadowNetworkCapabilities.newInstance()
        listOf(NET_CAPABILITY_INTERNET, NET_CAPABILITY_VALIDATED).forEach(shadowOf(capabilities)::addCapability)
        shadowOf(connectivity).setNetworkCapabilities(connectivity.activeNetwork, capabilities)
    }

    protected fun showApp() {
        val mapModel = compose.activity.appMapViewModel()
        compose.setContent {
            DostepnyKampusTheme {
                AppShell(firstLaunch = false, roles = TestRoles(), onFirstHelpLeft = {}) {
                    TestAppScreen(route = it, mapViewModel = mapModel, mapView = fakeMap)
                }
            }
        }
        compose.waitUntil(timeoutMillis = DATA_TIMEOUT) { data != null }
        compose.waitForIdle()
    }

    private companion object {
        const val DATA_TIMEOUT = 10_000L
    }
}
