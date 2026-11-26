/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.grounds

import android.Manifest
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.dostepnykampus.app.TestAppScreen
import io.github.dostepnykampus.app.appMapViewModel
import io.github.dostepnykampus.app.quick.showQuickApp
import io.github.dostepnykampus.core.data.building.BUILDINGS_ASSET
import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.building.BuildingFloors
import io.github.dostepnykampus.core.location.LocationSwitch
import io.github.dostepnykampus.core.location.PositionFix
import io.github.dostepnykampus.core.location.PositionSource
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.feature.capture.location.BuildingLookup
import io.github.dostepnykampus.feature.capture.photo.PhotoTranscoder
import io.github.dostepnykampus.feature.capture.quick.QuickLookup
import io.github.dostepnykampus.feature.capture.quick.QuickReportModels
import io.github.dostepnykampus.feature.capture.quick.QuickSendViewModel
import io.github.dostepnykampus.feature.map.camera.CampusMapView
import io.github.dostepnykampus.feature.map.campus.CampusBounds
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.coroutines.EmptyCoroutineContext

/** Mapa i zgłoszenie szybkie na prawdziwych konturach PWr dla pozycji daleko od budynków. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class FarFromBuildingsTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var outlines = emptyMap<String, CampusBounds>()
    private var lookup: QuickLookup by mutableStateOf(QuickLookup.Locating)
    private val fakeMap = CampusMapView { shown, _, _, _, _, _, _, _ -> outlines = shown.outlinesBySymbol }

    /**
     * Odwiedzający na terenie PWr, 60 m od najbliższego konturu, widzi mapę z konturami budynków PWr, a pozycja
     * jest przyjęta do zgłoszenia szybkiego, którego krok 1 otwiera „Zgłoś barierę”.
     * Scenariusz: budynki-bez-szczegolow.feature: Mapa działa daleko od budynków PWr
     */
    @Test
    fun reportsFarFromBuildings() {
        val mapModel = compose.activity.appMapViewModel()
        val quick = quickModel()
        val models = QuickReportModels(photoTranscoder = PhotoTranscoder(), send = quick)
        shadowOf(compose.activity.application).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        compose.showQuickApp(models) { route ->
            LaunchedEffect(Unit) { quick.lookup.collect { lookup = it } }
            TestAppScreen(route = route, mapViewModel = mapModel, mapView = fakeMap)
        }
        compose.waitUntil(timeoutMillis = WAIT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            outlines.isNotEmpty() && lookup !is QuickLookup.Locating
        }

        assertTrue("kontur budynku A-1 z katalogu PWr na mapie", "A-1" in outlines)
        val position = (lookup as? QuickLookup.Found)?.position
        assertEquals("pozycja przyjęta na terenie PWr", FAR_FROM_BUILDINGS, position?.point)
        assertFalse("pozycja poza budynkiem", position?.isInBuilding != false)
        compose.onNodeWithText("Zgłoś barierę").performClick()
        compose.onNodeWithText("Krok 1 z 3").assertExists("krok 1 zgłoszenia szybkiego")
    }

    private fun quickModel(): QuickSendViewModel {
        val assets = compose.activity.assets
        val catalog =
            BuildingCatalogRepository(EmptyCoroutineContext) {
                assets.open(BUILDINGS_ASSET).bufferedReader().use { it.readText() }
            }
        return QuickSendViewModel(
            positions = PositionSource { flowOf(PositionFix(FAR_FROM_BUILDINGS, ACCURACY, elapsedMillis = 0)) },
            locationSwitch = LocationSwitch { flowOf(true) },
            saver = { _, _, _ -> },
            networkStatus = { flowOf(true) },
            buildingFloors = BuildingFloors { null },
            buildingLookup = BuildingLookup(catalog, EmptyCoroutineContext),
        ).apply { start(entranceId = null) }
    }

    private companion object {
        const val WAIT_MS = 10_000L
        const val ACCURACY = 6.0
        val FAR_FROM_BUILDINGS = GeoPoint(longitude = 17.05519, latitude = 51.10351)
    }
}
