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
import androidx.compose.runtime.rememberUpdatedState
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
import io.github.dostepnykampus.feature.map.camera.MapInset
import io.github.dostepnykampus.feature.map.camera.MapLimits
import io.github.dostepnykampus.feature.map.camera.MapMotion
import io.github.dostepnykampus.feature.map.camera.START_ZOOM
import io.github.dostepnykampus.feature.map.camera.belowTop
import io.github.dostepnykampus.feature.map.camera.besideStart
import io.github.dostepnykampus.feature.map.camera.byControl
import io.github.dostepnykampus.feature.map.camera.cameraTransition
import io.github.dostepnykampus.feature.map.camera.fitted
import io.github.dostepnykampus.feature.map.camera.mapLimits
import io.github.dostepnykampus.feature.map.camera.pickLine
import io.github.dostepnykampus.feature.map.camera.placed
import io.github.dostepnykampus.feature.map.camera.rememberCameraView
import io.github.dostepnykampus.feature.map.camera.within
import io.github.dostepnykampus.feature.map.campus.CampusBounds
import io.github.dostepnykampus.feature.map.campus.CampusMapData
import io.github.dostepnykampus.feature.map.campus.selection
import io.github.dostepnykampus.feature.map.navigation.MapFocus
import io.github.dostepnykampus.feature.map.overlay.MapControlTouchSize
import timber.log.Timber

