/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.area

import io.github.dostepnykampus.core.geo.toPosition
import io.github.dostepnykampus.feature.map.campus.CampusBounds
import io.github.dostepnykampus.feature.map.campus.MapArea
import org.maplibre.spatialk.turf.measurement.bearingTo
import org.maplibre.spatialk.units.Bearing
import org.maplibre.spatialk.units.extensions.inDegrees

private const val SECTOR_DEGREES = 45.0

/**
 * Jeden z ośmiu kierunków świata, od północy zgodnie z ruchem wskazówek zegara.
 *
 * Wymagania: US1.8, NFR-56
 */
enum class CompassDirection {
    /** Na północ. */
    North,

    /** Na północny wschód. */
    NorthEast,

    /** Na wschód. */
    East,

    /** Na południowy wschód. */
    SouthEast,

    /** Na południe. */
    South,

    /** Na południowy zachód. */
    SouthWest,

    /** Na zachód. */
    West,

    /** Na północny zachód. */
    NorthWest,
}

/**
 * Zmiana terenu mapy do napisu „Pokazuję teren: Wittigowo · 2 km na południowy wschód”.
 *
 * Wymagania: US1.8, NFR-56
 *
 * @property name nazwa terenu, który mapa teraz pokazuje
 * @property kilometers odległość od terenu, o którym osoba wiedziała, w pełnych kilometrach jak na liście terenów
 * @property direction kierunek, w którym nowy teren leży od terenu, o którym osoba wiedziała
 */
data class AreaShift(val name: String, val kilometers: Int, val direction: CompassDirection)

/**
 * Zmiana z tego terenu na teren [shown]: odległość i kierunek między środkami prostokątów terenów.
 *
 * Wymagania: US1.8, NFR-56
 */
fun MapArea.shiftTo(shown: MapArea): AreaShift =
    AreaShift(
        name = shown.name,
        kilometers = fullKilometers(bounds.kilometersTo(shown.bounds)),
        direction = bounds.directionTo(shown.bounds),
    )

/** Kierunek świata od środka tego prostokąta do środka [other], najbliższy z ośmiu. Wymagania: US1.8 */
internal fun CampusBounds.directionTo(other: CampusBounds): CompassDirection {
    val bearing = center.toPosition().bearingTo(other.center.toPosition())
    val degrees = (bearing - Bearing.North).inDegrees
    val sectors = CompassDirection.entries
    return sectors[((degrees + SECTOR_DEGREES / 2) / SECTOR_DEGREES).toInt() % sectors.size]
}
