/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import io.github.dostepnykampus.core.model.BuildingCatalog
import io.github.dostepnykampus.core.model.Entrance
import io.github.dostepnykampus.core.model.EntranceMarking

private val NOT_FOR_VISITORS = setOf("emergency", "exit", "garage")

private val MARKING_ORDER = listOf(EntranceMarking.ACCESSIBLE, EntranceMarking.INACCESSIBLE, EntranceMarking.NO_DATA)

/**
 * Wejście na liście karty budynku i na V3, w kolejności z [orderedEntrances].
 *
 * @property entrance wejście z katalogu, już bez rodzajów spoza odwiedzających
 * @property isRecommended czy to jedyne wejście zalecane budynku (BR-25); napis „Zalecane”
 */
data class OrderedEntrance(val entrance: Entrance, val isRecommended: Boolean)

/**
 * Wejścia odwiedzających w kolejności karty budynku (BR-25, decyzja 48).
 *
 * Zalecane jest jedyne: wejście „dostępne”, a gdy jest ich kilka — wejście główne, a gdy żadne dostępne nie jest
 * główne — pierwsze „dostępne” w kolejności z danych. Wejście „brak danych” nigdy nie jest zalecane. Bez wejścia
 * „dostępne” nie ma zalecanego, a lista zaczyna się od „niedostępne”. Wejścia `emergency`, `exit` i `garage` nie
 * są wejściami dla odwiedzających: nie ma ich na liście i nie są zalecane.
 */
fun orderedEntrances(entrances: List<Entrance>): List<OrderedEntrance> {
    val visitors = entrances.filterNot { it.type in NOT_FOR_VISITORS }
    val accessible = visitors.filter { it.marking == EntranceMarking.ACCESSIBLE }
    val recommended = accessible.firstOrNull { it.isMain } ?: accessible.firstOrNull()
    val rest = visitors.filter { it !== recommended }.sortedBy { MARKING_ORDER.indexOf(it.marking) }
    val head = recommended?.let { OrderedEntrance(entrance = it, isRecommended = true) }
    return listOfNotNull(head) + rest.map { OrderedEntrance(entrance = it, isRecommended = false) }
}

/**
 * Stan karty budynku [symbol] z katalogu (US2, US2.7). Brak symbolu w katalogu to „budynku nie ma w telefonie”.
 */
fun buildingCard(catalog: BuildingCatalog, symbol: String): BuildingCardUiState {
    val building = catalog.buildings.find { it.symbol == symbol }
    val entrances = catalog.entrances.filter { it.buildingSymbol == symbol }
    return if (building == null) {
        BuildingCardUiState.Missing
    } else {
        BuildingCardUiState.Ready(
            symbol = building.symbol,
            name = building.name,
            entrances = orderedEntrances(entrances),
        )
    }
}