private const val CONTROLS_COLUMNS = 3
private const val FIT_MARGIN_DP = 24.0
private val ControlsMinFreeMap = 300.dp
private val FloorColumnReserve = 80.dp

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

    /** Zasłonięte krawędzie mapy, zob. [SceneCamera.inset]. */
    val inset: MapInset get() = shown?.inset ?: MapInset()

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

    /**
     * Pokazuje teren wybrany z listy: wszystkie jego budynki [buildings] w wolnej części mapy, w powiększeniu
     * najwyżej startowym, przy którym symbole budynków są czytelne.
     *
     * Wymagania: US1.7, BR-G5
     */
    fun land(buildings: CampusBounds) {
        shown?.let { keep(fitted(outline = buildings, free = it.landing, maxZoom = START_ZOOM)) }
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
 * [cover]. Nad arkuszem budynek mieści się też nad napisem o zmianie terenu wysokim na [notice]. Widok kamery
 * i lista budynków do wyboru są zapisywane, więc wracają po „Wstecz” z V2 i po obrocie ekranu. Każdy ruch nad kartą
 * do nowego budynku trafia do [motion].
 *
 * Wymagania: US1.7, US1.8, NFR-22, NFR-56
 */
@Composable
internal fun rememberMapSceneState(
    layers: CampusMapData?,
    catalog: BuildingCatalog?,
    focus: MapFocus?,
    area: String?,
    cover: Dp,
    size: IntSize,
    top: Dp = 0.dp,
    motion: MapMotion? = null,
    notice: Dp = 0.dp,
): MapSceneState {
    val scene = rememberMapScene(layers = layers, catalog = catalog, focus = focus, area = area)
    val selection = scene?.selection
    val view = rememberCameraView(focus, layers)
    val choices = rememberSaveable(stateSaver = ChoiceSaver) { mutableStateOf(emptyList<String>()) }
    val isCovered = cover > 0.dp
    val density = LocalDensity.current
    val shown =
        view.camera?.let {
            density.besideFloorColumn(camera = it, symbol = view.shown, catalog = catalog, layers = layers, size = size)
        }
    val own = shown ?: selection?.entrance?.let { MapCamera(center = it.location, zoom = ENTRANCE_ZOOM) }
    val topInset = with(density) { top.toPx() }.toDouble()
    val limits = density.mapLimits(selection?.bounds, size.belowTop(topInset))
    val start = layers?.start?.let { MapCamera(center = it, zoom = START_ZOOM) }
    val uncovered = (own ?: start)?.let { limits?.clamp(it) ?: it }
    val reserve = rememberNoticeReserve(building = selection?.building, notice = notice)
    val bottomInset = with(density) { (cover + reserve).toPx() }.toDouble()
    val free = density.freeMap(size, cover)
    val fit = free.copy(heightDp = free.heightDp - reserve.value)
    val outline = layers?.run { outlinesBySymbol[selection?.building] }.takeIf { isCovered }
    val last = remember { ShownCamera() }
    val isCoveredBefore = rememberPrevious(isCovered)
    val scale = rememberAnimatorScale()
    val placing = remember { PlacedPick(isPending = isCovered) }
    val pick =
        remember(key1 = selection?.building, key2 = outline != null, key3 = size != IntSize.Zero) {
            val from = last.startOr(uncovered)
            pickMove(from = from, outline = outline, free = fit, scale = scale, keepsZoom = isCoveredBefore)
                ?.within(limits, free = fit)
                ?.let { placing.take(move = it, scale = scale) }
        }
    motion.RecordPick(pick) {
        pickLine(building = selection?.building, area = area, limits = limits, free = fit, move = it)
    }
    val card = rememberCardMove(building = selection?.building, pick = pick)
    val above = card.move?.takeIf { isCovered }
    val move = above ?: uncovered?.byControl()
    SideEffect { last.keep(move?.camera) }
    val inset = sheetOrBarInset(isAbove = above != null, top = topInset, bottom = bottomInset)
    return MapSceneState(
        scene = scene,
        shown = move?.let { SceneCamera(move = it, inset = inset, landing = fit) },
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
 * @property inset zasłonięte krawędzie mapy: dół zasłonięty arkuszem V2, gdy [move] stawia budynek nad arkuszem
 * @property landing wolna część mapy, w której staje widok po wyborze terenu
 */
internal data class SceneCamera(val move: CameraTransition, val inset: MapInset, val landing: FreeMap)

private fun sheetOrBarInset(isAbove: Boolean, top: Double, bottom: Double): MapInset =
    if (isAbove) MapInset(bottom = bottom) else MapInset(top = top)

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

/**
 * Wysokość napisu o zmianie terenu [notice] z chwili wyboru budynku [building]. Zostaje do wyboru innego budynku,
 * więc zgaszenie napisu nie zmienia ruchu ani dopełnienia kamery.
 *
 * Wymagania: US1.8, US2.8, NFR-53
 */
@Composable
internal fun rememberNoticeReserve(building: String?, notice: Dp): Dp = remember(building) { notice }

/** Ruch mapy nad arkuszem V2 [move] i zapis widoku po ruchu osoby [keep]. */
internal data class CardMove(val move: CameraTransition?, val keep: (MapCamera) -> Unit)

/**
 * Pierwszy wybór na mapie, która powstała już pod kartą, czyli po wyborze z listy budynków: widok od razu, bez ruchu
 * kamery ([placed]). Następne wybory przy otwartej karcie ruszają mapę zwykle.
 */
private class PlacedPick(private var isPending: Boolean) {
    fun take(move: CameraTransition, scale: Float): CameraTransition {
        val taken = if (isPending) move.placed(animationScale = scale) else move
        isPending = false
        return taken
    }
}

/** Ostatni widok podany mapie: początek ruchu do następnego wybranego budynku. Nie jest stanem kompozycji. */
private class ShownCamera {
    private var camera: MapCamera? = null

    fun startOr(fallback: MapCamera?): MapCamera? = camera ?: fallback

    fun keep(shown: MapCamera?) {
        camera = shown
    }
}

@Composable
private fun MapMotion?.RecordPick(pick: CameraTransition?, line: (CameraTransition) -> String) {
    val motion = this
    val currentLine by rememberUpdatedState(line)
    DisposableEffect(motion, pick) {
        if (pick != null) Timber.d(currentLine(pick))
        if (motion != null && pick != null) motion.record(pick)
        onDispose {}
    }
}

private fun pickMove(
    from: MapCamera?,
    outline: CampusBounds?,
    free: FreeMap,
    scale: Float,
    keepsZoom: Boolean,
): CameraTransition? {
    if (outline == null || from == null || !free.isMeasured) return null
    return cameraTransition(from = from, outline = outline, free = free, animationScale = scale, keepsZoom = keepsZoom)
}

/** Skala animacji systemu („Skala długości animacji”); 0 przy „Usuń animacje”. Wymagania: NFR-56 */
@Composable
internal fun rememberAnimatorScale(): Float {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) { Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) }
}

/**
 * Widok [camera] po poleceniu „Pokaż budynek na mapie” dla budynku [symbol]: budynek szczegółowy z katalogu
 * [catalog] stoi obok pasa kolumny pięter przy lewym brzegu mapy o rozmiarze [size] w pikselach, z konturem
 * z [layers]. Budynek podstawowy, nieznany albo mapa bez rozmiaru zostawiają [camera].
 *
 * Wymagania: US16.1, US1.3, US1.4, NFR-17
 */
internal fun Density.besideFloorColumn(
    camera: MapCamera,
    symbol: String?,
    catalog: BuildingCatalog?,
    layers: CampusMapData?,
    size: IntSize,
): MapCamera {
    val outline = layers?.outlinesBySymbol.orEmpty()[symbol].takeIf { symbol in catalog?.detailed.orEmpty() }
    val width = size.width.toDp()
    val widthDp = width.value.toDouble()
    val reserve = FloorColumnReserve * fontScale.coerceAtLeast(minimumValue = 1f)
    val startDp = reserve.value.toDouble()
    if (outline == null || widthDp <= startDp) return camera
    return camera.besideStart(outline = outline, widthDp = widthDp, startDp = startDp)
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
