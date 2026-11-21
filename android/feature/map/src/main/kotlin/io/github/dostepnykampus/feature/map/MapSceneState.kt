/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import io.github.dostepnykampus.feature.map.camera.PickOrigin
import io.github.dostepnykampus.feature.map.camera.START_ZOOM
import io.github.dostepnykampus.feature.map.camera.ShownCamera
import io.github.dostepnykampus.feature.map.camera.TapSight
import io.github.dostepnykampus.feature.map.camera.belowTop
import io.github.dostepnykampus.feature.map.camera.besideStart
import io.github.dostepnykampus.feature.map.camera.byControl
import io.github.dostepnykampus.feature.map.camera.fitted
import io.github.dostepnykampus.feature.map.camera.mapLimits
import io.github.dostepnykampus.feature.map.camera.pickLine
import io.github.dostepnykampus.feature.map.camera.pickView
import io.github.dostepnykampus.feature.map.camera.rememberCameraView
import io.github.dostepnykampus.feature.map.camera.uncoveredMove
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
 * @property taps wybór budynku dotykiem i arkusz „Który budynek?”
 * @param onCamera zapis widoku kamery po ruchu osoby, zob. [cameraKeeper]
 */
internal class MapSceneState(
    val scene: MapScene?,
    val shown: SceneCamera?,
    val limits: MapLimits?,
    val cover: MapCover,
    val taps: MapPicking,
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
}

/**
 * Stan sceny mapy z danych [layers], katalogu [catalog], zaznaczenia [focus] z trasy i nazwy terenu [area] dla mapy
 * o rozmiarze [size] zasłoniętej od dołu arkuszem V2 na [cover] albo innym arkuszem na [sheet] pikseli. Nad arkuszem
 * V2 budynek mieści się też nad napisem o zmianie terenu wysokim na [notice]. Widok kamery i lista budynków do wyboru
 * wracają po obrocie ekranu. Zamknięcie każdego arkusza zostawia obraz mapy bez ruchu: zapisany widok to widok
 * z chwili zamknięcia. Każdy ruch nad kartą do nowego budynku trafia do [motion].
 *
 * Wymagania: US1.7, US1.8, US3.3, NFR-22, NFR-56
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
    sheet: Int = 0,
    roomCamera: MapCamera? = null,
): MapSceneState {
    val scene = rememberMapScene(layers = layers, catalog = catalog, focus = focus, area = area)
    val selection = scene?.selection
    val view = rememberCameraView(focus = focus, layers = layers, roomCamera = roomCamera)
    val choices = rememberSaveable(stateSaver = ChoiceSaver) { mutableStateOf(emptyList<String>()) }
    val isCovered = cover > 0.dp
    val density = LocalDensity.current
    val last = remember { ShownCamera() }
    val (isCoveredBefore, topBefore) = rememberPrevious(isCovered to top)
    val bar = barGrowth(wasCovered = isCoveredBefore, top = top, topBefore = topBefore)
    val (seen, closed) = last.rememberSeen(isCovered = isCovered, bar = bar, sheet = sheet, view = view)
    val shown =
        seen?.let {
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
    val scale = rememberAnimatorScale()
    val origin = remember { PickOrigin(isUnderCard = isCovered) }
    SideEffect { origin.follow(selection?.building) }
    val pick =
        remember(key1 = selection?.building, key2 = outline != null, key3 = size != IntSize.Zero) {
            val from = last.startOr(uncovered)
            val sight = tapSight(wasCovered = isCoveredBefore, bar = topBefore, under = cover + reserve, free = fit)
            pickView(from = from, outline = outline, free = fit, limits = limits, scale = scale)?.let { view ->
                origin.move(building = selection?.building, view = view, keepsZoom = isCoveredBefore, sight = sight)
            }
        }
    motion.RecordPick(pick) {
        pickLine(building = selection?.building, area = area, limits = limits, free = fit, move = it)
    }
    val card = rememberCardMove(building = selection?.building, pick = pick)
    val above = card.move?.takeIf { isCovered }
    val move = above ?: uncovered?.uncoveredMove(closed = closed, from = last.rest)
    val covered =
        density.keptCover(isAboveCard = above != null, isCovered = isCovered, card = cover + reserve, sheet = sheet)
    SideEffect { last.keep(shown = move?.camera, covered = covered) }
    val inset = sheetOrBarInset(isAbove = above != null, top = topInset, bottom = bottomInset, sign = sheet.toDouble())
    return MapSceneState(
        scene = scene,
        shown = move?.let { SceneCamera(move = it, inset = inset, landing = fit) },
        limits = limits,
        cover = mapCover(isCovered, hasControls = free.hasRoomForControls),
        taps = MapPicking(choices = choices, onTap = origin::tap),
        onCamera = cameraKeeper(isCovered, onMap = last.settling(view.keep), onCard = card.keep),
    )
}

/**
 * Ruch mapy V1 do pokazania.
 *
 * @property move ruch kamery do widoku: nad arkuszem V2 do zaznaczonego budynku według [cameraTransition], liczony raz
 *   na wybór, dopóki osoba nie ruszy mapy; po zamknięciu arkusza skok do tego samego obrazu mapy
 *   ([uncoveredMove]); poza tym przelot przycisków ([byControl]) do widoku zapisanego, na zaznaczonym wejściu albo
 *   startowego
 * @property inset zasłonięte krawędzie mapy: dół zasłonięty arkuszem V2, gdy [move] stawia budynek nad arkuszem
 *   V2, a poza tym góra pod paskiem i dół pod arkuszem zaznaczonego wejścia
 * @property landing wolna część mapy, w której staje widok po wyborze terenu
 */
internal data class SceneCamera(val move: CameraTransition, val inset: MapInset, val landing: FreeMap)

private fun sheetOrBarInset(isAbove: Boolean, top: Double, bottom: Double, sign: Double): MapInset =
    if (isAbove) MapInset(bottom = bottom) else MapInset(top = top, bottom = sign)

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
 * ustawia już budynku na nowo. Każdy widok trafia też do [onMap].
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

private fun tapSight(wasCovered: Boolean, bar: Dp, under: Dp, free: FreeMap): TapSight {
    val bottom = free.heightDp + under.value
    return if (wasCovered) {
        TapSight(belowDp = 0.0, topDp = 0.0, bottomDp = bottom)
    } else {
        TapSight(belowDp = (bar + under).value / 2.0, topDp = bar.value.toDouble(), bottomDp = bottom)
    }
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
