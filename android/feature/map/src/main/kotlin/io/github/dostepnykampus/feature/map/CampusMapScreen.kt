/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import io.github.dostepnykampus.core.designsystem.Odstepy
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
 * Ekran V1 „Mapa kampusu” (US1): kontury z pliku w aplikacji, przyciski powiększania i przesuwania (NFR-53)
 * i podpis OSM (NFR-37). Mapa jest pod paskiem systemowym, przyciski i podpis w `WindowInsets.safeDrawing`.
 * Widok kamery jest stanem zapisywanym, więc wraca po „Wstecz” z V2 (decyzja 83).
 *
 * @param repository katalog budynków z zewnątrz, więc test podaje własny kontekst wczytywania
 * @param onBuildingClick symbol budynku, gdy dotknięcie z tolerancją 24 dp trafiło jeden kontur, albo budynku
 *   wybranego w arkuszu „Który budynek?”, gdy trafiło kontur wspólny albo dwa kontury (US2, decyzja 85)
 * @param modifier wymiary ekranu nadane przez aplikację
 * @param focus symbol budynku zaznaczonego obrysem, np. po „Wstecz” z V2 (US1.1)
 * @param contentPadding część ekranu zasłonięta z zewnątrz, np. przez arkusz V2 (decyzja 87). Przyciski i podpis
 *   OSM stoją nad nią, a mapa dalej wypełnia cały ekran (NFR-37, NFR-53)
 * @param mapView widok mapy; testy ekranu podają widok bez natywnej biblioteki
 */
@Composable
fun CampusMapScreen(
    repository: BuildingCatalogRepository,
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
    Box(modifier.fillMaxSize()) {
        val loaded = data
        val shown = camera ?: loaded?.start?.let { MapCamera(center = it, zoom = START_ZOOM) }
        if (loaded != null && shown != null) {
            mapView.Show(
                data = loaded,
                camera = shown,
                onCameraIdle = { camera = it },
                onBuildingsTouch = { symbols ->
                    if (symbols.size > 1) choice = symbols else symbols.singleOrNull()?.let(onBuildingClick)
                },
                modifier = Modifier.fillMaxSize(),
            )
            Box(Modifier.fillMaxSize().safeDrawingPadding().padding(contentPadding)) {
                MapControls(shown, onCameraChange = { camera = it }, modifier = Modifier.align(Alignment.TopEnd))
                OsmAttribution(Modifier.align(Alignment.BottomStart))
                val buildings = catalog?.buildings.orEmpty()
                val chosen = choice.mapNotNull { symbol -> buildings.firstOrNull { it.symbol == symbol } }
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
