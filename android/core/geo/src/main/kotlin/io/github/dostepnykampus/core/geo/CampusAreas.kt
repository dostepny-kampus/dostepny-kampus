/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.geo

import io.github.dostepnykampus.core.model.Building

/** Największa odległość między budynkami jednego terenu w metrach na ziemi. Wymagania: BR-G5 */
internal const val AREA_LINK_METERS = 600.0

private val AREA_NAMES =
    mapOf(
        "A-1" to "Kampus główny",
        "T-15" to "Wittigowo",
        "P-14" to "Bartoszowice",
        "M-3" to "Długa",
        "T-9" to "T-9 Atol",
    )

/**
 * Teren PWr: grupa budynków leżących blisko siebie (słownik: „teren”).
 *
 * Wymagania: BR-G5
 *
 * @property name nazwa terenu, np. „Kampus główny”; teren bez nazwy nazywa się symbolem swojego
 *   pierwszego budynku
 * @property buildings budynki terenu w kolejności katalogu
 */
public data class CampusArea(val name: String, val buildings: List<Building>) {
    /** Symbole budynków terenu, każdy raz; ich liczba to „liczba budynków” terenu na V1. Wymagania: US1.7 */
    val symbols: List<String> get() = buildings.map(Building::symbol).distinct()
}

/**
 * Tereny PWr z budynków katalogu, bez sieci. Do jednego terenu należą budynki, z których
 * każdy leży najwyżej [AREA_LINK_METERS] od innego budynku tego terenu. Odległość liczymy od wierzchołków konturu
 * jednego budynku do krawędzi konturu drugiego.
 *
 * Wymagania: BR-G5, US1.6, NFR-4
 *
 * @param buildings budynki z konturami z katalogu w telefonie
 * @return tereny w kolejności pierwszego budynku w katalogu; każdy budynek należy do dokładnie jednego terenu
 */
public fun campusAreas(buildings: List<Building>): List<CampusArea> {
    val left = buildings.toMutableList()
    val areas = mutableListOf<CampusArea>()
    while (left.isNotEmpty()) {
        val area = mutableListOf(left.removeAt(index = 0))
        var checked = 0
        while (checked < area.size) {
            val current = area[checked]
            val near = left.filter { it.isNear(current) }
            left.removeAll(near)
            area += near
            checked += 1
        }
        val ordered = buildings.filter { it in area }
        val name = ordered.firstNotNullOfOrNull { AREA_NAMES[it.symbol] } ?: ordered.first().symbol
        areas += CampusArea(name, ordered)
    }
    return areas
}

private fun Building.isNear(other: Building): Boolean =
    outline.allRings().any { ring -> ring.any { point -> other.edgeDistanceMeters(point) <= AREA_LINK_METERS } } ||
        other.outline.allRings().any { ring -> ring.any { point -> edgeDistanceMeters(point) <= AREA_LINK_METERS } }
