/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.compose.runtime.MutableState
import io.github.dostepnykampus.feature.map.campus.CampusMapData
import io.github.dostepnykampus.feature.map.campus.outlineOf

/**
 * Wybór budynku dotykiem na mapie V1: budynki czekające na wybór w arkuszu „Który budynek?” są zapisywane w
 * [choices], a budynek wybrany dotykiem trafia do [onTap], bo od tego zależy ruch mapy.
 *
 * Wymagania: US2.8
 */
internal class MapPicking(
    private val choices: MutableState<List<String>>,
    private val onTap: (String) -> Unit = {},
    private val singleTarget: (List<String>) -> String? = List<String>::singleOrNull,
) {
    /** Symbole budynków czekających na wybór w arkuszu „Który budynek?”. */
    val choice: List<String> get() = choices.value

    /**
     * Dotknięcie konturów [symbols]: pojedynczy budynek albo jeden symbol wskazany przez [singleTarget] otwiera
     * [onBuilding], kilka czeka na wybór w arkuszu, a puste miejsce zgłasza [onEmpty].
     */
    fun touch(symbols: List<String>, onBuilding: (String) -> Unit, onEmpty: () -> Unit) {
        if (symbols.isEmpty()) {
            onEmpty()
        } else {
            val target = singleTarget(symbols)
            if (target != null) choose(target, onBuilding) else choices.value = symbols
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

/**
 * Symbol budynku do otwarcia bez arkusza „Który budynek?” przy dotknięciu konturów [symbols]: sam pojedynczy symbol
 * albo pierwszy symbol wspólnego konturu bez planów pięter (żaden z jego symboli nie leży w [detailed]). `null`
 * przy kilku różnych konturach albo konturze wspólnym z planami pięter.
 *
 * Wymagania: US2, US3.2, US2.8, NFR-17
 */
internal fun singleTouchedBuilding(
    symbols: List<String>,
    data: CampusMapData?,
    detailed: Set<String> = emptySet(),
): String? {
    if (symbols.size <= 1) return symbols.singleOrNull()
    val outline = data?.outlineOf(symbols.first()).orEmpty()
    return outline.firstOrNull().takeIf { symbols.all { it in outline } && outline.none { it in detailed } }
}
