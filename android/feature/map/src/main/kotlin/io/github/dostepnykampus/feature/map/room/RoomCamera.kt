/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.room

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.core.geo.allRings
import io.github.dostepnykampus.core.model.GeoArea
import io.github.dostepnykampus.core.model.room.FoundRoom
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.camera.MapInset
import io.github.dostepnykampus.feature.map.camera.dpPerDegree
import io.github.dostepnykampus.feature.map.camera.mercatorY
import io.github.dostepnykampus.feature.map.camera.shiftedNorth
import io.github.dostepnykampus.feature.map.campus.ShownPlan
import io.github.dostepnykampus.feature.map.navigation.MapFocus
import io.github.dostepnykampus.feature.map.plan.MapPanel
import kotlin.math.PI

private const val REVEAL_MARGIN_DP = 16.0
private const val HALF_TURN_DEGREES = 180.0

/**
 * Ostatni widok mapy w spoczynku i sala dotknięta na planie, zapamiętane między czynnościami osoby. Dotknięcie
 * zostaje, aż zamknie się otwarta karta; karta otwiera się chwilę po dotknięciu, gdy czeka na drogę.
 */
internal class RoomMemory {
    var idle: MapCamera? = null
    var inset = MapInset()
    var tapped: String? = null
    private var isOpen = false

    /** Scena [scene] pokazuje dotknięte na planie: arkusz znaku planu albo kartę dotkniętej sali bez kroku. */
    fun isTouched(scene: RoomScene): Boolean =
        scene.sign != null || scene.room?.run { card.step == null && card.found.room.id == tapped } == true

    /** Karta otwarta ([isOpenNow]) albo nie: zamknięcie otwartej karty kasuje dotknięcie. */
    fun follow(isOpenNow: Boolean) {
        if (isOpen && !isOpenNow) tapped = null
        isOpen = isOpenNow
    }
}

/**
 * Dotknięcie na planie w jednej czynności osoby: sala albo znak planu ([isSign]), którego karta albo arkusz leży
 * nad mapą z dopełnieniem kamery [held] sprzed dotknięcia; [isJump], gdy kamera nie rusza się wcale. Bez dotknięcia
 * [held] jest `null`.
 *
 * Wymagania: US18.1, US16.1, US2.8, NFR-56
 */
internal data class PlanTouch(val isJump: Boolean = false, val held: MapInset? = null, val isSign: Boolean = false)

/**
 * Kamera karty sali albo arkusza znaku planu na mapie dla jednej czynności osoby (otwarcie sali, wybór kroku,
 * dotknięcie znaku): widok [camera], karta albo arkusz wysoki na [kept] pikseli z pierwszego pomiaru w tej
 * czynności i dół mapy [mapCover] bieżąco dla przycisków. Karta albo arkusz po dotknięciu na planie ([touch]) leży
 * nad mapą z dopełnieniem kamery sprzed dotknięcia, więc obraz mapy stoi.
 *
 * Wymagania: US18.1, US18.2, US16.1, US2.8, NFR-56
 */
internal class RoomView(
    val camera: MapCamera?,
    private val kept: Int,
    val mapCover: Dp,
    private val touch: PlanTouch,
    private val memory: RoomMemory,
) {
    /** Dół mapy zasłonięty kartą albo arkuszem dla dopełnienia kamery w pikselach. */
    val cover: Int get() = kept

    /** Kamera nie rusza się po dotknięciu na planie. */
    val isJump: Boolean get() = touch.isJump

    /** Dopełnienie kamery sprzed dotknięcia na planie, które zostaje przy dotkniętym; `null` poza dotknięciem. */
    val heldInset: MapInset? get() = touch.held

    /** Widok karty sali; `null` przy arkuszu znaku planu. */
    val roomCamera: MapCamera? get() = camera.takeUnless { touch.isSign }

    /** Widok po dotknięciu znaku planu; `null` bez arkusza znaku. */
    val signCamera: MapCamera? get() = camera.takeIf { touch.isSign }

    /** Mapa stanęła po geście w widoku [view]. */
    fun onIdle(view: MapCamera) {
        memory.idle = view
    }

    /** Mapa dostała od aplikacji widok [view] z dopełnieniem kamery [inset]. */
    fun onShown(view: MapCamera, inset: MapInset) {
        memory.idle = view
        memory.inset = inset
    }

    /** Otwarcie przez [open] sali dotkniętej na planie; ta sala nie rusza potem kamery. */
    fun tapped(open: (FoundRoom) -> Unit): (FoundRoom) -> Unit =
        { found ->
            memory.tapped = found.room.id
            open(found)
        }
}

