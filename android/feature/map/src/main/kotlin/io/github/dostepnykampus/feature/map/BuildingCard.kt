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
 * Odpowiedź V3 „Którędy wejść” (US3.1, BR-25).
 */
sealed interface VisitAnswer {
    /** Brak wejść odwiedzających w danych nie potwierdza niedostępności budynku (US17.1). */
    data object NoData : VisitAnswer

    /** Jest wejście zalecane; [name] to jego nazwa, `null`, gdy danych nazwy nie ma. */
    data class Accessible(val name: String?) : VisitAnswer

    /** Żadne wejście odwiedzających nie jest „dostępne” i żadne nie ma „brak danych”. */
    data object NoAccessible : VisitAnswer

    /**
     * Nie ma wejścia „dostępne”, a o części nie ma danych.
     *
     * @property missing ile wejść odwiedzających ma „brak danych”
     * @property total ile wejść odwiedzających ma budynek
     */
    data class Unknown(val missing: Int, val total: Int) : VisitAnswer
}

/**
 * Zdanie V3 z listy [entrances] ułożonej przez [orderedEntrances] (BR-25, US3.1).
 *
 * Wejście zalecane jest w zdaniu „ma wejście dostępne”. Bez niego, gdy choć jedno wejście ma „brak danych”,
 * zdanie podaje, o ilu z ilu wejść brak danych — także wtedy, gdy danych brak o wszystkich. Inaczej budynek
 * nie ma wejścia dostępnego. Pusta lista oznacza brak danych o wejściach (US17.1).
 */
fun visitAnswer(entrances: List<OrderedEntrance>): VisitAnswer {
    if (entrances.isEmpty()) return VisitAnswer.NoData
    val recommended = entrances.firstOrNull { it.isRecommended }
    return recommended?.let { VisitAnswer.Accessible(it.entrance.name) } ?: missingOrNone(entrances)
}

private fun missingOrNone(entrances: List<OrderedEntrance>): VisitAnswer {
    val missing = entrances.count { it.entrance.marking == EntranceMarking.NO_DATA }
    return if (missing == 0) VisitAnswer.NoAccessible else VisitAnswer.Unknown(missing, entrances.size)
}

/** Wiersz o wejściach pod nagłówkiem karty budynku (US2.8, US2.1, US3.1). */
enum class EntranceSummary {
    /** „Wejście dostępne”: choć jedno wejście odwiedzających jest „dostępne”. */
    Accessible,

    /** „Brak wejścia dostępnego”: lista niepusta, a każde wejście jest „niedostępne”. */
    NoAccessible,

    /** „Brak danych o wejściach”: lista pusta albo żadne nie jest „dostępne”, a choć jedno ma „brak danych”. */
    NoData,
}

/** Wiersz o wejściach dla listy [entrances] ułożonej przez [orderedEntrances] (US2.8, US17.1). */
fun entranceSummary(entrances: List<OrderedEntrance>): EntranceSummary =
    if (entrances.isEmpty()) EntranceSummary.NoData else EntranceSummary.Accessible

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
