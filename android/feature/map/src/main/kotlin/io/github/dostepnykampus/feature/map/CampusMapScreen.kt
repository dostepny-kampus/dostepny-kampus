/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.core.data.buildings.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.network.NetworkStatus
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.geo.labelPoint
import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.core.model.BuildingCatalog
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.feature.map.data.campusMapData

private const val OSM_COPYRIGHT = "https://www.openstreetmap.org/copyright"
private const val SHORT_ATTRIBUTION_FONT_SCALE = 1.5f
private val ButtonSize = 48.dp

private val ChoiceSaver = listSaver<List<String>, String>(save = { it }, restore = { it })

private val CameraSaver =
    listSaver<MapCamera?, Double>(
        save = { camera -> camera?.run { listOf(center.longitude, center.latitude, zoom) }.orEmpty() },
        restore = { saved ->
            saved.takeIf { it.isNotEmpty() }?.run { MapCamera(GeoPoint(longitude = get(0), latitude = get(1)), get(2)) }
        },
    )

/**
 * Ekran V1 „Mapa kampusu” (US1): nagłówek z polem symbolu budynku (US1.1), bez sieci linia „Bez internetu”
 * (US9.1), pod nimi kontury z pliku w aplikacji,
 * przyciski powiększania i przesuwania (NFR-53) i podpis OSM (NFR-37). Przyciski i podpis są
 * w `WindowInsets.safeDrawing`.
 * Widok kamery jest stanem zapisywanym, więc wraca po „Wstecz” z V2 (decyzja 83).
 *
 * @param repository katalog budynków z zewnątrz, więc test podaje własny kontekst wczytywania
 * @param networkStatus stan internetu; bez niego pod polem symbolu stoi linia „Bez internetu” (US9.1, NFR-10)
 * @param onBuildingClick symbol budynku, gdy dotknięcie z tolerancją 24 dp trafiło jeden kontur, albo budynku
 *   wybranego w arkuszu „Który budynek?”, gdy trafiło kontur wspólny albo dwa kontury (US2, decyzja 85)
 * @param modifier wymiary ekranu nadane przez aplikację
 * @param focus symbol budynku zaznaczonego obrysem, np. po „Wstecz” z V2 (US1.1)
 * @param contentPadding część ekranu zasłonięta z zewnątrz przez arkusz V2 (decyzja 87). Podpis OSM stoi nad nią,
 *   a mapa dalej wypełnia cały ekran (NFR-37). Pod arkuszem nie ma przycisków mapy, bo wymagania V2 ich nie mają;
 *   wracają po „Wstecz” (NFR-53). Budynek [focus] staje w środku części nad arkuszem, a po „Wstecz” wraca widok
 *   sprzed otwarcia V2 (decyzja 83)
 * @param mapView widok mapy; testy ekranu podają widok bez natywnej biblioteki
 */
