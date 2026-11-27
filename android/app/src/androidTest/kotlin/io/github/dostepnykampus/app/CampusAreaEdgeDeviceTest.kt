/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.core.model.CampusBounds
import io.github.dostepnykampus.feature.map.campus.campusMapData
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import kotlin.math.abs

/** Test na emulatorze weryfikujący zatrzymanie gestu przesunięcia mapy na granicy terenu. */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class CampusAreaEdgeDeviceTest {
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
     * Sprawdza na emulatorze, że przesuwanie mapy palcem w stronę sąsiedniego terenu zatrzymuje wschodni brzeg
     * widoku na brzegu prostokąta „Kampusu głównego”, a mapa nie ma warstw granicy terenu ani wyblaknięcia.
     *
     * Scenariusz: przegladanie-mapy.feature: Mapa nie wychodzi poza wybrany teren
     * Scenariusz (częściowo): przegladanie-mapy.feature:
     *   Przy najmniejszym powiększeniu teren wypełnia mapę w krótszym wymiarze
     *
     * Wymagania: US1.7, BR-G5, NFR-39, NFR-41
     */
    @Test
    fun stopsGestureAtEdge() {
        val areas = runBlocking { campusMapData(catalog.catalog()).areas }
        val mainBounds = areas.first { it.name == MAIN_AREA_NAME }.bounds
        dismissHelpIfShown()
        val (mapView, map) = awaitReadyMap(mainBounds)
        val tracker = MotionTracker()
        composeRule.runOnUiThread { tracker.attach(map) }
        try {
            awaitUiPause(INITIAL_SETTLE_PAUSE_MS)
            composeRule.waitUntil(SWIPE_SETTLE_TIMEOUT_MS) { !tracker.isMoving.get() }
            composeRule.waitForIdle()
            val swipeLine = eastSwipeLine(mapView)
            panEastUntilEdge(swipeLine, tracker) { visibleEastOf(map) >= mainBounds.east - EDGE_TOLERANCE_DEGREES }
            val edgeTarget = composeRule.runOnUiThread { requireNotNull(map.cameraPosition.target) }
            assertAtEastEdgeWithinBounds(edgeTarget, visibleEastOf(map), mainBounds)
            repeat(EXTRA_SWIPES_AT_EDGE) {
                performEastSwipe(swipeLine, tracker)
                awaitUiPause(EXTRA_SETTLE_PAUSE_MS)
                composeRule.waitForIdle()
            }
            val afterExtra = composeRule.runOnUiThread { requireNotNull(map.cameraPosition.target) }
            assertAtEastEdgeWithinBounds(afterExtra, visibleEastOf(map), mainBounds)
            assertTrue(
                "Kamera wyszła poza krawędź: edge=$edgeTarget, after=$afterExtra",
                abs(afterExtra.longitude - edgeTarget.longitude) <= EDGE_TOLERANCE_DEGREES,
            )
            assertNoAreaLayers(map)
        } finally {
            composeRule.runOnUiThread { tracker.detach(map) }
        }
    }

    private fun dismissHelpIfShown() {
        val skipMatcher = hasText(SKIP_HELP_TEXT)
        val areaMatcher = hasContentDescription(MAIN_AREA_BUTTON_DESCRIPTION)
        composeRule.waitUntil(READY_TIMEOUT_MS) { nodeExists(skipMatcher) || nodeExists(areaMatcher) }
        if (nodeExists(skipMatcher)) composeRule.onNode(skipMatcher).performClick()
        composeRule.waitUntil(READY_TIMEOUT_MS) { nodeExists(areaMatcher) }
    }

    private fun awaitReadyMap(bounds: CampusBounds): Pair<MapView, MapLibreMap> {
        var foundView: MapView? = null
        composeRule.waitUntil(READY_TIMEOUT_MS) {
            foundView =
                composeRule.runOnUiThread {
                    val root = composeRule.activity.findViewById<View>(android.R.id.content)
                    findAttachedMapView(root)
                }
            foundView != null
        }
        val mapView = requireNotNull(foundView)
        val mapRef = AtomicReference<MapLibreMap?>(null)
        composeRule.runOnUiThread { mapView.getMapAsync(mapRef::set) }
        composeRule.waitUntil(READY_TIMEOUT_MS) {
            val readyMap = mapRef.get()
            readyMap != null && composeRule.runOnUiThread { isMapInitialized(readyMap, bounds) }
        }
        composeRule.waitForIdle()
        return mapView to requireNotNull(mapRef.get())
    }

    private fun isMapInitialized(map: MapLibreMap, bounds: CampusBounds): Boolean {
        val style = map.style?.takeIf { it.isFullyLoaded } ?: return false
        val target = map.cameraPosition.target?.takeIf { map.cameraPosition.zoom >= MIN_READY_ZOOM }
        return style.getLayer(BASEMAP_LAYER) != null && target != null && isInsideBounds(target, bounds)
    }

    private fun eastSwipeLine(mapView: MapView): Pair<Offset, Offset> {
        val location = IntArray(WINDOW_COORDINATES_SIZE)
        composeRule.runOnUiThread { mapView.getLocationOnScreen(location) }
        val y = location[1] + mapView.height * SWIPE_Y_FRACTION
        return Offset(location[0] + mapView.width * SWIPE_START_X_FRACTION, y) to
            Offset(location[0] + mapView.width * SWIPE_END_X_FRACTION, y)
    }

    private fun panEastUntilEdge(line: Pair<Offset, Offset>, tracker: MotionTracker, isAtEdge: () -> Boolean) {
        var swipes = 0
        while (swipes < MAX_PAN_SWIPES && !isAtEdge()) {
            performEastSwipe(line, tracker)
            composeRule.waitForIdle()
            swipes++
        }
    }

    private fun visibleEastOf(map: MapLibreMap): Double =
        composeRule.runOnUiThread { map.projection.visibleRegion.latLngBounds.longitudeEast }

    private fun performEastSwipe(swipeLine: Pair<Offset, Offset>, tracker: MotionTracker) {
        val idleBefore = tracker.idleCount.get()
        val moveBefore = tracker.moveCount.get()
        val (start, end) = swipeLine
        val command = "input swipe ${start.x} ${start.y} ${end.x} ${end.y} $SWIPE_DURATION_MS"
        val output = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        ParcelFileDescriptor.AutoCloseInputStream(output).use { it.readBytes() }
        if (tracker.moveCount.get() > moveBefore) {
            composeRule.waitUntil(SWIPE_SETTLE_TIMEOUT_MS) {
                tracker.idleCount.get() > idleBefore && !tracker.isMoving.get()
            }
        } else {
            awaitUiPause(EDGE_BOUNCE_PAUSE_MS)
        }
    }

    private fun assertAtEastEdgeWithinBounds(target: LatLng, visibleEast: Double, bounds: CampusBounds) {
        assertTrue("Środek mapy wyszedł poza teren: target=$target, bounds=$bounds", isInsideBounds(target, bounds))
        assertTrue(
            "Wschodni brzeg widoku nie doszedł do brzegu terenu: visible=$visibleEast, east=${bounds.east}",
            visibleEast >= bounds.east - EDGE_TOLERANCE_DEGREES,
        )
    }

    private fun assertNoAreaLayers(map: MapLibreMap) {
        val ids =
            composeRule.runOnUiThread {
                val style = map.style
                if (style == null) emptyList() else style.layers.map { it.id }
            }
        assertTrue("Mapa ma warstwę granicy terenu: $ids", ids.none { it.startsWith(AREA_LAYER_PREFIX) })
    }

    private fun awaitUiPause(delayMillis: Long) {
        val latch = CountDownLatch(1)
        Handler(Looper.getMainLooper()).postDelayed(latch::countDown, delayMillis)
        latch.await(delayMillis + LATCH_MARGIN_MS, TimeUnit.MILLISECONDS)
    }

    private fun nodeExists(matcher: SemanticsMatcher): Boolean =
        composeRule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()

    private fun isInsideBounds(point: LatLng, bounds: CampusBounds): Boolean =
        point.longitude in bounds.west - EDGE_TOLERANCE_DEGREES..bounds.east + EDGE_TOLERANCE_DEGREES &&
            point.latitude in bounds.south - EDGE_TOLERANCE_DEGREES..bounds.north + EDGE_TOLERANCE_DEGREES

    private fun findAttachedMapView(root: View): MapView? {
        val group = root as? ViewGroup
        return when {
            root is MapView && hasVisibleSize(root) -> root
            group == null -> null
            else -> (0 until group.childCount).firstNotNullOfOrNull { findAttachedMapView(group.getChildAt(it)) }
        }
    }

    private fun hasVisibleSize(view: View): Boolean = view.isAttachedToWindow && view.width > 0 && view.height > 0

    private class MotionTracker {
        val isMoving = AtomicBoolean(false)
        val moveCount = AtomicInteger(0)
        val idleCount = AtomicInteger(0)
        private val moveListener =
            MapLibreMap.OnCameraMoveStartedListener {
                isMoving.set(true)
                moveCount.incrementAndGet()
            }
        private val idleListener =
            MapLibreMap.OnCameraIdleListener {
                isMoving.set(false)
                idleCount.incrementAndGet()
            }

        fun attach(map: MapLibreMap) {
            map.addOnCameraMoveStartedListener(moveListener)
            map.addOnCameraIdleListener(idleListener)
        }

        fun detach(map: MapLibreMap) {
            map.removeOnCameraMoveStartedListener(moveListener)
            map.removeOnCameraIdleListener(idleListener)
        }
    }

    private companion object {
        const val MAIN_AREA_NAME = "Kampus główny"
        const val SKIP_HELP_TEXT = "Pomiń instrukcję"
        const val MAIN_AREA_BUTTON_DESCRIPTION = "Teren: Kampus główny. Zmień teren"
        const val BASEMAP_LAYER = "ulice"
        const val AREA_LAYER_PREFIX = "teren"
        const val EDGE_TOLERANCE_DEGREES = 1e-5
        const val MIN_READY_ZOOM = 10.0
        const val READY_TIMEOUT_MS = 30_000L
        const val SWIPE_SETTLE_TIMEOUT_MS = 3_000L
        const val INITIAL_SETTLE_PAUSE_MS = 300L
        const val EDGE_BOUNCE_PAUSE_MS = 150L
        const val EXTRA_SETTLE_PAUSE_MS = 250L
        const val LATCH_MARGIN_MS = 500L
        const val SWIPE_DURATION_MS = 280L
        const val MAX_PAN_SWIPES = 40
        const val EXTRA_SWIPES_AT_EDGE = 2
        const val WINDOW_COORDINATES_SIZE = 2

        /** Wysokość gestu nad podpisem OSM; początek gestu leży na lewo od przycisków mapy. */
        const val SWIPE_Y_FRACTION = 0.82f
        const val SWIPE_START_X_FRACTION = 0.45f
        const val SWIPE_END_X_FRACTION = 0.12f
    }
}
