/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.room

import io.github.dostepnykampus.core.model.GeoMultiPolygon
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.core.model.building.Floor
import io.github.dostepnykampus.core.model.room.FoundRoom
import io.github.dostepnykampus.core.model.room.Room
import io.github.dostepnykampus.core.model.room.RoomCategory

private const val METERS_PER_LON_AT_CAMPUS = 69_970.0
private const val LAT = 51.11
private const val LON = 17.06

/** Sala A-1 329 na 2 piętrze i winda „w” z parteru, 28 m od wejścia; droga przez windę ma trzy kroki. */
internal object RoomCardFixtures {
    val GROUND = Floor(id = "a1-0", label = "Parter", order = 0)
    val SECOND = Floor(id = "a1-2", label = "2 piętro", order = 2)

    val FOUND =
        FoundRoom(
            building = "A-1",
            floor = SECOND,
            room =
                Room(
                    id = "329",
                    number = "329",
                    function = "Sala wykładowa",
                    category = RoomCategory.LECTURE_HALL,
                    geometry = GeoMultiPolygon(emptyList()),
                    labelPoint = point(meters = 113.0),
                    isLabelled = true,
                ),
        )

    val ENTRANCE =
        Entrance(
            id = "node/1",
            buildingSymbol = "A-1",
            name = "Wejście dostosowane",
            isMain = false,
            type = null,
            location = point(meters = 0.0),
            marking = EntranceMarking.ACCESSIBLE,
            floorId = GROUND.id,
        )

    val VIA_LIFT =
        RoomWay.Steps(
            entrance = ENTRANCE,
            entranceFloor = GROUND,
            isFloorAssumed = false,
            lift = RoomWay.Lift(id = "w", metersFromEntrance = 30),
            roomMeters = 85,
        )

    /** Winda „w” na piętrze [floor], stająca na parterze i na 2 piętrze. */
    fun lift(floor: String) =
        Room(
            id = "w-$floor",
            number = null,
            function = "Winda",
            category = RoomCategory.ELEVATOR,
            geometry = GeoMultiPolygon(emptyList()),
            labelPoint = point(meters = 28.0),
            isLabelled = false,
            liftId = "w",
            liftFloors = listOf(GROUND.id, SECOND.id),
        )

    /** Punkt [meters] metrów na wschód od wejścia. */
    fun point(meters: Double) = GeoPoint(longitude = LON + meters / METERS_PER_LON_AT_CAMPUS, latitude = LAT)
}
