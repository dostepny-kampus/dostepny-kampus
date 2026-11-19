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
 * „‹” jest wyłączona na pierwszym kroku, a „›” na ostatnim.
 *
 * Wymagania: US18.1, US18.2
 *
 * @property found sala z piętrem
 * @property way droga od wejścia zalecanego; `null` do końca wczytywania
 * @property step numer wybranego kroku od zera; `null` przed pokazem drogi
 */
@Immutable
data class RoomCard(val found: FoundRoom, val way: RoomWay?, val step: Int? = null) {
    /** Liczba kroków drogi: trzy przez windę, dwa na piętrze wejścia, zero bez drogi. */
    val stepCount: Int
        get() =
            when (val steps = way as? RoomWay.Steps) {
                null -> 0
                else -> if (steps.lift == null) STEPS_WITHOUT_LIFT else STEPS_WITH_LIFT
            }

    /** Czy „‹” jest włączona: wybrany krok nie jest pierwszy. */
    val hasPrevious: Boolean
        get() = step != null && step > 0

    /** Czy „›” jest włączona: wybrany krok nie jest ostatni. */
    val hasNext: Boolean
        get() = step != null && step < stepCount - 1

    /** Karta z wybranym krokiem [index]; numer spoza kroków drogi niczego nie zmienia. */
    fun chosen(index: Int): RoomCard = if (index in 0 until stepCount) copy(step = index) else this

    /** Karta po „Pokaż po kolei”: wybrany krok 1. */
    fun started(): RoomCard = chosen(0)

    /** Karta po „›”: następny krok. */
    fun next(): RoomCard = step?.let { chosen(it + 1) } ?: this

    /** Karta po „‹”: poprzedni krok. */
    fun previous(): RoomCard = step?.let { chosen(it - 1) } ?: this
}
