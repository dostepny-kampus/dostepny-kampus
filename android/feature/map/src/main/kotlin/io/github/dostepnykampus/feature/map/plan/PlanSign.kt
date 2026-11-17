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
 * Znak planu w arkuszu: nazwa [title], symbol budynku [building], etykieta piętra [floor] z danych PWr i etykiety
 * pięter windy [liftFloors] od najniższego, puste poza windą.
 *
 * Wymagania: US16.1, US18.2
 */
@Immutable
internal data class PlanSign(
    val title: Int,
    val building: String,
    val floor: String,
    val liftFloors: List<String> = emptyList(),
)

/**
 * Znak windy, toalety dostosowanej albo schodów w pomieszczeniu [id] tego planu; `null` dla pomieszczenia spoza
 * planu i dla innych grup. Piętra windy bez etykiety w planie są pominięte.
 *
 * Wymagania: US16.1, US18.2
 */
internal fun ShownPlan.signOf(id: String): PlanSign? {
    val (floor, room) =
        rooms.firstNotNullOfOrNull { (floor, list) -> list.rooms.firstOrNull { it.id == id }?.let { floor to it } }
            ?: return null
    val title = PLAN_SIGN_TITLES[room.category]
    val label = floors.firstOrNull { it.id == floor }?.label
    val stops = room.liftFloors.mapNotNull { stop -> floors.firstOrNull { it.id == stop }?.label }
    return when {
        title == null || label == null -> null
        else -> PlanSign(title = title, building = building, floor = label, liftFloors = stops)
    }
}

private val PLAN_SIGN_TITLES =
    mapOf(
        RoomCategory.ELEVATOR to R.string.karta_winda,
        RoomCategory.ACCESSIBLE_TOILET to R.string.karta_toaleta,
        RoomCategory.STAIRCASE to R.string.mapa_plan_schody,
    )
