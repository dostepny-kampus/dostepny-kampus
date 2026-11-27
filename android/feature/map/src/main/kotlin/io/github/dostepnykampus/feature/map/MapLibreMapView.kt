/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.gson.JsonObject
import dagger.hilt.android.EntryPointAccessors
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.designsystem.Typografia
import io.github.dostepnykampus.core.model.CampusBounds
import io.github.dostepnykampus.feature.map.barrier.barrierImages
import io.github.dostepnykampus.feature.map.camera.BasemapReveal
import io.github.dostepnykampus.feature.map.camera.CameraController
import io.github.dostepnykampus.feature.map.camera.CameraMotion
import io.github.dostepnykampus.feature.map.camera.CameraPort
import io.github.dostepnykampus.feature.map.camera.CameraTransition
import io.github.dostepnykampus.feature.map.camera.CampusMapView
import io.github.dostepnykampus.feature.map.camera.FlightArea
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.camera.MapInset
import io.github.dostepnykampus.feature.map.camera.MapLimits
import io.github.dostepnykampus.feature.map.camera.StyleArea
import io.github.dostepnykampus.feature.map.camera.TouchSign
import io.github.dostepnykampus.feature.map.camera.belowTop
import io.github.dostepnykampus.feature.map.camera.mapLimits
import io.github.dostepnykampus.feature.map.camera.styleAreaOf
import io.github.dostepnykampus.feature.map.camera.styleColors
import io.github.dostepnykampus.feature.map.camera.toCamera
import io.github.dostepnykampus.feature.map.camera.toPosition
import io.github.dostepnykampus.feature.map.camera.traceSection
import io.github.dostepnykampus.feature.map.campus.BARRIER_BUILDING
import io.github.dostepnykampus.feature.map.campus.BARRIER_ENTRANCE
import io.github.dostepnykampus.feature.map.campus.BARRIER_OFFSET
import io.github.dostepnykampus.feature.map.campus.CampusMapData
import io.github.dostepnykampus.feature.map.campus.MapSelection
import io.github.dostepnykampus.feature.map.campus.OUTLINE_NUMBER
import io.github.dostepnykampus.feature.map.campus.selection
import io.github.dostepnykampus.feature.map.campus.touchedSymbols
import io.github.dostepnykampus.feature.map.entrance.entranceCaption
import io.github.dostepnykampus.feature.map.entrance.entranceImages
import io.github.dostepnykampus.feature.map.plan.planSignImages
import io.github.dostepnykampus.feature.map.style.BUILDINGS_LAYER
import io.github.dostepnykampus.feature.map.style.Basemap
import io.github.dostepnykampus.feature.map.style.BesideEntrance
import io.github.dostepnykampus.feature.map.style.ContourStates
import io.github.dostepnykampus.feature.map.style.MapImages
import io.github.dostepnykampus.feature.map.style.MapStyleColors
import io.github.dostepnykampus.feature.map.style.MapStyleTarget
import io.github.dostepnykampus.feature.map.style.PLAN_ROOM_NUMBER_ZOOM
import io.github.dostepnykampus.feature.map.style.ROOM_STEP_OFFSET
import io.github.dostepnykampus.feature.map.style.SelectionLook
import io.github.dostepnykampus.feature.map.style.asStyleText
import io.github.dostepnykampus.feature.map.style.campusMapShell
import io.github.dostepnykampus.feature.map.style.hideBasemap
import io.github.dostepnykampus.feature.map.style.rememberMapImages
import io.github.dostepnykampus.feature.map.style.revealBasemap
import io.github.dostepnykampus.feature.map.style.roomStepImages
import io.github.dostepnykampus.feature.map.style.selectionLook
import io.github.dostepnykampus.feature.map.style.showBasemap
import io.github.dostepnykampus.feature.map.style.showData
import io.github.dostepnykampus.feature.map.style.showPlanData
import io.github.dostepnykampus.feature.map.style.showSelection
import io.github.dostepnykampus.feature.map.style.styleBuilder
import io.github.dostepnykampus.feature.map.tiles.MapTileHttp
import io.github.dostepnykampus.feature.map.tiles.MapTileHttpEntryPoint
import io.github.dostepnykampus.feature.map.trace.MapTrace
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.constants.MapLibreConstants
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.module.http.HttpRequestUtil
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LayoutPropertyValue
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PaintPropertyValue
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.layers.TransitionOptions
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.Point
import timber.log.Timber
import kotlin.math.roundToLong

