/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.core.model.BuildingCatalog
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.feature.map.data.CampusMapData
import io.github.dostepnykampus.feature.map.navigation.MapFocus

private val ChoiceSaver = listSaver<List<String>, String>(save = { it }, restore = { it })

/**
 * Stan sceny mapy V1: co mapa pokazuje i jak odpowiada na gesty. Układ ekranu tylko go czyta i przekazuje gesty.
 *
 * @property scene dane katalogu z zaznaczeniem; `null`, dopóki nie ma danych mapy
 * @property camera widok do pokazania: zapisany, na zaznaczonym wejściu albo startowy; pod arkuszem V2 zaznaczony
 *   budynek stoi w części nad arkuszem (decyzja 87); `null` bez danych
 * @property limits granice kamery dla terenu i rozmiaru mapy (BR-G5, NFR-39); `null` przed pomiarem mapy
 * @property isCovered arkusz V2 zasłania dół mapy
 * @param choices zapisywana lista budynków do wyboru
 * @param onCamera zapis widoku kamery
 */
internal class MapSceneState(
    val scene: MapScene?,
    val camera: MapCamera?,
    val limits: MapLimits?,
    val isCovered: Boolean,
    private val choices: MutableState<List<String>>,
    private val onCamera: (MapCamera) -> Unit,
) {
    /** Symbole budynków czekających na wybór w arkuszu „Który budynek?” (decyzja 85). */
    val choice: List<String> get() = choices.value

    /** Zapisuje widok [idle] po geście; pod arkuszem V2 nie, więc po „Wstecz” wraca widok sprzed V2 (decyzja 83). */
    fun keep(idle: MapCamera) {
        if (!isCovered) onCamera(idle)
    }

    /** Przesuwa kamerę przyciskiem na [next], w granicach terenu (NFR-53, BR-G5). */
    fun move(next: MapCamera) {
        limits?.run { onCamera(clamp(next)) }
    }

    /**
     * Dotknięcie konturów [symbols]: jeden budynek otwiera [onBuilding], kilka czeka na wybór w arkuszu, a puste
     * miejsce zgłasza [onEmpty] (decyzja 85, US2.8).
     */
    fun touch(symbols: List<String>, onBuilding: (String) -> Unit, onEmpty: () -> Unit) {
        when (symbols.size) {
            0 -> onEmpty()
            1 -> choose(symbols.single(), onBuilding)
            else -> choices.value = symbols
        }
    }

    /** Budynek [symbol] wybrany w arkuszu albo jedynym dotknięciem: zamyka wybór i otwiera [onBuilding]. */
    fun choose(symbol: String, onBuilding: (String) -> Unit) {
        dismiss()
        onBuilding(symbol)
    }

    /** Zamyka arkusz „Który budynek?” bez wyboru. */
    fun dismiss() {
        choices.value = emptyList()
    }
}

/**
 * Stan sceny mapy z danych [layers], katalogu [catalog] i zaznaczenia [focus] z trasy, dla mapy o rozmiarze [size]
 * zasłoniętej od dołu arkuszem V2 na wysokość [cover]. Widok kamery i lista budynków do wyboru są zapisywane,
 * więc wracają po „Wstecz” z V2 i po obrocie ekranu (decyzja 83, NFR-22).
 */
@Composable
internal fun rememberMapSceneState(
    layers: CampusMapData?,
    catalog: BuildingCatalog?,
    focus: MapFocus?,
    cover: Dp,
    size: IntSize,
): MapSceneState {
    val scene = rememberMapScene(layers = layers, catalog = catalog, focus = focus)
    val selection = scene?.selection
    val view = rememberCameraView(focus, catalog)
    val choices = rememberSaveable(stateSaver = ChoiceSaver) { mutableStateOf(emptyList<String>()) }
    val isCovered = cover > 0.dp
    val focusPoint = remember(catalog, selection?.building) { catalog?.pointOf(selection?.building) }
    val density = LocalDensity.current
    val camera =
        shownCamera(
            own = view.camera ?: selection?.entrance?.let { MapCamera(center = it.location, zoom = ENTRANCE_ZOOM) },
            start = layers?.start,
            focus = focusPoint.takeIf { isCovered },
            bottomInset = with(density) { cover.toPx() }.toDouble(),
        )
    return MapSceneState(
        scene = scene,
        camera = camera,
        limits = density.mapLimits(selection?.bounds, size),
        isCovered = isCovered,
        choices = choices,
        onCamera = view.keep,
    )
}

private fun shownCamera(own: MapCamera?, start: GeoPoint?, focus: GeoPoint?, bottomInset: Double): MapCamera? =
    (own ?: start?.let { MapCamera(center = it, zoom = START_ZOOM) })?.aboveSheet(focus, bottomInset)
