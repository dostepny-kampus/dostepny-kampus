/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import io.github.dostepnykampus.core.model.building.Floor
import io.github.dostepnykampus.core.model.room.Room
import io.github.dostepnykampus.core.model.room.RoomCategory
import io.github.dostepnykampus.core.model.room.RoomList

private const val MAX_FUNCTIONS = 4

private val UNDESCRIBED =
    setOf(
        RoomCategory.CORRIDOR,
        RoomCategory.STAIRCASE,
        RoomCategory.ELEVATOR,
        RoomCategory.TOILET,
        RoomCategory.ACCESSIBLE_TOILET,
    )

/**
 * Nazwa z danych PWr i liczba pomieszczeń.
 *
 * @property name etykieta piętra albo funkcja pomieszczenia z danych PWr
 * @property count liczba pomieszczeń
 */
data class Counted(val name: String, val count: Int)

/**
 * Piętra z pomieszczeniem jednej grupy, od najniższego.
 *
 * @property floors piętra z tym pomieszczeniem i liczbą takich pomieszczeń
 * @property missing etykiety pięter bez niego; pusta lista znaczy „na każdym piętrze”
 */
data class FloorPresence(val floors: List<Counted>, val missing: List<String>)

/**
 * Opis piętra z funkcji pomieszczeń.
 *
 * @property floor piętro z danych PWr
 * @property functions funkcje pomieszczeń od najczęstszej; pusta lista znaczy piętro bez opisu
 */
data class FloorDescription(val floor: Floor, val functions: List<Counted>)

/**
 * Fakty o piętrach budynku szczegółowego z planów PWr.
 *
 * @property elevator piętra z windą; `null`, gdy plany nie mają windy
 * @property toilet piętra z toaletą dostosowaną; `null`, gdy plany jej nie mają
 * @property floors opisy pięter od najniższego
 */
data class FloorFacts(val elevator: FloorPresence?, val toilet: FloorPresence?, val floors: List<FloorDescription>)

/**
 * Fakty o piętrach [floors] z pomieszczeniami [rooms] (identyfikator piętra → jego plan). Opis piętra bierze
 * funkcje poza korytarzem, klatką schodową, windą i toaletami, najwyżej cztery, od najczęstszej; przy równej
 * liczbie w kolejności pierwszego wystąpienia. Piętro bez planu nie ma windy, toalety ani opisu.
 *
 * TODO(#1480): grupy „Winda i toaleta” i „Piętra” na karcie budynku.
 *
 * Wymagania: US16.3, US2.3, NFR-19
 */
fun floorFacts(floors: List<Floor>, rooms: Map<String, RoomList>): FloorFacts {
    val ordered = floors.sortedBy { it.order }
    val descriptions =
        ordered.map { floor ->
            val described = rooms.of(floor).filter { it.category !in UNDESCRIBED }
            val functions = described.groupingBy { it.function }.eachCount()
            val top = functions.entries.sortedByDescending { it.value }.take(MAX_FUNCTIONS)
            FloorDescription(floor = floor, functions = top.map { Counted(name = it.key, count = it.value) })
        }
    return FloorFacts(
        elevator = presence(ordered, rooms, RoomCategory.ELEVATOR),
        toilet = presence(ordered, rooms, RoomCategory.ACCESSIBLE_TOILET),
        floors = descriptions,
    )
}

private fun presence(floors: List<Floor>, rooms: Map<String, RoomList>, category: RoomCategory): FloorPresence? {
    val counts = floors.associateWith { floor -> rooms.of(floor).count { it.category == category } }
    val present = floors.filter { counts.getValue(it) > 0 }
    return present.takeIf { it.isNotEmpty() }?.let {
        FloorPresence(
            floors = present.map { Counted(name = it.label, count = counts.getValue(it)) },
            missing = floors.filter { counts.getValue(it) == 0 }.map { it.label },
        )
    }
}

private fun Map<String, RoomList>.of(floor: Floor): List<Room> = this[floor.id]?.rooms.orEmpty()
