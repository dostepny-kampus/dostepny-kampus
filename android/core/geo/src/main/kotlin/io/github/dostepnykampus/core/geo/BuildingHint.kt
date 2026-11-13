/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.geo

import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.core.model.GeoPoint

/**
 * Najdalsza odległość od konturu, z której aplikacja proponuje budynek; ten sam próg co na serwerze.
 *
 * Wymagania: BR-G4
 */
const val NEAR_METERS = 50.0

/**
 * Podpowiedź budynku po pomiarze na C1: „Czy to budynek C-13?” z nazwą i odległością.
 *
 * Wymagania: US6.2
 *
 * @property building budynek, w którego konturze leży punkt, albo najbliższy
 * @property distanceMeters odległość od punktu do krawędzi konturu w metrach; 0 także dla punktu leżącego dokładnie
 *   na krawędzi; taki punkt nie leży w konturze
 * @property isInside czy punkt leży ściśle w konturze budynku (wynik [locateBuilding]); punkt na krawędzi
 *   konturu jest poza budynkiem, mimo odległości 0
 */
data class BuildingHint(val building: Building, val distanceMeters: Double, val isInside: Boolean)

/**
 * Wynik dopasowania budynków do punktu pomiaru.
 *
 * @property nearby budynki do wyboru w kolejności od najbliższego, także poza zasięgiem podpowiedzi
 * @property suggested budynek proponowany do potwierdzenia lub `null`, gdy żaden nie jest dostatecznie blisko
 */
data class BuildingHints(val nearby: List<BuildingHint>, val suggested: BuildingHint?)

/** Ile budynków pokazuje sekcja „Wybierz budynek” na C1; makieta ma trzy najbliższe. */
const val NEAREST_COUNT = 3

/**
 * Oblicza odległość od konturu raz na budynek. Budynek zawierający punkt ma 0 m i stoi przed
 * budynkiem, na którego krawędzi punkt leży. Podpowiedź obejmuje budynek w konturze albo najbliższy do 50 m.
 *
 * Wymagania: US6.2, NFR-4
 *
 * @param point uśredniony punkt pomiaru
 * @param buildings budynki z konturami z katalogu w telefonie
 * @param limit najwięcej budynków na liście
 * @return lista od najbliższego, najwyżej [limit], i niezależna od limitu podpowiedź
 */
fun buildingHints(point: GeoPoint, buildings: List<Building>, limit: Int = NEAREST_COUNT): BuildingHints {
    val ranked = buildings.map { building -> building.hintFor(point) }.sortedWith(nearestFirst)
    return BuildingHints(ranked.take(limit), ranked.firstOrNull()?.takeIf { it.distanceMeters <= NEAR_METERS })
}

private val nearestFirst = compareBy<BuildingHint> { it.distanceMeters }.thenBy { !it.isInside }

private fun Building.hintFor(point: GeoPoint): BuildingHint {
    val isInside = outline.containsStrictly(point)
    val meters = if (isInside) 0.0 else edgeDistanceMeters(point)
    return BuildingHint(this, distanceMeters = meters, isInside = isInside)
}
