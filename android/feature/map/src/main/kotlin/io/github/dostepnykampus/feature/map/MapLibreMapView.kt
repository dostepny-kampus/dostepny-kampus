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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dagger.hilt.android.EntryPointAccessors
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.feature.map.barrier.BARRIER_ID
import io.github.dostepnykampus.feature.map.barrier.barrierImages
import io.github.dostepnykampus.feature.map.building.entranceCaption
import io.github.dostepnykampus.feature.map.building.entranceImages
import io.github.dostepnykampus.feature.map.camera.CameraMotion
import io.github.dostepnykampus.feature.map.camera.CameraTransition
import io.github.dostepnykampus.feature.map.camera.CampusMapView
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.camera.MapInset
import io.github.dostepnykampus.feature.map.camera.MapLimits
import io.github.dostepnykampus.feature.map.camera.mapLimits
import io.github.dostepnykampus.feature.map.camera.padding
import io.github.dostepnykampus.feature.map.camera.styleColors
import io.github.dostepnykampus.feature.map.camera.toCamera
import io.github.dostepnykampus.feature.map.camera.toPosition
import io.github.dostepnykampus.feature.map.campus.CampusBounds
import io.github.dostepnykampus.feature.map.campus.CampusMapData
import io.github.dostepnykampus.feature.map.campus.MapSelection
import io.github.dostepnykampus.feature.map.campus.OUTLINE_NUMBER
import io.github.dostepnykampus.feature.map.campus.selection
import io.github.dostepnykampus.feature.map.campus.symbolsAt
import io.github.dostepnykampus.feature.map.style.BARRIERS_LAYER
import io.github.dostepnykampus.feature.map.style.BUILDINGS_LAYER
import io.github.dostepnykampus.feature.map.style.MapImages
import io.github.dostepnykampus.feature.map.style.MapStyleColors
import io.github.dostepnykampus.feature.map.style.MapStyleTarget
import io.github.dostepnykampus.feature.map.style.MapTileHttpEntryPoint
import io.github.dostepnykampus.feature.map.style.SelectionLook
import io.github.dostepnykampus.feature.map.style.asStyleText
import io.github.dostepnykampus.feature.map.style.campusMapStyle
import io.github.dostepnykampus.feature.map.style.labelImage
import io.github.dostepnykampus.feature.map.style.numbers
import io.github.dostepnykampus.feature.map.style.rememberMapImages
import io.github.dostepnykampus.feature.map.style.roomNumberBitmap
import io.github.dostepnykampus.feature.map.style.roomNumberImage
import io.github.dostepnykampus.feature.map.style.selectionLook
import io.github.dostepnykampus.feature.map.style.showSelection
import io.github.dostepnykampus.feature.map.style.styleBuilder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
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
private const val PADDING_MILLIS = 300

/**
 * Widok MapLibre Android bez kluczy i kont (ADR-0005); logo i podpis biblioteki zastępuje podpis ekranu. Podpis konturu
 * rośnie z czcionką najwyżej 1,4 raza, a dotknięcie do 24 dp od konturu trafia w kontur. Pobiera tylko widoczne
 * kafelki. Widok kamery zgłasza tylko po geście osoby. Każdy ruch kamery wykonuje raz; zasłonięcie dołu mapy zmienia
 * dopełnienie dopiero po końcu trwającego ruchu. Natywny widok powstaje i zwalnia pamięć raz, z ekranem; zmiana cyklu
 * życia sceny tylko zatrzymuje i wznawia mapę.
 *
 * Wymagania: NFR-39, NFR-56
 */
