/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import android.content.Context
import android.graphics.Bitmap
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dagger.hilt.android.EntryPointAccessors
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.designsystem.MapColors
import io.github.dostepnykampus.core.designsystem.nameRes
import io.github.dostepnykampus.core.model.Severity
import io.github.dostepnykampus.feature.map.data.BARRIER_ID
import io.github.dostepnykampus.feature.map.data.CampusBounds
import io.github.dostepnykampus.feature.map.data.CampusMapData
import io.github.dostepnykampus.feature.map.data.MapSelection
import io.github.dostepnykampus.feature.map.data.OUTLINE_NUMBER
import io.github.dostepnykampus.feature.map.data.SelectedEntrance
import io.github.dostepnykampus.feature.map.data.symbolsAt
import io.github.dostepnykampus.feature.map.style.BARRIERS_LAYER
import io.github.dostepnykampus.feature.map.style.BUILDINGS_LAYER
import io.github.dostepnykampus.feature.map.style.MapStyleColors
import io.github.dostepnykampus.feature.map.style.MapStyleTarget
import io.github.dostepnykampus.feature.map.style.MapTileHttpEntryPoint
import io.github.dostepnykampus.feature.map.style.SelectionLook
import io.github.dostepnykampus.feature.map.style.asStyleText
import io.github.dostepnykampus.feature.map.style.campusMapStyle
import io.github.dostepnykampus.feature.map.style.numbers
import io.github.dostepnykampus.feature.map.style.selectionLook
import io.github.dostepnykampus.feature.map.style.showSelection
import io.github.dostepnykampus.feature.map.style.styleBuilder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.module.http.HttpRequestUtil
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PaintPropertyValue
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource

private const val LABEL_TEXT_SP = 14f
private const val LABEL_MAX_FONT_SCALE = 1.4f
private const val TOUCH_TOLERANCE_DP = 24f

/**
 * Widok MapLibre Android bez kluczy i kont (ADR-0005); logo i podpis biblioteki zastępuje podpis ekranu. Podpis konturu
 * rośnie z czcionką najwyżej 1,4 raza, a dotknięcie do 24 dp od konturu trafia w kontur. Pobiera tylko widoczne
 * kafelki. Widok kamery zgłasza tylko po geście osoby; zmiana rozmiaru mapy przerywa ruch kamery i wraca do zleconej
 * kamery. Natywny widok powstaje i zwalnia pamięć raz, z ekranem; zmiana cyklu życia sceny tylko zatrzymuje i wznawia
 * mapę.
 *
 * Wymagania: NFR-39
 */
object MapLibreMapView : CampusMapView {
    @Composable
    override fun Show(
        data: CampusMapData,
        selection: MapSelection,
        camera: MapCamera,
        onCameraIdle: (MapCamera) -> Unit,
        onBuildingsTouch: (List<String>) -> Unit,
        onBarrierTouch: (String) -> Unit,
        modifier: Modifier,
    ) {
        if (!remember { isNativeMapAvailable() }) return
        val context = LocalContext.current
        val density = LocalDensity.current
        val colors = DostepnyKampusTheme.mapColors
        val styleColors = colors.styleColors()
        val style = rememberMapStyle(data = data, bounds = selection.bounds, colors = styleColors)
        val look = remember(data, selection, styleColors) { selectionLook(data, selection, styleColors) }
        val labelSize = LABEL_TEXT_SP * density.density * density.fontScale.coerceAtMost(LABEL_MAX_FONT_SCALE)
        val tolerance = TOUCH_TOLERANCE_DP * density.density
        val images = rememberMapImages(colors = colors, density = density.density, labelSize = labelSize)
        val mapView =
            remember {
                MapLibre.getInstance(context)
                val tiles = EntryPointAccessors.fromApplication<MapTileHttpEntryPoint>(context.applicationContext)
                HttpRequestUtil.setOkHttpClient(tiles.mapTileHttp().calls)
                MapView(context, northUpOptions(context))
            }
        FollowScreenLifecycle(mapView)
        var map by remember { mutableStateOf<MapLibreMap?>(null) }
        var size by remember { mutableStateOf(IntSize.Zero) }
        val limits = remember(selection.bounds, size, density) { density.mapLimits(selection.bounds, size) }
        val currentCamera by rememberUpdatedState(camera)
        val currentData by rememberUpdatedState(data)
        val cameraIdle by rememberUpdatedState(onCameraIdle)
        val buildingsTouch by rememberUpdatedState(onBuildingsTouch)
        val barrierTouch by rememberUpdatedState(onBarrierTouch)
        DisposableEffect(mapView) {
            mapView.getMapAsync { ready ->
                ready.prefetchZoomDelta = 0
                ready.cameraPosition = currentCamera.toPosition()
                ready.onPersonIdle { cameraIdle(it) }
                ready.onSignOrOutlinesTouch(tolerance, onBarrier = { barrierTouch(it) }) { numbers ->
                    currentData.symbolsAt(numbers).also { buildingsTouch(it) }.isNotEmpty()
                }
                map = ready
            }
            onDispose { map = null }
        }
        map.FollowStyle(
            style = style,
            data = data,
            look = look,
            entrance = selection.entrance,
            images = images,
        )
        DisposableEffect(map, limits) {
            if (size != IntSize.Zero) limits?.let { map?.limitTo(it) }
            onDispose {}
        }
        DisposableEffect(map, camera, limits) {
            val ready = map
            if (ready != null && ready.cameraPosition.toCamera() != camera) {
                ready.animateCamera(CameraUpdateFactory.newCameraPosition(camera.toPosition()))
            }
            onDispose {}
        }
        AndroidView(factory = { mapView }, modifier = modifier.onSizeChanged { size = it })
    }

