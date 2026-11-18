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
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.feature.map.building.orderedEntrances
import io.github.dostepnykampus.feature.map.navigation.MapFocus

/**
 * Wejście w arkuszu wejścia na V1.
 *
 * Wymagania: BR-25, US3.3
 *
 * @property entrance wejście z katalogu
 * @property isRecommended czy to wejście zalecane swojego budynku
 * @property others inne wejścia „dostępne” tego budynku; pusta lista przy wejściu niezalecanym
 * @property barriers zgłoszenia przyjęte przy tym wejściu; pusta lista przy wejściu niezalecanym
 */
@Immutable
internal data class ShownEntrance(
    val entrance: Entrance,
    val isRecommended: Boolean = false,
    val others: List<Entrance> = emptyList(),
    val barriers: List<AcceptedBarrier> = emptyList(),
)

/**
 * Czynności arkusza wejścia na V1.
 *
 * Wymagania: US3.3, US4.6
 *
 * @property onShow zaznacza na mapie podane wejście i otwiera jego arkusz
 * @property onReport zgłoszenie bariery przy wejściu o podanym identyfikatorze
 */
@Immutable
data class EntranceActions(val onShow: (MapFocus.Entrance) -> Unit = {}, val onReport: (String) -> Unit = {})

/**
 * Wejście [id] z katalogu z jego miejscem wśród wejść budynku według [orderedEntrances] i, gdy jest zalecane,
 * zgłoszeniami z [barriers] przy tym wejściu; `null`, gdy katalog nie ma takiego wejścia.
 *
 * Wymagania: BR-25, US3.3, US2.2
 */
internal fun BuildingCatalog.shownEntrance(id: String, barriers: List<AcceptedBarrier> = emptyList()): ShownEntrance? {
    val entrance = entrances.firstOrNull { it.id == id } ?: return null
    val ordered = orderedEntrances(entrances.filter { it.buildingSymbol == entrance.buildingSymbol })
    val isRecommended = ordered.any { it.isRecommended && it.entrance.id == id }
    val others = ordered.filter { !it.isRecommended && it.entrance.marking == EntranceMarking.ACCESSIBLE }
    if (!isRecommended) return ShownEntrance(entrance)
    return ShownEntrance(
        entrance = entrance,
        isRecommended = true,
        others = others.map { it.entrance },
        barriers = barriers.filter { it.entranceId == id },
    )
}
