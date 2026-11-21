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
import io.github.dostepnykampus.feature.map.campus.RoomStepMarker

/** Piętro planu [floor] i punkt kamery [point]. Wymagania: US18.1, US18.2 */
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

/**
 * Kamera na punkcie kroku. Karta na mapie ([isRoomOnMap]) zasłania dół mapy dopełnieniem kamery, więc punkt staje
 * na środku części mapy nad kartą; do pomiaru karty ([cover] 0) kamery nie ma. Wymagania: US18.2, US16.1
 */
internal fun OpenRoom.cameraForRoom(isRoomOnMap: Boolean, cover: Dp): MapCamera? =
    MapCamera(center = card.mapTarget().point, zoom = ENTRANCE_ZOOM).takeUnless { isRoomOnMap && cover == 0.dp }

/** Numery kroków drogi, których punkty są na wybranym [floor]. Wymagania: US18.2 */
internal fun RoomCard.markersOn(floor: String): List<RoomStepMarker> =
    (0 until stepCount).mapNotNull { index ->
        val target = copy(step = index).mapTarget()
        target.point.takeIf { target.floor == floor }?.let { point ->
            RoomStepMarker(
                index = index,
                point = point,
                isSelected = index == step,
                isDestination = index == stepCount - 1,
            )
        }
    }
