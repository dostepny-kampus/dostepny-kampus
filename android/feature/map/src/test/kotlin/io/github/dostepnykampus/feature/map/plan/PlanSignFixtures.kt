/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.plan

import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.core.model.building.Floor
import io.github.dostepnykampus.core.model.room.Room
import io.github.dostepnykampus.core.model.room.RoomCategory
import io.github.dostepnykampus.core.model.room.RoomList

private val CORNERS = listOf(0.0 to 0.0, 1.0 to 0.0, 1.0 to 1.0, 0.0 to 0.0)
private val AREA = GeoPolygon(listOf(CORNERS.map { (lon, lat) -> GeoPoint(longitude = lon, latitude = lat) }))

/** Plan A-1 do arkuszy znaków: na parterze toaleta dostosowana `wc` i winda `winda`, na 1 piętrze schody `schody`. */
internal val SIGN_PLAN: ShownPlan =
    ShownPlan(
        building = "A-1",
        floors =
            listOf(Floor(id = "f0", label = "Parter", order = 0), Floor(id = "f1", label = "1 piętro", order = 1)),
        floor = "f0",
        rooms =
            mapOf(
                "f0" to rooms(room(id = "wc", category = RoomCategory.ACCESSIBLE_TOILET), room(id = "winda")),
                "f1" to rooms(room(id = "schody", category = RoomCategory.STAIRCASE)),
            ),
    )

private fun room(id: String, category: RoomCategory = RoomCategory.ELEVATOR) =
    Room(
        id = id,
        number = null,
        function = category.name,
        category = category,
        geometry = AREA,
        labelPoint = GeoPoint(longitude = 0.5, latitude = 0.5),
        isLabelled = false,
    )

private fun rooms(vararg rooms: Room) =
    RoomList(source = "Politechnika Wrocławska", floorOutline = AREA, rooms = rooms.toList())
