/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.model.BuildingCatalog
import io.github.dostepnykampus.feature.map.data.CampusMapData
import io.github.dostepnykampus.feature.map.navigation.MapFocus

private const val CONTROLS_COLUMNS = 3
private const val FIT_MARGIN_DP = 24.0
private val ControlsMinFreeMap = 300.dp

private val ChoiceSaver = listSaver<List<String>, String>(save = { it }, restore = { it })

/**
 * Stan sceny mapy V1: co mapa pokazuje i jak odpowiada na gesty. Układ ekranu tylko go czyta i przekazuje gesty.
 *
 * @property scene dane katalogu z zaznaczeniem; `null`, dopóki nie ma danych mapy
 * @property camera widok do pokazania: zapisany, na zaznaczonym wejściu albo startowy; nad arkuszem V2 zaznaczony
 *   budynek mieści się w wolnej części mapy, dopóki osoba nie ruszy mapy (decyzja 151); `null` bez danych
 * @property limits granice kamery dla terenu i rozmiaru mapy (BR-G5, NFR-39); `null` przed pomiarem mapy
 * @property isCovered arkusz V2 zasłania dół mapy
 * @param choices zapisywana lista budynków do wyboru
 * @param onCamera zapis widoku kamery V1
 * @property hasControls czy mapa mieści przyciski mapy: zawsze bez arkusza V2, a nad arkuszem przy co najmniej
 *   300 dp wolnej mapy (NFR-53, decyzja 151)
 * @param onCardCamera zapis widoku, który osoba ustawiła nad arkuszem V2
 */
internal class MapSceneState(
    val scene: MapScene?,
    val camera: MapCamera?,
    val limits: MapLimits?,
    val isCovered: Boolean,
    private val choices: MutableState<List<String>>,
    private val onCamera: (MapCamera) -> Unit,
    val hasControls: Boolean = !isCovered,
    private val onCardCamera: (MapCamera) -> Unit = {},
) {
    /** Symbole budynków czekających na wybór w arkuszu „Który budynek?” (decyzja 85). */
    val choice: List<String> get() = choices.value

    /**
     * Zapisuje widok [idle] po geście. Nad arkuszem V2 to widok karty, osobny od widoku V1, więc po „Wstecz” wraca
     * widok sprzed V2 (decyzja 83), a kamera nie wpisuje już budynku na nowo (decyzja 151).
     */
    fun keep(idle: MapCamera) {
        if (isCovered) onCardCamera(idle) else onCamera(idle)
    }

    /** Przesuwa kamerę przyciskiem na [next], w granicach terenu, także nad arkuszem V2 (NFR-53, BR-G5). */
    fun move(next: MapCamera) {
        limits?.run { keep(clamp(next)) }
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
 * Stan sceny mapy z danych [layers], katalogu [catalog], zaznaczenia [focus] z trasy i nazwy terenu [area]
 * wybranego przez osobę (US1.7), dla mapy o rozmiarze [size] zasłoniętej od dołu arkuszem V2 na wysokość
 * [cover]. Widok kamery i lista budynków do wyboru są zapisywane,
 * więc wracają po „Wstecz” z V2 i po obrocie ekranu (decyzja 83, NFR-22).
 */
@Composable
internal fun rememberMapSceneState(
    layers: CampusMapData?,
    catalog: BuildingCatalog?,
    focus: MapFocus?,
    area: String?,
    cover: Dp,
    size: IntSize,
): MapSceneState {
    val scene = rememberMapScene(layers = layers, catalog = catalog, focus = focus, area = area)
    val selection = scene?.selection
    val view = rememberCameraView(focus, layers)
    val choices = rememberSaveable(stateSaver = ChoiceSaver) { mutableStateOf(emptyList<String>()) }
    val isCovered = cover > 0.dp
    val density = LocalDensity.current
    val free = density.freeMap(size, cover)
    val limits = density.mapLimits(selection?.bounds, size)
    val outline = layers?.outlinesBySymbol?.get(selection?.building).takeIf { isCovered }
    val fit = outline?.let { fitted(it, free) }?.let { limits?.clamp(it) ?: it }
    val card = rememberCardCamera(building = selection?.building, fit = fit)
    val own = view.camera ?: selection?.entrance?.let { MapCamera(center = it.location, zoom = ENTRANCE_ZOOM) }
    val uncovered = own ?: layers?.start?.let { MapCamera(center = it, zoom = START_ZOOM) }
    val inset = with(density) { cover.toPx() }.toDouble()
    return MapSceneState(
        scene = scene,
        camera = card.camera?.takeIf { isCovered }?.copy(bottomInset = inset) ?: uncovered,
        limits = limits,
        isCovered = isCovered,
        choices = choices,
        onCamera = view.keep,
        hasControls = free.hasRoomForControls,
        onCardCamera = card.keep,
    )
}

/**
 * Zapisywany widok mapy nad arkuszem V2 budynku [building]: [fit], dopóki osoba nie ruszy mapy gestem albo
 * przyciskiem, potem jej widok (US2.8, NFR-53, decyzja 151). Inny budynek zaczyna od swojego [fit].
 */
@Composable
internal fun rememberCardCamera(building: String?, fit: MapCamera?): CameraView {
    var moved by rememberSaveable(building, stateSaver = CameraSaver) { mutableStateOf<MapCamera?>(null) }
    return CameraView(camera = moved ?: fit, keep = { moved = it })
}

/** Wolna część mapy o rozmiarze [size] w pikselach nad arkuszem V2 wysokim na [cover]. */
internal fun Density.freeMap(size: IntSize, cover: Dp): FreeMap {
    val width = size.width.toDp()
    val height = size.height.toDp() - cover
    val controls = MapControlTouchSize * CONTROLS_COLUMNS + Odstepy.s * 2
    val hasControls = cover == 0.dp || height >= ControlsMinFreeMap
    return FreeMap(
        widthDp = width.value.toDouble(),
        heightDp = height.value.toDouble(),
        endDp = if (hasControls) controls.value.toDouble() else 0.0,
        marginDp = FIT_MARGIN_DP,
    )
}
