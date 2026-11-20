/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.room

import androidx.compose.runtime.Immutable
import io.github.dostepnykampus.core.geo.metersTo
import io.github.dostepnykampus.core.geo.roundedMetersTo
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.Floor
import io.github.dostepnykampus.core.model.room.FoundRoom
import io.github.dostepnykampus.core.model.room.Room
import io.github.dostepnykampus.core.model.room.RoomCategory

/**
 * Droga do znalezionej sali bez schodów: kroki od wejścia zalecanego albo powód, dla którego ich nie ma.
 *
 * Wymagania: US18.2, US18.3, BR-25
 */
@Immutable
sealed interface RoomWay {
    /**
     * Kroki: wejście zalecane, winda, gdy sala jest na innym piętrze, i sala.
     *
     * @property entrance wejście wybrane dla tej drogi
     * @property entranceFloor piętro wejścia z danych, a bez niego parter
     * @property isFloorAssumed czy dane nie podają piętra wejścia i krok przyjmuje parter
     * @property lift winda z piętra wejścia na piętro sali; `null`, gdy sala jest na piętrze wejścia
     * @property roomMeters odległość sali od windy, a bez windy od wejścia
     */
    data class Steps(
        val entrance: Entrance,
        val entranceFloor: Floor,
        val isFloorAssumed: Boolean,
        val lift: Lift?,
        val roomMeters: Int,
    ) : RoomWay

    /**
     * Winda w krokach.
     *
     * @property id identyfikator windy wspólny dla jej pięter
     * @property room pomieszczenie windy na piętrze wejścia
     * @property stops etykiety pięter, na których staje winda
     * @property metersFromEntrance odległość windy od wejścia
     */
    data class Lift(val id: String, val room: Room, val stops: List<String>, val metersFromEntrance: Int)

    /** Budynek nie ma wejścia zalecanego albo dane nie znają jego piętra ani parteru. */
    data object NoEntrance : RoomWay

    /** Żadna winda nie staje na piętrze wejścia i na piętrze sali. */
    data object NoLift : RoomWay
}

/**
 * Droga do sali [found] od wejścia zalecanego [entrance]. Winda to najbliższa wejściu winda, która staje na piętrze
 * wejścia i na piętrze sali. Odległości są w linii prostej, zaokrąglone do 5 m, najmniej 5 m.
 *
 * @param found znaleziona sala z piętrem
 * @param entrance wejście zalecane budynku sali; `null`, gdy budynek go nie ma
 * @param floors piętra budynku sali
 * @param rooms pomieszczenia piętra o podanym identyfikatorze
 */
fun roomWay(found: FoundRoom, entrance: Entrance?, floors: List<Floor>, rooms: (String) -> List<Room>): RoomWay {
    val known = entrance?.floorId?.let { id -> floors.firstOrNull { it.id == id } }
    val floor = known ?: floors.firstOrNull { it.order == 0 }
    return when {
        entrance == null || floor == null -> {
            RoomWay.NoEntrance
        }

        floor.id == found.floor.id -> {
            val meters = entrance.location.roundedMetersTo(found.room.labelPoint)
            RoomWay.Steps(
                entrance = entrance,
                entranceFloor = floor,
                isFloorAssumed = known == null,
                lift = null,
                roomMeters = meters,
            )
        }

        else -> {
            viaLift(
                found = found,
                entrance = entrance,
                floor = floor,
                isFloorAssumed = known == null,
                rooms = rooms,
                floors = floors,
            )
        }
    }
}

private fun viaLift(
    found: FoundRoom,
    entrance: Entrance,
    floor: Floor,
    isFloorAssumed: Boolean,
    rooms: (String) -> List<Room>,
    floors: List<Floor>,
): RoomWay {
    val lift =
        rooms(floor.id)
            .filter { it.category == RoomCategory.ELEVATOR && found.floor.id in it.liftFloors }
            .minByOrNull { it.labelPoint.metersTo(entrance.location) }
            ?: return RoomWay.NoLift
    val upstairs = rooms(found.floor.id).firstOrNull { it.liftId != null && it.liftId == lift.liftId } ?: lift
    val meters = entrance.location.roundedMetersTo(lift.labelPoint)
    val stops = floors.filter { it.id in lift.liftFloors }.sortedBy(Floor::order).map(Floor::label)
    val step = RoomWay.Lift(id = lift.liftId ?: lift.id, room = lift, stops = stops, metersFromEntrance = meters)
    return RoomWay.Steps(
        entrance = entrance,
        entranceFloor = floor,
        isFloorAssumed = isFloorAssumed,
        lift = step,
        roomMeters = upstairs.labelPoint.roundedMetersTo(found.room.labelPoint),
    )
}
