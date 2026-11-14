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
import io.github.dostepnykampus.feature.map.controls.MapControlTouchSize
import io.github.dostepnykampus.feature.map.data.CampusMapData
import io.github.dostepnykampus.feature.map.data.ENTRANCE_ZOOM
import io.github.dostepnykampus.feature.map.data.FreeMap
import io.github.dostepnykampus.feature.map.data.MapCamera
import io.github.dostepnykampus.feature.map.data.MapLimits
import io.github.dostepnykampus.feature.map.data.START_ZOOM
import io.github.dostepnykampus.feature.map.data.fitted
import io.github.dostepnykampus.feature.map.navigation.MapFocus

private const val CONTROLS_COLUMNS = 3
private const val FIT_MARGIN_DP = 24.0
private val ControlsMinFreeMap = 300.dp

private val ChoiceSaver = listSaver<List<String>, String>(save = { it }, restore = { it })

/**
 * Stan sceny mapy V1: co mapa pokazuje i jak odpowiada na gesty. Układ ekranu tylko go czyta i przekazuje gesty.
 *
 * Wymagania: BR-G5, NFR-39
 *
 * @property scene dane katalogu z zaznaczeniem; `null`, dopóki nie ma danych mapy
 * @property camera widok do pokazania: zapisany, na zaznaczonym wejściu albo startowy; nad arkuszem V2 zaznaczony
 *   budynek mieści się w wolnej części mapy, dopóki osoba nie ruszy mapy; `null` bez danych
 * @property limits granice kamery dla terenu i rozmiaru mapy; `null` przed pomiarem mapy
 * @property cover czy arkusz V2 zasłania dół mapy i czy nad nim mieszczą się przyciski mapy
 * @param choices zapisywana lista budynków do wyboru
 * @param onCamera zapis widoku kamery po ruchu osoby, zob. [cameraKeeper]
 */
internal class MapSceneState(
    val scene: MapScene?,
    val camera: MapCamera?,
    val limits: MapLimits?,
    val cover: MapCover,
    private val choices: MutableState<List<String>>,
    private val onCamera: (MapCamera) -> Unit,
) {
    /** Arkusz V2 zasłania dół mapy. */
    val isCovered: Boolean get() = cover != MapCover.NONE

    /** Mapa ma miejsce na przyciski powiększania i przesuwania. Wymagania: NFR-53 */
    val hasControls: Boolean get() = cover != MapCover.SHEET_WITHOUT_CONTROLS

    /** Symbole budynków czekających na wybór w arkuszu „Który budynek?”. */
    val choice: List<String> get() = choices.value

    /** Zapisuje widok [idle] po geście osoby. */
    fun keep(idle: MapCamera) {
        onCamera(idle)
    }

    /** Przesuwa kamerę przyciskiem na [next], w granicach terenu, także nad arkuszem V2. Wymagania: NFR-53, BR-G5 */
    fun move(next: MapCamera) {
        limits?.run { keep(clamp(next)) }
    }

    /**
     * Dotknięcie konturów [symbols]: jeden budynek otwiera [onBuilding], kilka czeka na wybór w arkuszu, a puste
     * miejsce zgłasza [onEmpty].
     *
     * Wymagania: US2.8
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
 * wybranego przez osobę, dla mapy o rozmiarze [size] zasłoniętej od dołu arkuszem V2 na wysokość
 * [cover]. Widok kamery i lista budynków do wyboru są zapisywane,
 * więc wracają po „Wstecz” z V2 i po obrocie ekranu.
 *
 * Wymagania: US1.7, NFR-22
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
    val own = view.camera ?: selection?.entrance?.let { MapCamera(center = it.location, zoom = ENTRANCE_ZOOM) }
    val uncovered = own ?: layers?.start?.let { MapCamera(center = it, zoom = START_ZOOM) }
    val inset = with(density) { cover.toPx() }.toDouble()
    val free = density.freeMap(size, cover)
    val limits = density.mapLimits(selection?.bounds, size)
    val outline = layers?.run { outlinesBySymbol[selection?.building] }.takeIf { isCovered }
    val fit = outline?.run { fitted(this, free) }
    val card = rememberCardCamera(building = selection?.building, fit = fit?.run { limits?.clamp(this) ?: this })
    return MapSceneState(
        scene = scene,
        camera = card.camera?.takeIf { isCovered }?.copy(bottomInset = inset) ?: uncovered,
        limits = limits,
        cover = mapCover(isCovered, hasControls = free.hasRoomForControls),
        choices = choices,
        onCamera = cameraKeeper(isCovered, onMap = view.keep, onCard = card.keep),
    )
}

/**
 * Zasłonięcie mapy V1 przez arkusz V2. Nad arkuszem przyciski mapy stoją tylko przy co najmniej 300 dp wolnej mapy.
 *
 * Wymagania: US2.8, NFR-53
 */
internal enum class MapCover { NONE, SHEET, SHEET_WITHOUT_CONTROLS }

/** Zasłonięcie mapy: bez arkusza ([isCovered]) żadne, z arkuszem zależne od miejsca na przyciski [hasControls]. */
internal fun mapCover(isCovered: Boolean, hasControls: Boolean): MapCover =
    when {
        !isCovered -> MapCover.NONE
        hasControls -> MapCover.SHEET
        else -> MapCover.SHEET_WITHOUT_CONTROLS
    }

/**
 * Zapis widoku kamery po ruchu osoby. Nad arkuszem V2 ([isCovered]) widok trafia do [onCard], więc kamera nie
 * ustawia już budynku na nowo. Trafia też do [onMap], bez dopełnienia arkusza, więc po zamknięciu
 * karty zostaje widok osoby, a widok sprzed karty wraca tylko wtedy, gdy osoba nie ruszyła mapy.
 */
internal fun cameraKeeper(
    isCovered: Boolean,
    onMap: (MapCamera) -> Unit,
    onCard: (MapCamera) -> Unit,
): (MapCamera) -> Unit =
    { idle ->
        if (isCovered) onCard(idle)
        onMap(idle.copy(bottomInset = 0.0))
    }

/**
 * Zapisywany widok mapy nad arkuszem V2 budynku [building]: [fit], dopóki osoba nie ruszy mapy gestem albo
 * przyciskiem, potem jej widok. Inny budynek zaczyna od swojego [fit].
 *
 * Wymagania: US2.8, NFR-53
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
