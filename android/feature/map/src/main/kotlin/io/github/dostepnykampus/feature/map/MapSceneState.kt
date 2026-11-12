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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.core.model.BuildingCatalog
import io.github.dostepnykampus.feature.map.data.CampusMapData
import io.github.dostepnykampus.feature.map.navigation.MapFocus

private val ControlsMinFreeMap = 300.dp

private val ChoiceSaver = listSaver<List<String>, String>(save = { it }, restore = { it })

/**
 * Stan sceny mapy V1: co mapa pokazuje i jak odpowiada na gesty. Układ ekranu tylko go czyta i przekazuje gesty.
 *
 * @property scene dane katalogu z zaznaczeniem; `null`, dopóki nie ma danych mapy
 * @property camera widok do pokazania: zapisany, na zaznaczonym wejściu albo startowy; nad arkuszem V2 zaznaczony
 *   budynek stoi w części nad arkuszem (decyzja 87), dopóki osoba nie ruszy mapy (decyzja 151); `null` bez danych
 * @property limits granice kamery dla terenu i rozmiaru mapy (BR-G5, NFR-39); `null` przed pomiarem mapy
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

    /** Mapa ma miejsce na przyciski powiększania i przesuwania (NFR-53, decyzja 151). */
    val hasControls: Boolean get() = cover != MapCover.SHEET_WITHOUT_CONTROLS

    /** Symbole budynków czekających na wybór w arkuszu „Który budynek?” (decyzja 85). */
    val choice: List<String> get() = choices.value

    /** Zapisuje widok [idle] po geście osoby. */
    fun keep(idle: MapCamera) {
        onCamera(idle)
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
    val focusPoint = remember(layers, selection?.building) { layers?.pointOf(selection?.building) }
    val own = view.camera ?: selection?.entrance?.let { MapCamera(center = it.location, zoom = ENTRANCE_ZOOM) }
    val uncovered = own ?: layers?.start?.let { MapCamera(center = it, zoom = START_ZOOM) }
    val inset = with(density) { cover.toPx() }.toDouble()
    val onBuilding = uncovered.takeIf { isCovered && focusPoint != null }?.aboveSheet(focusPoint, inset)
    val card = rememberCardCamera(building = selection?.building, fit = onBuilding)
    val hasRoom = with(density) { size.height.toDp() } - cover >= ControlsMinFreeMap
    return MapSceneState(
        scene = scene,
        camera = card.camera?.takeIf { isCovered }?.copy(bottomInset = inset) ?: uncovered,
        limits = density.mapLimits(selection?.bounds, size),
        cover = mapCover(isCovered, hasControls = hasRoom),
        choices = choices,
        onCamera = cameraKeeper(isCovered, onMap = view.keep, onCard = card.keep),
    )
}

/**
 * Zasłonięcie mapy V1 przez arkusz V2. Nad arkuszem przyciski mapy stoją tylko przy co najmniej 300 dp wolnej mapy
 * (US2.8, NFR-53, decyzja 151).
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
 * ustawia już budynku na nowo (decyzja 151). Trafia też do [onMap], bez dopełnienia arkusza, więc po zamknięciu
 * karty zostaje widok osoby, a widok sprzed karty wraca tylko wtedy, gdy osoba nie ruszyła mapy (decyzje 83 i 153).
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
 * przyciskiem, potem jej widok (US2.8, NFR-53, decyzja 151). Inny budynek zaczyna od swojego [fit].
 */
@Composable
internal fun rememberCardCamera(building: String?, fit: MapCamera?): CameraView {
    var moved by rememberSaveable(building, stateSaver = CameraSaver) { mutableStateOf<MapCamera?>(null) }
    return CameraView(camera = moved ?: fit, keep = { moved = it })
}