/**
 * Karta sali [room] przy mapie: na mapie ([isOnMap]) albo na cały ekran, plan piętra [plan] pokazany na mapie
 * i pomieszczenie [sign] znaku planu z arkuszem nad mapą; `null` bez takiego arkusza.
 *
 * Wymagania: US18.1, US18.2, US16.1
 */
internal data class RoomScene(val room: OpenRoom?, val isOnMap: Boolean, val plan: ShownPlan?, val sign: String? = null)

/**
 * Widok karty [scene] przy zaznaczeniu [focus] i dole mapy zasłoniętym według [panel]. Czynność to polecenie
 * [MapFocus.Building.command] albo dotknięcie znaku planu: widzi widok, dopełnienie kamery i plan z chwili przed
 * nią, a wysokość karty z pierwszego pomiaru z drogą, a arkusza znaku z pierwszego pomiaru ([heldWith]), więc
 * późniejsza zmiana wysokości nie rusza mapy. Dotknięta sala i dotknięty znak nie zmieniają ani widoku, ani
 * dopełnienia kamery; tylko gdy karta albo arkusz je zasłoni, mapa wysoka na [height] pikseli przesuwa się w górę
 * ([revealing]).
 *
 * Wymagania: US18.1, US18.2, US16.1, US2.8, NFR-56
 */
@Composable
internal fun rememberRoomView(scene: RoomScene, focus: MapFocus?, panel: MapPanel, height: Int): RoomView {
    val (room, isOnMap, plan) = scene
    val isSign = scene.sign != null
    val memory = remember { RoomMemory() }
    val act = (focus as? MapFocus.Building)?.command to scene.sign
    val seen = remember(act) { memory.idle?.let { SeenPlan(camera = it, plan = plan?.building) } }
    val seenInset = remember(act) { memory.inset }
    val held = remember(act) { IntArray(1) }
    held[0] = held[0].heldWith(cover = panel.cover.takeIf { isSign } ?: panel.wayCover)
    memory.follow(isOpenNow = room != null)
    val kept = if (isOnMap || isSign) held[0] else panel.cover
    val isTouched = memory.isTouched(scene)
    val density = LocalDensity.current
    val keptDp = with(density) { kept.toDp() }
    val screen = density.tapScreen(map = with(density) { height.toDp() }, inset = seenInset, card = keptDp)
    val tap = seen?.takeIf { isTouched }?.tapped(area = scene.touchedArea(), screen = screen)
    val searched = room?.cameraForRoom(isRoomOnMap = isOnMap, cover = keptDp, seen = seen, isTapped = isTouched)
    val mapCover = with(density) { panel.cover.toDp() }.takeIf { isOnMap } ?: 0.dp
    val touch = PlanTouch(isJump = tap?.isJump == true, held = seenInset.takeIf { isTouched }, isSign = isSign)
    return RoomView(camera = tap?.camera ?: searched, kept = kept, mapCover = mapCover, touch = touch, memory = memory)
}

/**
 * Obrys dotkniętego na planie: pomieszczenia znaku planu na pokazanym piętrze, a bez arkusza znaku sali z karty;
 * `null`, gdy takiego pomieszczenia nie ma. Wymagania: US16.1, US18.1
 */