@Composable
fun CampusMapScreen(
    repository: BuildingCatalogRepository,
    networkStatus: NetworkStatus,
    onBuildingClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    focus: String? = null,
    contentPadding: PaddingValues = PaddingValues(),
    mapView: CampusMapView = MapLibreMapView,
) {
    val catalog by produceState<BuildingCatalog?>(initialValue = null, repository) { value = repository.catalog() }
    val layers = remember(catalog) { catalog?.let(::campusMapData) }
    val data = remember(layers, focus) { layers?.copy(focus = focus) }
    var camera by rememberSaveable(stateSaver = CameraSaver) { mutableStateOf<MapCamera?>(null) }
    var choice by rememberSaveable(stateSaver = ChoiceSaver) { mutableStateOf(emptyList<String>()) }
    val cover = contentPadding.calculateBottomPadding()
    val isCovered = cover > 0.dp
    val coverPixels = with(LocalDensity.current) { cover.toPx() }.toDouble()
    val focusPoint = remember(catalog, focus) { catalog?.pointOf(focus) }
    // Na start „jest internet”, żeby linia nie mignęła, zanim przyjdzie stan bieżący.
    val isOnline by remember(networkStatus) { networkStatus.isOnline() }.collectAsState(initial = true)
    // Pod arkuszem widok kamery się nie zapisuje, więc po „Wstecz” wraca widok sprzed V2 (decyzja 83).
    val keepCamera = { idle: MapCamera -> if (!isCovered) camera = idle }
    Column(modifier.fillMaxSize()) {
        // Pod arkuszem V2 nagłówka nie ma, jak w hi-fi V2: mapa sięga wtedy pod pasek stanu.
        if (!isCovered) CampusSearch(catalog, isOnline, onBuildingClick)
        val topInset = WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
        Box(Modifier.weight(1f).fillMaxWidth().consumeWindowInsets(topInset)) {
            val loaded = data
            val shown =
                shownCamera(
                    own = camera,
                    start = loaded?.start,
                    focus = focusPoint.takeIf { isCovered },
                    bottomInset = coverPixels,
                )
            if (loaded != null && shown != null) {
                mapView.Show(
                    data = loaded,
                    camera = shown,
                    onCameraIdle = keepCamera,
                    onBuildingsTouch = { symbols -> choice = touched(symbols, onBuildingClick) },
                    modifier = Modifier.fillMaxSize(),
                )
                MapOverlay(
                    camera = shown,
                    hasControls = !isCovered,
                    onCameraChange = { camera = it },
                    modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(contentPadding),
                ) {
                    val chosen = catalog?.buildingsOf(choice).orEmpty()
                    if (chosen.isNotEmpty()) {
                        val sheetModifier = Modifier.align(Alignment.BottomCenter).padding(Odstepy.s)
                        BuildingChoiceSheet(onClose = { choice = emptyList() }, modifier = sheetModifier) {
                            chosen.forEach { building ->
                                BuildingChoice(building) {
                                    choice = emptyList()
                                    onBuildingClick(building.symbol)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Widok mapy: zapamiętany albo startowy, a pod arkuszem V2 z budynkiem [focus] w części nad arkuszem.
private fun shownCamera(own: MapCamera?, start: GeoPoint?, focus: GeoPoint?, bottomInset: Double): MapCamera? =
    (own ?: start?.let { MapCamera(center = it, zoom = START_ZOOM) })?.aboveSheet(focus, bottomInset)

// Jeden trafiony budynek otwiera V2 od razu, a kilka czeka na wybór w arkuszu „Który budynek?” (decyzja 85).
private fun touched(symbols: List<String>, onBuildingClick: (String) -> Unit): List<String> {
    symbols.singleOrNull()?.let(onBuildingClick)
    return symbols.takeIf { it.size > 1 }.orEmpty()
}

// Budynek [symbol] stoi nad arkuszem V2 punktem podpisu, który zawsze leży wewnątrz konturu.
private fun BuildingCatalog.pointOf(symbol: String?): GeoPoint? =
    buildings.firstOrNull { it.symbol == symbol }?.let { labelPoint(it.outline) }

private fun BuildingCatalog.buildingsOf(symbols: List<String>): List<Building> =
    symbols.mapNotNull { symbol -> buildings.firstOrNull { it.symbol == symbol } }

// Przyciski mapy, gdy są (bez arkusza V2), podpis OSM i [content] nad mapą (NFR-53, NFR-37).
@Composable
private fun MapOverlay(
    camera: MapCamera,
    hasControls: Boolean,
    onCameraChange: (MapCamera) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier) {
        if (hasControls) MapControls(camera, onCameraChange, modifier = Modifier.align(Alignment.TopEnd))
        OsmAttribution(Modifier.align(Alignment.BottomStart))
        content()
    }
}

// Strzałki przesuwania w krzyżu, pod nimi „Powiększ” i „Pomniejsz” (NFR-53, NFR-17, NFR-18, PW-8).
@Composable
private fun MapControls(camera: MapCamera, onCameraChange: (MapCamera) -> Unit, modifier: Modifier = Modifier) {
    val move = { direction: PanDirection -> onCameraChange(camera.moved(direction)) }
    Column(
        modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MapButton(name = R.string.mapa_przesun_w_gore, glyph = "↑") { move(PanDirection.UP) }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            MapButton(name = R.string.mapa_przesun_w_lewo, glyph = "←") { move(PanDirection.LEFT) }
            Spacer(Modifier.size(ButtonSize))
            MapButton(name = R.string.mapa_przesun_w_prawo, glyph = "→") { move(PanDirection.RIGHT) }
        }
        MapButton(name = R.string.mapa_przesun_w_dol, glyph = "↓") { move(PanDirection.DOWN) }
        MapButton(name = R.string.mapa_powieksz, glyph = "+") { onCameraChange(camera.zoomedIn()) }
        MapButton(name = R.string.mapa_pomniejsz, glyph = "−") { onCameraChange(camera.zoomedOut()) }
    }
}

// Przycisk 48 dp ze znakiem; TalkBack czyta tylko nazwę, np. „Przesuń mapę w górę”.
@Composable
private fun MapButton(name: Int, glyph: String, onClick: () -> Unit) {
    val description = stringResource(name)
    FilledTonalIconButton(onClick, Modifier.size(ButtonSize).semantics { contentDescription = description }) {
        Text(text = glyph, modifier = Modifier.clearAndSetSemantics {}, style = MaterialTheme.typography.titleLarge)
    }
}

// „© OpenStreetMap contributors” jako odnośnik, od skali czcionki 1,5 „© OpenStreetMap” (NFR-37).
@Composable
private fun OsmAttribution(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    val isShort = LocalDensity.current.fontScale >= SHORT_ATTRIBUTION_FONT_SCALE
    Surface(modifier.padding(8.dp), shape = MaterialTheme.shapes.small) {
        Text(
            text = stringResource(if (isShort) R.string.mapa_osm_krotko else R.string.mapa_osm),
            modifier =
                Modifier
                    .clickable { uriHandler.openUri(OSM_COPYRIGHT) }
                    .heightIn(min = ButtonSize)
                    .wrapContentHeight()
                    .padding(horizontal = 8.dp),
            maxLines = 1,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