private const val LABEL_MAX_FONT_SCALE = 1.4f
private const val TOUCH_TOLERANCE_DP = 24f
private const val BASEMAP_FADE_MILLIS = 200f

/**
 * Widok MapLibre Android bez kluczy i kont (ADR-0005); logo i podpis biblioteki zastępuje podpis ekranu. Podpis konturu
 * rośnie z czcionką najwyżej 1,4 raza, a dotknięcie do 24 dp od konturu trafia w kontur. Pobiera tylko widoczne
 * kafelki. Widok kamery zgłasza tylko po geście osoby. Kamerę zmienia tylko [CameraController] (ADR-0015). Natywny
 * widok powstaje i zwalnia pamięć raz, z ekranem; zmiana cyklu życia sceny tylko zatrzymuje i wznawia mapę.
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
        onSignTouch: (TouchSign, String) -> Unit,
        modifier: Modifier,
    ) {
        if (!rememberNativeMap()) return
        val context = LocalContext.current
        val density = LocalDensity.current
        val colors = DostepnyKampusTheme.mapColors
        val fontScale = density.fontScale.coerceAtMost(LABEL_MAX_FONT_SCALE)
        val styleColors = colors.styleColors(fontScale)
        val look = remember(data, selection) { selectionLook(data, selection) }
        val labelSize = Typografia.napisyMapy.value * density.density * fontScale
        val tolerance = TOUCH_TOLERANCE_DP * density.density
        val images = rememberMapImages(colors = colors, density = density.density, labelSize = labelSize)
        val tiles = rememberMapTiles(context)
        val mapView = rememberMapView(context, background = colors.lad.toArgb(), tiles = tiles)
        val tileAreas = remember(tiles) { tiles.newView() }
        var map by remember { mutableStateOf<MapLibreMap?>(null) }
        var size by remember { mutableStateOf(IntSize.Zero) }
        val limits =
            remember(selection.bounds, size, density, inset.top) {
                density.mapLimits(selection.bounds, size.belowTop(inset.top)).takeIf { size != IntSize.Zero }
            }
        val basemap = remember(selection.bounds, size, density) { density.basemapOf(selection.bounds, size) }
        val area = styleAreaOf(selection = selection, basemap = basemap)
        val areas = remember(tileAreas) { FlightArea(first = area, onTileAreas = tileAreas::update) }
        FollowScreenLifecycle(mapView, tileAreas)
        val reveal = remember { BasemapReveal() }
        val currentMove by rememberUpdatedState(move)
        val currentInset by rememberUpdatedState(inset)
        val currentData by rememberUpdatedState(data)
        val cameraIdle by rememberUpdatedState(onCameraIdle)
        val buildingsTouch by rememberUpdatedState(onBuildingsTouch)
        val signTouch by rememberUpdatedState(onSignTouch)
        val currentLook by rememberUpdatedState(look)
        DisposableEffect(mapView) {
            mapView.getMapAsync { ready ->
                ready.prefetchZoomDelta = 0
                ready.cameraPosition = currentMove.camera.toPosition(currentInset)
                ready.onPersonIdle { cameraIdle(it) }
                val onSign = { touch: MapTouch.Sign -> signTouch(touch.sign, touch.id) }
                ready.onTouch(tolerance = tolerance, look = { currentLook }, onSign = onSign) { hit, near ->
                    currentData.touchedSymbols(hit, near).also { buildingsTouch(it) }.isNotEmpty()
                }
                map = ready
            }
            onDispose { map = null }
        }
        map.FollowStyle(
            data = data,
            look = look,
            selection = selection,
            images = images,
            colors = styleColors,
            areas = areas,
            reveal = reveal,
        )
        map.FollowBasemapReveal(mapView = mapView, reveal = reveal, area = areas.shown)
        map.FollowEachMoveOnce(move = move, inset = inset, limits = limits, area = area, areas = areas, view = mapView)
        AndroidView(factory = { mapView }, modifier = modifier.onSizeChanged { size = it })
    }

    @Composable
    private fun MapLibreMap?.FollowEachMoveOnce(
        move: CameraTransition,
        inset: MapInset,
        limits: MapLimits?,
        area: StyleArea,
        areas: FlightArea,
        view: View,
    ) {
        val map = this
        val controller =
            remember(map) { map?.let { CameraController(MapLibreCamera(map = it, areas = areas, view = view)) } }
        val currentInset by rememberUpdatedState(inset)
        DisposableEffect(map, controller) {
            val gesture =
                MapLibreMap.OnCameraMoveStartedListener { reason ->
                    if (reason == REASON_API_GESTURE) controller?.byPerson()
                }
            map?.addOnCameraMoveStartedListener(gesture)
            onDispose { map?.removeOnCameraMoveStartedListener(gesture) }
        }
        DisposableEffect(controller, move) {
            controller?.move(move, currentInset)
            onDispose {}
        }
        DisposableEffect(controller, limits) {
            controller?.limit(limits)
            onDispose {}
        }
        DisposableEffect(area) {
            MapTrace.areaChange { areas.area(area) }
            onDispose {}
        }
        DisposableEffect(controller, inset) {
            controller?.pad(inset)
            onDispose {}
        }
    }

    @Composable
    private fun MapLibreMap?.FollowStyle(
        data: CampusMapData,
        look: SelectionLook,
        selection: MapSelection,
        images: MapImages,
        colors: MapStyleColors,
        areas: FlightArea,
        reveal: BasemapReveal,
    ) {
        val map = this
        val area = areas.shown
        val style = rememberMapStyle(selection = area.area, colors = colors, basemap = area.basemap)
        var loaded by remember { mutableStateOf<Style?>(null) }
        val currentLook by rememberUpdatedState(look)
        val currentSelection by rememberUpdatedState(selection)
        val currentArea by rememberUpdatedState(area)
        var shown by remember { mutableStateOf<StyleArea?>(null) }
        var styleLoads by remember { mutableIntStateOf(0) }
        val sent = remember<ContourStates>(map, style, images) { ContourStates() }
        DisposableEffect(map, style, images) {
            loaded = null
            val built = currentArea
            style?.let { json ->
                styleLoads += 1
                val load = styleLoads
                MapTrace.startStyle(load)
                map?.setStyle(styleBuilder(json)) { ready ->
                    MapTrace.endStyle(load)
                    ready.addMapImages(images = images)
                    MapTrace.section(name = MapTrace.SELECTION_SECTION) {
                        StyleTarget(ready).showSelection(currentLook, selectionImages(currentSelection, images), sent)
                    }
                    StyleTarget(ready).hideBasemap()
                    reveal.hide()
                    shown = built
                    loaded = ready
                }
            }
            onDispose {}
        }
        val labelled = remember(loaded) { mutableSetOf<String>() }
        LaunchedEffect(loaded, data) {
            loaded?.let { StyleTarget(it).showData(data, images, labelled) }
        }
        val numbered = remember(loaded) { mutableSetOf<String>() }
        val floors = selection.plan?.rooms.orEmpty()
        LaunchedEffect(loaded, floors) {
            loaded?.let { StyleTarget(it).showPlanData(floors, images, numbered) }
        }
        DisposableEffect(loaded, look) {
            loaded?.let { ready ->
                MapTrace.section(name = MapTrace.SELECTION_SECTION) {
                    StyleTarget(ready).showSelection(look, selectionImages(currentSelection, images), sent)
                }
            }
            onDispose {}
        }
        DisposableEffect(loaded, area) {
            loaded?.takeIf { area != shown }?.let { ready ->
                ready.showStyleArea(area = area, isKept = areas.isBasemapKept, reveal = reveal)
                shown = area
            }
            onDispose {}
        }
    }

    private fun Style.showStyleArea(area: StyleArea, isKept: Boolean, reveal: BasemapReveal) {
        StyleTarget(this).showBasemap(basemap = area.basemap)
        if (!isKept) {
            StyleTarget(this).hideBasemap()
            reveal.hide()
        }
    }

    @Composable
    private fun MapLibreMap?.FollowBasemapReveal(mapView: MapView, reveal: BasemapReveal, area: StyleArea) {
        val map = this
        val fadeMillis = (BASEMAP_FADE_MILLIS * rememberAnimatorScale()).roundToLong()
        val currentArea by rememberUpdatedState(area)
        DisposableEffect(mapView, map) {
            val listener =
                MapView.OnDidFinishRenderingFrameListener { isFull, _, _ ->
                    val style: Style? = map?.style
                    val zoom = map?.run { cameraPosition.zoom } ?: 0.0
                    val isDue = reveal.frame(isFull = isFull, zoom = zoom, minZoom = currentArea.hiddenBelow)
                    val isLoaded = style != null && style.isFullyLoaded
                    if (isLoaded && isDue) StyleTarget(style).revealBasemap(millis = fadeMillis)
                }
            mapView.addOnDidFinishRenderingFrameListener(listener)
            onDispose { mapView.removeOnDidFinishRenderingFrameListener(listener) }
        }
    }

    @Composable
    private fun rememberMapTiles(context: Context): MapTileHttp =
        remember {
            EntryPointAccessors.fromApplication<MapTileHttpEntryPoint>(context.applicationContext).mapTileHttp()
        }

    @Composable
    private fun rememberMapView(context: Context, background: Int, tiles: MapTileHttp): MapView =
        remember {
            MapLibre.getInstance(context)
            HttpRequestUtil.setOkHttpClient(tiles.calls)
            MapView(context, northUpOptions(context, background)).apply {
                addOnDidFailLoadingMapListener { Timber.w("Mapa: błąd stylu: %s", it) }
            }
        }

    @Composable
    private fun FollowScreenLifecycle(mapView: MapView, tileAreas: MapTileHttp.ViewAreas) {
        DisposableEffect(mapView) {
            mapView.onCreate(null)
            onDispose { mapView.onDestroy() }
        }
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        DisposableEffect(lifecycle, mapView, tileAreas) {
            val observer =
                LifecycleEventObserver { _, event ->
                    tileAreas.setVisible(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
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
                tileAreas.setVisible(false)
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onPause()
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStop()
            }
        }
    }

    private fun MapLibreMap.onTouch(
        tolerance: Float,
        look: () -> SelectionLook,
        onSign: (MapTouch.Sign) -> Unit,
        isTaken: (hit: List<Int>, near: () -> List<Int>) -> Boolean,
    ) {
        addOnMapClickListener { point ->
            val at = projection.toScreenLocation(point)
            val area = RectF(at.x - tolerance, at.y - tolerance, at.x + tolerance, at.y + tolerance)
            val number = { feature: Feature -> feature.getNumberProperty(OUTLINE_NUMBER)?.toInt() }
            val outlines =
                OutlinesQuery(
                    hit = { queryRenderedFeatures(at, BUILDINGS_LAYER).mapNotNull(number) },
                    near = { queryRenderedFeatures(area, BUILDINGS_LAYER).mapNotNull(number) },
                )
            val zoom = cameraPosition.zoom
            val beside = look().besideEntrance
            val signsUnder = { sign: TouchSign ->
                if (sign == TouchSign.ROOM && zoom < PLAN_ROOM_NUMBER_ZOOM) {
                    emptyList()
                } else {
                    queryRenderedFeatures(at, sign.layer).mapNotNull { it.getStringProperty(sign.idKey) }
                }
            }
            val signsNear = { sign: TouchSign ->
                val under = signsUnder(sign)
                val near =
                    queryRenderedFeatures(area, sign.layer).mapNotNull { feature ->
                        signPoint(
                            feature = feature,
                            idKey = sign.idKey,
                            at = at,
                            pxPerDp = tolerance / TOUCH_TOLERANCE_DP,
                            offset = feature.signOffset(beside = beside, zoom = zoom),
                        )
                    }
                signsInReach(under = under, near = near, reach = tolerance)
            }
            when (val touch = chooseTouch(signsUnder = signsUnder, signsNear = signsNear, outlines = outlines)) {
                is MapTouch.Sign -> {
                    onSign(touch)
                    true
                }

                is MapTouch.Outlines -> {
                    isTaken(touch.hit, touch.near)
                }
            }
        }
    }

    private fun MapLibreMap.signPoint(
        feature: Feature,
        idKey: String,
        at: PointF,
        pxPerDp: Float,
        offset: PointF,
    ): SignPoint? {
        val id = feature.getStringProperty(idKey)
        val point = (feature.geometry() as? Point)?.let { LatLng(latitude = it.latitude(), longitude = it.longitude()) }
        val screen = point?.let(projection::toScreenLocation)
        if (id == null || screen == null) return null
        return SignPoint(id = id, dx = screen.x + offset.x * pxPerDp - at.x, dy = screen.y + offset.y * pxPerDp - at.y)
    }

    private fun MapLibreMap.onPersonIdle(report: (MapCamera) -> Unit) {
        var isByPerson = false
        addOnCameraMoveStartedListener { reason -> isByPerson = reason == REASON_API_GESTURE }
        addOnCameraIdleListener {
            if (isByPerson) report(cameraPosition.toCamera())
            isByPerson = false
        }
    }

    /**
     * Kamera MapLibre [map] dla [CameraController]. Granica środka przy bieżącym powiększeniu i dopełnieniu idzie do
     * mapy przy każdym ruchu gestem, więc odkryta część nie wychodzi poza teren. Przelot zgłasza start
     * i koniec do [areas].
     */
    private class MapLibreCamera(private val map: MapLibreMap, private val areas: FlightArea, private val view: View) :
        CameraPort {
        private var area: MapLimits? = null
        private var centerBox: CampusBounds? = null
        private var moves = 0

        init {
            map.addOnCameraMoveListener {
                val position = map.cameraPosition
                areas.zoom(position.zoom)
                position.target?.let { at ->
                    MapTrace.frame(zoom = position.zoom, latitude = at.latitude, longitude = at.longitude)
                }
                limitCenterToArea()
            }
        }

        override val camera: MapCamera get() = map.cameraPosition.toCamera()

        override val inset: MapInset
            get() = map.cameraPosition.padding?.let { MapInset(top = it[1], bottom = it[3]) } ?: MapInset()

        override fun lift() = limitTo(null)

        override fun move(camera: MapCamera, inset: MapInset, motion: CameraMotion, millis: Long, end: () -> Unit) {
            val update = CameraUpdateFactory.newCameraPosition(camera.toPosition(inset))
            val number = ++moves
            MapTrace.startMove(section = motion.traceSection, move = number)
            val callback =
                object : MapLibreMap.CancelableCallback {
                    override fun onCancel() = done()

                    override fun onFinish() = done()

                    private fun done() {
                        MapTrace.endMove(section = motion.traceSection, move = number)
                        end()
                    }
                }
            areas.start(motion)
            when (motion) {
                CameraMotion.NONE -> map.moveCamera(update, callback)
                CameraMotion.EASE -> map.easeCamera(update, millis.toInt(), callback)
                CameraMotion.FLIGHT -> map.animateCamera(update, millis.toInt(), callback)
            }
        }

        override fun rest(limits: MapLimits?) {
            areas.endFlight()
            limitTo(limits)
        }

        override fun post(block: () -> Unit) {
            view.post(block)
        }

        private fun limitTo(next: MapLimits?) {
            area = next
            centerBox = null
            if (next == null) {
                map.setLatLngBoundsForCameraTarget(null)
                map.setMinZoomPreference(MapLibreConstants.MINIMUM_ZOOM.toDouble())
                return
            }
            map.setMinZoomPreference(next.minZoom)
            limitCenterToArea()
        }

        private fun limitCenterToArea() {
            val limits = area ?: return
            val padding = map.cameraPosition.padding ?: DoubleArray(size = 4)
            val density = view.resources.displayMetrics.density
            val width = (view.width - padding[0] - padding[2]) / density
            val height = (view.height - padding[1] - padding[3]) / density
            val box =
                limits
                    .centerBounds(zoom = map.cameraPosition.zoom, viewWidthDp = width, viewHeightDp = height)
                    .takeIf { width > 0 && height > 0 && it != centerBox }
            if (box != null) {
                centerBox = box
                val bounds =
                    LatLngBounds.from(
                        latNorth = box.north,
                        lonEast = box.east,
                        latSouth = box.south,
                        lonWest = box.west,
                    )
                map.setLatLngBoundsForCameraTarget(bounds)
            }
        }
    }

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

        override fun state(source: String, feature: Int, key: String, value: Boolean) {
            val state = JsonObject().apply { addProperty(key, value) }
            style.getSourceAs<GeoJsonSource>(source)?.setFeatureState(feature.toString(), state)
        }

        override fun layout(layer: String, property: String, value: JsonElement) {
            val styled: Any = if (value is JsonArray) expressionOf(value) else value.jsonPrimitive.content
            style.getLayer(layer)?.setProperties(LayoutPropertyValue(property, styled))
        }

        override fun minZoom(layer: String, zoom: Double) {
            style.getLayer(layer)?.minZoom = zoom.toFloat()
        }

        override fun data(source: String, geoJson: String) {
            style.getSourceAs<GeoJsonSource>(source)?.setGeoJson(geoJson)
        }

        override fun image(name: String, image: Bitmap) = style.addImage(name, image)

        override fun fade(layer: String, opacity: Float, millis: Long) {
            val transition = TransitionOptions(millis, 0L)
            when (val found = style.getLayer(layer)) {
                is FillLayer -> {
                    found.setFillOpacityTransition(transition)
                    found.setProperties(PropertyFactory.fillOpacity(opacity))
                }

                is LineLayer -> {
                    found.setLineOpacityTransition(transition)
                    found.setProperties(PropertyFactory.lineOpacity(opacity))
                }

                is SymbolLayer -> {
                    found.setTextOpacityTransition(transition)
                    found.setIconOpacityTransition(transition)
                    found.setProperties(PropertyFactory.textOpacity(opacity), PropertyFactory.iconOpacity(opacity))
                }

                else -> {}
            }
        }

        private fun expressionOf(value: JsonElement): Expression = Expression.Converter.convert(value.asStyleText())
    }
}

