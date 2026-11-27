/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import android.graphics.PointF
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.dostepnykampus.app.navigation.AppScreen
import io.github.dostepnykampus.core.data.barrier.AcceptedBarrier
import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.submission.Severity
import io.github.dostepnykampus.feature.map.CampusMapScreen
import io.github.dostepnykampus.feature.map.CampusMapUiState
import io.github.dostepnykampus.feature.map.MapLibreMapView
import io.github.dostepnykampus.feature.map.camera.CameraMotion
import io.github.dostepnykampus.feature.map.camera.CameraTransition
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.camera.MapInset
import io.github.dostepnykampus.feature.map.campus.CampusBounds
import io.github.dostepnykampus.feature.map.campus.CampusMapData
import io.github.dostepnykampus.feature.map.campus.MapSelection
import io.github.dostepnykampus.feature.map.campus.OUTLINE_NUMBER
import io.github.dostepnykampus.feature.map.campus.SelectedEntrance
import io.github.dostepnykampus.feature.map.campus.campusMapData
import io.github.dostepnykampus.feature.map.campus.withBarriers
import io.github.dostepnykampus.feature.map.navigation.MapFocus
import io.github.dostepnykampus.feature.map.navigation.MapRoute
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE
import org.maplibre.android.maps.MapView
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

