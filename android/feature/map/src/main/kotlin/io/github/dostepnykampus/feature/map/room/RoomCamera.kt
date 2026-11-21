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

    /** Karta otwarta ([isOpenNow]) albo nie: zamknięcie otwartej karty kasuje dotknięcie. */
    fun follow(isOpenNow: Boolean) {
        if (isOpen && !isOpenNow) tapped = null
        isOpen = isOpenNow
    }
}

/**
 * Kamera karty sali na mapie dla jednej czynności osoby (otwarcie sali, wybór kroku): widok [camera], karta wysoka
 * na [kept] pikseli z pierwszego pomiaru w tej czynności i dół mapy [mapCover] bieżąco dla przycisków. Karta sali
 * dotkniętej na planie leży nad mapą z dopełnieniem kamery [held] sprzed dotknięcia, więc obraz mapy stoi.
 *
 * Wymagania: US18.1, US18.2, US2.8, NFR-56
 */
internal class RoomView(
    val camera: MapCamera?,
    private val kept: Int,
    val mapCover: Dp,
    val isJump: Boolean,
    private val held: MapInset?,
    private val memory: RoomMemory,
) {
    /** Dół mapy zasłonięty kartą dla dopełnienia kamery w pikselach. */
    val cover: Int get() = kept

    /** Dopełnienie kamery sprzed dotknięcia sali, które zostaje przy dotkniętej sali; `null` poza dotknięciem. */
    val heldInset: MapInset? get() = held

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
 * Karta sali [room] przy mapie: na mapie ([isOnMap]) albo na cały ekran, i plan piętra [plan] pokazany na mapie.
 *
 * Wymagania: US18.1, US18.2
 */
internal data class RoomScene(val room: OpenRoom?, val isOnMap: Boolean, val plan: ShownPlan?)

/**
 * Widok karty [scene] przy zaznaczeniu [focus] i dole mapy zasłoniętym według [panel]. Czynność to polecenie
 * [MapFocus.Building.command]: widzi widok, dopełnienie kamery i plan z chwili przed nią, a wysokość karty
 * z pierwszego pomiaru z drogą ([heldWith]), więc późniejsza zmiana wysokości karty nie rusza mapy. Dotknięta sala
 * nie zmienia ani widoku, ani dopełnienia kamery; tylko gdy karta ją zasłoni, mapa wysoka na [height] pikseli
 * przesuwa się w górę ([revealing]).
 *
 * Wymagania: US18.1, US18.2, US2.8, NFR-56
 */
@Composable
internal fun rememberRoomView(scene: RoomScene, focus: MapFocus?, panel: MapPanel, height: Int): RoomView {
    val (room, isOnMap, plan) = scene
    val cover = panel.cover
    val memory = remember { RoomMemory() }
    val command = (focus as? MapFocus.Building)?.command
    val seen = remember(command) { memory.idle?.let { SeenPlan(camera = it, plan = plan?.building) } }
    val seenInset = remember(command) { memory.inset }
    val held = remember(command) { IntArray(1) }
    held[0] = held[0].heldWith(cover = panel.wayCover)
    memory.follow(isOpenNow = room != null)
    val kept = if (isOnMap) held[0] else cover
    val isTapped = room?.run { card.step == null && card.found.room.id == memory.tapped } == true
    val density = LocalDensity.current
    val keptDp = with(density) { kept.toDp() }
    val mapDp = with(density) { height.toDp() }
    val area = room?.run { card.found.room.geometry }
    val screen = density.tapScreen(map = mapDp, inset = seenInset, card = keptDp)
    val tap = seen?.takeIf { isTapped }?.tapped(area = area, screen = screen)
    val searched = room?.cameraForRoom(isRoomOnMap = isOnMap, cover = keptDp, seen = seen, isTapped = isTapped)
    val camera = tap?.camera ?: searched
    val mapCover = with(density) { cover.toDp() }.takeIf { isOnMap } ?: 0.dp
    return RoomView(
        camera = camera,
        kept = kept,
        mapCover = mapCover,
        isJump = tap?.isJump == true,
        held = seenInset.takeIf { isTapped },
        memory = memory,
    )
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
