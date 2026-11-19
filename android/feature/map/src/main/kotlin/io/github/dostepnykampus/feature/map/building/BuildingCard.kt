/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import io.github.dostepnykampus.core.data.barrier.AcceptedBarrier
import io.github.dostepnykampus.core.model.building.BuildingCatalog
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.core.model.submission.Severity
import java.text.Collator
import java.util.Locale

private val MARKING_ORDER = listOf(EntranceMarking.ACCESSIBLE, EntranceMarking.INACCESSIBLE, EntranceMarking.NO_DATA)

private val NAME_PART = Regex("""\d+|\D+""")

/**
 * Porządek nazw wejść na liście karty: alfabetycznie po polsku, a liczby w nazwie według wartości, więc
 * „Wejście 2” stoi przed „Wejście 10”.
 *
 * Wymagania: BR-25, US2.1
 */
internal object EntranceNameOrder : Comparator<String> {
    private val polish = Collator.getInstance(Locale.forLanguageTag("pl"))

    override fun compare(first: String, second: String): Int {
        val left = NAME_PART.findAll(first).map { it.value }.toList()
        val right = NAME_PART.findAll(second).map { it.value }.toList()
        val parts = left.zip(right).map { (a, b) -> comparePart(first = a, second = b) }
        return parts.firstOrNull { it != 0 } ?: left.size.compareTo(right.size)
    }

    private fun comparePart(first: String, second: String): Int =
        if (first[0].isDigit() && second[0].isDigit()) {
            val left = first.trimStart('0')
            val right = second.trimStart('0')
            if (left.length == right.length) left.compareTo(right) else left.length.compareTo(right.length)
        } else {
            polish.compare(first, second)
        }
}

/**
 * Wejście na liście karty budynku, w kolejności z [orderedEntrances].
 *
 * Wymagania: BR-25, US2.2
 *
 * @property entrance wejście z katalogu, już bez rodzajów spoza odwiedzających
 * @property isRecommended czy to jedyne wejście zalecane budynku; napis „Zalecane”
 * @property barrier przyjęta bariera przy wejściu o najwyższej uciążliwości; `null` bez barier
 */
data class OrderedEntrance(val entrance: Entrance, val isRecommended: Boolean, val barrier: AcceptedBarrier? = null) {
    /** Czy wejście „dostępne” ma przy sobie blokadę, przez którą nie może być zalecane. Wymagania: BR-25 */
    val isBlocked: Boolean
        get() = entrance.marking == EntranceMarking.ACCESSIBLE && barrier?.severity == Severity.BLOCKING
}

/**
 * Wejścia odwiedzających w kolejności karty budynku, każde z najcięższą barierą z [barriers] przy nim. Zalecane
 * jest jedyne: wejście „dostępne” bez blokady, a z kilku — bez przyjętych barier, a gdy każde je ma, to z najlżejszą
 * najcięższą barierą; przy równych główne, potem pierwsze z nazwą w danych ([Entrance.hasName]), potem pierwsze
 * z danych. Bez takiego wejścia nie ma zalecanego. Wejścia stoją w grupach oznaczeń, w grupie najpierw z nazwą
 * według [EntranceNameOrder], potem pozostałe w kolejności z danych.
 *
 * Wymagania: BR-25, US2.2
 */
fun orderedEntrances(entrances: List<Entrance>, barriers: List<AcceptedBarrier> = emptyList()): List<OrderedEntrance> {
    val worst =
        barriers
            .filter { it.entranceId != null }
            .groupBy { it.entranceId }
            .mapValues { (_, found) -> found.maxBy { it.severity.value } }
    val visitors = entrances.filter(Entrance::isForVisitors)
    val accessible =
        visitors.filter {
            it.marking == EntranceMarking.ACCESSIBLE && worst[it.id]?.severity != Severity.BLOCKING
        }
    val weights = worst.mapValues { (_, found) -> found.severity.value }
    val preference = compareBy<Entrance>({ weights[it.id] ?: 0 }, { !it.isMain }, { !it.hasName })
    val recommended = accessible.minWithOrNull(preference)
    val order =
        compareBy<Entrance>({ MARKING_ORDER.indexOf(it.marking) }, { !it.hasName })
            .thenBy(EntranceNameOrder) { if (it.hasName) it.name.orEmpty() else "" }
    val rest = visitors.filter { it !== recommended }.sortedWith(order)
    val head = recommended?.let { OrderedEntrance(entrance = it, isRecommended = true, barrier = worst[it.id]) }
    return listOfNotNull(head) + rest.map { OrderedEntrance(it, isRecommended = false, barrier = worst[it.id]) }
}

/**
 * Odpowiedź o wejściu do budynku.
 *
 * Wymagania: US3.1, BR-25
 */
sealed interface VisitAnswer {
    /** Brak wejść odwiedzających w danych nie potwierdza niedostępności budynku. Wymagania: US17.1 */
    data object NoData : VisitAnswer

