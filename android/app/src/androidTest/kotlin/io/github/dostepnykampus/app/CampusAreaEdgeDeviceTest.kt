/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import android.graphics.PointF
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.feature.map.data.CampusBounds
import io.github.dostepnykampus.feature.map.data.campusMapData
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
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
     * Sprawdza na emulatorze, że przesuwanie mapy palcem w stronę sąsiedniego terenu zatrzymuje
     * kamerę na granicy „Kampusu głównego”, pokazuje przyciemnienie poza nim oraz wyświetla przycisk
     * przejścia do „Wittigowa”.
     *
     * Scenariusz: przegladanie-mapy.feature: Mapa nie wychodzi poza wybrany teren
     *
     * Wymagania: US1.7, BR-G5, NFR-39, NFR-41
     */
    @Test
    fun us1_7_gesture_stays_inside_area() {
        val areas = runBlocking { campusMapData(catalog.catalog()).areas }
        val mainBounds = areas.first { it.name == MAIN_AREA_NAME }.bounds
        dismissHelpIfShown()
        val (mapView, map) = awaitReadyMap(mainBounds)
        val tracker = MotionTracker()
        composeRule.runOnUiThread { tracker.attach(map) }
        try {
            awaitUiPause(INITIAL_SETTLE_PAUSE_MS)
            if (tracker.isMoving.get()) {
                composeRule.waitUntil(SWIPE_SETTLE_TIMEOUT_MS) { !tracker.isMoving.get() }
            }
            composeRule.waitForIdle()
            val swipeLine = eastSwipeLine(mapView)
            panEastUntilNeighbor(swipeLine, tracker)
            val edgeMatcher = neighborButtonMatcher()
            composeRule.onNode(edgeMatcher).assertIsDisplayed()
            val edgeTarget = composeRule.runOnUiThread { requireNotNull(map.cameraPosition.target) }
            assertAtEastEdgeWithinBounds(edgeTarget, mainBounds)
            repeat(EXTRA_SWIPES_AT_EDGE) {
                performEastSwipe(swipeLine, tracker)
                awaitUiPause(EXTRA_SETTLE_PAUSE_MS)
                composeRule.waitForIdle()
            }
            val afterExtra = composeRule.runOnUiThread { requireNotNull(map.cameraPosition.target) }
            assertAtEastEdgeWithinBounds(afterExtra, mainBounds)
            assertTrue(
                "Kamera wyszła poza krawędź: edge=$edgeTarget, after=$afterExtra",
                abs(afterExtra.longitude - edgeTarget.longitude) <= EDGE_TOLERANCE_DEGREES,
            )
            composeRule.onNode(edgeMatcher).assertIsDisplayed()
            assertOutsideAreaDimmed(mapView, map)
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
        val hasLayers = style.getLayer(OUTSIDE_LAYER) != null && style.getLayer(EDGE_LAYER) != null
        val position = map.cameraPosition
        val target = position.target
        return hasLayers && position.zoom >= MIN_READY_ZOOM && target != null && isInsideBounds(target, bounds)
    }

    private fun eastSwipeLine(mapView: MapView): Pair<Offset, Offset> {
        val rootBounds = composeRule.onRoot().fetchSemanticsNode().boundsInWindow
        val location = IntArray(WINDOW_COORDINATES_SIZE)
        val size =
            composeRule.runOnUiThread {
                mapView.getLocationInWindow(location)
                mapView.width to mapView.height
            }
        val left = location[0] - rootBounds.left
        val top = location[1] - rootBounds.top
        val y = top + size.second * SWIPE_Y_FRACTION
        return Offset(left + size.first * SWIPE_START_X_FRACTION, y) to
            Offset(left + size.first * SWIPE_END_X_FRACTION, y)
    }

    private fun panEastUntilNeighbor(swipeLine: Pair<Offset, Offset>, tracker: MotionTracker) {
        val edgeMatcher = neighborButtonMatcher()
        var swipes = 0
        while (swipes < MAX_PAN_SWIPES && !nodeExists(edgeMatcher)) {
            performEastSwipe(swipeLine, tracker)
            composeRule.waitForIdle()
            swipes++
        }
    }

    private fun performEastSwipe(swipeLine: Pair<Offset, Offset>, tracker: MotionTracker) {
        val idleBefore = tracker.idleCount.get()
        val moveBefore = tracker.moveCount.get()
        composeRule.onRoot().performTouchInput {
            swipe(start = swipeLine.first, end = swipeLine.second, durationMillis = SWIPE_DURATION_MS)
        }
        if (tracker.moveCount.get() > moveBefore) {
            composeRule.waitUntil(SWIPE_SETTLE_TIMEOUT_MS) {
                tracker.idleCount.get() > idleBefore && !tracker.isMoving.get()
            }
        } else {
            awaitUiPause(EDGE_BOUNCE_PAUSE_MS)
        }
    }

    private fun assertAtEastEdgeWithinBounds(target: LatLng, bounds: CampusBounds) {
        assertTrue("Środek mapy wyszedł poza teren: target=$target, bounds=$bounds", isInsideBounds(target, bounds))
        assertTrue(
            "Kamera nie osiągnęła wschodniej granicy: lon=${target.longitude}, east=${bounds.east}",
            target.longitude >= bounds.east - EDGE_TOLERANCE_DEGREES,
        )
    }

    private fun assertOutsideAreaDimmed(mapView: MapView, map: MapLibreMap) {
        composeRule.waitUntil(READY_TIMEOUT_MS) {
            composeRule.runOnUiThread {
                val style = map.style
                val outside = style?.getLayer(OUTSIDE_LAYER) as? FillLayer
                val edge = style?.getLayer(EDGE_LAYER) as? LineLayer
                val layersVisible =
                    outside != null &&
                        edge != null &&
                        outside.visibility.value != Property.NONE &&
                        (outside.fillOpacity.value ?: 0f) > 0f &&
                        edge.visibility.value != Property.NONE
                val y = mapView.height * CENTER_FRACTION
                val inside = map.queryRenderedFeatures(PointF(mapView.width * INSIDE_X_FRACTION, y), OUTSIDE_LAYER)
                val outsideHits =
                    map.queryRenderedFeatures(PointF(mapView.width * OUTSIDE_X_FRACTION, y), OUTSIDE_LAYER)
                layersVisible && inside.isEmpty() && outsideHits.isNotEmpty()
            }
        }
    }

    private fun awaitUiPause(delayMillis: Long) {
        val latch = CountDownLatch(1)
        Handler(Looper.getMainLooper()).postDelayed(latch::countDown, delayMillis)
        latch.await(delayMillis + LATCH_MARGIN_MS, TimeUnit.MILLISECONDS)
    }

    private fun nodeExists(matcher: SemanticsMatcher): Boolean =
        composeRule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()

    private fun neighborButtonMatcher(): SemanticsMatcher =
        hasContentDescription(NEIGHBOR_PREFIX, substring = true) and
            hasContentDescription(KILOMETER_SUFFIX, substring = true)

    private fun isInsideBounds(point: LatLng, bounds: CampusBounds): Boolean =
        point.longitude in (bounds.west - EDGE_TOLERANCE_DEGREES)..(bounds.east + EDGE_TOLERANCE_DEGREES) &&
            point.latitude in (bounds.south - EDGE_TOLERANCE_DEGREES)..(bounds.north + EDGE_TOLERANCE_DEGREES)

    private fun findAttachedMapView(root: View): MapView? {
        if (root is MapView && root.isAttachedToWindow && root.width > 0 && root.height > 0) return root
        val group = root as? ViewGroup ?: return null
        return (0 until group.childCount).firstNotNullOfOrNull { findAttachedMapView(group.getChildAt(it)) }
    }

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
        const val NEIGHBOR_PREFIX = "Tu kończy się teren Kampus główny. Obok: Wittigowo, "
        const val KILOMETER_SUFFIX = " km"
        const val OUTSIDE_LAYER = "teren-poza"
        const val EDGE_LAYER = "teren-granica"
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
        const val SWIPE_Y_FRACTION = 0.65f
        const val SWIPE_START_X_FRACTION = 0.78f
        const val SWIPE_END_X_FRACTION = 0.22f
        const val CENTER_FRACTION = 0.50f
        const val INSIDE_X_FRACTION = 0.25f
        const val OUTSIDE_X_FRACTION = 0.85f
    }
}