/**
 * Sterownik kamery na prawdziwym MapLibre: wynik wyszukiwania z innego terenu to przelot, który kończy się nad
 * budynkiem w granicach nowego terenu. Granice nowego terenu przychodzą w trakcie przelotu, a MapLibre robi skok
 * kamery przy ich ustawieniu, więc test łapie przelot zatrzymany z dala od celu.
 *
 * Wymagania: US1.8, NFR-56, BR-G5
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class CameraFlightDeviceTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var catalog: BuildingCatalogRepository

    @Before
    fun injectDependencies() {
        hiltRule.inject()
    }

    /**
     * Wiersz: V1, wynik wyszukiwania „T-16” z terenu „Wittigowo” przy mapie na „Kampusie głównym”: po przelocie
     * środek mapy leży w prostokącie Wittigowa, a kontur T-16 jest w widoku mapy.
     *
     * Wymagania: US1.8, NFR-56, BR-G5
     */
    @Test
    fun flightLandsOnBuilding() {
        val data = runBlocking { campusMapData(catalog.catalog()) }
        val target = data.areas.first { it.name == TARGET_AREA }.bounds
        val outline = requireNotNull(data.outlinesBySymbol[TARGET_BUILDING]) { "Brak konturu $TARGET_BUILDING" }
        dismissHelpIfShown()
        val map = awaitReadyMap()
        val tracker = MoveTracker()
        composeRule.runOnUiThread { tracker.attach(map) }
        try {
            composeRule.onNode(hasSetTextAction()).performTextInput(TARGET_BUILDING)
            val result = hasContentDescription("$TARGET_BUILDING, ", substring = true)
            composeRule.waitUntil(READY_TIMEOUT_MS) { nodeExists(result) }
            composeRule.onNode(result).performClick()
            composeRule.waitUntil(FLIGHT_TIMEOUT_MS) { tracker.starts.get() > 0 && !tracker.isMoving.get() }
            composeRule.waitUntil(FLIGHT_TIMEOUT_MS) { isSettled(map, tracker) }
            val (center, visible) =
                composeRule.runOnUiThread {
                    requireNotNull(map.cameraPosition.target) to map.projection.visibleRegion.latLngBounds
                }
            val middle =
                LatLng(latitude = (outline.south + outline.north) / 2, longitude = (outline.west + outline.east) / 2)
            val moves = "ruchy kamery: ${tracker.starts.get()}"
            assertTrue("Środek mapy poza terenem $TARGET_AREA: $center, $target, $moves", center.isInside(target))
            assertTrue("$TARGET_BUILDING poza widokiem: $middle, widok $visible, $moves", visible.contains(middle))
        } finally {
            composeRule.runOnUiThread { tracker.detach(map) }
        }
    }

    /** Wiersz: V1 nad konturem budynku podstawowego po powiększeniu do 17. Wymagania: US16.2, BR-G1 */
    @Test
    fun showsMissingPlanOnMap() {
        val symbol = "A-4"
        val verifiedCatalog = runBlocking { catalog.catalog() }.copy(hasServerList = true)
        val data = campusMapData(verifiedCatalog)
        val point = data.labelPointsBySymbol.getValue(symbol)
        val target = LatLng(latitude = point.latitude, longitude = point.longitude)
        val outline = data.outlines.indexOfFirst { symbol in it }
        val focus = MapFocus.Building(symbol, command = 1L)
        showMap(CampusMapUiState(catalog = verifiedCatalog, mapData = data), focus)
        val map = awaitReadyMap()
        val notice = hasText("Nie mamy planu pięter tego budynku.")
        composeRule.waitUntil(READY_TIMEOUT_MS) { nodeExists(notice) }
        composeRule.onNode(hasContentDescription("Pomniejsz")).performClick()
        composeRule.waitUntil(READY_TIMEOUT_MS) { !nodeExists(notice) }
        composeRule.onNode(hasContentDescription("Powiększ")).performClick()
        composeRule.waitUntil(READY_TIMEOUT_MS) { nodeExists(notice) }
        composeRule.waitUntil(READY_TIMEOUT_MS) {
            composeRule.runOnUiThread {
                val pixel = map.projection.toScreenLocation(target)
                map.cameraPosition.zoom >= FLOOR_PLAN_ZOOM &&
                    map.queryRenderedFeatures(pixel, BUILDINGS_LAYER).any {
                        it.getNumberProperty(OUTLINE_NUMBER)?.toInt() == outline
                    }
            }
        }
        DeviceEvidence.record("v1-basic-no-floor-plan")
    }

    @Test
    fun barrierShowsBesidePin() {
        val entries = runBlocking { catalog.catalog() }.copy(hasServerList = true)
        val entrance = entries.entrances.first { it.buildingSymbol == "A-1" && it.isForVisitors }
        val barrier =
            AcceptedBarrier(
                id = "bariera-wejscia-test",
                location = entrance.location,
                severity = Severity.HINDRANCE,
                type = null,
                buildingSymbol = entrance.buildingSymbol,
                verifiedAt = null,
                entranceId = entrance.id,
            )
        val barriers = listOf(barrier)
        val data = campusMapData(entries).withBarriers(barriers)
        showMap(
            CampusMapUiState(catalog = entries, mapData = data, barriers = barriers),
            MapFocus.Entrance(id = entrance.id, buildingSymbol = entrance.buildingSymbol),
        )
        composeRule.waitUntil(READY_TIMEOUT_MS) { nodeExists(hasText("Bariery przy tym wejściu")) }
        awaitPinAndBarrier(map = awaitReadyMap(), entrance = entrance, barrierId = barrier.id)
        showMapForEvidence(data = data, entrance = entrance)
        awaitPinAndBarrier(map = awaitReadyMap(), entrance = entrance, barrierId = barrier.id)
        DeviceEvidence.record("v1-barrier-beside-pin")
    }

    private fun awaitPinAndBarrier(map: MapLibreMap, entrance: Entrance, barrierId: String) {
        val location = LatLng(latitude = entrance.location.latitude, longitude = entrance.location.longitude)
        composeRule.waitUntil(READY_TIMEOUT_MS) {
            composeRule.runOnUiThread {
                val density = composeRule.activity.resources.displayMetrics.density
                val anchor = map.projection.toScreenLocation(location)
                val sign = PointF(anchor.x + BARRIER_PIN_GAP_DP * density, anchor.y)
                val pin = PointF(anchor.x, anchor.y - PIN_BODY_DP * density)
                map.queryRenderedFeatures(pin, "wejscie_pinezka").isNotEmpty() &&
                    map.queryRenderedFeatures(sign, "bariery").any { it.getStringProperty("id") == barrierId }
            }
        }
    }

    private fun showMapForEvidence(data: CampusMapData, entrance: Entrance) {
        val pin =
            SelectedEntrance(
                id = entrance.id,
                location = entrance.location,
                label = entrance.name ?: "Wejście",
                buildingSymbol = entrance.buildingSymbol,
            )
        val camera = MapCamera(center = entrance.location, zoom = PIN_ZOOM)
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                DostepnyKampusTheme {
                    MapLibreMapView.Show(
                        data = data,
                        selection = MapSelection(entrance = pin),
                        move = CameraTransition(camera = camera, motion = CameraMotion.NONE, durationMillis = 0L),
                        inset = MapInset(),
                        onCameraIdle = {},
                        onBuildingsTouch = {},
                        onSignTouch = { _, _ -> },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }

    private fun showMap(state: CampusMapUiState, focus: MapFocus) {
        composeRule.activityRule.scenario.onActivity { activity ->
            val roles = EntryPointAccessors.fromActivity<MainActivityEntryPoint>(activity).roles()
            activity.setContent {
                DostepnyKampusTheme {
                    AppShell(firstLaunch = false, roles = roles, onFirstHelpLeft = {}) { route ->
                        if (route is MapRoute) {
                            CampusMapScreen(
                                state = state,
                                onListChange = {},
                                onSearch = {},
                                onBuildingClick = {},
                                focus = focus,
                            )
                        } else {
                            AppScreen(route)
                        }
                    }
                }
            }
        }
    }

    private fun isSettled(map: MapLibreMap, tracker: MoveTracker): Boolean {
        val before = tracker.starts.get()
        val position = composeRule.runOnUiThread { map.cameraPosition.target }
        Thread.sleep(SETTLE_PAUSE_MS)
        val after = composeRule.runOnUiThread { map.cameraPosition.target }
        return !tracker.isMoving.get() && tracker.starts.get() == before && position == after
    }

    private fun dismissHelpIfShown() {
        val skip = hasText(SKIP_HELP_TEXT)
        val area = hasContentDescription(MAIN_AREA_BUTTON_DESCRIPTION)
        composeRule.waitUntil(READY_TIMEOUT_MS) { nodeExists(skip) || nodeExists(area) }
        if (nodeExists(skip)) composeRule.onNode(skip).performClick()
        composeRule.waitUntil(READY_TIMEOUT_MS) { nodeExists(area) }
    }

    private fun awaitReadyMap(): MapLibreMap {
        val mapRef = AtomicReference<MapLibreMap?>(null)
        composeRule.waitUntil(READY_TIMEOUT_MS) {
            composeRule.runOnUiThread {
                val root = composeRule.activity.findViewById<View>(android.R.id.content)
                findAttachedMapView(root)?.getMapAsync(mapRef::set)
            }
            mapRef.get()?.let { map -> composeRule.runOnUiThread { map.style?.isFullyLoaded == true } } == true
        }
        composeRule.waitForIdle()
        return requireNotNull(mapRef.get())
    }

    private fun findAttachedMapView(root: View): MapView? {
        val group = root as? ViewGroup
        return when {
            root is MapView && root.isAttachedToWindow && root.width > 0 && root.height > 0 -> root
            group == null -> null
            else -> (0 until group.childCount).firstNotNullOfOrNull { findAttachedMapView(group.getChildAt(it)) }
        }
    }

    private fun nodeExists(matcher: SemanticsMatcher): Boolean =
        composeRule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()

    private fun LatLng.isInside(bounds: CampusBounds): Boolean =
        longitude in bounds.west..bounds.east && latitude in bounds.south..bounds.north

    private class MoveTracker {
        val isMoving = AtomicBoolean(false)
        val starts = AtomicInteger(0)
        private val startListener =
            MapLibreMap.OnCameraMoveStartedListener { reason ->
                isMoving.set(true)
                if (reason != REASON_API_GESTURE) starts.incrementAndGet()
            }
        private val idleListener = MapLibreMap.OnCameraIdleListener { isMoving.set(false) }

        fun attach(map: MapLibreMap) {
            map.addOnCameraMoveStartedListener(startListener)
            map.addOnCameraIdleListener(idleListener)
        }

        fun detach(map: MapLibreMap) {
            map.removeOnCameraMoveStartedListener(startListener)
            map.removeOnCameraIdleListener(idleListener)
        }
    }

    private companion object {
        const val TARGET_BUILDING = "T-16"
        const val TARGET_AREA = "Wittigowo"
        const val BUILDINGS_LAYER = "budynki"
        const val FLOOR_PLAN_ZOOM = 17.0
        const val BARRIER_PIN_GAP_DP = 48f
        const val PIN_BODY_DP = 24f
        const val PIN_ZOOM = 19.0
        const val SKIP_HELP_TEXT = "Pomiń instrukcję"
        const val MAIN_AREA_BUTTON_DESCRIPTION = "Teren: Kampus główny. Zmień teren"
        const val READY_TIMEOUT_MS = 30_000L
        const val FLIGHT_TIMEOUT_MS = 10_000L
        const val SETTLE_PAUSE_MS = 500L
    }
}
