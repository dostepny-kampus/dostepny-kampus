/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.plan

import androidx.compose.runtime.Immutable
import io.github.dostepnykampus.core.model.room.RoomCategory
import io.github.dostepnykampus.feature.map.R

/**
 * Znak planu w arkuszu: nazwa [title], symbol budynku [building] i etykieta piętra [floor] z danych PWr.
 *
 * Wymagania: US16.1
 */
@Immutable
internal data class PlanSign(val title: Int, val building: String, val floor: String)

/**
 * Znak toalety dostosowanej albo schodów w pomieszczeniu [id] tego planu; `null` dla pomieszczenia spoza planu
 * i dla innych grup. TODO(#2541): arkusz windy.
 *
 * Wymagania: US16.1
 */
internal fun ShownPlan.signOf(id: String): PlanSign? {
    val found =
        rooms.firstNotNullOfOrNull { (floor, list) -> list.rooms.firstOrNull { it.id == id }?.let { floor to it } }
    val title = found?.let { PLAN_SIGN_TITLES[it.second.category] }
    val label = found?.let { (floor, _) -> floors.firstOrNull { it.id == floor }?.label }
    return if (title != null && label != null) PlanSign(title = title, building = building, floor = label) else null
}

private val PLAN_SIGN_TITLES =
    mapOf(
        RoomCategory.ACCESSIBLE_TOILET to R.string.mapa_plan_toaleta,
        RoomCategory.STAIRCASE to R.string.mapa_plan_schody,
    )
