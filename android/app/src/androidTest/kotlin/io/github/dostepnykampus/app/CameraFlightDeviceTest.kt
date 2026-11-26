/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.feature.map.campus.CampusBounds
import io.github.dostepnykampus.feature.map.campus.campusMapData
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
        const val SKIP_HELP_TEXT = "Pomiń instrukcję"
        const val MAIN_AREA_BUTTON_DESCRIPTION = "Teren: Kampus główny. Zmień teren"
        const val READY_TIMEOUT_MS = 30_000L
        const val FLIGHT_TIMEOUT_MS = 10_000L
        const val SETTLE_PAUSE_MS = 500L
    }
}
