/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.room

import io.github.dostepnykampus.core.data.cache.decodeCache
import io.github.dostepnykampus.core.data.cache.encodeCache
import io.github.dostepnykampus.core.database.room.FloorPlanEntity
import io.github.dostepnykampus.core.database.room.FloorRoomEntity
import io.github.dostepnykampus.core.database.room.FloorRoomFact
import io.github.dostepnykampus.core.geo.GeoAreaJson
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.building.Floor
import io.github.dostepnykampus.core.model.room.Room
import io.github.dostepnykampus.core.model.room.RoomCategory
import io.github.dostepnykampus.core.model.room.RoomList
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

private val LIFT_FLOORS = ListSerializer(String.serializer())

/** Wiersz `floor_plans` z piętra [floor] i jego planu z serwera. Wymagania: US16.1, NFR-10 */
internal fun RoomList.toPlanEntity(building: String, floor: Floor, syncedAt: Long): FloorPlanEntity =
    FloorPlanEntity(
        building = building,
        floor = floor.id,
        label = floor.label,
        order = floor.order,
        source = source,
        outlineJson = encodeCache(GeoAreaJson, floorOutline),
        syncedAt = syncedAt,
    )

/** Wiersz `floor_rooms` z pomieszczenia z serwera. Wymagania: US16.1, NFR-10 */
internal fun Room.toEntity(building: String, floor: String): FloorRoomEntity =
    FloorRoomEntity(
        building = building,
        floor = floor,
        id = id,
        number = number,
        function = function,
        category = category.name,
        geometryJson = encodeCache(GeoAreaJson, geometry),
        labelLongitude = labelPoint.longitude,
        labelLatitude = labelPoint.latitude,
        isLabelled = isLabelled,
        liftId = liftId,
        liftFloorsJson = liftFloors.takeIf { it.isNotEmpty() }?.let { encodeCache(LIFT_FLOORS, it) },
    )

/** Plan piętra z kopii; `null` dla zepsutego GeoJSON albo nieznanej grupy funkcji. Wymagania: US16.1, NFR-10 */
internal fun FloorPlanEntity.toRoomList(rooms: List<FloorRoomEntity>): RoomList? =
    runCatching {
        RoomList(
            source = source,
            floorOutline = decodeCache(GeoAreaJson, outlineJson),
            rooms =
                rooms.map { row ->
                    Room(
                        id = row.id,
                        number = row.number,
                        function = row.function,
                        category = RoomCategory.valueOf(row.category),
                        geometry = decodeCache(GeoAreaJson, row.geometryJson),
                        labelPoint = GeoPoint(longitude = row.labelLongitude, latitude = row.labelLatitude),
                        isLabelled = row.isLabelled,
                        liftId = row.liftId,
                        liftFloors = row.liftFloorsJson?.let { decodeCache(LIFT_FLOORS, it) }.orEmpty(),
                    )
                },
        )
    }.getOrNull()

/** Kategorie i funkcje pomieszczeń piętra z kopii; `null` dla nieznanej grupy funkcji. Wymagania: US2.4, NFR-56 */
internal fun List<FloorRoomFact>.toRoomSummaries(): List<RoomSummary>? =
    runCatching {
        map { row -> RoomSummary(category = RoomCategory.valueOf(row.category), function = row.function) }
    }.getOrNull()
