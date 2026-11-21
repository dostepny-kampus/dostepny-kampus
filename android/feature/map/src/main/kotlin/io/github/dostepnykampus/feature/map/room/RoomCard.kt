/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.room

import androidx.compose.runtime.Immutable
import io.github.dostepnykampus.core.model.room.FoundRoom

private const val STEPS_WITHOUT_LIFT = 2
private const val STEPS_WITH_LIFT = 3

/**
 * Stan karty sali: sala, droga do niej i krok wybrany w pokazie drogi. Karta otwiera się bez wybranego kroku;
 * „Wstecz” jest wyłączony na pierwszym kroku, a „Dalej” na ostatnim.
 *
 * Wymagania: US18.1, US18.2
 *
 * @property found sala z piętrem
 * @property way droga od wejścia zalecanego; `null` do końca wczytywania
 * @property step numer wybranego kroku od zera; `null` przed pokazem drogi
 * @property isStepByStep karta pokazuje jeden krok naraz zamiast listy kroków
 */
@Immutable
data class RoomCard(val found: FoundRoom, val way: RoomWay?, val step: Int? = null, val isStepByStep: Boolean = false) {
    /** Liczba kroków drogi: trzy przez windę, dwa na piętrze wejścia, zero bez drogi. */
    val stepCount: Int
        get() {
            val steps = way as? RoomWay.Steps ?: return 0
            return if (steps.lift == null) STEPS_WITHOUT_LIFT else STEPS_WITH_LIFT
        }

    /** Czy „Wstecz” jest włączony: wybrany krok nie jest pierwszy. */
    val hasPrevious: Boolean
        get() = step != null && step > 0

    /** Czy „Dalej” jest włączony: wybrany krok nie jest ostatni. */
    val hasNext: Boolean
        get() = step != null && step < stepCount - 1
}

/** Karta z wybranym krokiem [index]; numer spoza kroków drogi niczego nie zmienia. */
internal fun RoomCard.chosen(index: Int): RoomCard = if (index in 0 until stepCount) copy(step = index) else this

/** Karta po „Pokaż po kolei”: jeden krok naraz od kroku 1; bez kroków drogi bez zmian. */
internal fun RoomCard.started(): RoomCard = if (stepCount > 0) copy(step = 0, isStepByStep = true) else this

/** Karta po „Wszystkie kroki”: lista kroków z tym samym krokiem wybranym. */
internal fun RoomCard.listed(): RoomCard = copy(isStepByStep = false)

/** Karta po „Dalej”: następny krok. */
internal fun RoomCard.next(): RoomCard = step?.let { chosen(it + 1) } ?: this

/** Karta po „Wstecz”: poprzedni krok. */
internal fun RoomCard.previous(): RoomCard = step?.let { chosen(it - 1) } ?: this

/**
 * Otwarta karta [card] z zamknięciem [onClose], wyborem kroku [onStep] i przełączeniem trybu po kolei
 * [onStepByStep] (`true` po „Pokaż po kolei”, `false` po „Wszystkie kroki”). Wymagania: US18.1, US18.2
 */
data class OpenRoom(
    val card: RoomCard,
    val onClose: () -> Unit,
    val onStep: (Int) -> Unit,
    val onStepByStep: (Boolean) -> Unit = {},
)
