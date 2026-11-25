/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import io.github.dostepnykampus.core.data.room.RoomSummary
import io.github.dostepnykampus.core.model.building.Floor
import io.github.dostepnykampus.core.model.room.RoomCategory
import io.github.dostepnykampus.core.model.room.RoomList
import java.util.Locale

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
 * Wymagania: US16.3, US2.3, NFR-19
 */
fun floorFacts(floors: List<Floor>, rooms: Map<String, RoomList>): FloorFacts =
    floorFactsOf(
        floors = floors,
        rooms = rooms.mapValues { (_, list) -> list.rooms.map { RoomSummary(it.category, it.function) } },
    )

/**
 * Fakty o piętrach [floors] z kategoriami i funkcjami pomieszczeń [rooms] (identyfikator piętra → lista). Opis
 * piętra bierze funkcje poza korytarzem, klatką schodową, windą i toaletami, najwyżej cztery, od najczęstszej;
 * przy równej liczbie w kolejności pierwszego wystąpienia. Piętro bez wpisu nie ma windy, toalety ani opisu.
 *
 * Wymagania: US16.3, US2.3, US2.4, NFR-19, NFR-56
 */
internal fun floorFactsOf(floors: List<Floor>, rooms: Map<String, List<RoomSummary>>): FloorFacts {
    val ordered = floors.sortedBy { it.order }
    val descriptions =
        ordered.map { floor ->
            val described = rooms[floor.id].orEmpty().filter { it.category !in UNDESCRIBED }
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

private fun presence(
    floors: List<Floor>,
    rooms: Map<String, List<RoomSummary>>,
    category: RoomCategory,
): FloorPresence? {
    val counts = floors.associateWith { floor -> rooms[floor.id].orEmpty().count { it.category == category } }
    val present = floors.filter { counts.getValue(it) > 0 }
    return FloorPresence(
        floors = present.map { Counted(name = it.label, count = counts.getValue(it)) },
        missing = floors.filter { counts.getValue(it) == 0 }.map { it.label },
    ).takeIf { present.isNotEmpty() }
}

/**
 * Słowa tekstu pięter z windą albo toaletą.
 *
 * @property onFloors „Na piętrach: %1$s.”
 * @property everywhere „Na każdym piętrze: %1$s – %2$s.”
 * @property missing „Nie ma na piętrach: %1$s.”
 * @property and spójnik przed ostatnią pozycją listy
 */
internal data class PresenceWords(val onFloors: String, val everywhere: String, val missing: String, val and: String)

/**
 * Tekst pięter z pomieszczeniem [presence] w słowach [words]: lista pięter z liczbą w nawiasie przy kilku i piętra
 * bez niego albo zakres od najniższego do najwyższego, gdy jest na każdym piętrze.
 *
 * Wymagania: US16.3, NFR-40
 */
internal fun presenceText(presence: FloorPresence, words: PresenceWords): String {
    val floors = presence.floors
    if (presence.missing.isEmpty()) {
        return words.everywhere.format(Locale.ROOT, floors.first().name, floors.last().name)
    }
    val listed = floors.map { if (it.count > 1) "${it.name} (${it.count})" else it.name }
    val present = words.onFloors.format(Locale.ROOT, joined(listed, words.and))
    return "$present ${words.missing.format(Locale.ROOT, joined(presence.missing, words.and))}"
}

/** Pozycje [items] po przecinku, ostatnia po spójniku [and], np. „a, b i c”. */
internal fun joined(items: List<String>, and: String): String =
    if (items.size < 2) items.joinToString() else "${items.dropLast(1).joinToString()} $and ${items.last()}"
