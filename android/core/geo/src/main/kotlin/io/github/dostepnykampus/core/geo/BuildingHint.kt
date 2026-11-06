/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.geo

import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.core.model.GeoPoint

/** Najdalsza odległość od konturu, z której aplikacja proponuje budynek (BR-G4); ten sam próg co na serwerze. */
const val NEAR_METERS = 50.0

/**
 * Podpowiedź budynku po pomiarze na C1: „Czy to budynek C-13?” z nazwą i odległością (US6.2).
 *
 * @property building budynek, w którego konturze leży punkt, albo najbliższy
 * @property distanceMeters odległość od punktu do krawędzi konturu w metrach; 0 także dla punktu leżącego dokładnie
 *   na krawędzi; taki punkt nie leży w konturze
 * @property isInside czy punkt leży ściśle w konturze budynku (wynik [locateBuilding]); punkt na krawędzi
 *   konturu jest poza budynkiem, mimo odległości 0
 */
data class BuildingHint(val building: Building, val distanceMeters: Double, val isInside: Boolean)

/**
 * Podpowiedź budynku dla punktu z pomiaru, bez sieci (US6.2, NFR-4). Punkt ściśle w konturze daje ten budynek, punkt
 * poza konturem lub na jego krawędzi najbliższy budynek do 50 m włącznie, a dalej nic: zgłoszenie będzie
 * „poza budynkiem” (BR-G2, BR-G4).
 *
 * @param point uśredniony punkt pomiaru
 * @param buildings budynki z konturami, jak dla [locateBuilding]
 * @return podpowiedź albo `null`, gdy żaden budynek nie jest dość blisko
 */
fun hintBuilding(point: GeoPoint, buildings: List<Building>): BuildingHint? =
    when (val located = locateBuilding(point, buildings)) {
        is LocateResult.InsideOutline -> {
            BuildingHint(located.building, distanceMeters = 0.0, isInside = true)
        }

        is LocateResult.OutsideBuilding -> {
            val nearest = located.nearest
            val meters = nearest?.edgeDistanceMeters(point)
            if (nearest != null && meters != null && meters <= NEAR_METERS) {
                BuildingHint(nearest, meters, isInside = false)
            } else {
                null
            }
        }
    }
