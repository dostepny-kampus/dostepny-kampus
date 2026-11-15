/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.plan

import io.github.dostepnykampus.core.geo.building.buildingHints
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.building.Building

/** Najmniejsze powiększenie, od którego pojawia się plan piętra (R2). */
internal const val PLAN_SHOW_ZOOM = 17.0

/** Powiększenie, poniżej którego plan znika (histereza R3). */
internal const val PLAN_HIDE_ZOOM = 16.5

/** Największa odległość budynku od środka mapy, przy której budynek dostaje plan (R2). */
internal const val PLAN_NEAR_METERS = 60.0

/** Przewaga odległości, przy której plan przechodzi na inny budynek (histereza R3). */
internal const val PLAN_SWITCH_METERS = 10.0

/**
 * Budynek z planem piętra przy środku mapy [center] i powiększeniu [zoom]; `null`, gdy planu nie widać.
 *
 * Plan ma budynek szczegółowy w środku mapy albo najbliżej środka, nie dalej niż [PLAN_NEAR_METERS] (R2). Pojawia się
 * od [PLAN_SHOW_ZOOM], a znika dopiero poniżej [PLAN_HIDE_ZOOM]; budynek [shown] zostaje, dopóki inny nie jest bliżej
 * środka o więcej niż [PLAN_SWITCH_METERS] (R3), więc plan nie skacze przy drobnym ruchu mapy.
 *
 * Wymagania: US16.1, BR-G1
 *
 * @param center środek mapy
 * @param zoom powiększenie mapy
 * @param detailed budynki szczegółowe, które mają plan pięter
 * @param shown symbol budynku z planem dotąd; `null`, gdy planu nie było
 */
internal fun planFocus(center: GeoPoint, zoom: Double, detailed: List<Building>, shown: String?): String? {
    if (zoom < PLAN_HIDE_ZOOM) return null
    val hints = buildingHints(center, detailed, limit = detailed.size).nearby
    val near = hints.filter { it.distanceMeters <= PLAN_NEAR_METERS }
    val nearest = near.firstOrNull()
    val kept = near.firstOrNull { it.building.symbol == shown }
    val isSwitched =
        kept != null && nearest != null && nearest.distanceMeters + PLAN_SWITCH_METERS < kept.distanceMeters
    return when {
        isSwitched -> {
            nearest.building.symbol
        }

        kept != null -> {
            kept.building.symbol
        }

        zoom >= PLAN_SHOW_ZOOM -> {
            nearest?.run { building.symbol }
        }

        else -> {
            null
        }
    }
}