/** Przesunięcie punktu dotknięcia znaku względem współrzędnych obiektu, w dp. */
internal fun Feature.signOffset(beside: BesideEntrance, zoom: Double): PointF {
    val pinned = beside.pinnedOffset(getStringProperty(BARRIER_ENTRANCE))
    if (pinned != null) return PointF(pinned.toFloat(), 0f)
    val offset =
        getProperty(BARRIER_OFFSET)
            ?.takeIf { value ->
                val building = getStringProperty(BARRIER_BUILDING)
                value.isJsonArray && beside.isBeside(zoom = zoom, building = building)
            }?.asJsonArray
            ?: getProperty(ROOM_STEP_OFFSET)?.takeIf { it.isJsonArray }?.asJsonArray
    return offset?.run { PointF(get(0).asFloat, get(1).asFloat) } ?: PointF()
}

/**
 * Ustawienia widoku mapy V1: bez logo i podpisu biblioteki, bez gestu obrotu i pochylenia. Północ jest zawsze
 * u góry, bo tak liczą strzałki przesuwania i granica terenu, a [MapCamera] nie zna obrotu. Przesuwanie
 * i powiększanie gestem zostają. Do pierwszej klatki widok ma barwę [background] (ARGB).
 */
internal fun northUpOptions(context: Context, background: Int): MapLibreMapOptions =
    MapLibreMapOptions
        .createFromAttributes(context)
        .textureMode(true)
        .logoEnabled(false)
        .attributionEnabled(false)
        .rotateGesturesEnabled(false)
        .tiltGesturesEnabled(false)
        .foregroundLoadColor(background)

