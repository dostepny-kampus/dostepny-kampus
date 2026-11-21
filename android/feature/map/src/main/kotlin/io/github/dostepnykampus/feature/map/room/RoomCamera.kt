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
import io.github.dostepnykampus.core.model.room.FoundRoom
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.camera.shiftedNorth
import io.github.dostepnykampus.feature.map.campus.ShownPlan
import io.github.dostepnykampus.feature.map.navigation.MapFocus

/** Ostatni widok mapy w spoczynku i sala dotknięta na planie, zapamiętane między czynnościami osoby. */
internal class RoomMemory {
    var idle: MapCamera? = null
    var tapped: String? = null
    var top: Dp = 0.dp
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
 * [top]. Czynność to polecenie [MapFocus.Building.command]: widzi widok
 * i plan z chwili przed nią, a wysokość karty z pierwszego pomiaru, więc późniejsza zmiana wysokości karty nie rusza
 * mapy. Dotknięta sala zostawia obraz mapy w miejscu mimo karty i paska, które zmieniają dopełnienie kamery.
 *
 * Wymagania: US18.1, US18.2, US2.8, NFR-56
 */
@Composable
internal fun rememberRoomView(scene: RoomScene, focus: MapFocus?, cover: Int, top: Dp): RoomView {
    val (room, isOnMap, plan) = scene
    val memory = remember { RoomMemory() }
    val command = (focus as? MapFocus.Building)?.command
    val seen = remember(command) { memory.idle?.let { SeenPlan(camera = it, plan = plan?.building) } }
    val held = remember(command) { IntArray(1) }
    if (held[0] == 0) held[0] = cover
    if (room == null) {
        memory.tapped = null
        memory.top = top
    }
    val kept = if (isOnMap) held[0] else cover
    val isTapped = room?.run { card.step == null && card.found.room.id == memory.tapped } == true
    val density = LocalDensity.current
    val keptDp = with(density) { kept.toDp() }
    val still = seen?.camera?.takeIf { isTapped && kept > 0 }?.stillUnder(card = keptDp, bar = memory.top)
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
