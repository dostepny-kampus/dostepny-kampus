/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.data

import io.github.dostepnykampus.core.geo.toPosition
import io.github.dostepnykampus.core.model.GeoPoint
import org.maplibre.spatialk.turf.measurement.distance
import org.maplibre.spatialk.units.extensions.inKilometers
import kotlin.math.roundToInt

/**
 * Teren na liście terenów mapy V1.
 *
 * Wymagania: US1.7
 *
 * @property name nazwa terenu, np. „Wittigowo”
 * @property buildings liczba symboli budynków terenu
 * @property kilometers odległość od wybranego terenu w pełnych kilometrach, co najmniej 1; `null` dla wybranego
 */
data class AreaChoice(val name: String, val buildings: Int, val kilometers: Int?)

/** Środek prostokąta terenu: punkt startowy widoku po wybraniu terenu i punkt, od którego liczy się odległość. */
val CampusBounds.center: GeoPoint
    get() = GeoPoint(longitude = (west + east) / 2, latitude = (south + north) / 2)

/**
 * Teren pokazany na mapie: teren o nazwie [chosen] wybrany przez osobę, bez wyboru teren budynku [symbol],
 * np. z wyszukiwania, a bez niego teren startowy. `null` bez budynków w katalogu.
 *
 * Wymagania: US1.7, US1.8, US1.6
 */
fun CampusMapData.areaOf(chosen: String?, symbol: String?): MapArea? =
    areas.firstOrNull { it.name == chosen } ?: areas.firstOrNull { it.bounds == boundsOf(symbol) }

/**
 * Lista terenów dla wybranego terenu [current]: wybrany stoi pierwszy, pozostałe od najbliższego. Odległość liczy
 * się między środkami prostokątów terenów.
 *
 * Wymagania: US1.7
 */
fun CampusMapData.areaChoices(current: MapArea): List<AreaChoice> {
    val away = (areas - current).associateWith { current.bounds.kilometersTo(it.bounds) }
    val others = away.entries.sortedBy { it.value }.map { (area, kilometers) -> area.choice(kilometers) }
    val shown = AreaChoice(name = current.name, buildings = current.symbols.size, kilometers = null)
    return listOf(shown) + others
}

private fun MapArea.choice(kilometers: Double) =
    AreaChoice(name = name, buildings = symbols.size, kilometers = fullKilometers(kilometers))

/** Odległość w kilometrach między środkami prostokątów terenów. Wymagania: US1.7 */
internal fun CampusBounds.kilometersTo(other: CampusBounds): Double =
    distance(from = center.toPosition(), to = other.center.toPosition()).inKilometers

/**
 * Odległość [kilometers] w pełnych kilometrach, co najmniej 1: tak czyta ją osoba na liście i przycisku.
 *
 * Wymagania: US1.7
 */
internal fun fullKilometers(kilometers: Double): Int = kilometers.roundToInt().coerceAtLeast(1)
