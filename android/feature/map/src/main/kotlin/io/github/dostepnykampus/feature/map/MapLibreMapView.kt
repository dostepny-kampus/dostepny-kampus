/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import dagger.hilt.android.EntryPointAccessors
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.designsystem.Typografia
import io.github.dostepnykampus.feature.map.barrier.BARRIER_ID
import io.github.dostepnykampus.feature.map.barrier.barrierImages
import io.github.dostepnykampus.feature.map.building.entranceCaption
import io.github.dostepnykampus.feature.map.building.entranceImages
import io.github.dostepnykampus.feature.map.camera.CameraMotion
import io.github.dostepnykampus.feature.map.camera.CameraTransition
import io.github.dostepnykampus.feature.map.camera.CampusMapView
import io.github.dostepnykampus.feature.map.camera.FlightArea
import io.github.dostepnykampus.feature.map.camera.FlightLimits
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.camera.MapInset
import io.github.dostepnykampus.feature.map.camera.MapLimits
import io.github.dostepnykampus.feature.map.camera.StyleArea
import io.github.dostepnykampus.feature.map.camera.belowTop
import io.github.dostepnykampus.feature.map.camera.isSameView
import io.github.dostepnykampus.feature.map.camera.mapLimits
import io.github.dostepnykampus.feature.map.camera.padding
import io.github.dostepnykampus.feature.map.camera.styleAreaOf
import io.github.dostepnykampus.feature.map.camera.styleColors
import io.github.dostepnykampus.feature.map.camera.toCamera
import io.github.dostepnykampus.feature.map.camera.toPosition
import io.github.dostepnykampus.feature.map.campus.CampusBounds
import io.github.dostepnykampus.feature.map.campus.CampusMapData
import io.github.dostepnykampus.feature.map.campus.MapSelection
import io.github.dostepnykampus.feature.map.campus.OUTLINE_NUMBER
import io.github.dostepnykampus.feature.map.campus.selection
import io.github.dostepnykampus.feature.map.campus.symbolsAt
import io.github.dostepnykampus.feature.map.plan.planSignImages
import io.github.dostepnykampus.feature.map.style.BARRIERS_LAYER
import io.github.dostepnykampus.feature.map.style.BUILDINGS_LAYER
import io.github.dostepnykampus.feature.map.style.Basemap
import io.github.dostepnykampus.feature.map.style.MapImages
import io.github.dostepnykampus.feature.map.style.MapStyleColors
import io.github.dostepnykampus.feature.map.style.MapStyleTarget
import io.github.dostepnykampus.feature.map.style.MapTileHttpEntryPoint
import io.github.dostepnykampus.feature.map.style.SelectionLook
import io.github.dostepnykampus.feature.map.style.asStyleText
import io.github.dostepnykampus.feature.map.style.campusMapShell
import io.github.dostepnykampus.feature.map.style.numbers
import io.github.dostepnykampus.feature.map.style.rememberMapImages
import io.github.dostepnykampus.feature.map.style.roomNumberBitmap
import io.github.dostepnykampus.feature.map.style.roomNumberImage
import io.github.dostepnykampus.feature.map.style.selectionLook
import io.github.dostepnykampus.feature.map.style.showBasemap
import io.github.dostepnykampus.feature.map.style.showData
import io.github.dostepnykampus.feature.map.style.showSelection
import io.github.dostepnykampus.feature.map.style.styleBuilder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.constants.MapLibreConstants
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
        if (!rememberNativeMap()) return
        val context = LocalContext.current
        val density = LocalDensity.current
        val colors = DostepnyKampusTheme.mapColors
        val fontScale = density.fontScale.coerceAtMost(LABEL_MAX_FONT_SCALE)
        val styleColors = colors.styleColors(fontScale)
        val look = remember(data, selection, styleColors) { selectionLook(data, selection, styleColors) }
        val labelSize = Typografia.napisyMapy.value * density.density * fontScale
        val tolerance = TOUCH_TOLERANCE_DP * density.density
        val images = rememberMapImages(colors = colors, density = density.density, labelSize = labelSize)
        val mapView = rememberMapView(context, background = colors.lad.toArgb())
        FollowScreenLifecycle(mapView)
        var map by remember { mutableStateOf<MapLibreMap?>(null) }
        var size by remember { mutableStateOf(IntSize.Zero) }
        val limits =
            remember(selection.bounds, size, density, inset.top) {
                density.mapLimits(selection.bounds, size.belowTop(inset.top)).takeIf { size != IntSize.Zero }
            }
        val basemap = remember(selection.bounds, size, density) { density.basemapOf(selection.bounds, size) }
        val area = styleAreaOf(selection = selection, basemap = basemap)
        val areas = remember { FlightArea(area) }
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
            data = data,
            look = look,
            selection = selection,
            images = images,
            colors = styleColors,
            area = areas.shown,
        )
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
        val driver = remember(this) { this?.let { CameraDriver(map = it, areas = areas, view = view) } }
        val currentInset by rememberUpdatedState(inset)
        DisposableEffect(driver, move) {
            driver?.follow(move, currentInset)
            onDispose {}
        }
        DisposableEffect(driver, limits) {
            driver?.limit(limits)
            onDispose {}
        }
        DisposableEffect(area) {
            areas.area(area)
            onDispose {}
        }
        DisposableEffect(driver, inset) {
            driver?.pad(inset)
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
        area: StyleArea,
    ) {
        val map = this
        val style = rememberMapStyle(selection = area.area, colors = colors, basemap = area.basemap)
        var loaded by remember { mutableStateOf<Style?>(null) }
        val currentLook by rememberUpdatedState(look)
        val currentSelection by rememberUpdatedState(selection)
        val currentArea by rememberUpdatedState(area)
        var shown by remember { mutableStateOf<StyleArea?>(null) }
        DisposableEffect(map, style, images) {
            loaded = null
            val built = currentArea
            style?.let { json ->
                map?.setStyle(styleBuilder(json)) { ready ->
                    ready.addMapImages(images = images)
                    StyleTarget(ready).showSelection(currentLook, selectionImages(currentSelection, images))
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
        DisposableEffect(loaded, look) {
            loaded?.let { StyleTarget(it).showSelection(look, selectionImages(currentSelection, images)) }
            onDispose {}
        }
        DisposableEffect(loaded, area) {
            loaded?.takeIf { area != shown }?.let { ready ->
                StyleTarget(ready).showBasemap(basemap = area.basemap)
                shown = area
            }
            onDispose {}
        }
    }

    @Composable
    private fun rememberMapView(context: Context, background: Int): MapView =
        remember {
            MapLibre.getInstance(context)
            val tiles = EntryPointAccessors.fromApplication<MapTileHttpEntryPoint>(context.applicationContext)
            HttpRequestUtil.setOkHttpClient(tiles.mapTileHttp().calls)
            MapView(context, northUpOptions(context, background))
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

    /**
     * Ruchy kamery mapy [map] po jednym. Ruch niesie dopełnienie z chwili startu; dopełnienie zmienione
     * w trakcie ruchu czeka na jego koniec, bo przerwałoby ruch, a samo przesuwa mapę płynnie w 300 ms. Gest osoby
     * przerywa ruch sam (MapLibre). Na czas przelotu granice kamery są zdjęte ([FlightLimits]), a teren i podkład
     * starego terenu zostają do zejścia poniżej powiększenia podkładu ([areas]). Brzeg odkrytej części mapy nie
     * wychodzi poza prostokąt terenu: MapLibre ogranicza tylko środek kamery, więc granica środka przy bieżącym
     * powiększeniu i dopełnieniu idzie do mapy na nowo przy każdym ruchu.
     */
    private class CameraDriver(private val map: MapLibreMap, private val areas: FlightArea, private val view: View) {
        private var inset = MapInset()
        private var running: Run? = null
        private var area: MapLimits? = null
        private var centerBox: CampusBounds? = null
        private val limits = FlightLimits { limitTo(it) }

        init {
            map.addOnCameraMoveListener {
                areas.zoom(map.cameraPosition.zoom)
                limitCenterToArea()
            }
        }

        fun limit(next: MapLimits?) = limits.limit(next)

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

        fun pad(next: MapInset) {
            inset = next
            val padding = next.padding()
            if (running == null && !padding.contentEquals(map.cameraPosition.padding)) {
                map.easeCamera(CameraUpdateFactory.paddingTo(padding), PADDING_MILLIS)
            }
        }

        fun follow(move: CameraTransition, next: MapInset) {
            if (running == null && move.camera.isSameView(map.cameraPosition.toCamera())) {
                pad(next)
                return
            }
            inset = next
            val update = CameraUpdateFactory.newCameraPosition(move.camera.toPosition(inset))
            val millis = move.durationMillis.toInt()
            running = null
            if (move.motion == CameraMotion.FLIGHT) startFlight() else endFlight()
            when (move.motion) {
                CameraMotion.NONE -> map.moveCamera(update)
                CameraMotion.EASE -> map.easeCamera(update, millis, start())
                CameraMotion.FLIGHT -> map.animateCamera(update, millis, start())
            }
        }

        private fun start(): Run = Run().also { running = it }

        fun startFlight() {
            limits.startFlight()
            areas.startFlight()
        }

        fun endFlight() {
            limits.endFlight()
            areas.endFlight()
        }

        /** Jeden ruch; koniec ruchu wcześniejszego, przerwanego przez nowy, niczego nie zmienia. */
        private inner class Run : MapLibreMap.CancelableCallback {
            override fun onCancel() = settle()

            override fun onFinish() = settle()

            private fun settle() {
                if (running !== this) return
                running = null
                endFlight()
                pad(inset)
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

private fun Density.basemapOf(bounds: CampusBounds?, size: IntSize): Basemap? =
    mapLimits(bounds, size).takeIf { size != IntSize.Zero }?.basemap