/**
 * Styl mapy z pustymi źródłami danych ([campusMapShell]) jako napis JSON bez zaznaczenia budynku, wejścia
 * i bariery: z [selection] bierze tylko prostokąt terenu, a podkład ogranicza do obszaru [basemap]. Powstaje od nowa
 * tylko po zmianie barw [colors]; dane katalogu i barier, teren i podkład idą do wczytanego stylu, a zaznaczenie
 * i ruch kamery stylu nie budują. Z terenem, a bez [basemap] (mapa jeszcze bez rozmiaru) stylu nie ma. Funkcja
 * [build] składa styl.
 */
@Composable
internal fun rememberMapStyle(
    selection: MapSelection,
    colors: MapStyleColors,
    basemap: Basemap?,
    build: (MapSelection, MapStyleColors, Basemap?) -> String = ::campusMapShell,
): String? =
    (selection.bounds == null || basemap != null).let { isMeasured ->
        remember(colors, isMeasured) {
            val area = MapSelection(bounds = selection.bounds, boundary = selection.boundary)
            if (isMeasured) build(area, colors, basemap) else null
        }
    }

/**
 * Dodaje do stylu obrazy niezależne od zaznaczenia i danych: znaki wejść, pinezkę oraz znaki i słowa uciążliwości
 * barier oraz znaki planu, bo napisy ze stylu wymagają sieci. Podpisy konturów dodaje `showData`.
 */
internal fun Style.addMapImages(images: MapImages) {
    entranceImages(colors = images.colors, density = images.density).forEach { (name, image) -> addImage(name, image) }
    barrierImages(images).forEach { (name, image) -> addImage(name, image) }
    val signs = planSignImages(colors = images.colors, density = images.density)
    signs.forEach { (name, image) -> addImage(name, image) }
    roomStepImages(images).forEach { (name, image) -> addImage(name, image) }
}

/**
 * Obrazy zależne od zaznaczenia [selection]: podpis zaznaczonego wejścia z jego nazwą. Numery sal planu dodaje
 * `showPlanData`. Wymagania: US3.3
 */
internal fun selectionImages(selection: MapSelection, images: MapImages): Map<String, Bitmap> =
    entranceCaption(selected = selection.entrance, colors = images.colors, labelSize = images.labelSize)

private fun Density.basemapOf(bounds: CampusBounds?, size: IntSize): Basemap? =
    mapLimits(bounds, size).takeIf { size != IntSize.Zero }?.basemap
