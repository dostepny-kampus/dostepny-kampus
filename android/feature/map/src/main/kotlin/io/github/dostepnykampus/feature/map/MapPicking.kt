/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.compose.runtime.MutableState

/**
 * Wybór budynku dotykiem na mapie V1: budynki czekające na wybór w arkuszu „Który budynek?” są zapisywane w
 * [choices], a budynek wybrany dotykiem trafia do [onTap], bo od tego zależy ruch mapy ([PickOrigin]).
 *
 * Wymagania: US2.8
 */
internal class MapPicking(private val choices: MutableState<List<String>>, private val onTap: (String) -> Unit = {}) {
    /** Symbole budynków czekających na wybór w arkuszu „Który budynek?”. */
    val choice: List<String> get() = choices.value

    /**
     * Dotknięcie konturów [symbols]: jeden budynek otwiera [onBuilding], kilka czeka na wybór w arkuszu, a puste
     * miejsce zgłasza [onEmpty].
     */
    fun touch(symbols: List<String>, onBuilding: (String) -> Unit, onEmpty: () -> Unit) {
        when (symbols.size) {
            0 -> onEmpty()
            1 -> choose(symbols.single(), onBuilding)
            else -> choices.value = symbols
        }
    }

    /** Budynek [symbol] wybrany w arkuszu albo jedynym dotknięciem: zamyka wybór i otwiera [onBuilding]. */
    fun choose(symbol: String, onBuilding: (String) -> Unit) {
        onTap(symbol)
        dismiss()
        onBuilding(symbol)
    }

    /** Zamyka arkusz „Który budynek?” bez wyboru. */
    fun dismiss() {
        choices.value = emptyList()
    }
}
