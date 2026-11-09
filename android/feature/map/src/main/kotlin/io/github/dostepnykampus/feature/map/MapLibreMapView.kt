/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.designsystem.MapColors
import io.github.dostepnykampus.feature.map.data.CampusMapData
import io.github.dostepnykampus.feature.map.data.symbolsOf
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

private const val LABEL_TEXT_SP = 14f
private const val LABEL_MAX_FONT_SCALE = 1.4f
private const val TOUCH_TOLERANCE_DP = 24f

/**
 * Widok MapLibre Android bez kluczy i kont (ADR-0005); logo i podpis biblioteki zastępuje podpis ekranu.
 * Podpis konturu rośnie z czcionką telefonu najwyżej 1,4 raza (reguła 4 wymagań wspólnych). Dotknięcie do 24 dp od
 * konturu, połowa celu 48 dp, trafia w kontur (decyzja 85). Kafelki nie są pobierane na zapas, tylko widoczne
 * (NFR-39). Natywny widok powstaje i zwalnia pamięć raz, razem z ekranem; przy V2 jako arkuszu ten sam widok
 * przechodzi do innej sceny z innym cyklem życia, więc zmiana cyklu tylko zatrzymuje i wznawia mapę.
 */
object MapLibreMapView : CampusMapView {
    @Composable
    override fun Show(
        data: CampusMapData,
        camera: MapCamera,
        onCameraIdle: (MapCamera) -> Unit,
        onBuildingsTouch: (List<String>) -> Unit,
        modifier: Modifier,
    ) {
        if (!remember { isNativeMapAvailable() }) return
        val context = LocalContext.current
        val density = LocalDensity.current
        val colors = DostepnyKampusTheme.mapColors
        val style = campusMapStyle(data = data, colors = colors.styleColors())
        val labelSize = LABEL_TEXT_SP * density.density * density.fontScale.coerceAtMost(LABEL_MAX_FONT_SCALE)
        val tolerance = TOUCH_TOLERANCE_DP * density.density
        val mapView =
            remember {
                MapLibre.getInstance(context)
                mapHttpClient.value
                val options = MapLibreMapOptions.createFromAttributes(context)
                MapView(context, options.textureMode(true).logoEnabled(false).attributionEnabled(false))
            }
        FollowScreenLifecycle(mapView)
        var map by remember { mutableStateOf<MapLibreMap?>(null) }
        var size by remember { mutableStateOf(IntSize.Zero) }
        val limits = remember(data.bounds, size, density) { density.mapLimits(data.bounds, size) }
        val currentCamera by rememberUpdatedState(camera)
        val cameraIdle by rememberUpdatedState(onCameraIdle)
        val buildingsTouch by rememberUpdatedState(onBuildingsTouch)
        DisposableEffect(mapView) {
            mapView.getMapAsync { ready ->
                ready.prefetchZoomDelta = 0
                ready.cameraPosition = currentCamera.toPosition()
                ready.addOnCameraIdleListener { cameraIdle(ready.cameraPosition.toCamera()) }
                ready.addOnMapClickListener { point ->
                    val at = ready.projection.toScreenLocation(point)
                    val area = RectF(at.x - tolerance, at.y - tolerance, at.x + tolerance, at.y + tolerance)
                    val features = ready.queryRenderedFeatures(area, BUILDINGS_LAYER)
                    val symbols = symbolsOf(features.mapNotNull { it.getStringProperty("label") })
                    buildingsTouch(symbols)
                    symbols.isNotEmpty()
                }
                map = ready
            }
            onDispose { map = null }
        }
        DisposableEffect(map, style, labelSize) {
            map?.setStyle(Style.Builder().fromJson(style)) { loaded ->
                loaded.addMapImages(data = data, colors = colors, density = density.density, labelSize = labelSize)
            }
            onDispose {}
        }
        DisposableEffect(map, limits) {
            if (size != IntSize.Zero) limits?.let { map?.limitTo(it) }
            onDispose {}
        }
        DisposableEffect(map, camera) {
            val ready = map
            if (ready != null && ready.cameraPosition.toCamera() != camera) {
                ready.animateCamera(CameraUpdateFactory.newCameraPosition(camera.toPosition()))
            }
            onDispose {}
        }
        AndroidView(factory = { mapView }, modifier = modifier.onSizeChanged { size = it })
    }

    @Composable
    private fun FollowScreenLifecycle(mapView: MapView) {
        DisposableEffect(mapView) {
            mapView.onCreate(null)
            onDispose { mapView.onDestroy() }
        }
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        DisposableEffect(lifecycle, mapView) {
            val observer =
                LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_START -> mapView.onStart()
                        Lifecycle.Event.ON_RESUME -> mapView.onResume()
                        Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                        Lifecycle.Event.ON_STOP -> mapView.onStop()
                        Lifecycle.Event.ON_CREATE, Lifecycle.Event.ON_DESTROY, Lifecycle.Event.ON_ANY -> Unit
                    }
                }
            lifecycle.addObserver(observer)
            onDispose {
                lifecycle.removeObserver(observer)
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onPause()
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStop()
            }
        }
    }

    private fun MapLibreMap.limitTo(limits: MapLimits) {
        val bounds = limits.bounds
        val target =
            LatLngBounds.from(
                latNorth = bounds.north,
                lonEast = bounds.east,
                latSouth = bounds.south,
                lonWest = bounds.west,
            )
        setLatLngBoundsForCameraTarget(target)
        setMinZoomPreference(limits.minZoom)
    }

    private fun isNativeMapAvailable(): Boolean = runCatching { System.loadLibrary("maplibre") }.isSuccess
}

// Podpisy konturów, znaki wejść oraz pinezka i podpis zaznaczonego wejścia: obrazy, bo napisy ze stylu wymagają sieci.
internal fun Style.addMapImages(data: CampusMapData, colors: MapColors, density: Float, labelSize: Float) {
    data.labels.forEach { label -> addImage(label, labelImage(label, colors, labelSize)) }
    val selected = data.selectedEntrance
    entranceImages(selected = selected, colors = colors, density = density, labelSize = labelSize)
        .forEach { (name, image) -> addImage(name, image) }
}
