/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.entrance

import androidx.compose.runtime.Immutable
import io.github.dostepnykampus.core.data.barrier.AcceptedBarrier
import io.github.dostepnykampus.core.model.building.BuildingCatalog
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.feature.map.building.orderedEntrances
import io.github.dostepnykampus.feature.map.plan.PlanReport

/**
 * Wejście w arkuszu wejścia na V1.
 *
 * Wymagania: BR-25, US3.3
 *
 * @property entrance wejście z katalogu
 * @property isRecommended czy to wejście zalecane swojego budynku
 * @property barriers zgłoszenia przyjęte przy tym wejściu
 * @property lift najbliższa winda za drzwiami; `null` bez windy w danych
 */
@Immutable
internal data class ShownEntrance(
    val entrance: Entrance,
    val isRecommended: Boolean = false,
    val barriers: List<AcceptedBarrier> = emptyList(),
    val lift: EntranceLift? = null,
)

/**
 * Czynności arkusza wejścia na V1 i dane, o które arkusz prosi, oraz zgłoszenie z arkusza znaku planu.
 *
 * Wymagania: US3.3, US4.6, US16.3
 *
 * @property onReport zgłoszenie bariery przy wejściu o podanym identyfikatorze
 * @property onPlanReport zgłoszenie bariery przy schodach, toalecie albo windzie z arkusza znaku planu
 * @property onShow wejście pokazane w arkuszu, którego windy szukamy
 * @property lift najbliższa winda ostatnio pokazanego wejścia
 */
@Immutable
data class EntranceActions(
    val onReport: (String) -> Unit = {},
    val onPlanReport: (PlanReport) -> Unit = {},
    val onShow: (Entrance) -> Unit = {},
    val lift: EntranceLift? = null,
)

/**
 * Wejście [id] z katalogu z jego miejscem wśród wejść budynku według [orderedEntrances] i zgłoszeniami
 * z [barriers] przy tym wejściu; `null`, gdy katalog nie ma takiego wejścia.
 *
 * Wymagania: BR-25, US3.3, US2.2
 */
internal fun BuildingCatalog.shownEntrance(id: String, barriers: List<AcceptedBarrier> = emptyList()): ShownEntrance? {
    val entrance = entrances.firstOrNull { it.id == id } ?: return null
    val ordered = orderedEntrances(entrances.filter { it.buildingSymbol == entrance.buildingSymbol }, barriers)
    return ShownEntrance(
        entrance = entrance,
        isRecommended = ordered.any { it.isRecommended && it.entrance.id == id },
        barriers = barriers.filter { it.entranceId == id },
    )
}