internal fun RoomScene.touchedArea(): GeoArea? {
    val shown = plan?.run { rooms[floor]?.rooms }
    return sign?.let { id -> shown?.firstOrNull { it.id == id }?.geometry } ?: room?.run { card.found.room.geometry }
}

/**
 * Ekran mapy przy dotkniętej sali: mapa wysoka na [map], dopełnienie kamery [top] u góry i [bottom] u dołu, które
 * zostaje, i karta u dołu wysoka na [card], która leży nad mapą.
 */
internal data class TapScreen(val map: Dp, val top: Dp, val bottom: Dp, val card: Dp)

/** Ekran mapy wysokiej na [map] z dopełnieniem kamery [inset] w pikselach i kartą wysoką na [card]. */
internal fun Density.tapScreen(map: Dp, inset: MapInset, card: Dp): TapScreen =
    TapScreen(map = map, top = inset.top.toFloat().toDp(), bottom = inset.bottom.toFloat().toDp(), card = card)

/**
 * Widok [camera] po dotknięciu sali; [isJump], gdy to widok sprzed dotknięcia, czyli kamera nie rusza się wcale.
 * Wymagania: US18.1, US2.8, NFR-56
 */
internal data class TappedView(val camera: MapCamera, val isJump: Boolean)

/**
 * Widok po dotknięciu sali [area] na ekranie [screen]: widok sprzed dotknięcia, a gdy karta zasłoni salę, ten widok
 * przesunięty w górę ([revealing]). Wymagania: US18.1, US2.8, NFR-56
 */
internal fun SeenPlan.tapped(area: GeoArea?, screen: TapScreen): TappedView {
    val shown = camera.revealing(area = area, screen = screen)
    return TappedView(camera = shown, isJump = shown == camera)
}

/**
 * Widok, w którym sala [area] zasłonięta kartą ekranu [screen] stoi dolnym brzegiem 16 dp nad kartą, a górnym
 * najwyżej 16 dp pod górnym dopełnieniem. Środek widoku stoi na środku mapy między dopełnieniami. Ruch jest tylko
 * w pionie i bez zmiany powiększenia. Sala cała widoczna nad kartą, ucięta brzegiem mapy albo bez konturu zostaje.
 *
 * Wymagania: US18.1, US2.8, NFR-56
 */
internal fun MapCamera.revealing(area: GeoArea?, screen: TapScreen): MapCamera {
    val latitudes = area?.run { allRings().flatten().map { it.latitude } }.orEmpty()
    val south = latitudes.minOrNull() ?: return this
    val perRadian = dpPerDegree(zoom) * HALF_TURN_DEGREES / PI
    val middle = mercatorY(center.latitude)
    val map = screen.map.value.toDouble()
    val upper = screen.top.value.toDouble()
    val card = screen.card.value.toDouble()
    val centerDp = upper + (map - upper - screen.bottom.value) / 2
    val bottom = centerDp + (middle - mercatorY(south)) * perRadian
    val top = centerDp + (middle - mercatorY(latitudes.max())) * perRadian
    val isSeen = top >= upper && bottom <= map
    val covered = bottom - (map - card - REVEAL_MARGIN_DP)
    val lift = minOf(a = covered, b = (top - upper - REVEAL_MARGIN_DP).coerceAtLeast(minimumValue = 0.0))
    return if (isSeen && covered > 0) shiftedNorth(-lift) else this
}

/**
 * Wysokość karty zapamiętana dla czynności: pierwszy pomiar [cover] karty tej sali już z drogą ([MapPanel.wayCover]).
 * Karta bez drogi jeszcze rośnie, więc do pomiaru z drogą zostaje 0 i kamera czeka. Wymagania: US18.2, NFR-56
 */
internal fun Int.heldWith(cover: Int): Int = if (this == 0) cover else this
