/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.model.building.BuildingCatalog
import io.github.dostepnykampus.feature.map.camera.CameraSaver
import io.github.dostepnykampus.feature.map.camera.CameraTransition
import io.github.dostepnykampus.feature.map.camera.ENTRANCE_ZOOM
import io.github.dostepnykampus.feature.map.camera.FreeMap
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.camera.MapLimits
import io.github.dostepnykampus.feature.map.camera.MapMotion
import io.github.dostepnykampus.feature.map.camera.START_ZOOM
import io.github.dostepnykampus.feature.map.camera.byControl
import io.github.dostepnykampus.feature.map.camera.cameraTransition
import io.github.dostepnykampus.feature.map.camera.mapLimits
import io.github.dostepnykampus.feature.map.camera.rememberCameraView
import io.github.dostepnykampus.feature.map.campus.CampusBounds
import io.github.dostepnykampus.feature.map.campus.CampusMapData
import io.github.dostepnykampus.feature.map.campus.selection
import io.github.dostepnykampus.feature.map.navigation.MapFocus
import io.github.dostepnykampus.feature.map.overlay.MapControlTouchSize

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
 * @property shown ruch kamery do pokazania z zasłoniętym dołem mapy; `null` bez danych
 * @property limits granice kamery dla terenu i rozmiaru mapy; `null` przed pomiarem mapy
 * @property cover czy arkusz V2 zasłania dół mapy i czy nad nim mieszczą się przyciski mapy
 * @param choices zapisywana lista budynków do wyboru
 * @param onCamera zapis widoku kamery po ruchu osoby, zob. [cameraKeeper]
 */
internal class MapSceneState(
    val scene: MapScene?,
    val shown: SceneCamera?,
    val limits: MapLimits?,
    val cover: MapCover,
    private val choices: MutableState<List<String>>,
    private val onCamera: (MapCamera) -> Unit,
) {
    /** Ruch kamery do pokazania, zob. [SceneCamera.move]; `null` bez danych. */
    val move: CameraTransition? get() = shown?.move

    /** Widok do pokazania, cel [move]. */
    val camera: MapCamera? get() = move?.camera

    /** Zasłonięty dół mapy w pikselach, zob. [SceneCamera.bottomInset]. */
    val bottomInset: Double get() = shown?.bottomInset ?: 0.0

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
 * więc wracają po „Wstecz” z V2 i po obrocie ekranu. Każdy ruch nad kartą do nowego budynku trafia do [motion].
 *
 * Wymagania: US1.7, NFR-22, NFR-56
 */
@Composable
internal fun rememberMapSceneState(
    layers: CampusMapData?,
    catalog: BuildingCatalog?,
    focus: MapFocus?,
    area: String?,
    cover: Dp,
    size: IntSize,
    motion: MapMotion? = null,
): MapSceneState {
    val scene = rememberMapScene(layers = layers, catalog = catalog, focus = focus, area = area)
    val selection = scene?.selection
    val view = rememberCameraView(focus, layers)
    val choices = rememberSaveable(stateSaver = ChoiceSaver) { mutableStateOf(emptyList<String>()) }
    val isCovered = cover > 0.dp
    val density = LocalDensity.current
    val own = view.camera ?: selection?.entrance?.let { MapCamera(center = it.location, zoom = ENTRANCE_ZOOM) }
    val limits = density.mapLimits(selection?.bounds, size)
    val start = layers?.start?.let { MapCamera(center = it, zoom = START_ZOOM) }
    val uncovered = (own ?: start)?.let { limits?.clamp(it) ?: it }
    val inset = with(density) { cover.toPx() }.toDouble()
    val free = density.freeMap(size, cover)
    val outline = layers?.run { outlinesBySymbol[selection?.building] }.takeIf { isCovered }
    val last = remember { ShownCamera() }
    val scale = rememberAnimatorScale()
    val pick =
        remember(selection?.building, outline != null) {
            pickMove(from = last.startOr(uncovered), outline = outline, free = free, scale = scale, limits = limits)
        }
    motion.RecordPick(pick)
    val card = rememberCardMove(building = selection?.building, pick = pick)
    val above = card.move?.takeIf { isCovered }
    val move = above ?: uncovered?.byControl()
    SideEffect { last.keep(move?.camera) }
    return MapSceneState(
        scene = scene,
        shown = move?.let { SceneCamera(it, bottomInset = if (above == null) 0.0 else inset) },
        limits = limits,
        cover = mapCover(isCovered, hasControls = free.hasRoomForControls),
        choices = choices,
        onCamera = cameraKeeper(isCovered, onMap = view.keep, onCard = card.keep),
    )
}

/**
 * Ruch mapy V1 do pokazania.
 *
 * @property move ruch kamery do widoku: nad arkuszem V2 do zaznaczonego budynku według [cameraTransition], liczony raz
 *   na wybór, dopóki osoba nie ruszy mapy; poza tym przelot przycisków ([byControl]) do widoku zapisanego, na
 *   zaznaczonym wejściu albo startowego
 * @property bottomInset zasłonięty arkuszem V2 dół mapy w pikselach, gdy [move] stawia budynek nad arkuszem; 0 bez
 *   tego
 */
internal data class SceneCamera(val move: CameraTransition, val bottomInset: Double = 0.0)

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
 * ustawia już budynku na nowo. Trafia też do [onMap], więc po zamknięciu karty zostaje widok osoby, a widok
 * sprzed karty wraca tylko wtedy, gdy osoba nie ruszyła mapy.
 */
internal fun cameraKeeper(
    isCovered: Boolean,
    onMap: (MapCamera) -> Unit,
    onCard: (MapCamera) -> Unit,
): (MapCamera) -> Unit =
    { idle ->
        if (isCovered) onCard(idle)
        onMap(idle)
    }

/**
 * Ruch mapy nad arkuszem V2 budynku [building]: [pick], dopóki osoba nie ruszy mapy gestem albo przyciskiem, potem
 * przelot do jej zapisanego widoku. Inny budynek zaczyna od swojego [pick].
 *
 * Wymagania: US2.8, NFR-53, NFR-56
 */
@Composable
internal fun rememberCardMove(building: String?, pick: CameraTransition?): CardMove {
    var moved by rememberSaveable(building, stateSaver = CameraSaver) { mutableStateOf<MapCamera?>(null) }
    return CardMove(move = moved?.byControl() ?: pick, keep = { moved = it })
}

/** Ruch mapy nad arkuszem V2 [move] i zapis widoku po ruchu osoby [keep]. */
internal data class CardMove(val move: CameraTransition?, val keep: (MapCamera) -> Unit)

/** Ostatni widok podany mapie: początek ruchu do następnego wybranego budynku. Nie jest stanem kompozycji. */
private class ShownCamera {
    private var camera: MapCamera? = null

    fun startOr(fallback: MapCamera?): MapCamera? = camera ?: fallback

    fun keep(shown: MapCamera?) {
        camera = shown
    }
}

@Composable
private fun MapMotion?.RecordPick(pick: CameraTransition?) {
    val motion = this
    DisposableEffect(motion, pick) {
        if (motion != null && pick != null) motion.record(pick)
        onDispose {}
    }
}

private fun pickMove(
    from: MapCamera?,
    outline: CampusBounds?,
    free: FreeMap,
    scale: Float,
    limits: MapLimits?,
): CameraTransition? {
    if (outline == null || from == null) return null
    val move = cameraTransition(from = from, outline = outline, free = free, animationScale = scale)
    return limits?.let { move.copy(camera = it.clamp(move.camera)) } ?: move
}

@Composable
private fun rememberAnimatorScale(): Float {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) { Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) }
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
