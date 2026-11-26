/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.plan

import io.github.dostepnykampus.core.geo.building.BuildingHint
import io.github.dostepnykampus.core.geo.building.buildingHints
import io.github.dostepnykampus.core.geo.building.containingBuilding
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.building.Building
import io.github.dostepnykampus.core.model.building.BuildingCatalog
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.campus.CampusMapData

/** Najmniejsze powiększenie, od którego pojawia się plan piętra (R2). */
internal const val PLAN_SHOW_ZOOM = 17.0

/** Powiększenie, poniżej którego plan znika (histereza R3). */
internal const val PLAN_HIDE_ZOOM = 16.5

/** Największa odległość budynku od środka mapy, przy której budynek dostaje plan (R2). */
internal const val PLAN_NEAR_METERS = 60.0

/** Przewaga odległości, przy której plan przechodzi na inny budynek (histereza R3). */
internal const val PLAN_SWITCH_METERS = 10.0

/** Odległość poza obrysem budynku podstawowego, po której plan sąsiada wraca bez otwartej karty. */
internal const val PLAN_BASIC_EXIT_METERS = 5.0

/**
 * Budynki mapy podzielone według obecności planu piętra do wyboru planu w [planFocus].
 *
 * @property detailed budynki szczegółowe z planami pięter
 * @property basic budynki podstawowe bez planów pięter; domyślnie puste
 */
internal data class PlanBuildings(val detailed: List<Building>, val basic: List<Building> = emptyList())

/**
 * Czy od powiększenia planu środek [camera] leży w konturze budynku, który lista z serwera określa jako podstawowy.
 * Przy wspólnym konturze pierwszeństwo ma budynek szczegółowy. [data] ogranicza sprawdzanie konturów [catalog].
 *
 * Wymagania: US16.2, BR-G1
 */
internal fun hasBasicBuildingInCenter(camera: MapCamera, catalog: BuildingCatalog?, data: CampusMapData?): Boolean {
    val confirmed = catalog?.takeIf { it.hasServerList }
    if (camera.zoom < PLAN_SHOW_ZOOM || confirmed == null || data == null) return false
    val point = camera.center
    val nearby =
        confirmed.buildings.filter { building ->
            data.outlinesBySymbol[building.symbol]?.run {
                point.longitude in west..east && point.latitude in south..north
            } == true
        }
    val (detailed, basic) = nearby.partition { it.symbol in confirmed.detailed }
    return containingBuilding(point, basic) != null && containingBuilding(point, detailed) == null
}

/**
 * Budynek z planem piętra przy środku mapy [center], powiększeniu [zoom] i wybranym budynku [selected]; `null`,
 * gdy planu nie widać. Przy otwartej karcie albo arkuszu ([selected]) wybiera wyłącznie ten budynek do
 * [PLAN_NEAR_METERS], bez sąsiada; bez wyboru szuka w [PlanBuildings.detailed] poza konturami
 * [PlanBuildings.basic] (histereza 17 / [PLAN_HIDE_ZOOM], [PLAN_SWITCH_METERS] i [PLAN_BASIC_EXIT_METERS]).
 *
 * Wymagania: US16.1, US16.2, BR-G1
 *
 * @param center środek mapy
 * @param zoom powiększenie mapy
 * @param buildings budynki szczegółowe i podstawowe na mapie
 * @param shown symbol budynku z planem dotąd; `null`, gdy planu nie było
 * @param selected symbol wybranego budynku (karta V2 albo arkusz wejścia); `null` bez wyboru
 */
internal fun planFocus(
    center: GeoPoint,
    zoom: Double,
    buildings: PlanBuildings,
    shown: String?,
    selected: String? = null,
): String? {
    if (zoom < PLAN_HIDE_ZOOM) return null
    val near =
        buildingHints(center, buildings.detailed, limit = buildings.detailed.size)
            .nearby
            .filter { it.distanceMeters <= PLAN_NEAR_METERS }
    return when {
        selected != null -> selectedFocus(selected = selected, near = near, zoom = zoom, shown = shown)
        isBlockedByBasic(center = center, basic = buildings.basic, near = near, shown = shown) -> null
        else -> cameraFocus(near = near, zoom = zoom, shown = shown)
    }
}

private fun selectedFocus(selected: String, near: List<BuildingHint>, zoom: Double, shown: String?): String? =
    near
        .firstOrNull { it.building.symbol == selected }
        ?.takeIf { zoom >= PLAN_SHOW_ZOOM || shown != null }
        ?.run { building.symbol }

private fun isBlockedByBasic(
    center: GeoPoint,
    basic: List<Building>,
    near: List<BuildingHint>,
    shown: String?,
): Boolean {
    if (basic.isEmpty() || near.any { it.isInside }) return false
    val closest = buildingHints(center, basic, limit = basic.size).nearby.firstOrNull()
    return closest != null &&
        if (shown != null) closest.isInside else closest.distanceMeters <= PLAN_BASIC_EXIT_METERS
}

private fun cameraFocus(near: List<BuildingHint>, zoom: Double, shown: String?): String? {
    val nearest = near.firstOrNull()
    val kept = near.firstOrNull { it.building.symbol == shown }
    val isSwitched =
        kept != null && nearest != null && nearest.distanceMeters + PLAN_SWITCH_METERS < kept.distanceMeters
    return when {
        isSwitched -> nearest.building.symbol
        kept != null -> kept.building.symbol
        zoom >= PLAN_SHOW_ZOOM -> nearest?.run { building.symbol }
        else -> null
    }
}
