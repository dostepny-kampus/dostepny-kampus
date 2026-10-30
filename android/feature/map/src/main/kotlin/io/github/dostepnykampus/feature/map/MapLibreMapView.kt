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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.feature.map.data.CampusMapData
import io.github.dostepnykampus.feature.map.data.symbolsOf
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

private const val LABEL_TEXT_SP = 14f
private const val LABEL_MAX_FONT_SCALE = 1.4f
private const val TOUCH_TOLERANCE_DP = 24f

/** Widok MapLibre Android bez kluczy i kont (ADR-0005); logo i podpis biblioteki zastępuje podpis ekranu. */
internal object MapLibreMapView : CampusMapView {
    @Composable
    override fun Show(
        data: CampusMapData,
        camera: MapCamera,
        onCameraIdle: (MapCamera) -> Unit,
        onBuildingsTouch: (List<String>) -> Unit,
        modifier: Modifier,
    ) {
        // Bez biblioteki natywnej (np. testy JVM, telefon bez wspieranej architektury) mapa się nie rysuje,
        // a ekran z przyciskami działa dalej.
        if (!remember { nativeMapLoads() }) return
        val context = LocalContext.current
        val density = LocalDensity.current
        val colors = DostepnyKampusTheme.mapColors
        val style = campusMapStyle(data = data, colors = colors.styleColors())
        // Podpis rośnie z czcionką telefonu najwyżej 1,4 raza (reguła 4 wymagań wspólnych).
        val labelSize = LABEL_TEXT_SP * density.density * density.fontScale.coerceAtMost(LABEL_MAX_FONT_SCALE)
        // Dotknięcie do 24 dp od konturu, połowa celu 48 dp, trafia w kontur (decyzja 85).
        val tolerance = TOUCH_TOLERANCE_DP * density.density
        val mapView =
            remember {
                MapLibre.getInstance(context)
                mapHttpClient.value
                val options = MapLibreMapOptions.createFromAttributes(context)
                MapView(context, options.logoEnabled(false).attributionEnabled(false))
            }
        FollowScreenLifecycle(mapView)
        var map by remember { mutableStateOf<MapLibreMap?>(null) }
        val currentCamera by rememberUpdatedState(camera)
        val cameraIdle by rememberUpdatedState(onCameraIdle)
        val buildingsTouch by rememberUpdatedState(onBuildingsTouch)
        DisposableEffect(mapView) {
            mapView.getMapAsync { ready ->
                // NFR-39: bez pobierania kafelków na zapas, tylko widoczne.
                ready.prefetchZoomDelta = 0
                ready.cameraPosition = currentCamera.toPosition()
                ready.addOnCameraIdleListener { cameraIdle(ready.cameraPosition.toCamera()) }
                ready.addOnMapClickListener { point ->
                    val at = ready.projection.toScreenLocation(point)
                    val area = RectF(at.x - tolerance, at.y - tolerance, at.x + tolerance, at.y + tolerance)
                    val features = ready.queryRenderedFeatures(area, BUILDINGS_LAYER)
                    val symbols = symbolsOf(features.mapNotNull { it.getStringProperty("label") })
                    if (symbols.isNotEmpty()) buildingsTouch(symbols)
                    symbols.isNotEmpty()
                }
                map = ready
            }
            onDispose { map = null }
        }
        DisposableEffect(map, style, labelSize) {
            map?.setStyle(Style.Builder().fromJson(style)) { loaded ->
                data.labels.forEach { label ->
                    loaded.addImage(label, labelImage(label, colors, labelSize))
                }
            }
            onDispose {}
        }
        DisposableEffect(map, camera) {
            val ready = map
            if (ready != null && ready.cameraPosition.toCamera() != camera) {
                ready.animateCamera(CameraUpdateFactory.newCameraPosition(camera.toPosition()))
            }
            onDispose {}
        }
        AndroidView(factory = { mapView }, modifier = modifier)
    }

    @Composable
    private fun FollowScreenLifecycle(mapView: MapView) {
        // Natywny widok idzie za cyklem życia ekranu, a po zejściu z ekranu zwalnia pamięć.
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        DisposableEffect(lifecycle, mapView) {
            val observer =
                LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_CREATE -> mapView.onCreate(null)
                        Lifecycle.Event.ON_START -> mapView.onStart()
                        Lifecycle.Event.ON_RESUME -> mapView.onResume()
                        Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                        Lifecycle.Event.ON_STOP -> mapView.onStop()
                        Lifecycle.Event.ON_DESTROY, Lifecycle.Event.ON_ANY -> Unit
                    }
                }
            lifecycle.addObserver(observer)
            onDispose {
                lifecycle.removeObserver(observer)
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onPause()
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStop()
                mapView.onDestroy()
            }
        }
    }

    private fun nativeMapLoads(): Boolean = runCatching { System.loadLibrary("maplibre") }.isSuccess
}
