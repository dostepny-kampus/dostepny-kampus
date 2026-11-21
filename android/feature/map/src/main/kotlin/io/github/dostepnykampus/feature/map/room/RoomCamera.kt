/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.room

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.core.geo.allRings
import io.github.dostepnykampus.core.model.GeoArea
import io.github.dostepnykampus.core.model.room.FoundRoom
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.camera.dpPerDegree
import io.github.dostepnykampus.feature.map.camera.mercatorY
import io.github.dostepnykampus.feature.map.camera.shiftedNorth
import io.github.dostepnykampus.feature.map.campus.ShownPlan
import io.github.dostepnykampus.feature.map.navigation.MapFocus
import kotlin.math.PI

private const val REVEAL_MARGIN_DP = 16.0
private const val HALF_TURN_DEGREES = 180.0

/**
 * Ostatni widok mapy w spoczynku i sala dotknięta na planie, zapamiętane między czynnościami osoby. Dotknięcie
 * zostaje, aż zamknie się otwarta karta ([isOpen]); karta otwiera się chwilę po dotknięciu, gdy czeka na drogę.
 */
internal class RoomMemory {
    var idle: MapCamera? = null
    var tapped: String? = null
    var top: Dp = 0.dp
    var isOpen = false
}

/**
 * Kamera karty sali na mapie dla jednej czynności osoby (otwarcie sali, wybór kroku) i dół mapy zasłonięty kartą:
 * [cover] w pikselach dla kamery z pierwszego pomiaru karty w tej czynności, [mapCover] bieżąco dla przycisków.
 *
 * Wymagania: US18.1, US18.2, US2.8, NFR-56
 */
internal class RoomView(val camera: MapCamera?, val cover: Int, val mapCover: Dp, private val memory: RoomMemory) {
    /** Mapa stanęła w widoku [view]. */
    fun onIdle(view: MapCamera) {
        memory.idle = view
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
 * Widok karty [scene] przy zaznaczeniu [focus], dole mapy zasłoniętym na [cover] pikseli i górze zasłoniętej na
 * [top]. Czynność to polecenie [MapFocus.Building.command]: widzi widok i plan z chwili przed nią, a wysokość karty
 * z pierwszego pomiaru z drogą ([heldWith]), więc późniejsza zmiana wysokości karty nie rusza mapy. Dotknięta sala
 * zostawia obraz mapy w miejscu mimo karty i paska, które zmieniają dopełnienie kamery, a gdy karta ją zasłoni,
 * mapa wysoka na [height] pikseli przesuwa się w górę ([revealing]).
 *
 * Wymagania: US18.1, US18.2, US2.8, NFR-56
 */
@Composable
internal fun rememberRoomView(scene: RoomScene, focus: MapFocus?, cover: Int, top: Dp, height: Int): RoomView {
    val (room, isOnMap, plan) = scene
    val memory = remember { RoomMemory() }
    val command = (focus as? MapFocus.Building)?.command
    val seen = remember(command) { memory.idle?.let { SeenPlan(camera = it, plan = plan?.building) } }
    val held = remember(command) { IntArray(1) }
    held[0] = held[0].heldWith(cover = cover, hasWay = room?.run { card.way } != null)
    if (room == null) {
        if (memory.isOpen) memory.tapped = null
        memory.top = top
    }
    memory.isOpen = room != null
    val kept = if (isOnMap) held[0] else cover
    val isTapped = room?.run { card.step == null && card.found.room.id == memory.tapped } == true
    val density = LocalDensity.current
    val keptDp = with(density) { kept.toDp() }
    val mapDp = with(density) { height.toDp() }
    val area = room?.run { card.found.room.geometry }
    val still = seen?.takeIf { isTapped && kept > 0 }?.tapped(area = area, card = keptDp, bar = memory.top, map = mapDp)
    val camera = still ?: room?.cameraForRoom(isRoomOnMap = isOnMap, cover = keptDp, seen = seen, isTapped = isTapped)
    val mapCover = with(density) { cover.toDp() }.takeIf { isOnMap } ?: 0.dp
    return RoomView(camera = camera, cover = kept, mapCover = mapCover, memory = memory)
}

/**
 * Ten sam obraz mapy po zasłonięciu dołu kartą wysoką na [card] i odsłonięciu góry spod paska wysokiego na [bar]:
 * środek części mapy pod dopełnieniem kamery idzie w górę o połowę obu, więc środek widoku idzie tyle na północ.
 * Wymagania: US18.1, NFR-56
 */
internal fun MapCamera.stillUnder(card: Dp, bar: Dp): MapCamera = shiftedNorth(((card + bar) / 2).value.toDouble())

/**
 * Widok po dotknięciu sali [area] na mapie wysokiej na [map]: ten sam obraz mimo karty [card] i paska [bar]
 * ([stillUnder]), a sala zasłonięta kartą nad nią ([revealing]). Wymagania: US18.1, US2.8, NFR-56
 */
internal fun SeenPlan.tapped(area: GeoArea?, card: Dp, bar: Dp, map: Dp): MapCamera =
    camera.stillUnder(card = card, bar = bar).revealing(
        area = area,
        free = (map - card).value.toDouble(),
        card = card.value.toDouble(),
    )

/**
 * Widok, w którym sala [area] zasłonięta kartą wysoką na [card] dp stoi dolnym brzegiem 16 dp nad kartą, a górnym
 * najwyżej 16 dp pod górą wolnej części mapy wysokiej na [free] dp. Ruch jest tylko w pionie i bez zmiany
 * powiększenia. Sala cała widoczna nad kartą, ucięta brzegiem ekranu albo bez konturu zostaje.
 *
 * Wymagania: US18.1, US2.8, NFR-56
 */
internal fun MapCamera.revealing(area: GeoArea?, free: Double, card: Double): MapCamera {
    val latitudes = area?.run { allRings().flatten().map { it.latitude } }.orEmpty()
    val south = latitudes.minOrNull() ?: return this
    val perRadian = dpPerDegree(zoom) * HALF_TURN_DEGREES / PI
    val middle = mercatorY(center.latitude)
    val bottom = free / 2 + (middle - mercatorY(south)) * perRadian
    val top = free / 2 + (middle - mercatorY(latitudes.max())) * perRadian
    val isSeen = top >= 0 && bottom <= free + card
    val covered = bottom - (free - REVEAL_MARGIN_DP)
    val lift = minOf(a = covered, b = (top - REVEAL_MARGIN_DP).coerceAtLeast(minimumValue = 0.0))
    return if (isSeen && covered > 0) shiftedNorth(-lift) else this
}

/**
 * Wysokość karty zapamiętana dla czynności: pierwszy pomiar [cover] karty z drogą ([hasWay]).
 * Karta bez drogi jeszcze rośnie, więc do wczytania drogi zostaje 0 i kamera czeka. Wymagania: US18.2, NFR-56
 */
internal fun Int.heldWith(cover: Int, hasWay: Boolean): Int = if (this == 0 && hasWay) cover else this
