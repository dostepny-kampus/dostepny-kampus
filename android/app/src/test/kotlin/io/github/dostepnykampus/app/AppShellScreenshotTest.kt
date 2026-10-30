/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.data.account.GuestRoleProvider
import io.github.dostepnykampus.core.data.buildings.BuildingCatalogRepository
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.feature.map.CampusMapScreen
import io.github.dostepnykampus.feature.map.CampusMapView
import io.github.dostepnykampus.feature.map.data.CampusMapData
import io.github.dostepnykampus.feature.map.navigation.MapRoute
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Zrzuty oboczki z ekranami: V1 z przyciskami mapy i podpisem OSM, V2 jako arkusz nad V1 i pasek nawigacji.
 * Natywnej mapy MapLibre w testach JVM nie ma: zamiast niej jest tło lądu z palety mapy, bez konturów i ulic.
 * Katalog to „Budynek testowy” C-13, nie dane PWr.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class AppShellScreenshotTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var touch: (List<String>) -> Unit = {}
    private val landOnly =
        CampusMapView { _: CampusMapData, _, _, onBuildingsTouch, modifier: Modifier ->
            touch = onBuildingsTouch
            Box(modifier = modifier.fillMaxSize().background(DostepnyKampusTheme.mapColors.lad))
        }
    private val repository =
        BuildingCatalogRepository(ioContext = EmptyCoroutineContext) {
            """{"type":"FeatureCollection","attribution":"© OpenStreetMap contributors","features":[""" +
                """{"type":"Feature","properties":{"kind":"building","symbols":["C-13"],"osm_id":"way/1"},""" +
                """"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}}]}"""
        }

    private fun capture(isSheetOpen: Boolean = true) {
        compose.setContent {
            DostepnyKampusTheme {
                AppShell(firstLaunch = false, roles = GuestRoleProvider, onFirstHelpLeft = {}) { route ->
                    if (route is MapRoute) {
                        CampusMapScreen(
                            repository = repository,
                            onBuildingClick = { openBuilding(it) },
                            contentPadding = LocalSheetPadding.current,
                            mapView = landOnly,
                        )
                    } else {
                        AppScreen(route)
                    }
                }
            }
        }
        compose.waitForIdle()
        if (isSheetOpen) compose.runOnUiThread { touch(listOf("C-13")) }
        compose.onRoot().captureRoboImage()
    }

    // V1: przyciski mapy, podpis OSM i pasek dolny, NFR-16, NFR-37.
    @Test
    fun nfr37_v1_jasny() {
        capture(isSheetOpen = false)
    }

    // V2 jako arkusz do połowy ekranu nad V1, decyzja 48, NFR-16.
    @Test
    fun nfr16_v2_arkusz_jasny() {
        capture()
    }

    @Test
    fun nfr16_v2_arkusz_ciemny() {
        RuntimeEnvironment.setQualifiers("+night")
        capture()
    }

    // Czcionka 200 %: nagłówek, uchwyt i pasek dolny, NFR-20.
    @Test
    fun nfr20_v2_arkusz_200() {
        RuntimeEnvironment.setFontScale(2f)
        capture()
    }

    // Okno ≥ 600 dp: pasek z boku, a arkusz dalej od dołu, decyzja 69.
    @Test
    @Config(qualifiers = "w640dp-h480dp-xhdpi")
    fun nfr51_v2_arkusz_szerokie() {
        capture()
    }
}
