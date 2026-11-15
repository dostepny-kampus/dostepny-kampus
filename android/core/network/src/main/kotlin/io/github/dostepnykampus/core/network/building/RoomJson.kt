/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network.building

import io.github.dostepnykampus.core.geo.GeoAreaJson
import io.github.dostepnykampus.core.model.building.Floor
import io.github.dostepnykampus.core.model.room.FoundRoom
import io.github.dostepnykampus.core.model.room.Room
import io.github.dostepnykampus.core.model.room.RoomCategory
import io.github.dostepnykampus.core.model.room.RoomList
import io.github.dostepnykampus.core.network.decodeContract
import kotlinx.serialization.json.Json
import io.github.dostepnykampus.core.network.json.Room as RoomJson
import io.github.dostepnykampus.core.network.json.RoomList as RoomListJson
import io.github.dostepnykampus.core.network.json.RoomSearchResults as RoomSearchJson

/**
 * Pomieszczenia piętra z [body] (`RoomList`): grupa, punkt podpisu, znacznik podpisu, piętra windy i obrys piętra
 * bez zmian.
 * Zła treść jak w [decodeBuildingList]. Wymagania: US16.1
 */
internal fun Json.decodeRoomList(body: String): RoomList {
    val list = decodeContract(RoomListJson.serializer(), body)
    return RoomList(
        source = list.source.value,
        floorOutline = decodeFromJsonElement(GeoAreaJson, list.floorOutline),
        rooms = list.rooms.map { toModel(it) },
    )
}

/**
 * Sale pasujące do wyszukiwania z [body] (`RoomSearchResults`): budynek, piętro i sala w kolejności serwera.
 * Zła treść jak w [decodeBuildingList]. Wymagania: US18.1
 */
internal fun Json.decodeRoomSearch(body: String): List<FoundRoom> {
    val found = decodeContract(RoomSearchJson.serializer(), body)
    return found.results.map {
        FoundRoom(
            building = it.buildingSymbol,
            floor = Floor(id = it.floor.id, label = it.floor.label, order = it.floor.order),
            room = toModel(it.room),
        )
    }
}

private fun Json.toModel(room: RoomJson): Room =
    Room(
        id = room.id,
        number = room.number,
        function = room.function,
        category = RoomCategory.valueOf(room.category.name),
        geometry = decodeFromJsonElement(GeoAreaJson, room.geometry),
        labelPoint = room.labelPoint.toModel(),
        isLabelled = room.labelled,
        liftId = room.liftId,
        liftFloors = room.liftFloors.orEmpty(),
    )