    @Composable
    private fun MapLibreMap?.FollowStyle(
        style: String,
        data: CampusMapData,
        look: SelectionLook,
        entrance: SelectedEntrance?,
        images: MapImages,
    ) {
        val map = this
        var loaded by remember { mutableStateOf<Style?>(null) }
        val currentData by rememberUpdatedState(data)
        val currentLook by rememberUpdatedState(look)
        val currentEntrance by rememberUpdatedState(entrance)
        DisposableEffect(map, style, images) {
            loaded = null
            map?.setStyle(styleBuilder(style)) { ready ->
                ready.addMapImages(labels = currentData.labels, images = images)
                StyleTarget(ready).showSelection(currentLook, selectionImages(currentEntrance, images))
                loaded = ready
            }
            onDispose {}
        }
        DisposableEffect(loaded, look) {
            loaded?.let { StyleTarget(it).showSelection(look, selectionImages(currentEntrance, images)) }
            onDispose {}
        }
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

    private fun MapLibreMap.onSignOrOutlinesTouch(
        tolerance: Float,
        onBarrier: (String) -> Unit,
        isTaken: (List<Int>) -> Boolean,
    ) {
        addOnMapClickListener { point ->
            val at = projection.toScreenLocation(point)
            val area = RectF(at.x - tolerance, at.y - tolerance, at.x + tolerance, at.y + tolerance)
            val signs = queryRenderedFeatures(area, BARRIERS_LAYER)
            val barrier = signs.firstNotNullOfOrNull { it.getStringProperty(BARRIER_ID) }
            if (barrier == null) {
                val outlines = queryRenderedFeatures(area, BUILDINGS_LAYER)
                isTaken(outlines.mapNotNull { it.getNumberProperty(OUTLINE_NUMBER)?.toInt() })
            } else {
                onBarrier(barrier)
                true
            }
        }
    }

    private fun MapLibreMap.onPersonIdle(report: (MapCamera) -> Unit) {
        var isByPerson = false
        addOnCameraMoveStartedListener { reason -> isByPerson = reason == REASON_API_GESTURE }
        addOnCameraIdleListener {
            if (isByPerson) report(cameraPosition.toCamera())
            isByPerson = false
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

    /** Wczytany styl MapLibre [style]; zmiany warstw, źródeł i obrazów nie wczytują stylu od nowa. */
    private class StyleTarget(private val style: Style) : MapStyleTarget {
        override fun filter(layer: String, filter: JsonElement) {
            val expression = expressionOf(filter)
            when (val found = style.getLayer(layer)) {
                is FillLayer -> found.setFilter(expression)
                is LineLayer -> found.setFilter(expression)
                is SymbolLayer -> found.setFilter(expression)
                else -> Unit
            }
        }

        override fun paint(layer: String, property: String, value: JsonElement) {
            val styled: Any = if (value is JsonArray) expressionOf(value) else value.jsonPrimitive.content
            style.getLayer(layer)?.setProperties(PaintPropertyValue(property, styled))
        }

        override fun data(source: String, geoJson: String) {
            style.getSourceAs<GeoJsonSource>(source)?.setGeoJson(geoJson)
        }

        override fun image(name: String, image: Bitmap) = style.addImage(name, image)

        private fun expressionOf(value: JsonElement): Expression =
            if (value is JsonArray) {
                Expression.Converter.convert(value.asStyleText())
            } else {
                Expression.literal(value.jsonPrimitive.boolean)
            }
    }
}

/**
 * Ustawienia widoku mapy V1: bez logo i podpisu biblioteki, bez gestu obrotu i pochylenia. Północ jest zawsze
 * u góry, bo tak liczą strzałki przesuwania, granica terenu i przycisk sąsiedniego terenu, a [MapCamera] nie zna
 * obrotu. Przesuwanie i powiększanie gestem zostają.
 */
internal fun northUpOptions(context: Context): MapLibreMapOptions =
    MapLibreMapOptions
        .createFromAttributes(context)
        .textureMode(true)
        .logoEnabled(false)
        .attributionEnabled(false)
        .rotateGesturesEnabled(false)
        .tiltGesturesEnabled(false)

/**
 * Styl mapy jako napis JSON bez zaznaczenia budynku, wejścia i bariery. Powstaje od nowa tylko po zmianie danych
 * [data], granic terenu [bounds] albo barw [colors]; zaznaczenie i ruch kamery rekomponują widok mapy, ale stylu
 * nie budują. Funkcja [build] składa styl.
 */
@Composable
internal fun rememberMapStyle(
    data: CampusMapData,
    bounds: CampusBounds?,
    colors: MapStyleColors,
    build: (CampusMapData, CampusBounds?, MapStyleColors) -> String = { mapData, area, palette ->
        campusMapStyle(data = mapData, selection = MapSelection(bounds = area), colors = palette)
    },
): String = remember(data, bounds, colors) { build(data, bounds, colors) }

/**
 * Dodaje do stylu obrazy niezależne od zaznaczenia: podpisy konturów [labels], znaki wejść, pinezkę oraz znaki
 * i słowa uciążliwości barier, bo napisy ze stylu wymagają sieci.
 */
internal fun Style.addMapImages(labels: List<String>, images: MapImages) {
    labels.forEach { label -> addImage(label, labelImage(label, images.colors, images.labelSize)) }
    entranceImages(colors = images.colors, density = images.density).forEach { (name, image) -> addImage(name, image) }
    barrierImages(images).forEach { (name, image) -> addImage(name, image) }
}

/** Obrazy zależne od zaznaczonego wejścia [selected]: tylko podpis z jego nazwą. Wymagania: US3.3 */
internal fun selectionImages(selected: SelectedEntrance?, images: MapImages): Map<String, Bitmap> =
    entranceCaption(selected = selected, colors = images.colors, labelSize = images.labelSize)

/**
 * Wygląd obrazów mapy w barwach [colors], gęstości ekranu [density] i wysokości podpisu [labelSize] w pikselach,
 * ze słowami uciążliwości w języku ekranu; nowy tylko po zmianie któregoś z nich.
 */
@Composable
internal fun rememberMapImages(colors: MapColors, density: Float, labelSize: Float): MapImages {
    val words = Severity.entries.map { stringResource(it.nameRes()) }
    return remember(colors, density, labelSize, words) {
        MapImages(colors = colors, density = density, labelSize = labelSize, words = words)
    }
}

/**
 * Wygląd obrazów mapy: barwy [colors], gęstość ekranu [density], wysokość podpisu [labelSize] w pikselach i słowa
 * uciążliwości [words] w kolejności [Severity]; bez słów bariery mają same znaki.
 */
internal data class MapImages(
    val colors: MapColors,
    val density: Float,
    val labelSize: Float,
    val words: List<String> = emptyList(),
)