    /**
     * Jest wejście zalecane.
     *
     * @property id identyfikator wejścia zalecanego
     * @property name nazwa wejścia; `null`, gdy danych nazwy nie ma
     * @property doorWidthCm szerokość drzwi w centymetrach; 0, gdy danych szerokości nie ma
     * @property place opis położenia wejścia „Wejście” bez nazwy w danych; pusty przy innym wejściu
     * @property barrier uciążliwość najcięższej bariery przy wejściu zalecanym; `null` bez barier
     * @property isNameFromData czy [name] pochodzi z danych, a nie od importu
     */
    data class Accessible(
        val id: String,
        val name: String?,
        val doorWidthCm: Int = 0,
        val place: String = "",
        val barrier: Severity? = null,
        val isNameFromData: Boolean = name != null,
    ) : VisitAnswer

    /** Żadne z [total] wejść odwiedzających nie jest „dostępne” i żadne nie ma „brak danych”. */
    data class NoAccessible(val total: Int) : VisitAnswer

    /** Każde z [count] wejść „dostępne” ma blokadę, więc budynek nie ma wejścia zalecanego. Wymagania: BR-25 */
    data class Blocked(val count: Int) : VisitAnswer

    /**
     * Nie ma wejścia „dostępne”, a o części nie ma danych.
     *
     * @property missing ile wejść odwiedzających ma „brak danych”
     * @property total ile wejść odwiedzających ma budynek
     */
    data class Unknown(val missing: Int, val total: Int) : VisitAnswer
}

/**
 * Odpowiedź z listy [entrances] ułożonej przez [orderedEntrances].
 *
 * Wejście zalecane jest w zdaniu „ma wejście dostępne”. Bez niego, gdy każde wejście „dostępne” ma blokadę,
 * odpowiedź podaje ich liczbę. Inaczej, gdy choć jedno wejście ma „brak danych”, zdanie podaje, o ilu z ilu wejść
 * brak danych — także wtedy, gdy danych brak o wszystkich. Inaczej budynek nie ma wejścia dostępnego. Pusta lista
 * oznacza brak danych o wejściach.
 *
 * Wymagania: BR-25, US3.1, US17.1
 */
fun visitAnswer(entrances: List<OrderedEntrance>): VisitAnswer {
    if (entrances.isEmpty()) return VisitAnswer.NoData
    val recommended = entrances.firstOrNull { it.isRecommended }
    val blocked = entrances.count { it.isBlocked }
    val answer =
        recommended?.let { listed ->
            val found = listed.entrance
            VisitAnswer.Accessible(
                id = found.id,
                name = found.name,
                doorWidthCm = found.doorWidthCm,
                place = if (found.isNameGenerated && !found.isMain) found.placeDescription else "",
                barrier = listed.barrier?.severity,
                isNameFromData = found.hasName,
            )
        } ?: VisitAnswer.Blocked(blocked).takeIf { blocked > 0 }
    return answer ?: missingOrNone(entrances)
}

private fun missingOrNone(entrances: List<OrderedEntrance>): VisitAnswer {
    val missing = entrances.count { it.entrance.marking == EntranceMarking.NO_DATA }
    return if (missing == 0) VisitAnswer.NoAccessible(entrances.size) else VisitAnswer.Unknown(missing, entrances.size)
}

/**
 * Wejście zalecane gotowej karty; `null` w trakcie wczytywania, bez budynku i bez wejścia zalecanego.
 *
 * Wymagania: BR-25
 */
internal fun BuildingCardUiState.recommendedEntrance(): OrderedEntrance? =
    (this as? BuildingCardUiState.Ready)?.run { entrances.firstOrNull { it.isRecommended } }

/** Wiersz o wejściach pod nagłówkiem karty budynku. Wymagania: US2.8, US2.1, US3.1 */
enum class EntranceSummary {
    /** „Wejście dostępne”: choć jedno wejście odwiedzających jest „dostępne”. */
    Accessible,

    /** „Brak wejścia dostępnego”: lista niepusta, a każde wejście jest „niedostępne”. */
    NoAccessible,

    /** „Brak danych o wejściach”: lista pusta albo żadne nie jest „dostępne”, a choć jedno ma „brak danych”. */
    NoData,
}

/** Wiersz o wejściach dla listy [entrances] ułożonej przez [orderedEntrances]. Wymagania: US2.8, US17.1 */
fun entranceSummary(entrances: List<OrderedEntrance>): EntranceSummary =
    when (visitAnswer(entrances)) {
        is VisitAnswer.Accessible -> EntranceSummary.Accessible
        is VisitAnswer.NoAccessible, is VisitAnswer.Blocked -> EntranceSummary.NoAccessible
        VisitAnswer.NoData, is VisitAnswer.Unknown -> EntranceSummary.NoData
    }

/**
 * Stan karty budynku [symbol] z katalogu. Brak symbolu w katalogu to „budynku nie ma w telefonie”.
 *
 * Wymagania: US2, US2.7
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
            isBasic = catalog.hasServerList && symbol !in catalog.detailed,
        )
    }
}
