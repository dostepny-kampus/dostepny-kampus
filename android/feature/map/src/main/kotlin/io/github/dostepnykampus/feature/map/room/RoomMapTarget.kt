/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.room

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.feature.map.camera.ENTRANCE_ZOOM
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.camera.shiftedNorth

/** Piętro planu i punkt, który ma wskazać kamera. Wymagania: US18.1, US18.2 */
data class RoomMapTarget(val floor: String, val point: GeoPoint)

/** Cel dla aktualnego kroku [RoomCard.step] albo sali, kiedy pokaz drogi jeszcze nie trwa. Wymagania: US18.2 */
fun RoomCard.mapTarget(): RoomMapTarget {
    val way = way as? RoomWay.Steps
    return when {
        step == 0 && way != null -> RoomMapTarget(floor = way.entranceFloor.id, point = way.entrance.location)
        step == 1 && way?.lift != null -> RoomMapTarget(floor = way.entranceFloor.id, point = way.lift.room.labelPoint)
        else -> RoomMapTarget(floor = found.floor.id, point = found.room.labelPoint)
    }
}

/** Kamera kroku z miejscem na kartę pod mapą. Wymagania: US18.2 */
internal fun OpenRoom.cameraForRoom(isRoomOnMap: Boolean, cover: Dp): MapCamera? {
    val target = card.mapTarget()
    val camera = MapCamera(center = target.point, zoom = ENTRANCE_ZOOM)
    return when {
        !isRoomOnMap -> camera
        cover == 0.dp -> null
        else -> camera.shiftedNorth(-cover.value.toDouble() / 2.0)
    }
}
