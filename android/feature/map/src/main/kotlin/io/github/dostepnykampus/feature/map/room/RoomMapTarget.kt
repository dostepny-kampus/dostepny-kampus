/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.room

import io.github.dostepnykampus.core.model.GeoPoint

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