object MapLibreMapView : CampusMapView {
    @Composable
    override fun Show(
        data: CampusMapData,
        selection: MapSelection,
        move: CameraTransition,
        inset: MapInset,
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
        val limits =
            remember(selection.bounds, size, density) {
                density.mapLimits(selection.bounds, size).takeIf { size != IntSize.Zero }
            }
        val style =
            rememberMapStyle(data = data, selection = selection, colors = styleColors, tiles = limits?.tileBounds)
        val currentMove by rememberUpdatedState(move)
        val currentInset by rememberUpdatedState(inset)
        val currentData by rememberUpdatedState(data)
        val cameraIdle by rememberUpdatedState(onCameraIdle)
        val buildingsTouch by rememberUpdatedState(onBuildingsTouch)
        val barrierTouch by rememberUpdatedState(onBarrierTouch)
        DisposableEffect(mapView) {
            mapView.getMapAsync { ready ->
                ready.prefetchZoomDelta = 0
                ready.cameraPosition = currentMove.camera.toPosition(currentInset)
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
            selection = selection,
            images = images,
        )
        DisposableEffect(map, limits) {
            limits?.let { map?.limitTo(it) }
            onDispose {}
        }
        map.FollowEachMoveOnce(move = move, inset = inset)
        AndroidView(factory = { mapView }, modifier = modifier.onSizeChanged { size = it })
    }

    @Composable
    private fun MapLibreMap?.FollowEachMoveOnce(move: CameraTransition, inset: MapInset) {
        val driver = remember(this) { this?.let(::CameraDriver) }
        val currentInset by rememberUpdatedState(inset)
        DisposableEffect(driver, move) {
            driver?.follow(move, currentInset)
            onDispose {}
        }
        DisposableEffect(driver, inset) {
            driver?.pad(inset)
            onDispose {}
        }
    }

    @Composable
    private fun MapLibreMap?.FollowStyle(
        style: String?,
        data: CampusMapData,
        look: SelectionLook,
        selection: MapSelection,
        images: MapImages,
    ) {
        val map = this
        var loaded by remember { mutableStateOf<Style?>(null) }
        val currentData by rememberUpdatedState(data)
        val currentLook by rememberUpdatedState(look)
        val currentSelection by rememberUpdatedState(selection)
        DisposableEffect(map, style, images) {
            loaded = null
            style?.let { json ->
                map?.setStyle(styleBuilder(json)) { ready ->
                    ready.addMapImages(labels = currentData.labels, images = images)
                    StyleTarget(ready).showSelection(currentLook, selectionImages(currentSelection, images))
                    loaded = ready
                }
            }
            onDispose {}
        }
        DisposableEffect(loaded, look) {
            loaded?.let { StyleTarget(it).showSelection(look, selectionImages(currentSelection, images)) }
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

    /**
     * Ruchy kamery mapy [map] po jednym. Ruch niesie dopełnienie z chwili startu; dopełnienie zmienione
     * w trakcie ruchu czeka na jego koniec, bo przerwałoby ruch, a samo przesuwa mapę płynnie w 300 ms. Gest osoby
     * przerywa ruch sam (MapLibre).
     */
    private class CameraDriver(private val map: MapLibreMap) {
        private var inset = MapInset()
        private var running: Run? = null

        fun pad(next: MapInset) {
            inset = next
            val padding = next.padding()
            if (running == null && !padding.contentEquals(map.cameraPosition.padding)) {
                map.easeCamera(CameraUpdateFactory.paddingTo(padding), PADDING_MILLIS)
            }
        }

        fun follow(move: CameraTransition, next: MapInset) {
            inset = next
            val update = CameraUpdateFactory.newCameraPosition(move.camera.toPosition(inset))
            val millis = move.durationMillis.toInt()
            running = null
            when (move.motion) {
                CameraMotion.NONE -> map.moveCamera(update)
                CameraMotion.EASE -> map.easeCamera(update, millis, start())
                CameraMotion.FLIGHT -> map.animateCamera(update, millis, start())
            }
        }

        private fun start(): Run = Run().also { running = it }

        /** Jeden ruch; koniec ruchu wcześniejszego, przerwanego przez nowy, niczego nie zmienia. */
        private inner class Run : MapLibreMap.CancelableCallback {
            override fun onCancel() = settle()

            override fun onFinish() = settle()

            private fun settle() {
                if (running !== this) return
                running = null
                pad(inset)
            }
        }
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

        override fun minZoom(layer: String, zoom: Double) {
            style.getLayer(layer)?.minZoom = zoom.toFloat()
        }

        override fun data(source: String, geoJson: String) {
            style.getSourceAs<GeoJsonSource>(source)?.setGeoJson(geoJson)
        }

        override fun image(name: String, image: Bitmap) = style.addImage(name, image)

        private fun expressionOf(value: JsonElement): Expression = Expression.Converter.convert(value.asStyleText())
    }
}

/**
 * Ustawienia widoku mapy V1: bez logo i podpisu biblioteki, bez gestu obrotu i pochylenia. Północ jest zawsze
 * u góry, bo tak liczą strzałki przesuwania i granica terenu, a [MapCamera] nie zna obrotu. Przesuwanie
 * i powiększanie gestem zostają.
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
 * Styl mapy jako napis JSON bez zaznaczenia budynku, wejścia i bariery: z [selection] bierze tylko prostokąt
 * i granicę terenu, a podkład ogranicza do prostokąta [tiles]. Powstaje od nowa tylko po zmianie danych [data],
 * terenu, barw [colors] albo [tiles]; zaznaczenie i ruch kamery stylu nie budują. Z terenem, a bez [tiles] (mapa
 * jeszcze bez rozmiaru) stylu nie ma. Funkcja [build] składa styl.
 */
@Composable
internal fun rememberMapStyle(
    data: CampusMapData,
    selection: MapSelection,
    colors: MapStyleColors,
    tiles: CampusBounds?,
    build: (CampusMapData, MapSelection, MapStyleColors, CampusBounds?) -> String = ::campusMapStyle,
): String? =
    remember(data, selection.bounds, selection.boundary, colors, tiles) {
        val area = MapSelection(bounds = selection.bounds, boundary = selection.boundary)
        if (selection.bounds != null && tiles == null) null else build(data, area, colors, tiles)
    }

/**
 * Dodaje do stylu obrazy niezależne od zaznaczenia: podpisy konturów [labels], znaki wejść, pinezkę oraz znaki
 * i słowa uciążliwości barier, bo napisy ze stylu wymagają sieci.
 */
internal fun Style.addMapImages(labels: List<String>, images: MapImages) {
    labels.forEach { label -> addImage(label, labelImage(label, images.colors, images.labelSize)) }
    entranceImages(colors = images.colors, density = images.density).forEach { (name, image) -> addImage(name, image) }
    barrierImages(images).forEach { (name, image) -> addImage(name, image) }
}

/**
 * Obrazy zależne od zaznaczenia [selection]: podpis zaznaczonego wejścia z jego nazwą i numery sal wybranego piętra
 * planu. Wymagania: US3.3, US16.1
 */
internal fun selectionImages(selection: MapSelection, images: MapImages): Map<String, Bitmap> {
    val floor = selection.plan?.let { it.rooms[it.floor] }
    val rooms = floor?.rooms.orEmpty()
    val numbers =
        rooms
            .asSequence()
            .filter { it.isLabelled }
            .mapNotNull { it.number }
            .toSet()
    val caption = entranceCaption(selected = selection.entrance, colors = images.colors, labelSize = images.labelSize)
    return caption + numbers.associate { roomNumberImage(it) to roomNumberBitmap(it, images.colors, images.labelSize) }
}
