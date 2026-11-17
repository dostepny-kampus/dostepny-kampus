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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.feature.map.CampusMapScreen
import io.github.dostepnykampus.feature.map.camera.CameraTransition
import io.github.dostepnykampus.feature.map.camera.CampusMapView
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.camera.MapMotion
import io.github.dostepnykampus.feature.map.campus.CampusMapData
import io.github.dostepnykampus.feature.map.navigation.MapFocus
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Przygotowanie ekranu V1 do testów: widok zastępczy zamiast MapLibre i katalog „Budynek testowy” (nie dane PWr).
 *
 * `KEYBOARD_HEIGHT`: Wysokość klawiatury w pikselach: przy gęstości zrzutów 2× to 300 dp, jak na telefonie.
 *
 * Wymagania: US2, US1
 */
open class CampusMapScreenFixture {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    protected val cameras = mutableListOf<MapCamera>()
    protected val moves = mutableListOf<CameraTransition>()
    protected val insets = mutableListOf<Double>()
    protected val tops = mutableListOf<Double>()
    protected val focuses = mutableListOf<String?>()
    protected val opened = mutableListOf<String>()
    protected var emptyTaps = 0
    protected var touch: (List<String>) -> Unit = {}
    protected val fakeMap =
        CampusMapView { _: CampusMapData, selection, move, inset, _, onBuildingsTouch, _, _: Modifier ->
            cameras += move.camera
            moves += move
            insets += inset.bottom
            tops += inset.top
            focuses += selection.building
            touch = onBuildingsTouch
        }
    protected val online = MutableStateFlow(true)
    internal val choice = TestMapListChoice()
    protected val showList = hasText("Pokaż listę") and hasClickAction()
    protected val showMap = hasText("Pokaż mapę") and hasClickAction()
    protected var ring = "[[0,0],[1,0],[1,1],[0,0]]"
    protected var entrances = ""
    protected val repository =
        BuildingCatalogRepository(ioContext = EmptyCoroutineContext) {
            """{"type":"FeatureCollection","attribution":"© OpenStreetMap contributors","features":[""" +
                """{"type":"Feature","properties":{"kind":"building","symbols":["C-13","C-14"],""" +
                """"display_name":"Budynek testowy",""" +
                """"osm_id":"way/1"},""" +
                """"geometry":{"type":"Polygon","coordinates":[$ring]}}$entrances]}"""
        }

    protected val viewModel by lazy { inlineMapViewModel(repository, { online }, choice) }

    @Composable
    protected fun Screen(fontScale: Float = 1f, focus: String? = null, sheet: Dp = 0.dp, motion: MapMotion? = null) {
        DostepnyKampusTheme {
            val density = Density(density = LocalDensity.current.density, fontScale = fontScale)
            CompositionLocalProvider(LocalDensity provides density) {
                CampusMapScreen(
                    onBuildingClick = { opened += it },
                    focus = focus?.let { MapFocus.Building(it) },
                    contentPadding = PaddingValues(bottom = sheet),
                    onEmptyMapClick = { emptyTaps++ },
                    mapView = fakeMap,
                    motion = motion,
                    viewModel = viewModel,
                )
            }
        }
    }

    protected fun enterTouchMode() {
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val root = view.javaClass.getMethod("getViewRootImpl").invoke(view)
            root?.javaClass?.getDeclaredMethod("ensureTouchMode", Boolean::class.javaPrimitiveType)?.run {
                isAccessible = true
                invoke(root, true)
            }
        }
    }

    protected fun keyboard(isOpen: Boolean) {
        compose.runOnUiThread { dispatchKeyboard(compose.activity, isOpen) }
        compose.waitForIdle()
    }
}
